import { api, poll } from './apiClient';

const LUGARES_KEY = 'nexvia_lugares_guardados';

function readLugares() {
  try {
    return JSON.parse(localStorage.getItem(LUGARES_KEY) || '[]');
  } catch {
    return [];
  }
}
function writeLugares(list) {
  localStorage.setItem(LUGARES_KEY, JSON.stringify(list));
}

const auth = {
  async me() {
    const data = await api.get('/auth/me');
    return data.user;
  },
  async login(email, password) {
    const data = await api.post('/auth/login', { email, password });
    return data.user;
  },
  // Base44 compatibility alias
  async loginViaEmailPassword(email, password) {
    return this.login(email, password);
  },
  async register({ email, password }) {
    return api.post('/auth/register', { email, password });
  },
  async verifyOtp({ email, otpCode }) {
    const data = await api.post('/auth/verify-email', { email, code: otpCode });
    return data;
  },
  async resendOtp(email) {
    return api.post('/auth/resend-otp', { email });
  },
  async resetPasswordRequest(email) {
    return api.post('/auth/forgot-password', { email });
  },
  async resetPassword({ resetToken, newPassword }) {
    return api.post('/auth/reset-password', { token: resetToken, newPassword });
  },
  async logout() {
    try {
      await api.post('/auth/logout');
    } catch {
      /* ignore */
    }
    if (typeof window !== 'undefined') window.location.href = '/login';
  },
  redirectToLogin() {
    if (typeof window !== 'undefined') window.location.href = '/login';
  },
  // Cookie-based sessions: token handling is a no-op kept for API compatibility.
  setToken() {},
  loginWithProvider() {
    if (typeof window !== 'undefined') window.location.href = '/login';
  },
};

const Viaje = {
  async create(payload) {
    const data = await api.post('/viajes', payload);
    return data.trip;
  },
  async filter(criteria = {}, _sort, limit = 10) {
    let data;
    if (criteria.estado === 'solicitado') {
      data = await api.get(`/viajes/disponibles?limit=${limit}`);
    } else {
      data = await api.get(`/viajes/mis-viajes?limit=${limit}`);
    }
    return data.trips || [];
  },
  async list(_sort, limit = 10) {
    const data = await api.get(`/viajes/mis-viajes?limit=${limit}`);
    return data.trips || [];
  },
  async update(id, patch) {
    if (patch.estado === 'en_camino') return (await api.post(`/viajes/${id}/en-camino`)).trip;
    if (patch.estado === 'completado') return (await api.post(`/viajes/${id}/completar`)).trip;
    if (patch.estado === 'cancelado')
      return (await api.post(`/viajes/${id}/cancelar`, { razon: patch.cancelacion_razon || 'Cancelado' })).trip;
    if (patch.estado === 'aceptado') return (await api.post(`/viajes/${id}/aceptar`)).trip;
    return null;
  },
  subscribe(cb) {
    return poll(async () => {
      const data = await api.get('/viajes/mis-viajes?limit=20').catch(() => ({ trips: [] }));
      return data.trips || [];
    }, cb);
  },
};

const Camion = {
  async list() {
    const data = await api.get('/camiones/disponibles');
    return data.trucks || [];
  },
  async create() {
    // Backend has no truck-registration endpoint yet; surfaced for a future slice.
    throw new Error('Registro de camión aún no disponible en el backend.');
  },
  async filter(criteria = {}) {
    if (criteria.user_id) {
      const data = await api.get('/camiones/mi-camion').catch(() => ({ truck: null }));
      return data.truck ? [data.truck] : [];
    }
    const data = await api.get('/camiones/disponibles');
    return data.trucks || [];
  },
  async get(id) {
    const data = await api.get('/camiones/disponibles');
    return (data.trucks || []).find((t) => t.id === id) || null;
  },
  async update(id, patch) {
    if (patch.lat !== undefined && patch.lng !== undefined) {
      return (await api.post(`/camiones/${id}/ubicacion`, { lat: patch.lat, lng: patch.lng })).truck;
    }
    return (await api.put(`/camiones/${id}`, patch)).truck;
  },
  async delete(id) {
    return api.del(`/camiones/${id}`);
  },
  subscribe(cb) {
    return poll(async () => {
      const data = await api.get('/camiones/disponibles').catch(() => ({ trucks: [] }));
      return data.trucks || [];
    }, cb);
  },
};

const Configuracion = {
  async list() {
    // Read via the public endpoint (any authenticated user); writes stay admin-only.
    const data = await api.get('/config');
    return data.config ? [data.config] : [];
  },
  async update(_id, patch) {
    return (await api.put('/admin/configuracion', patch)).config;
  },
  async create(patch) {
    return (await api.put('/admin/configuracion', patch)).config;
  },
};

const User = {
  async list() {
    const data = await api.get('/admin/usuarios');
    return data.users || [];
  },
  async get(id) {
    return api.get(`/admin/usuarios/${id}`);
  },
  async update(id, patch) {
    return api.put(`/admin/usuarios/${id}`, patch);
  },
};

const LugarGuardado = {
  async list() {
    return readLugares();
  },
  async create(payload) {
    const list = readLugares();
    const item = { id: crypto.randomUUID(), ...payload, updated_date: new Date().toISOString() };
    list.unshift(item);
    writeLugares(list);
    return item;
  },
  async delete(id) {
    writeLugares(readLugares().filter((l) => l.id !== id));
  },
};

const users = {
  async inviteUser() {
    throw new Error('Invitaciones no disponibles en el backend actual.');
  },
};

const functions = {
  async invoke(name, payload) {
    if (name === 'asignarRol') {
      const data = await api.post('/auth/choose-role', { role: payload.role });
      return data;
    }
    throw new Error(`Función no soportada: ${name}`);
  },
};

export const nexvia = {
  auth,
  users,
  functions,
  entities: { Viaje, Camion, Configuracion, User, LugarGuardado },
};

export default nexvia;
