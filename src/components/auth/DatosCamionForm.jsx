import { Truck, Building2, IdCard, Phone, Weight, User } from 'lucide-react';

export default function DatosCamionForm({ datos, onChange }) {
  const update = (campo, valor) => {
    onChange({ ...datos, [campo]: valor });
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 pb-2 border-b border-stone-200">
        <Truck className="w-5 h-5 text-blue-600" />
        <p className="text-sm font-semibold text-stone-900">Datos del camión</p>
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium text-stone-700">Nombre del chofer</label>
        <div className="relative">
          <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
          <input
            type="text"
            value={datos.chofer_nombre || ''}
            onChange={(e) => update('chofer_nombre', e.target.value)}
            placeholder="Ej: Juan Pérez"
            className="w-full pl-10 pr-3 h-11 rounded-lg border border-stone-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none text-sm"
          />
        </div>
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium text-stone-700">Nombre de transporte</label>
        <div className="relative">
          <Building2 className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
          <input
            type="text"
            value={datos.transporte_nombre || ''}
            onChange={(e) => update('transporte_nombre', e.target.value)}
            placeholder="Ej: Transporte Rodríguez SRL"
            className="w-full pl-10 pr-3 h-11 rounded-lg border border-stone-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none text-sm"
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-2">
          <label className="text-sm font-medium text-stone-700">CUIT transporte</label>
          <div className="relative">
            <IdCard className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
            <input
              type="text"
              value={datos.transporte_cuit || ''}
              onChange={(e) => update('transporte_cuit', e.target.value)}
              placeholder="30-12345678-9"
              className="w-full pl-10 pr-3 h-11 rounded-lg border border-stone-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none text-sm"
            />
          </div>
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium text-stone-700">CUIT chofer</label>
          <div className="relative">
            <IdCard className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
            <input
              type="text"
              value={datos.chofer_cuit || ''}
              onChange={(e) => update('chofer_cuit', e.target.value)}
              placeholder="20-12345678-9"
              className="w-full pl-10 pr-3 h-11 rounded-lg border border-stone-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none text-sm"
            />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-2">
          <label className="text-sm font-medium text-stone-700">Patente chasis</label>
          <input
            type="text"
            value={datos.patente || ''}
            onChange={(e) => update('patente', e.target.value.toUpperCase())}
            placeholder="AB123CD"
            className="w-full px-3 h-11 rounded-lg border border-stone-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none text-sm uppercase"
          />
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium text-stone-700">Patente acoplado</label>
          <input
            type="text"
            value={datos.patente_acoplado || ''}
            onChange={(e) => update('patente_acoplado', e.target.value.toUpperCase())}
            placeholder="EF456GH"
            className="w-full px-3 h-11 rounded-lg border border-stone-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none text-sm uppercase"
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-2">
          <label className="text-sm font-medium text-stone-700">Teléfono</label>
          <div className="relative">
            <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
            <input
              type="tel"
              value={datos.telefono || ''}
              onChange={(e) => update('telefono', e.target.value)}
              placeholder="351 123 4567"
              className="w-full pl-10 pr-3 h-11 rounded-lg border border-stone-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none text-sm"
            />
          </div>
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium text-stone-700">Capacidad (kg)</label>
          <div className="relative">
            <Weight className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
            <input
              type="number"
              value={datos.capacidad_kg || ''}
              onChange={(e) => update('capacidad_kg', e.target.value)}
              placeholder="12000"
              className="w-full pl-10 pr-3 h-11 rounded-lg border border-stone-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none text-sm"
            />
          </div>
        </div>
      </div>
    </div>
  );
}