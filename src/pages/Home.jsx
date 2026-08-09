import { useState, useEffect, useMemo } from 'react';
import { base44 } from '@/api/base44Client';
import { useAuth } from '@/lib/AuthContext';
import MapaCamiones from '@/components/mapa/MapaCamiones';
import PanelSolicitud from '@/components/mapa/PanelSolicitud';
import { haversineKm } from '@/lib/distance';

export default function Home() {
  const { user } = useAuth();
  const [camiones, setCamiones] = useState([]);
  const [config, setConfig] = useState(null);
  const [modo, setModo] = useState('origen');
  const [origen, setOrigen] = useState(null);
  const [destino, setDestino] = useState(null);
  const [solicitando, setSolicitando] = useState(false);
  const [viajeActual, setViajeActual] = useState(null);
  const [camionAsignado, setCamionAsignado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [tipoTarifa, setTipoTarifa] = useState('por_km');
  const [toneladas, setToneladas] = useState('');
  const [precioOfrecido, setPrecioOfrecido] = useState(null);
  const [carga, setCarga] = useState('');
  const [userLocation, setUserLocation] = useState(null);

  useEffect(() => {
    cargarDatos();
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => setUserLocation({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        () => {},
        { enableHighAccuracy: true, timeout: 10000 }
      );
    }
  }, []);

  // Subscribe to available truck position updates in real-time
  useEffect(() => {
    const unsub = base44.entities.Camion.subscribe((event) => {
      if (event.type === 'update' || event.type === 'create') {
        setCamiones((prev) => {
          const idx = prev.findIndex((c) => c.id === event.id);
          if (event.data.estado !== 'disponible') {
            return idx >= 0 ? prev.filter((c) => c.id !== event.id) : prev;
          }
          if (idx >= 0) {
            const updated = [...prev];
            updated[idx] = event.data;
            return updated;
          }
          if (event.type === 'create') {
            return [...prev, event.data];
          }
          return prev;
        });
      } else if (event.type === 'delete') {
        setCamiones((prev) => prev.filter((c) => c.id !== event.id));
      }
    });
    return unsub;
  }, []);

  // Subscribe to viaje updates
  useEffect(() => {
    if (!viajeActual) return;
    const viajeId = viajeActual.id;
    const unsub = base44.entities.Viaje.subscribe((event) => {
      if (event.id === viajeId && (event.type === 'update' || event.type === 'create')) {
        setViajeActual(event.data);
      }
    });
    return unsub;
  }, [viajeActual?.id]);

  // Subscribe to assigned camion position updates
  useEffect(() => {
    const camionId = viajeActual?.camion_id;
    if (!camionId) {
      setCamionAsignado(null);
      return;
    }
    // Load the camion first
    base44.entities.Camion.get(camionId).then(setCamionAsignado).catch(() => {});
    const unsub = base44.entities.Camion.subscribe((event) => {
      if (event.id === camionId && event.type === 'update') {
        setCamionAsignado(event.data);
      }
    });
    return unsub;
  }, [viajeActual?.camion_id]);

  const cargarDatos = async () => {
    try {
      const [camionesData, configsData, misViajes] = await Promise.all([
        base44.entities.Camion.filter({ estado: 'disponible' }),
        base44.entities.Configuracion.list(),
        user ? base44.entities.Viaje.filter({ usuario_id: user.id }, '-created_date', 5) : [],
      ]);
      setCamiones(camionesData);
      const cfg = configsData[0] || { tarifa_por_km: 500, centro_lat: -32.4341, centro_lng: -63.2433, zona_nombre: 'Zona Agrícola' };
      setConfig(cfg);
      const activo = misViajes.find?.((v) => ['solicitado', 'aceptado', 'en_camino'].includes(v.estado));
      if (activo) {
        setViajeActual(activo);
        if (activo.origen_lat && activo.destino_lat) {
          setOrigen({ lat: activo.origen_lat, lng: activo.origen_lng });
          setDestino({ lat: activo.destino_lat, lng: activo.destino_lng });
        }
      }
    } finally {
      setCargando(false);
    }
  };

  const distancia = origen && destino ? haversineKm(origen.lat, origen.lng, destino.lat, destino.lng) : null;
  useEffect(() => {
    if (!config) return;
    const suggestedRate = tipoTarifa === 'por_tonelada' ? config.tarifa_por_tonelada : config.tarifa_por_km;
    if (suggestedRate != null) setPrecioOfrecido(suggestedRate);
  }, [config, tipoTarifa]);

  const precio = useMemo(() => {
    if (!distancia || !config) return null;
    const rate = Number(precioOfrecido) || 0;
    if (rate <= 0) return null;
    if (tipoTarifa === 'por_tonelada') {
      const tons = Number(toneladas) || 0;
      if (tons <= 0) return null;
      return Math.round(tons * rate);
    }
    return Math.round(distancia * rate);
  }, [distancia, config, tipoTarifa, toneladas, precioOfrecido]);

  const handleMapClick = (latlng) => {
    if (viajeActual) return;
    const point = { lat: latlng.lat, lng: latlng.lng };
    if (modo === 'origen') setOrigen(point);
    else setDestino(point);
  };

  const handleUbicacion = (target, point) => {
    if (target === 'origen') setOrigen(point);
    else setDestino(point);
  };

  // BuscarLugar passes point with optional nombre; same handler works

  const handleSolicitar = async () => {
    if (!origen || !destino || !config) return;
    setSolicitando(true);
    try {
      const viaje = await base44.entities.Viaje.create({
        origen_lat: origen.lat,
        origen_lng: origen.lng,
        destino_lat: destino.lat,
        destino_lng: destino.lng,
        distancia_km: distancia,
        toneladas: tipoTarifa === 'por_tonelada' ? Number(toneladas) : 0,
        tipo_tarifa: tipoTarifa,
        precio: precio,
        carga: carga,
        estado: 'solicitado',
        usuario_id: user?.id || '',
        usuario_nombre: user?.full_name || user?.email || '',
      });
      setViajeActual(viaje);
    } finally {
      setSolicitando(false);
    }
  };

  const handleCancelar = async () => {
    if (!viajeActual) return;
    await base44.entities.Viaje.update(viajeActual.id, { estado: 'cancelado' });
    limpiarViaje();
  };

  const limpiarViaje = () => {
    setViajeActual(null);
    setCamionAsignado(null);
    setOrigen(null);
    setDestino(null);
    setModo('origen');
    setTipoTarifa('por_km');
    setToneladas('');
    setPrecioOfrecido(null);
    setCarga('');
    cargarDatos();
  };

  const camionesParaMostrar = useMemo(() => {
    if (camionAsignado) {
      return [...camiones.filter((c) => c.id !== camionAsignado.id), camionAsignado];
    }
    return camiones;
  }, [camiones, camionAsignado]);

  if (cargando || !config) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-4rem)]">
        <div className="w-8 h-8 border-4 border-stone-200 border-t-emerald-600 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="flex flex-col lg:flex-row h-[calc(100vh-4rem)]">
      <div className="h-[50vh] lg:h-full lg:flex-1 relative">
        <MapaCamiones
          centro={config}
          camiones={camionesParaMostrar}
          origen={origen}
          destino={destino}
          onMapClick={handleMapClick}
          userLocation={userLocation}
          recenterOn={camionAsignado && viajeActual && ['aceptado', 'en_camino'].includes(viajeActual.estado) ? { lat: camionAsignado.lat, lng: camionAsignado.lng } : null}
        />
        {!viajeActual && (
          <div className="absolute top-4 left-1/2 -translate-x-1/2 z-[1000] bg-white/95 backdrop-blur px-4 py-2 rounded-full shadow-lg border border-stone-200 text-sm font-medium text-stone-700 whitespace-nowrap pointer-events-none">
            {modo === 'origen'
              ? '📍 Tocá el mapa para elegir el origen'
              : '🎯 Tocá el mapa para elegir el destino'}
          </div>
        )}
      </div>
      <div className="flex-1 lg:w-96 lg:flex-none overflow-y-auto bg-white border-t lg:border-t-0 lg:border-l border-stone-200">
        <PanelSolicitud
          modo={modo}
          setModo={setModo}
          origen={origen}
          destino={destino}
          distancia={distancia}
          precio={precio}
          config={config}
          camiones={camiones}
          onSolicitar={handleSolicitar}
          solicitando={solicitando}
          viajeActual={viajeActual}
          camionAsignado={camionAsignado}
          onNuevoViaje={limpiarViaje}
          onCancelar={handleCancelar}
          onUbicacion={handleUbicacion}
          tipoTarifa={tipoTarifa}
          setTipoTarifa={setTipoTarifa}
          toneladas={toneladas}
          setToneladas={setToneladas}
          precioOfrecido={precioOfrecido}
          setPrecioOfrecido={setPrecioOfrecido}
          carga={carga}
          setCarga={setCarga}
        />
      </div>
    </div>
  );
}