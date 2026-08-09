import { Truck, User, Phone, CreditCard, Hash, Weight, Building2 } from 'lucide-react';

function DatoFila({ icon: Icon, label, value }) {
  if (!value) return null;
  return (
    <div className="flex items-center gap-2.5 py-1.5">
      <Icon className="w-3.5 h-3.5 text-stone-400 shrink-0" />
      <span className="text-xs text-stone-500 shrink-0">{label}</span>
      <span className="text-sm font-medium text-stone-900 ml-auto text-right truncate">{value}</span>
    </div>
  );
}

export default function DatosCamionero({ camion }) {
  if (!camion) return null;

  return (
    <div className="rounded-2xl border border-blue-100 bg-blue-50/50 overflow-hidden">
      <div className="flex items-center gap-2.5 px-4 py-3 bg-blue-100/50 border-b border-blue-100">
        <div className="w-8 h-8 rounded-lg bg-blue-100 flex items-center justify-center shrink-0">
          <Truck className="w-4 h-4 text-blue-600" />
        </div>
        <div className="min-w-0">
          <p className="text-sm font-bold text-stone-900">Datos del transportista</p>
          <p className="text-xs text-stone-500">Para la carta de porte</p>
        </div>
      </div>

      <div className="px-4 py-2">
        <p className="text-[10px] font-semibold text-stone-400 uppercase tracking-wide mt-1 mb-0.5">Transporte</p>
        <DatoFila icon={Building2} label="Empresa" value={camion.transporte_nombre} />
        <DatoFila icon={CreditCard} label="CUIT" value={camion.transporte_cuit} />

        <p className="text-[10px] font-semibold text-stone-400 uppercase tracking-wide mt-2 mb-0.5">Chofer</p>
        <DatoFila icon={User} label="Nombre" value={camion.chofer_nombre} />
        <DatoFila icon={CreditCard} label="CUIT" value={camion.chofer_cuit} />
        <DatoFila icon={Phone} label="Teléfono" value={camion.telefono} />

        <p className="text-[10px] font-semibold text-stone-400 uppercase tracking-wide mt-2 mb-0.5">Vehículo</p>
        <DatoFila icon={Hash} label="Patente" value={camion.patente} />
        <DatoFila icon={Hash} label="Patente acoplado" value={camion.patente_acoplado} />
        <DatoFila icon={Weight} label="Capacidad" value={camion.capacidad_kg ? `${camion.capacidad_kg} kg` : null} />
      </div>
    </div>
  );
}