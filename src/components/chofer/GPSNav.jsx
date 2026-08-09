import { Navigation, Clock, MapPin } from 'lucide-react';
import { haversineKm, formatKm, formatPrecio } from '@/lib/distance';

const VELOCIDAD_PROMEDIO = 60;

export default function GPSNav({ viajeActivo, miCamion }) {
  if (!viajeActivo || !miCamion) return null;

  const goingToPickup = viajeActivo.estado === 'aceptado';
  const target = goingToPickup
    ? { lat: viajeActivo.origen_lat, lng: viajeActivo.origen_lng }
    : { lat: viajeActivo.destino_lat, lng: viajeActivo.destino_lng };

  const distRestante = haversineKm(miCamion.lat, miCamion.lng, target.lat, target.lng);
  const minutos = Math.max(1, Math.ceil((distRestante / VELOCIDAD_PROMEDIO) * 60));

  return (
    <div className="absolute bottom-0 left-0 right-0 z-[1000]">
      <div className="bg-white/95 backdrop-blur-md border-t border-stone-200 shadow-2xl rounded-t-3xl px-5 py-4 pb-6 space-y-3">
        <div className="flex items-center gap-3">
          <div className={`w-11 h-11 rounded-full flex items-center justify-center shrink-0 ${goingToPickup ? 'bg-blue-100' : 'bg-emerald-100'}`}>
            <Navigation className={`w-5 h-5 ${goingToPickup ? 'text-blue-600' : 'text-emerald-600'}`} />
          </div>
          <div className="min-w-0">
            <p className="text-sm font-bold text-stone-900">
              {goingToPickup ? 'Yendo a buscar al productor' : 'En camino al destino'}
            </p>
            <p className="text-xs text-stone-500 truncate">{viajeActivo.usuario_nombre || 'Cliente'}</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1.5">
            <Clock className="w-4 h-4 text-stone-400" />
            <span className="text-lg font-bold text-stone-900">{minutos}</span>
            <span className="text-xs text-stone-500">min</span>
          </div>
          <div className="w-px h-8 bg-stone-200" />
          <div className="flex items-center gap-1.5">
            <MapPin className="w-4 h-4 text-stone-400" />
            <span className="text-lg font-bold text-stone-900">{formatKm(distRestante)}</span>
          </div>
          <div className="ml-auto">
            <span className="text-lg font-bold text-emerald-600">{formatPrecio(viajeActivo.precio)}</span>
          </div>
        </div>
      </div>
    </div>
  );
}