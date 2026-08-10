import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/lib/AuthContext';
import TruckWireframeBackground from '@/components/TruckWireframeBackground';
import { Button } from '@/components/ui/button';
import { ArrowRight, MapPin, TrendingUp, Shield, Zap, Users, Globe, ChevronDown } from 'lucide-react';

const FEATURES = [
  {
    icon: MapPin,
    title: 'Geolocalización en Tiempo Real',
    desc: 'Rastreo exacto de camiones y rutas optimizadas automáticamente.'
  },
  {
    icon: TrendingUp,
    title: 'Precios Dinámicos',
    desc: 'Cálculo automático basado en distancia, peso y tarifa actual.'
  },
  {
    icon: Shield,
    title: 'Seguridad Garantizada',
    desc: 'Autenticación de 2FA, auditoría completa de transacciones.'
  },
  {
    icon: Zap,
    title: 'Instant Matching',
    desc: 'Conecta usuarios y choferes en segundos usando IA.'
  },
  {
    icon: Users,
    title: 'Comunidad Confiable',
    desc: 'Sistema de ratings y reseñas verificadas.'
  },
  {
    icon: Globe,
    title: 'Disponible 24/7',
    desc: 'Plataforma siempre activa con soporte inmediato.'
  }
];

export default function Landing() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [currentFeature, setCurrentFeature] = useState(0);
  const containerRef = useRef(null);

  // Redirect if already authenticated
  useEffect(() => {
    if (user) {
      if (user.role === 'admin') navigate('/admin');
      else if (user.role === 'chofer') navigate('/chofer');
      else navigate('/home');
    }
  }, [user, navigate]);

  // Carousel auto-advance
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentFeature((prev) => (prev + 1) % FEATURES.length);
    }, 3000);
    return () => clearInterval(timer);
  }, []);

  const currentFeatureData = FEATURES[currentFeature];
  const FeatureIcon = currentFeatureData.icon;

  return (
    <div className="w-full overflow-x-hidden bg-gradient-to-b from-gray-950 via-blue-950 to-gray-950" ref={containerRef}>
      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 z-50 backdrop-blur-md bg-gradient-to-b from-gray-950/80 to-transparent border-b border-cyan-500/20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          {/* Responsive Logo */}
          <div className="flex items-center gap-1 sm:gap-2 group cursor-pointer" onClick={() => navigate('/')}>
            <div className="w-6 sm:w-8 h-6 sm:h-8 bg-gradient-to-br from-cyan-400 via-blue-500 to-blue-900 rounded-lg flex items-center justify-center transform group-hover:scale-110 transition-transform">
              <span className="text-white font-bold text-xs sm:text-sm">NX</span>
            </div>
            <div className="flex flex-col leading-tight">
              <span className="text-sm sm:text-lg font-bold bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-transparent">
                NEX
              </span>
              <span className="text-xs sm:text-sm font-bold text-blue-400">VIA</span>
            </div>
          </div>

          <div className="hidden md:flex items-center gap-8">
            <a href="#carousel" className="text-slate-300 hover:text-cyan-400 transition">
              Características
            </a>
            <a href="#cta" className="text-slate-300 hover:text-cyan-400 transition">
              Comenzar
            </a>
          </div>

          <div className="flex items-center gap-2 sm:gap-3">
            <Button
              variant="outline"
              size="sm"
              className="border-cyan-500/50 text-cyan-400 hover:bg-cyan-500/10 text-xs sm:text-sm"
              onClick={() => navigate('/login')}
            >
              Ingresar
            </Button>
            <Button
              size="sm"
              className="bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-600 hover:to-blue-700 text-xs sm:text-sm"
              onClick={() => navigate('/register')}
            >
              Registrarse
            </Button>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="relative min-h-screen pt-32 pb-10 px-4 sm:px-6 overflow-hidden">
        {/* 3D Wireframe Truck Background */}
        <TruckWireframeBackground />

        <div className="relative z-10 text-center space-y-8 max-w-4xl mx-auto">
          {/* Responsive Title */}
          <div className="space-y-4">
            <div className="inline-block px-3 sm:px-4 py-2 rounded-lg bg-cyan-500/20 border border-cyan-500/50">
              <span className="text-xs sm:text-sm font-medium text-cyan-300">Bienvenido a NEXVIA</span>
            </div>

            <h1 className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-bold leading-tight space-y-2 sm:space-y-3">
              <div className="text-white">Conectamos</div>
              <div className="bg-gradient-to-r from-cyan-400 via-blue-500 to-green-500 bg-clip-text text-transparent">
                carga impulsamos
              </div>
              <div className="text-white">el futuro</div>
            </h1>

            <p className="text-base sm:text-lg text-slate-400 max-w-2xl mx-auto">
              Plataforma de transporte 100% digital
            </p>
          </div>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-3 sm:gap-4 justify-center">
            <Button
              size="lg"
              className="bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-600 hover:to-blue-700 text-white w-full sm:w-auto"
              onClick={() => navigate('/register')}
            >
              Comenzar Gratis <ArrowRight className="ml-2 w-4 h-4" />
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="border-cyan-500/50 text-cyan-400 hover:bg-cyan-500/10 w-full sm:w-auto"
              onClick={() => document.getElementById('carousel').scrollIntoView({ behavior: 'smooth' })}
            >
              Ver Características
            </Button>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-3 gap-3 sm:gap-6 pt-6 sm:pt-8 border-t border-blue-900/50">
            <div>
              <p className="text-xl sm:text-2xl font-bold text-cyan-400">500+</p>
              <p className="text-xs sm:text-sm text-slate-400">Usuarios</p>
            </div>
            <div>
              <p className="text-xl sm:text-2xl font-bold text-cyan-400">1000+</p>
              <p className="text-xs sm:text-sm text-slate-400">Viajes</p>
            </div>
            <div>
              <p className="text-xl sm:text-2xl font-bold text-cyan-400">24/7</p>
              <p className="text-xs sm:text-sm text-slate-400">Soporte</p>
            </div>
          </div>

          {/* Scroll indicator */}
          <div className="pt-8 sm:pt-12 animate-bounce">
            <ChevronDown className="w-6 h-6 text-cyan-400 mx-auto" />
          </div>
        </div>
      </section>

      {/* Features Carousel */}
      <section id="carousel" className="relative py-16 sm:py-20 px-4 sm:px-6">
        <div className="max-w-3xl mx-auto">
          <div className="text-center mb-12 sm:mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-white mb-2">Características</h2>
            <p className="text-sm sm:text-base text-slate-400">
              Herramientas diseñadas para simplificar tu logística ({currentFeature + 1}/{FEATURES.length})
            </p>
          </div>

          {/* Carousel Card */}
          <div className="relative h-80 sm:h-96 rounded-2xl border border-cyan-500/30 bg-gradient-to-br from-blue-950/50 to-cyan-950/30 overflow-hidden group">
            {/* Animated background */}
            <div className="absolute inset-0 bg-gradient-to-r from-transparent via-cyan-500/10 to-transparent animate-pulse" />

            {/* Card content */}
            <div className="relative z-10 h-full flex flex-col items-center justify-center text-center p-6 sm:p-8">
              <div className="w-16 sm:w-20 h-16 sm:h-20 bg-gradient-to-br from-cyan-400 to-blue-600 rounded-2xl flex items-center justify-center mb-6 sm:mb-8 group-hover:scale-110 transition-transform duration-300">
                <FeatureIcon className="w-8 sm:w-10 h-8 sm:h-10 text-white" />
              </div>

              <h3 className="text-2xl sm:text-3xl font-bold text-white mb-3 sm:mb-4">
                {currentFeatureData.title}
              </h3>

              <p className="text-base sm:text-lg text-slate-300 max-w-xl">
                {currentFeatureData.desc}
              </p>
            </div>

            {/* Progress indicators */}
            <div className="absolute bottom-4 sm:bottom-6 left-1/2 transform -translate-x-1/2 flex gap-2 z-20">
              {FEATURES.map((_, idx) => (
                <button
                  key={idx}
                  onClick={() => setCurrentFeature(idx)}
                  className={`h-2 rounded-full transition-all duration-300 ${
                    idx === currentFeature
                      ? 'bg-cyan-500 w-8'
                      : 'bg-cyan-500/30 w-2 hover:bg-cyan-500/50'
                  }`}
                />
              ))}
            </div>
          </div>

          {/* Feature counter */}
          <div className="text-center mt-8 sm:mt-10">
            <p className="text-sm text-slate-400">
              Cambia automáticamente cada 3 segundos
            </p>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section id="cta" className="relative py-16 sm:py-20 px-4 sm:px-6">
        <div className="max-w-2xl mx-auto">
          <div className="rounded-2xl border border-cyan-500/30 bg-gradient-to-br from-cyan-600/15 via-blue-950/40 to-green-600/10 p-8 sm:p-12 overflow-hidden relative">
            {/* Animated background */}
            <div className="absolute inset-0 bg-gradient-to-r from-cyan-500/20 via-transparent to-green-500/20 blur-3xl" />

            <div className="relative z-10 text-center space-y-6">
              <div className="space-y-3">
                <h2 className="text-2xl sm:text-4xl font-bold text-white">
                  ¿Listo para transformar?
                </h2>
                <p className="text-base sm:text-lg text-slate-300">
                  Únete ahora y optimiza tus entregas
                </p>
              </div>

              <div className="flex flex-col sm:flex-row gap-3 sm:gap-4 justify-center">
                <Button
                  size="lg"
                  className="bg-gradient-to-r from-cyan-500 to-green-600 hover:from-cyan-600 hover:to-green-700 text-white w-full sm:w-auto"
                  onClick={() => navigate('/register')}
                >
                  Registrarse Gratis
                </Button>
                <Button
                  size="lg"
                  variant="outline"
                  className="border-cyan-500/50 text-cyan-400 hover:bg-cyan-500/10 w-full sm:w-auto"
                  onClick={() => navigate('/login')}
                >
                  Ya tengo cuenta
                </Button>
              </div>

              <p className="text-xs sm:text-sm text-slate-400">
                Sin tarjeta de crédito • Comienza en 2 minutos
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-blue-900/50 bg-gradient-to-b from-gray-950 to-blue-950 py-8 sm:py-12 px-4 sm:px-6">
        <div className="max-w-4xl mx-auto">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-6 sm:gap-8 mb-8">
            <div>
              <h4 className="font-bold text-white mb-3 text-sm">Producto</h4>
              <ul className="space-y-2 text-xs sm:text-sm text-slate-400">
                <li>
                  <a href="#carousel" className="hover:text-cyan-400 transition">
                    Características
                  </a>
                </li>
                <li>
                  <a href="#cta" className="hover:text-cyan-400 transition">
                    Comenzar
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-bold text-white mb-3 text-sm">Legal</h4>
              <ul className="space-y-2 text-xs sm:text-sm text-slate-400">
                <li>
                  <a href="#" className="hover:text-cyan-400 transition">
                    Privacidad
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-cyan-400 transition">
                    Términos
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-bold text-white mb-3 text-sm">Soporte</h4>
              <ul className="space-y-2 text-xs sm:text-sm text-slate-400">
                <li>
                  <a href="#" className="hover:text-cyan-400 transition">
                    Ayuda
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-cyan-400 transition">
                    Contacto
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-bold text-white mb-3 text-sm">Social</h4>
              <ul className="space-y-2 text-xs sm:text-sm text-slate-400">
                <li>
                  <a href="#" className="hover:text-cyan-400 transition">
                    LinkedIn
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-cyan-400 transition">
                    Twitter
                  </a>
                </li>
              </ul>
            </div>
          </div>

          <div className="border-t border-blue-900/50 pt-6 sm:pt-8 text-center text-xs sm:text-sm text-slate-500">
            <p>© 2026 NEXVIA. Conectamos carga, impulsamos el futuro.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
