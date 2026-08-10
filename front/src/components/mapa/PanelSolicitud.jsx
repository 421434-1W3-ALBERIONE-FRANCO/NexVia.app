import { useState, useMemo } from 'react';
import { MapPin, Navigation, Truck, Check, Loader2, RotateCcw, X, Clock, Navigation2, Crosshair, Pencil, Check as CheckIcon } from 'lucide-react';
import BuscarLugar from '@/components/mapa/BuscarLugar';
import DatosCamionero from '@/components/mapa/DatosCamionero';
import { formatPrecio, formatKm, haversineKm } from '@/lib/distance';

const VELOCIDAD_PROMEDIO = 60;

export default function PanelSolicitud({
  modo, setModo, origen, destino, distancia, precio, config, camiones,
  onSolicitar, solicitando, viajeActual, camionAsignado, onNuevoViaje, onCancelar, onUbicacion,
  tipoTarifa, setTipoTarifa, toneladas, setToneladas, precioOfrecido, setPrecioOfrecido,
  carga, setCarga,
}) {
  const [ubicando, setUbicando] = useState(null);
  const [editando, setEditando] = useState(null);
  const [nombreTemp, setNombreTemp] = useState('');

  const etaInfo = useMemo(() => {
    if (!viajeActual || !camionAsignado) return null;
    if (viajeActual.estado === 'solicitado' || viajeActual.estado === 'completado') return null;
    const target = viajeActual.estado === 'aceptado'
      ? { lat: viajeActual.origen_lat, lng: viajeActual.origen_lng }
      : { lat: viajeActual.destino_lat, lng: viajeActual.destino_lng };
    const distRestante = haversineKm(camionAsignado.lat, camionAsignado.lng, target.lat, target.lng);
    const minutos = Math.max(1, Math.ceil((distRestante / VELOCIDAD_PROMEDIO) * 60));
    return { distRestante, minutos };
  }, [viajeActual, camionAsignado]);

  const iniciarEdicion = (target) => {
    const punto = target === 'origen' ? origen : destino;
    setNombreTemp(punto?.nombre || '');
    setEditando(target);
  };

  const confirmarNombre = (target) => {
    const punto = target === 'origen' ? origen : destino;
    if (punto && nombreTemp.trim()) {
      onUbicacion(target, { ...punto, nombre: nombreTemp.trim() });
    }
    setEditando(null);
  };

  const usarUbicacion = (target) => {
    if (!navigator.geolocation) return;
    setUbicando(target);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const point = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        onUbicacion(target, point);
        setUbicando(null);
      },
      () => setUbicando(null),
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };
  if (viajeActual) {
    const estados = {
      solicitado: { label: 'Buscando camión disponible...', icon: Clock, color: 'text-amber-600', bg: 'bg-amber-50', spin: true },
      aceptado: { label: 'Camión yendo a buscarte', icon: Truck, color: 'text-blue-600', bg: 'bg-blue-50', spin: false },
      en_camino: { label: 'En camino al destino', icon: Navigation2, color: 'text-blue-600', bg: 'bg-blue-50', spin: false },
      completado: { label: '¡Viaje completado!', icon: Check, color: 'text-emerald-600', bg: 'bg-emerald-50', spin: false },
    };
    const estado = estados[viajeActual.estado] || estados.solicitado;
    const completado = viajeActual.estado === 'completado';
    const esperando = viajeActual.estado === 'solicitado';
    const EstadoIcon = estado.icon;

    return (
      <div className="p-6 space-y-4">
        <div className="text-center pt-2">
          <div className={`w-16 h-16 mx-auto rounded-full ${estado.bg} flex items-center justify-center`}>
            <EstadoIcon className={`w-8 h-8 ${estado.color} ${estado.spin ? 'animate-pulse' : ''}`} />
          </div>
          <h2 className="text-xl font-bold text-stone-900 mt-3">{estado.label}</h2>
          {!esperando && viajeActual.chofer_nombre && (
            <p className="text-sm text-stone-500 mt-1">Chofer: {viajeActual.chofer_nombre}</p>
          )}
          {esperando && (
            <p className="text-sm text-stone-500 mt-1">Esperando que un chofer acepte tu viaje</p>
          )}
          {etaInfo && (
            <div className="inline-flex items-center gap-2 mt-2 px-3 py-1.5 rounded-full bg-stone-100 text-stone-700 text-sm">
              <Clock className="w-4 h-4" />
              <span>Llega en <strong>{etaInfo.minutos} min</strong></span>
              <span className="text-stone-400">·</span>
              <span>{formatKm(etaInfo.distRestante)}</span>
              </div>
              )}
              </div>

              {!esperando && (
              <div className="flex items-center gap-1">
              {['Aceptado', 'En camino', 'Entregado'].map((label, i) => {
              const stages = ['aceptado', 'en_camino', 'completado'];
              const currentIdx = stages.indexOf(viajeActual.estado);
              const done = i <= currentIdx;
              return (
                <div key={label} className="flex items-center flex-1 last:flex-none">
                  <div className="flex flex-col items-center gap-1 shrink-0">
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold ${done ? 'bg-emerald-500 text-white' : 'bg-stone-200 text-stone-400'}`}>
                      {done ? <Check className="w-3 h-3" /> : i + 1}
                    </div>
                    <span className={`text-[10px] whitespace-nowrap ${done ? 'text-stone-700 font-medium' : 'text-stone-400'}`}>{label}</span>
                  </div>
                  {i < 2 && <div className={`flex-1 h-0.5 mx-1 mb-4 ${i < currentIdx ? 'bg-emerald-500' : 'bg-stone-200'}`} />}
                </div>
              );
              })}
              </div>
              )}

              <div className="bg-stone-50 rounded-2xl p-4 space-y-3">
          <div className="flex justify-between items-center">
            <span className="text-sm text-stone-500">Carga</span>
            <span className="text-sm font-semibold">{viajeActual.carga || '-'}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-sm text-stone-500">Distancia del viaje</span>
            <span className="text-sm font-semibold">{formatKm(viajeActual.distancia_km)}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-sm text-stone-500">Tarifa por km</span>
            <span className="text-sm font-semibold">{formatPrecio(config.tarifa_por_km)}</span>
          </div>
          <div className="border-t border-stone-200 pt-3 flex justify-between items-center">
            <span className="font-bold text-stone-900">Total a pagar</span>
            <span className="font-bold text-emerald-600 text-lg">{formatPrecio(viajeActual.precio)}</span>
          </div>
        </div>

        {camionAsignado && <DatosCamionero camion={camionAsignado} />}

        {esperando ? (
          <button
            onClick={onCancelar}
            className="w-full py-3 rounded-xl bg-red-50 text-red-600 font-semibold hover:bg-red-100 transition flex items-center justify-center gap-2"
          >
            <X className="w-4 h-4" /> Cancelar viaje
          </button>
        ) : completado ? (
          <button
            onClick={onNuevoViaje}
            className="w-full py-3 rounded-xl bg-stone-900 text-white font-semibold hover:bg-stone-800 transition flex items-center justify-center gap-2"
          >
            <RotateCcw className="w-4 h-4" /> Pedir otro viaje
          </button>
        ) : null}
      </div>
    );
  }

  const ambosPuntos = origen && destino;
  const puedeCobrar = ambosPuntos && carga && carga.trim() && (tipoTarifa === 'por_km' || (tipoTarifa === 'por_tonelada' && Number(toneladas) > 0)) && Number(precioOfrecido) > 0;

  return (
    <div className="p-6 space-y-5">
      <div>
        <h2 className="text-lg font-bold text-stone-900">Solicitar camión</h2>
        <p className="text-sm text-stone-500">Elegí origen y destino tocando el mapa</p>
      </div>

      <div className="grid grid-cols-2 gap-2 p-1 bg-stone-100 rounded-xl">
        <button
          onClick={() => setModo('origen')}
          className={`py-2.5 rounded-lg text-sm font-semibold transition flex items-center justify-center gap-1.5 ${
            modo === 'origen' ? 'bg-emerald-600 text-white shadow-sm' : 'text-stone-600'
          }`}
        >
          <MapPin className="w-4 h-4" /> Origen
        </button>
        <button
          onClick={() => setModo('destino')}
          className={`py-2.5 rounded-lg text-sm font-semibold transition flex items-center justify-center gap-1.5 ${
            modo === 'destino' ? 'bg-red-600 text-white shadow-sm' : 'text-stone-600'
          }`}
        >
          <Navigation className="w-4 h-4" /> Destino
        </button>
      </div>

      <div className="space-y-2">
        <div className="space-y-1.5">
          <div className={`flex items-center gap-3 p-3 rounded-xl border ${origen ? 'border-emerald-200 bg-emerald-50' : 'border-stone-200 bg-stone-50'}`}>
            <div className={`w-2.5 h-2.5 rounded-full ${origen ? 'bg-emerald-500' : 'bg-stone-300'}`} />
            <div className="flex-1 min-w-0">
              <p className="text-xs text-stone-500">Origen</p>
              {editando === 'origen' ? (
                <div className="flex items-center gap-1">
                  <input
                    type="text"
                    value={nombreTemp}
                    onChange={(e) => setNombreTemp(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && confirmarNombre('origen')}
                    placeholder="Nombre del lugar"
                    autoFocus
                    className="flex-1 min-w-0 text-sm font-medium text-stone-900 bg-white px-2 py-0.5 rounded border border-emerald-300 outline-none"
                  />
                  <button
                    onClick={() => confirmarNombre('origen')}
                    className="p-1 rounded text-emerald-600 hover:bg-emerald-100 transition shrink-0"
                  >
                    <CheckIcon className="w-4 h-4" />
                  </button>
                </div>
              ) : (
                <p className="text-sm font-medium text-stone-900 truncate">
                  {origen ? (origen.nombre || `${origen.lat.toFixed(4)}, ${origen.lng.toFixed(4)}`) : 'Sin seleccionar'}
                </p>
              )}
            </div>
            {origen && editando !== 'origen' && (
              <button
                onClick={() => iniciarEdicion('origen')}
                className="p-2 rounded-lg text-stone-400 hover:text-emerald-600 hover:bg-emerald-100 transition shrink-0"
                title="Poner nombre al lugar"
              >
                <Pencil className="w-4 h-4" />
              </button>
            )}
            <button
              onClick={() => usarUbicacion('origen')}
              disabled={ubicando !== null}
              className="p-2 rounded-lg text-emerald-600 hover:bg-emerald-100 disabled:opacity-50 transition shrink-0"
              title="Usar mi ubicación actual"
            >
              {ubicando === 'origen' ? <Loader2 className="w-4 h-4 animate-spin" /> : <Crosshair className="w-4 h-4" />}
            </button>
          </div>
          <BuscarLugar label="origen" puntoActual={origen} onSelect={(point) => onUbicacion('origen', point)} />
        </div>
        <div className="space-y-1.5">
          <div className={`flex items-center gap-3 p-3 rounded-xl border ${destino ? 'border-red-200 bg-red-50' : 'border-stone-200 bg-stone-50'}`}>
            <div className={`w-2.5 h-2.5 rounded-full ${destino ? 'bg-red-500' : 'bg-stone-300'}`} />
            <div className="flex-1 min-w-0">
              <p className="text-xs text-stone-500">Destino</p>
              {editando === 'destino' ? (
                <div className="flex items-center gap-1">
                  <input
                    type="text"
                    value={nombreTemp}
                    onChange={(e) => setNombreTemp(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && confirmarNombre('destino')}
                    placeholder="Nombre del lugar"
                    autoFocus
                    className="flex-1 min-w-0 text-sm font-medium text-stone-900 bg-white px-2 py-0.5 rounded border border-red-300 outline-none"
                  />
                  <button
                    onClick={() => confirmarNombre('destino')}
                    className="p-1 rounded text-red-600 hover:bg-red-100 transition shrink-0"
                  >
                    <CheckIcon className="w-4 h-4" />
                  </button>
                </div>
              ) : (
                <p className="text-sm font-medium text-stone-900 truncate">
                  {destino ? (destino.nombre || `${destino.lat.toFixed(4)}, ${destino.lng.toFixed(4)}`) : 'Sin seleccionar'}
                </p>
              )}
            </div>
            {destino && editando !== 'destino' && (
              <button
                onClick={() => iniciarEdicion('destino')}
                className="p-2 rounded-lg text-stone-400 hover:text-red-600 hover:bg-red-100 transition shrink-0"
                title="Poner nombre al lugar"
              >
                <Pencil className="w-4 h-4" />
              </button>
            )}
            <button
              onClick={() => usarUbicacion('destino')}
              disabled={ubicando !== null}
              className="p-2 rounded-lg text-red-600 hover:bg-red-100 disabled:opacity-50 transition shrink-0"
              title="Usar mi ubicación actual"
            >
              {ubicando === 'destino' ? <Loader2 className="w-4 h-4 animate-spin" /> : <Crosshair className="w-4 h-4" />}
            </button>
          </div>
          <BuscarLugar label="destino" puntoActual={destino} onSelect={(point) => onUbicacion('destino', point)} />
        </div>
      </div>

      {ambosPuntos && (
        <>
          <div className="space-y-2">
            <p className="text-sm font-medium text-stone-700">¿Qué tiene que transportar?</p>
            <div className="flex flex-wrap gap-1.5">
              {['Soja', 'Maíz', 'Trigo', 'Cebada', 'Girasol', 'Sorgo'].map((cereal) => (
                <button
                  key={cereal}
                  onClick={() => setCarga(cereal)}
                  className={`px-2.5 py-1 rounded-lg text-xs font-medium transition ${
                    carga === cereal ? 'bg-emerald-600 text-white' : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                  }`}
                >
                  {cereal}
                </button>
              ))}
            </div>
            <input
              type="text"
              value={carga || ''}
              onChange={(e) => setCarga(e.target.value)}
              placeholder="Ej. Soja, fertilizante, maquinaria..."
              className="w-full px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm"
            />
          </div>

          <div className="space-y-2">
            <p className="text-sm font-medium text-stone-700">Tipo de tarifa</p>
            <div className="grid grid-cols-2 gap-2 p-1 bg-stone-100 rounded-xl">
              <button
                onClick={() => setTipoTarifa('por_km')}
                className={`py-2.5 rounded-lg text-sm font-semibold transition ${
                  tipoTarifa === 'por_km' ? 'bg-emerald-600 text-white shadow-sm' : 'text-stone-600'
                }`}
              >
                Por kilómetro
              </button>
              <button
                onClick={() => setTipoTarifa('por_tonelada')}
                className={`py-2.5 rounded-lg text-sm font-semibold transition ${
                  tipoTarifa === 'por_tonelada' ? 'bg-emerald-600 text-white shadow-sm' : 'text-stone-600'
                }`}
              >
                Por tonelada
              </button>
            </div>
          </div>

          {tipoTarifa === 'por_tonelada' && (
            <div>
              <label className="text-sm font-medium text-stone-700">Toneladas a transportar</label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={toneladas}
                onChange={(e) => setToneladas(e.target.value)}
                placeholder="Ej: 12.5"
                className="mt-1 w-full px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm"
              />
            </div>
          )}

          <div className="bg-emerald-50 rounded-2xl p-4 space-y-2 border border-emerald-100">
            {tipoTarifa === 'por_km' ? (
              <div className="flex justify-between items-center">
                <span className="text-sm text-stone-600">Distancia estimada</span>
                <span className="font-semibold text-stone-900">{formatKm(distancia)}</span>
              </div>
            ) : (
              <div className="flex justify-between items-center">
                <span className="text-sm text-stone-600">Toneladas</span>
                <span className="font-semibold text-stone-900">{Number(toneladas) || 0} tn</span>
              </div>
            )}
            <div className="border-t border-emerald-200 pt-3 space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-xs text-stone-400">Tarifa sugerida</span>
                <span className="text-xs font-medium text-stone-400">
                  {formatPrecio(tipoTarifa === 'por_tonelada' ? config.tarifa_por_tonelada : config.tarifa_por_km)} / {tipoTarifa === 'por_tonelada' ? 'tn' : 'km'}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <label className="font-bold text-stone-900">Tu tarifa por {tipoTarifa === 'por_tonelada' ? 'tonelada' : 'km'}</label>
                <div className="flex items-center gap-1">
                  <span className="text-sm text-stone-400">$</span>
                  <input
                    type="number"
                    value={precioOfrecido ?? ''}
                    onChange={(e) => setPrecioOfrecido(e.target.value ? Number(e.target.value) : null)}
                    placeholder={(tipoTarifa === 'por_tonelada' ? config.tarifa_por_tonelada : config.tarifa_por_km)?.toString() || '0'}
                    className="w-28 px-2 py-1 rounded-lg border border-emerald-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-right font-bold text-emerald-600 text-lg"
                  />
                </div>
              </div>
              <div className="flex justify-between items-center pt-2">
                <span className="text-sm font-medium text-stone-600">Total del viaje</span>
                <span className="font-bold text-emerald-600 text-lg">{formatPrecio(precio)}</span>
              </div>
            </div>
          </div>
        </>
      )}

      <div className="flex items-center gap-2 text-sm text-stone-500">
        <Truck className="w-4 h-4 text-emerald-600" />
        <span>
          {camiones.length} camión{camiones.length !== 1 ? 'es' : ''} disponible{camiones.length !== 1 ? 's' : ''} cerca
        </span>
      </div>

      <button
        onClick={onSolicitar}
        disabled={!puedeCobrar || solicitando}
        className="w-full py-3.5 rounded-xl bg-emerald-600 text-white font-semibold disabled:opacity-40 disabled:cursor-not-allowed hover:bg-emerald-700 transition flex items-center justify-center gap-2"
      >
        {solicitando ? <Loader2 className="w-5 h-5 animate-spin" /> : <Truck className="w-5 h-5" />}
        {solicitando ? 'Solicitando...' : 'Pedir camión'}
      </button>
    </div>
  );
}