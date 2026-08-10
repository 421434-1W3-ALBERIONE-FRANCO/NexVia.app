import { useState, useEffect } from 'react';
import { nexvia } from '@/api/client';
import { Settings, Truck, Plus, Trash2, Save, Loader2, Check } from 'lucide-react';
import UsuariosSection from '@/components/admin/UsuariosSection';

export default function Admin() {
  const [config, setConfig] = useState(null);
  const [camiones, setCamiones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [guardadoOk, setGuardadoOk] = useState(false);
  const [nuevo, setNuevo] = useState({
    transporte_nombre: '', transporte_cuit: '', chofer_nombre: '', chofer_cuit: '',
    patente: '', patente_acoplado: '', telefono: '', capacidad_kg: ''
  });

  useEffect(() => {
    cargar();
  }, []);

  const cargar = async () => {
    try {
      const [configs, trucks] = await Promise.all([
        nexvia.entities.Configuracion.list(),
        nexvia.entities.Camion.list(),
      ]);
      setConfig(
        configs[0] || { tarifa_por_km: 500, tarifa_por_tonelada: 0, zona_nombre: '', centro_lat: -32.4341, centro_lng: -63.2433 }
      );
      setCamiones(trucks);
    } finally {
      setCargando(false);
    }
  };

  const guardarConfig = async () => {
    setGuardando(true);
    try {
      const data = {
        tarifa_por_km: Number(config.tarifa_por_km),
        tarifa_por_tonelada: Number(config.tarifa_por_tonelada) || 0,
        zona_nombre: config.zona_nombre,
        centro_lat: Number(config.centro_lat),
        centro_lng: Number(config.centro_lng),
      };
      if (config.id) {
        await nexvia.entities.Configuracion.update(config.id, data);
      } else {
        const created = await nexvia.entities.Configuracion.create(data);
        setConfig(created);
      }
      setGuardadoOk(true);
      setTimeout(() => setGuardadoOk(false), 2000);
    } finally {
      setGuardando(false);
    }
  };

  const agregarCamion = async () => {
    if (!nuevo.chofer_nombre || !nuevo.patente) return;
    await nexvia.entities.Camion.create({
      transporte_nombre: nuevo.transporte_nombre,
      transporte_cuit: nuevo.transporte_cuit,
      chofer_nombre: nuevo.chofer_nombre,
      chofer_cuit: nuevo.chofer_cuit,
      patente: nuevo.patente,
      patente_acoplado: nuevo.patente_acoplado,
      telefono: nuevo.telefono,
      capacidad_kg: Number(nuevo.capacidad_kg) || 0,
      lat: Number(config?.centro_lat) || -32.4341,
      lng: Number(config?.centro_lng) || -63.2433,
      estado: 'disponible',
    });
    setNuevo({
      transporte_nombre: '', transporte_cuit: '', chofer_nombre: '', chofer_cuit: '',
      patente: '', patente_acoplado: '', telefono: '', capacidad_kg: ''
    });
    cargar();
  };

  const eliminarCamion = async (id) => {
    await nexvia.entities.Camion.delete(id);
    cargar();
  };

  const actualizarCampo = (campo, valor) => {
    setConfig((prev) => ({ ...prev, [campo]: valor }));
  };

  if (cargando) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-4rem)]">
        <div className="w-8 h-8 border-4 border-stone-200 border-t-emerald-600 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-8">
      <section className="bg-white rounded-2xl border border-stone-200 p-6 space-y-4">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-emerald-100 flex items-center justify-center">
            <Settings className="w-5 h-5 text-emerald-600" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-stone-900">Configuración de tarifas</h2>
            <p className="text-sm text-stone-500">Definí la tarifa por kilómetro y la zona del mapa</p>
          </div>
        </div>
        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="text-sm font-medium text-stone-700">Tarifa por km (ARS)</label>
            <input
              type="number"
              value={config.tarifa_por_km || ''}
              onChange={(e) => actualizarCampo('tarifa_por_km', e.target.value)}
              className="mt-1 w-full px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-stone-700">Tarifa por tonelada (ARS)</label>
            <input
              type="number"
              value={config.tarifa_por_tonelada || ''}
              onChange={(e) => actualizarCampo('tarifa_por_tonelada', e.target.value)}
              placeholder="0"
              className="mt-1 w-full px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-stone-700">Nombre de la zona</label>
            <input
              type="text"
              value={config.zona_nombre || ''}
              onChange={(e) => actualizarCampo('zona_nombre', e.target.value)}
              placeholder="Ej: Zona Rural Villa María"
              className="mt-1 w-full px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-stone-700">Centro del mapa - Latitud</label>
            <input
              type="number"
              step="0.0001"
              value={config.centro_lat || ''}
              onChange={(e) => actualizarCampo('centro_lat', e.target.value)}
              className="mt-1 w-full px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-stone-700">Centro del mapa - Longitud</label>
            <input
              type="number"
              step="0.0001"
              value={config.centro_lng || ''}
              onChange={(e) => actualizarCampo('centro_lng', e.target.value)}
              className="mt-1 w-full px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none"
            />
          </div>
        </div>
        <button
          onClick={guardarConfig}
          disabled={guardando}
          className="px-5 py-2.5 rounded-xl bg-emerald-600 text-white font-semibold disabled:opacity-50 hover:bg-emerald-700 transition flex items-center gap-2"
        >
          {guardando ? <Loader2 className="w-4 h-4 animate-spin" /> : guardadoOk ? <Check className="w-4 h-4" /> : <Save className="w-4 h-4" />}
          {guardadoOk ? 'Guardado' : 'Guardar'}
        </button>
      </section>

      <section className="bg-white rounded-2xl border border-stone-200 p-6 space-y-4">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-amber-100 flex items-center justify-center">
            <Truck className="w-5 h-5 text-amber-600" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-stone-900">Camiones ({camiones.length})</h2>
            <p className="text-sm text-stone-500">Gestioná los camiones de tu flota</p>
          </div>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-3 p-4 bg-stone-50 rounded-xl">
          <input
            type="text"
            placeholder="Nombre de transporte"
            value={nuevo.transporte_nombre}
            onChange={(e) => setNuevo({ ...nuevo, transporte_nombre: e.target.value })}
            className="px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm"
          />
          <input
            type="text"
            placeholder="CUIT de transporte"
            value={nuevo.transporte_cuit}
            onChange={(e) => setNuevo({ ...nuevo, transporte_cuit: e.target.value })}
            className="px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm"
          />
          <input
            type="text"
            placeholder="Nombre del chofer"
            value={nuevo.chofer_nombre}
            onChange={(e) => setNuevo({ ...nuevo, chofer_nombre: e.target.value })}
            className="px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm"
          />
          <input
            type="text"
            placeholder="CUIT del chofer"
            value={nuevo.chofer_cuit}
            onChange={(e) => setNuevo({ ...nuevo, chofer_cuit: e.target.value })}
            className="px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm"
          />
          <input
            type="text"
            placeholder="Patente chasis"
            value={nuevo.patente}
            onChange={(e) => setNuevo({ ...nuevo, patente: e.target.value })}
            className="px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm uppercase"
          />
          <input
            type="text"
            placeholder="Patente acoplado/semirremolque"
            value={nuevo.patente_acoplado}
            onChange={(e) => setNuevo({ ...nuevo, patente_acoplado: e.target.value })}
            className="px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm uppercase"
          />
          <input
            type="text"
            placeholder="Teléfono"
            value={nuevo.telefono}
            onChange={(e) => setNuevo({ ...nuevo, telefono: e.target.value })}
            className="px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm"
          />
          <div className="flex gap-2">
            <input
              type="number"
              placeholder="Capacidad kg"
              value={nuevo.capacidad_kg}
              onChange={(e) => setNuevo({ ...nuevo, capacidad_kg: e.target.value })}
              className="px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm flex-1"
            />
            <button
              onClick={agregarCamion}
              disabled={!nuevo.chofer_nombre || !nuevo.patente}
              className="px-3 py-2 rounded-lg bg-stone-900 text-white disabled:opacity-40 hover:bg-stone-800 transition flex items-center justify-center"
            >
              <Plus className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="space-y-2">
          {camiones.length === 0 ? (
            <p className="text-sm text-stone-500 text-center py-6">No hay camiones cargados todavía</p>
          ) : (
            camiones.map((c) => (
              <div
                key={c.id}
                className="flex items-center gap-3 p-3 rounded-xl border border-stone-200 hover:bg-stone-50 transition"
              >
                <div className="w-10 h-10 rounded-xl bg-stone-100 flex items-center justify-center shrink-0">
                  <Truck className="w-5 h-5 text-stone-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-stone-900 truncate">{c.chofer_nombre}</p>
                  <p className="text-xs text-stone-500 truncate">
                    {c.transporte_nombre ? `${c.transporte_nombre} · ` : ''}
                    {c.patente}
                    {c.patente_acoplado ? ` + ${c.patente_acoplado}` : ''}
                    {' · '}{c.telefono || 'Sin teléfono'}
                    {c.capacidad_kg ? ` · ${c.capacidad_kg} kg` : ''}
                  </p>
                </div>
                <span
                  className={`text-xs font-semibold px-2 py-1 rounded-full shrink-0 ${
                    c.estado === 'disponible'
                      ? 'bg-emerald-100 text-emerald-700'
                      : c.estado === 'ocupado'
                      ? 'bg-amber-100 text-amber-700'
                      : 'bg-stone-100 text-stone-600'
                  }`}
                >
                  {c.estado}
                </span>
                <button
                  onClick={() => eliminarCamion(c.id)}
                  className="p-2 rounded-lg text-stone-400 hover:text-red-600 hover:bg-red-50 transition shrink-0"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))
          )}
        </div>
      </section>

      <UsuariosSection camiones={camiones} onCamionAsignado={cargar} />
    </div>
  );
}