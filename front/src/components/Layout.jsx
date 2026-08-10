import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import { Truck, MapPin, Settings, LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '@/lib/AuthContext';
import { useEffect, useRef } from 'react';

const LOGO_URL = '/icon-mark.png';

export default function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const redirectedRef = useRef(false);

  useEffect(() => {
    if (!user || redirectedRef.current) return;
    redirectedRef.current = true;
    if (!user.role || user.role === 'user') {
      navigate('/bienvenida', { replace: true });
      return;
    }
    if (user.role === 'admin' && location.pathname === '/') {
      navigate('/admin', { replace: true });
    } else if (user.role === 'chofer' && location.pathname === '/') {
      navigate('/chofer', { replace: true });
    }
  }, [user]);

  const navItems = [];
  navItems.push({ to: '/', label: user?.role === 'chofer' ? 'Viajes disponibles' : 'Pedir camión', icon: MapPin });
  if (user?.role === 'chofer') {
    navItems.push({ to: '/chofer', label: 'Mis viajes', icon: Truck });
  }
  if (user?.role === 'admin') {
    navItems.push({ to: '/admin', label: 'Admin', icon: Settings });
  }

  return (
    <div className="min-h-screen bg-stone-50">
      <header className="sticky top-0 z-[1100] bg-[#05090f] border-b border-[#00cc99]/20">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2.5">
            <img src={LOGO_URL} alt="NEXVIA" className="h-10 w-auto rounded-lg object-contain" />
          </Link>
          <div className="flex items-center gap-1">
            {navItems.map(({ to, label, icon: Icon }) => {
              const active = location.pathname === to;
              return (
                <Link
                  key={to}
                  to={to}
                  className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition ${
                    active ? 'bg-[#0066ff] text-white shadow-sm' : 'text-stone-300 hover:bg-white/10'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span className="hidden sm:inline">{label}</span>
                </Link>
              );
            })}
            <div className="flex items-center gap-2 pl-2 ml-1 border-l border-white/10">
              <div className="hidden sm:flex items-center gap-1.5 text-xs text-stone-400">
                <UserIcon className="w-3.5 h-3.5" />
                <span className="max-w-[120px] truncate">{user?.full_name || user?.email}</span>
                <span className="px-1.5 py-0.5 rounded bg-[#00cc99]/20 text-[#00cc99] font-medium">
                  {user?.role}
                </span>
              </div>
              <button
                onClick={() => logout()}
                className="p-2 rounded-lg text-stone-400 hover:text-red-400 hover:bg-red-500/10 transition"
                title="Cerrar sesión"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}