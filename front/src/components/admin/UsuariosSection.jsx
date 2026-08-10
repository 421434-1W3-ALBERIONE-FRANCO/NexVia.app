import { useState, useEffect } from 'react';
import { nexvia } from '@/api/client';
import { Users, UserPlus, Loader2, Mail } from 'lucide-react';

export default function UsuariosSection({ camiones, onCamionAsignado }) {
  const [usuarios, setUsuarios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [invitarEmail, setInvitarEmail] = useState('');
  const [invitarRol, setInvitarRol] = useState('usuario');
  const [invitacionOk, setInvitacionOk] = useState(false);
  const [invitando, setInvitando] = useState(false);

  useEffect(() => {
    cargar();
  }, []);

  const cargar = async () => {
    try {
      const data = await nexvia.entities.User.list();
      setUsuarios(data);
    } finally {
      setCargando(false);
    }
  };

  const cambiarRol = async (userId, nuevoRol) => {
    await nexvia.entities.User.update(userId, { role: nuevoRol });
    setUsuarios((prev) => prev.map((u) => (u.id === userId ? { ...u, role: nuevoRol } : u)));
  };

  const asignarCamion = async (userId, camionId) => {
    const camion = camiones.find((c) => c.id === camionId);
    const user = usuarios.find((u) => u.id === userId);
    await nexvia.entities.Camion.update(camionId, {
      user_id: userId,
      chofer_nombre: user?.full_name || user?.email || '',
    });
    onCamionAsignado();
  };

  const invitar = async () => {
    if (!invitarEmail) return;
    setInvitando(true);
    try {
      await nexvia.users.inviteUser(invitarEmail, invitarRol);
      setInvitarEmail('');
      setInvitacionOk(true);
      setTimeout(() => setInvitacionOk(false), 3000);
      cargar();
    } catch {
      // error bubbles
    } finally {
      setInvitando(false);
    }
  };

  if (cargando) {
    return (
      <section className="bg-white rounded-2xl border border-stone-200 p-6 flex items-center justify-center">
        <Loader2 className="w-6 h-6 animate-spin text-stone-400" />
      </section>
    );
  }

  return (
    <section className="bg-white rounded-2xl border border-stone-200 p-6 space-y-4">
      <div className="flex items-center gap-2.5">
        <div className="w-9 h-9 rounded-xl bg-indigo-100 flex items-center justify-center">
          <Users className="w-5 h-5 text-indigo-600" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-stone-900">Usuarios ({usuarios.length})</h2>
          <p className="text-sm text-stone-500">Gestioná roles y asigná camiones a los choferes</p>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row gap-2 p-4 bg-stone-50 rounded-xl">
        <div className="flex-1 flex items-center gap-2 px-3 py-2 rounded-lg border border-stone-300 bg-white">
          <Mail className="w-4 h-4 text-stone-400 shrink-0" />
          <input
            type="email"
            placeholder="email@ejemplo.com"
            value={invitarEmail}
            onChange={(e) => setInvitarEmail(e.target.value)}
            className="flex-1 outline-none text-sm bg-transparent"
          />
        </div>
        <select
          value={invitarRol}
          onChange={(e) => setInvitarRol(e.target.value)}
          className="px-3 py-2 rounded-lg border border-stone-300 bg-white text-sm outline-none"
        >
          <option value="usuario">Usuario</option>
          <option value="chofer">Chofer</option>
          <option value="admin">Admin</option>
        </select>
        <button
          onClick={invitar}
          disabled={!invitarEmail || invitando}
          className="px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-semibold disabled:opacity-40 hover:bg-indigo-700 transition flex items-center gap-2 justify-center"
        >
          {invitando ? <Loader2 className="w-4 h-4 animate-spin" /> : invitacionOk ? '✓ Invitado' : <><UserPlus className="w-4 h-4" /> Invitar</>}
        </button>
      </div>

      <div className="space-y-2">
        {usuarios.map((u) => (
          <div key={u.id} className="flex flex-col sm:flex-row sm:items-center gap-3 p-3 rounded-xl border border-stone-200 hover:bg-stone-50 transition">
            <div className="flex items-center gap-3 flex-1 min-w-0">
              <div className="w-9 h-9 rounded-full bg-stone-200 flex items-center justify-center shrink-0 text-sm font-bold text-stone-600">
                {(u.full_name || u.email || '?')[0].toUpperCase()}
              </div>
              <div className="min-w-0">
                <p className="font-medium text-stone-900 truncate">{u.full_name || u.email}</p>
                <p className="text-xs text-stone-500 truncate">{u.email}</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              {u.role === 'chofer' && (
                <select
                  value={camiones.find((c) => c.user_id === u.id)?.id || ''}
                  onChange={(e) => asignarCamion(u.id, e.target.value)}
                  className="text-xs px-2 py-1.5 rounded-lg border border-stone-300 bg-white outline-none max-w-[140px]"
                >
                  <option value="">Sin camión</option>
                  {camiones.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.patente} - {c.chofer_nombre}
                    </option>
                  ))}
                </select>
              )}
              <select
                value={u.role}
                onChange={(e) => cambiarRol(u.id, e.target.value)}
                className={`text-xs font-semibold px-2 py-1.5 rounded-lg border outline-none ${
                  u.role === 'admin' ? 'border-purple-200 bg-purple-50 text-purple-700' :
                  u.role === 'chofer' ? 'border-amber-200 bg-amber-50 text-amber-700' :
                  'border-blue-200 bg-blue-50 text-blue-700'
                }`}
              >
                <option value="usuario">Usuario</option>
                <option value="chofer">Chofer</option>
                <option value="admin">Admin</option>
              </select>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}