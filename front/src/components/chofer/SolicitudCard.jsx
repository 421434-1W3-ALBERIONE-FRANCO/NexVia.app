import { MapPin, DollarSign, Ruler, Check, X, Package } from 'lucide-react';
import { formatPrecio, formatKm } from '@/lib/distance';
import MiniRuta from '@/components/chofer/MiniRuta';

export default function SolicitudCard({ solicitud, onAceptar, onRechazar, aceptando }) {
  const origen = { lat: solicitud.origen_lat, lng: solicitud.origen_lng };
  const destino = { lat: solicitud.destino_lat, lng: solicitud.destino_lng };

  return (
    <div className="bg-white rounded-2xl border border-stone-200 p-4 space-y-3 hover:border-emerald-300 transition">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-emerald-100 flex items-center justify-center">
            <MapPin className="w-4 h-4 text-emerald-600" />
          </div>
          <div>
            <p className="text-sm font-semibold text-stone-900">Nueva solicitud</p>
            <p className="text-xs text-stone-500">{solicitud.usuario_nombre || 'Usuario'}</p>
          </div>
        </div>
        <span className="text-xs px-2 py-1 rounded-full bg-amber-100 text-amber-700 font-medium">
          Esperando
        </span>
      </div>

      <MiniRuta origen={origen} destino={destino} />

      <div className="flex items-center gap-2 p-2.5 rounded-xl bg-amber-50 border border-amber-100">
        <Package className="w-4 h-4 text-amber-600 shrink-0" />
        <div className="min-w-0">
          <p className="text-xs text-stone-500">Carga a transportar</p>
          <p className="text-sm font-semibold text-stone-900 truncate">{solicitud.carga || 'No especificada'}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2 text-xs">
        <div className="flex items-center gap-1.5 text-stone-600">
          <Ruler className="w-3.5 h-3.5 text-stone-400" />
          {formatKm(solicitud.distancia_km)}
        </div>
        <div className="flex items-center gap-1.5 text-stone-600">
          <DollarSign className="w-3.5 h-3.5 text-stone-400" />
          {formatPrecio(solicitud.precio)}
        </div>
      </div>

      <div className="space-y-1.5">
        <div className="flex items-center gap-2 text-xs">
          <div className="w-2 h-2 rounded-full bg-emerald-500 shrink-0" />
          <span className="text-stone-500 shrink-0">Origen:</span>
          <span className="text-stone-700 font-medium truncate">
            {solicitud.origen_lat?.toFixed(4)}, {solicitud.origen_lng?.toFixed(4)}
          </span>
        </div>
        <div className="flex items-center gap-2 text-xs">
          <div className="w-2 h-2 rounded-full bg-red-500 shrink-0" />
          <span className="text-stone-500 shrink-0">Destino:</span>
          <span className="text-stone-700 font-medium truncate">
            {solicitud.destino_lat?.toFixed(4)}, {solicitud.destino_lng?.toFixed(4)}
          </span>
        </div>
      </div>

      <div className="flex gap-2">
        <button
          onClick={() => onAceptar(solicitud)}
          disabled={aceptando}
          className="flex-1 py-2.5 rounded-xl bg-emerald-600 text-white font-semibold disabled:opacity-50 hover:bg-emerald-700 transition flex items-center justify-center gap-2 text-sm"
        >
          <Check className="w-4 h-4" />
          {aceptando ? 'Aceptando...' : 'Aceptar'}
        </button>
        <button
          onClick={() => onRechazar(solicitud)}
          disabled={aceptando}
          className="flex-1 py-2.5 rounded-xl bg-stone-100 text-stone-600 font-semibold disabled:opacity-50 hover:bg-stone-200 transition flex items-center justify-center gap-2 text-sm"
        >
          <X className="w-4 h-4" />
          Rechazar
        </button>
      </div>
    </div>
  );
}