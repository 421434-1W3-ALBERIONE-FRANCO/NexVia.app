import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/lib/AuthContext';
import TruckViewer3D from '@/components/TruckViewer3D';
import { Button } from '@/components/ui/button';
import { ArrowRight, MapPin, TrendingUp, Shield, Zap, Users, Globe } from 'lucide-react';

export default function Landing() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [isScrolled, setIsScrolled] = useState(false);

  // Redirect if already authenticated
  useEffect(() => {
    if (user) {
      if (user.role === 'admin') navigate('/admin');
      else if (user.role === 'chofer') navigate('/chofer');
      else navigate('/home');
    }
  }, [user, navigate]);

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 50);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="w-full overflow-x-hidden bg-slate-950">
      {/* Navigation */}
      <nav
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          isScrolled ? 'bg-slate-950/80 backdrop-blur-md border-b border-blue-500/20' : 'bg-transparent'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">NX</span>
            </div>
            <span className="text-xl font-bold text-white">NEXVIA</span>
          </div>

          <div className="hidden md:flex items-center gap-8">
            <a href="#features" className="text-slate-300 hover:text-white transition">
              Features
            </a>
            <a href="#benefits" className="text-slate-300 hover:text-white transition">
              Beneficios
            </a>
            <a href="#pricing" className="text-slate-300 hover:text-white transition">
              Precios
            </a>
          </div>

          <div className="flex items-center gap-3">
            <Button
              variant="outline"
              className="border-blue-500/50 text-blue-400 hover:bg-blue-500/10"
              onClick={() => navigate('/login')}
            >
              Ingresar
            </Button>
            <Button
              className="bg-gradient-to-r from-blue-600 to-cyan-600 hover:from-blue-700 hover:to-cyan-700"
              onClick={() => navigate('/register')}
            >
              Registrarse
            </Button>
          </div>
        </div>
      </nav>

      {/* Hero Section with 3D Truck */}
      <section className="relative min-h-screen pt-20 pb-20 flex items-center justify-center overflow-hidden">
        {/* Background elements */}
        <div className="absolute inset-0 bg-gradient-to-b from-blue-500/10 via-transparent to-transparent" />
        <div className="absolute top-20 right-10 w-72 h-72 bg-blue-500/20 rounded-full blur-3xl" />
        <div className="absolute bottom-20 left-10 w-72 h-72 bg-cyan-500/20 rounded-full blur-3xl" />

        <div className="relative z-10 w-full grid grid-cols-1 lg:grid-cols-2 gap-12 items-center px-4 sm:px-6 lg:px-8">
          {/* Left content */}
          <div className="space-y-8">
            <div className="space-y-4">
              <div className="inline-block px-4 py-2 rounded-lg bg-blue-500/20 border border-blue-500/50">
                <span className="text-sm font-medium text-blue-300">Bienvenido a NEXVIA</span>
              </div>
              <h1 className="text-5xl md:text-6xl font-bold leading-tight">
                <span className="text-white">Conectamos</span>
                <br />
                <span className="bg-gradient-to-r from-blue-400 via-cyan-400 to-blue-400 bg-clip-text text-transparent">
                  carga, impulsamos
                </span>
                <br />
                <span className="text-white">el futuro</span>
              </h1>
              <p className="text-lg text-slate-400 max-w-md">
                Plataforma de transporte de cargas 100% digital. Conecta usuarios y choferes en tiempo real.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-4">
              <Button
                size="lg"
                className="bg-gradient-to-r from-blue-600 to-cyan-600 hover:from-blue-700 hover:to-cyan-700 text-white text-base"
                onClick={() => navigate('/register')}
              >
                Comenzar Gratis <ArrowRight className="ml-2 w-5 h-5" />
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="border-blue-500/50 text-blue-400 hover:bg-blue-500/10"
                onClick={() => document.getElementById('features').scrollIntoView({ behavior: 'smooth' })}
              >
                Saber Más
              </Button>
            </div>

            <div className="grid grid-cols-3 gap-4 pt-8 border-t border-slate-800">
              <div>
                <p className="text-2xl font-bold text-white">500+</p>
                <p className="text-sm text-slate-400">Usuarios activos</p>
              </div>
              <div>
                <p className="text-2xl font-bold text-white">1000+</p>
                <p className="text-sm text-slate-400">Viajes completados</p>
              </div>
              <div>
                <p className="text-2xl font-bold text-white">24/7</p>
                <p className="text-sm text-slate-400">Soporte disponible</p>
              </div>
            </div>
          </div>

          {/* Right 3D viewer */}
          <div className="h-96 md:h-[500px] lg:h-[600px] rounded-2xl overflow-hidden border border-blue-500/30 shadow-2xl shadow-blue-500/20 bg-slate-900">
            <TruckViewer3D />
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="relative py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="text-center space-y-4 mb-16">
            <h2 className="text-4xl md:text-5xl font-bold text-white">Características Principales</h2>
            <p className="text-xl text-slate-400 max-w-2xl mx-auto">
              Herramientas modernas diseñadas para simplificar la logística de carga
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {/* Feature 1 */}
            <div className="group p-8 rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900/50 to-slate-800/20 hover:border-blue-500/50 transition-all duration-300">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-cyan-600 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition">
                <MapPin className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Geolocalización en Tiempo Real</h3>
              <p className="text-slate-400">Rastreo exacto de camiones y rutas optimizadas automáticamente.</p>
            </div>

            {/* Feature 2 */}
            <div className="group p-8 rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900/50 to-slate-800/20 hover:border-blue-500/50 transition-all duration-300">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-cyan-600 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition">
                <TrendingUp className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Precios Dinámicos</h3>
              <p className="text-slate-400">Cálculo automático basado en distancia, peso y tarifa actual.</p>
            </div>

            {/* Feature 3 */}
            <div className="group p-8 rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900/50 to-slate-800/20 hover:border-blue-500/50 transition-all duration-300">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-cyan-600 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition">
                <Shield className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Seguridad Garantizada</h3>
              <p className="text-slate-400">Autenticación de 2FA, auditoría completa de transacciones.</p>
            </div>

            {/* Feature 4 */}
            <div className="group p-8 rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900/50 to-slate-800/20 hover:border-blue-500/50 transition-all duration-300">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-cyan-600 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition">
                <Zap className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Instant Matching</h3>
              <p className="text-slate-400">Conecta usuarios y choferes en segundos usando IA.</p>
            </div>

            {/* Feature 5 */}
            <div className="group p-8 rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900/50 to-slate-800/20 hover:border-blue-500/50 transition-all duration-300">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-cyan-600 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition">
                <Users className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Comunidad Confiable</h3>
              <p className="text-slate-400">Sistema de ratings y reseñas verificadas.</p>
            </div>

            {/* Feature 6 */}
            <div className="group p-8 rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900/50 to-slate-800/20 hover:border-blue-500/50 transition-all duration-300">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-cyan-600 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition">
                <Globe className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Disponible 24/7</h3>
              <p className="text-slate-400">Plataforma siempre activa con soporte inmediato.</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section id="pricing" className="relative py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="relative rounded-2xl border border-blue-500/30 bg-gradient-to-br from-blue-600/10 via-slate-900/50 to-cyan-600/10 p-12 md:p-16 overflow-hidden">
            {/* Background glow */}
            <div className="absolute inset-0 bg-gradient-to-r from-blue-500/20 via-transparent to-cyan-500/20 blur-3xl" />

            <div className="relative z-10 text-center space-y-8">
              <div className="space-y-4">
                <h2 className="text-4xl md:text-5xl font-bold text-white">¿Listo para transformar tu logística?</h2>
                <p className="text-xl text-slate-400 max-w-2xl mx-auto">
                  Únete a cientos de usuarios que ya optimizan sus entregas con NEXVIA
                </p>
              </div>

              <div className="flex flex-col sm:flex-row gap-4 justify-center">
                <Button
                  size="lg"
                  className="bg-gradient-to-r from-blue-600 to-cyan-600 hover:from-blue-700 hover:to-cyan-700 text-white text-base"
                  onClick={() => navigate('/register')}
                >
                  Registrarse Gratis <ArrowRight className="ml-2 w-5 h-5" />
                </Button>
                <Button
                  size="lg"
                  variant="outline"
                  className="border-blue-500/50 text-blue-400 hover:bg-blue-500/10"
                  onClick={() => navigate('/login')}
                >
                  Tengo Cuenta
                </Button>
              </div>

              <p className="text-sm text-slate-500">
                No se requiere tarjeta de crédito. Comienza en 2 minutos.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-slate-800 bg-slate-950 py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-8 mb-8">
          <div>
            <h4 className="font-bold text-white mb-4">NEXVIA</h4>
            <ul className="space-y-2 text-sm text-slate-400">
              <li>
                <a href="#" className="hover:text-white transition">
                  Acerca de
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-white transition">
                  Blog
                </a>
              </li>
            </ul>
          </div>
          <div>
            <h4 className="font-bold text-white mb-4">Producto</h4>
            <ul className="space-y-2 text-sm text-slate-400">
              <li>
                <a href="#features" className="hover:text-white transition">
                  Features
                </a>
              </li>
              <li>
                <a href="#pricing" className="hover:text-white transition">
                  Precios
                </a>
              </li>
            </ul>
          </div>
          <div>
            <h4 className="font-bold text-white mb-4">Legal</h4>
            <ul className="space-y-2 text-sm text-slate-400">
              <li>
                <a href="#" className="hover:text-white transition">
                  Privacidad
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-white transition">
                  Términos
                </a>
              </li>
            </ul>
          </div>
          <div>
            <h4 className="font-bold text-white mb-4">Contacto</h4>
            <p className="text-sm text-slate-400">support@nexvia.com</p>
          </div>
        </div>

        <div className="border-t border-slate-800 pt-8 text-center text-sm text-slate-500">
          <p>© 2026 NEXVIA. Todos los derechos reservados.</p>
        </div>
      </footer>
    </div>
  );
}
