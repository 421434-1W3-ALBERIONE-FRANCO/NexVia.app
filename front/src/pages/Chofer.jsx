import { useState, useEffect, useRef } from 'react';
import { nexvia } from '@/api/client';
import { useAuth } from '@/lib/AuthContext';
import { toast } from '@/components/ui/use-toast';
import MapaCamiones from '@/components/mapa/MapaCamiones';
import PanelChofer from '@/components/chofer/PanelChofer';
import GPSNav from '@/components/chofer/GPSNav';
import { formatPrecio, haversineKm } from '@/lib/distance';

export default function Chofer() {
  const { user } = useAuth();
  const [miCamion, setMiCamion] = useState(null);
  const [solicitudes, setSolicitudes] = useState([]);
  const [viajeActivo, setViajeActivo] = useState(null);
  const [config, setConfig] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [aceptando, setAceptando] = useState(null);
  const viajeActivoRef = useRef(null);
  const gpsWatchRef = useRef(null);
  const llegadaProcesandoRef = useRef(false);

  useEffect(() => {
    viajeActivoRef.current = viajeActivo;
  }, [viajeActivo]);

  // Track real GPS position: update truck location and advance trip status on arrival
  useEffect(() => {
    if (!miCamion) return;
    if (!navigator.geolocation) return;

    const UMBRAL_LLEGADA_KM = 0.3;

    const updatePosition = async (pos) => {
      const { latitude, longitude } = pos.coords;
      setMiCamion((prev) => (prev ? { ...prev, lat: latitude, lng: longitude } : prev));
      nexvia.entities.Camion.update(miCamion.id, { lat: latitude, lng: longitude }).catch(() => {});

      const viaje = viajeActivoRef.current;
      if (!viaje || llegadaProcesandoRef.current) return;

      if (viaje.estado === 'aceptado') {
        const dist = haversineKm(latitude, longitude, viaje.origen_lat, viaje.origen_lng);
        if (dist <= UMBRAL_LLEGADA_KM) {
          llegadaProcesandoRef.current = true;
          await nexvia.entities.Viaje.update(viaje.id, { estado: 'en_camino' });
          llegadaProcesandoRef.current = false;
        }
      } else if (viaje.estado === 'en_camino') {
        const dist = haversineKm(latitude, longitude, viaje.destino_lat, viaje.destino_lng);
        if (dist <= UMBRAL_LLEGADA_KM) {
          llegadaProcesandoRef.current = true;
          await nexvia.entities.Viaje.update(viaje.id, { estado: 'completado' });
          await nexvia.entities.Camion.update(miCamion.id, { estado: 'disponible' });
          llegadaProcesandoRef.current = false;
          setViajeActivo(null);
          cargarDatos();
        }
      }
    };

    gpsWatchRef.current = navigator.geolocation.watchPosition(
      updatePosition,
      () => {},
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 15000 }
    );

    return () => {
      if (gpsWatchRef.current !== null) {
        navigator.geolocation.clearWatch(gpsWatchRef.current);
        gpsWatchRef.current = null;
      }
    };
  }, [miCamion?.id]);

  useEffect(() => {
    cargarDatos();
  }, []);

  // Subscribe to all viaje events
  useEffect(() => {
    const unsub = nexvia.entities.Viaje.subscribe((event) => {
      if (event.type === 'create' && event.data.estado === 'solicitado') {
        setSolicitudes((prev) => {
          if (prev.find((s) => s.id === event.id)) return prev;
          return [...prev, event.data];
        });
        toast({
          title: "Nueva solicitud de viaje",
          description: `${event.data.usuario_nombre || 'Un productor'} solicita un camión · ${formatPrecio(event.data.precio)}`,
        });
      } else if (event.type === 'update') {
        if (event.data.estado !== 'solicitado') {
          setSolicitudes((prev) => prev.filter((s) => s.id !== event.id));
        }
        if (viajeActivoRef.current && event.id === viajeActivoRef.current.id) {
          setViajeActivo(event.data);
        }
      } else if (event.type === 'delete') {
        setSolicitudes((prev) => prev.filter((s) => s.id !== event.id));
      }
    });
    return unsub;
  }, []);

  // Subscribe to my camion updates
  useEffect(() => {
    if (!miCamion) return;
    const camionId = miCamion.id;
    const unsub = nexvia.entities.Camion.subscribe((event) => {
      if (event.id === camionId && event.type === 'update') {
        setMiCamion(event.data);
      }
    });
    return unsub;
  }, [miCamion?.id]);

  // Keep screen awake during active trip + re-sync position when returning to foreground
  useEffect(() => {
    if (!miCamion) return;
    let wakeLock = null;

    const onVisible = () => {
      if (document.visibilityState !== 'visible') return;
      // Re-acquire wake lock if there's an active trip
      if (navigator.wakeLock && viajeActivoRef.current) {
        navigator.wakeLock.request('screen').then((wl) => { wakeLock = wl; }).catch(() => {});
      }
      // Immediately re-sync position when returning to foreground
      if (viajeActivoRef.current) {
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            const { latitude, longitude } = pos.coords;
            setMiCamion((prev) => (prev ? { ...prev, lat: latitude, lng: longitude } : prev));
            nexvia.entities.Camion.update(miCamion.id, { lat: latitude, lng: longitude }).catch(() => {});
          },
          () => {},
          { enableHighAccuracy: true, maximumAge: 0, timeout: 10000 }
        );
      }
    };

    if (navigator.wakeLock && viajeActivoRef.current) {
      navigator.wakeLock.request('screen').then((wl) => { wakeLock = wl; }).catch(() => {});
    }
    document.addEventListener('visibilitychange', onVisible);

    return () => {
      document.removeEventListener('visibilitychange', onVisible);
      if (wakeLock) wakeLock.release().catch(() => {});
    };
  }, [miCamion?.id]);

  const cargarDatos = async () => {
    try {
      const [camionesData, configsData, solicitudesData] = await Promise.all([
        nexvia.entities.Camion.filter({ user_id: user.id }),
        nexvia.entities.Configuracion.list(),
        nexvia.entities.Viaje.filter({ estado: 'solicitado' }),
      ]);
      const camion = camionesData[0];
      setMiCamion(camion);
      setConfig(configsData[0] || { tarifa_por_km: 500, centro_lat: -32.4341, centro_lng: -63.2433 });
      setSolicitudes(solicitudesData);

      if (camion) {
        const misViajes = await nexvia.entities.Viaje.filter({ camion_id: camion.id }, '-created_date', 10);
        const activo = misViajes.find((v) => v.estado === 'aceptado' || v.estado === 'en_camino');
        if (activo) setViajeActivo(activo);
      }
    } finally {
      setCargando(false);
    }
  };

  const handleAceptar = async (solicitud) => {
    setAceptando(solicitud.id);
    try {
      await nexvia.entities.Viaje.update(solicitud.id, {
        camion_id: miCamion.id,
        chofer_id: user.id,
        chofer_nombre: user?.full_name || miCamion.chofer_nombre,
        estado: 'aceptado',
      });
      await nexvia.entities.Camion.update(miCamion.id, { estado: 'ocupado' });
      setSolicitudes((prev) => prev.filter((s) => s.id !== solicitud.id));
      const viajeActualizado = { ...solicitud, camion_id: miCamion.id, chofer_id: user.id, chofer_nombre: user?.full_name || miCamion.chofer_nombre, estado: 'aceptado' };
      setViajeActivo(viajeActualizado);
    } finally {
      setAceptando(null);
    }
  };

  const handleRechazar = (solicitud) => {
    setSolicitudes((prev) => prev.filter((s) => s.id !== solicitud.id));
  };

  if (cargando || !config) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-4rem)]">
        <div className="w-8 h-8 border-4 border-stone-200 border-t-emerald-600 rounded-full animate-spin" />
      </div>
    );
  }

  const camionParaMapa = miCamion ? [miCamion] : [];
  const origenViaje = viajeActivo ? { lat: viajeActivo.origen_lat, lng: viajeActivo.origen_lng } : null;
  const destinoViaje = viajeActivo ? { lat: viajeActivo.destino_lat, lng: viajeActivo.destino_lng } : null;

  if (viajeActivo && (viajeActivo.estado === 'aceptado' || viajeActivo.estado === 'en_camino') && miCamion) {
    return (
      <div className="relative h-[calc(100vh-4rem)]">
        <MapaCamiones
          centro={config}
          camiones={camionParaMapa}
          origen={origenViaje}
          destino={destinoViaje}
          onMapClick={() => {}}
          recenterOn={{ lat: miCamion.lat, lng: miCamion.lng }}
        />
        <GPSNav viajeActivo={viajeActivo} miCamion={miCamion} config={config} />
      </div>
    );
  }

  return (
    <div className="flex flex-col lg:flex-row h-[calc(100vh-4rem)]">
      <div className="h-[40vh] lg:h-full lg:flex-1 relative">
        <MapaCamiones
          centro={config}
          camiones={camionParaMapa}
          origen={origenViaje}
          destino={destinoViaje}
          onMapClick={() => {}}
        />
      </div>
      <div className="flex-1 lg:w-96 lg:flex-none overflow-y-auto bg-white border-t lg:border-t-0 lg:border-l border-stone-200">
        <PanelChofer
          miCamion={miCamion}
          solicitudes={solicitudes}
          viajeActivo={viajeActivo}
          onAceptar={handleAceptar}
          onRechazar={handleRechazar}
          aceptando={aceptando}
          config={config}
        />
      </div>
    </div>
  );
}