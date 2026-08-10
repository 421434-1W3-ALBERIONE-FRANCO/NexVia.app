import { Truck, Check, Navigation2, MapPin, Navigation, Phone, Package } from 'lucide-react';
import { formatPrecio, formatKm } from '@/lib/distance';
import SolicitudCard from '@/components/chofer/SolicitudCard';

export default function PanelChofer({
  miCamion, solicitudes, viajeActivo, onAceptar, onRechazar, aceptando, config,
}) {
  if (!miCamion) {
    return (
      <div className="p-6 text-center space-y-3">
        <div className="w-16 h-16 mx-auto rounded-full bg-stone-100 flex items-center justify-center">
          <Truck className="w-8 h-8 text-stone-400" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-stone-900">Sin camión asignado</h2>
          <p className="text-sm text-stone-500">
            Contactá al administrador para que te asigne un camión.
          </p>
        </div>
      </div>
    );
  }

  if (viajeActivo) {
    const estados = {
      aceptado: { label: 'Yendo a buscar al cliente', icon: Truck, color: 'text-blue-600', bg: 'bg-blue-50' },
      en_camino: { label: 'En camino al destino', icon: Navigation2, color: 'text-blue-600', bg: 'bg-blue-50' },
      completado: { label: '¡Viaje completado!', icon: Check, color: 'text-emerald-600', bg: 'bg-emerald-50' },
    };
    const estado = estados[viajeActivo.estado] || estados.aceptado;
    const EstadoIcon = estado.icon;

    return (
      <div className="p-6 space-y-4">
        <div className="text-center pt-2">
          <div className={`w-16 h-16 mx-auto rounded-full ${estado.bg} flex items-center justify-center`}>
            <EstadoIcon className={`w-8 h-8 ${estado.color} animate-pulse`} />
          </div>
          <h2 className="text-xl font-bold text-stone-900 mt-3">{estado.label}</h2>
          <p className="text-sm text-stone-500">Cliente: {viajeActivo.usuario_nombre || 'Usuario'}</p>
        </div>

        <div className="flex items-center gap-3 p-3 rounded-xl bg-amber-50 border border-amber-100">
          <Package className="w-5 h-5 text-amber-600 shrink-0" />
          <div className="min-w-0">
            <p className="text-xs text-stone-500">Carga</p>
            <p className="text-sm font-semibold text-stone-900 truncate">{viajeActivo.carga || 'No especificada'}</p>
          </div>
        </div>

        <div className="bg-stone-50 rounded-2xl p-4 space-y-3">
          <div className="flex justify-between items-center">
            <span className="text-sm text-stone-500">Distancia</span>
            <span className="text-sm font-semibold">{formatKm(viajeActivo.distancia_km)}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-sm text-stone-500">Total a cobrar</span>
            <span className="text-sm font-bold text-emerald-600">{formatPrecio(viajeActivo.precio)}</span>
          </div>
        </div>

        <div className="space-y-2">
          <div className="flex items-center gap-3 p-3 rounded-xl bg-emerald-50 border border-emerald-100">
            <MapPin className="w-4 h-4 text-emerald-600 shrink-0" />
            <div className="text-sm">
              <p className="text-xs text-stone-500">Origen</p>
              <p className="font-medium text-stone-900">{viajeActivo.origen_lat?.toFixed(4)}, {viajeActivo.origen_lng?.toFixed(4)}</p>
            </div>
          </div>
          <div className="flex items-center gap-3 p-3 rounded-xl bg-red-50 border border-red-100">
            <Navigation className="w-4 h-4 text-red-600 shrink-0" />
            <div className="text-sm">
              <p className="text-xs text-stone-500">Destino</p>
              <p className="font-medium text-stone-900">{viajeActivo.destino_lat?.toFixed(4)}, {viajeActivo.destino_lng?.toFixed(4)}</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center gap-3 pb-4 border-b border-stone-200">
        <div className="w-10 h-10 rounded-xl bg-stone-100 flex items-center justify-center shrink-0">
          <Truck className="w-5 h-5 text-stone-600" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="font-semibold text-stone-900 truncate">{miCamion.chofer_nombre}</p>
          <p className="text-xs text-stone-500">{miCamion.patente} · {miCamion.capacidad_kg || '?'} kg</p>
        </div>
        <span className="text-xs font-semibold px-2 py-1 rounded-full bg-emerald-100 text-emerald-700">
          {miCamion.estado}
        </span>
      </div>

      <div>
        <h2 className="text-lg font-bold text-stone-900">Solicitudes de viaje</h2>
        <p className="text-sm text-stone-500">
          {solicitudes.length > 0
            ? `${solicitudes.length} viaje${solicitudes.length !== 1 ? 's' : ''} esperando respuesta`
            : 'No hay solicitudes por ahora'}
        </p>
      </div>

      <div className="space-y-3">
        {solicitudes.map((s) => (
          <SolicitudCard key={s.id} solicitud={s} onAceptar={onAceptar} onRechazar={onRechazar} aceptando={aceptando === s.id} />
        ))}
        {solicitudes.length === 0 && (
          <div className="text-center py-10">
            <div className="w-14 h-14 mx-auto rounded-full bg-stone-100 flex items-center justify-center">
              <Package className="w-7 h-7 text-stone-400" />
            </div>
            <p className="text-sm text-stone-500 mt-3">Esperando solicitudes...</p>
          </div>
        )}
      </div>
    </div>
  );
}