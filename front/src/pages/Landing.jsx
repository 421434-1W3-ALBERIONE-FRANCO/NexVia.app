import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/lib/AuthContext';
import TruckWireframeBackground from '@/components/TruckWireframeBackground';
import { Button } from '@/components/ui/button';
import { ArrowRight, ChevronDown } from 'lucide-react';

const LOGO_URL = '/icon-mark.png';

const FEATURES = [
  {
    icon: '/icons/geolocalizacion.webp',
    title: 'Geolocalización en Tiempo Real',
    desc: 'Rastreo exacto de camiones y rutas optimizadas automáticamente.'
  },
  {
    icon: '/icons/precios.webp',
    title: 'Precios Dinámicos',
    desc: 'Cálculo automático basado en distancia, peso y tarifa actual.'
  },
  {
    icon: '/icons/seguridad.webp',
    title: 'Seguridad Garantizada',
    desc: 'Autenticación de 2FA, auditoría completa de transacciones.'
  },
  {
    icon: '/icons/matching.webp',
    title: 'Instant Matching',
    desc: 'Conecta usuarios y choferes en segundos usando IA.'
  },
  {
    icon: '/icons/comunidad.webp',
    title: 'Comunidad Confiable',
    desc: 'Sistema de ratings y reseñas verificadas.'
  },
  {
    icon: '/icons/disponibilidad.webp',
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

  return (
    <div className="w-full overflow-x-hidden bg-gray-950" ref={containerRef}>
      {/* 3D Wireframe Truck Background — fixed across the whole page */}
      <TruckWireframeBackground />

      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 z-50 backdrop-blur-md bg-gray-950/70 border-b border-cyan-500/20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          {/* Logo */}
          <div className="flex items-center gap-2 group cursor-pointer" onClick={() => navigate('/')}>
            <img
              src={LOGO_URL}
              alt="NEXVIA"
              className="h-8 sm:h-10 w-auto rounded-lg object-contain transform group-hover:scale-110 transition-transform"
            />
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
        <div className="relative z-10 max-w-4xl mx-auto">
          {/* Title */}
          <div className="space-y-4 text-center">
            <h1
              className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-bold leading-tight bg-gradient-to-br from-cyan-400 via-teal-400 to-emerald-400 bg-clip-text text-transparent transition-[filter] duration-300 hover:drop-shadow-[0_0_25px_rgba(45,212,191,0.85)] cursor-default"
            >
              Conectamos carga e impulsamos el futuro
            </h1>

            <p className="text-base sm:text-lg text-slate-400 max-w-2xl mx-auto text-center">
              Plataforma de transporte 100% digital
            </p>
          </div>

          {/* CTA Buttons — pushed down below the truck */}
          <div className="flex flex-col sm:flex-row gap-3 sm:gap-4 justify-center mt-40 sm:mt-56">
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
          <div className="grid grid-cols-3 gap-3 sm:gap-6 pt-6 sm:pt-8">
            <div className="text-center">
              <p className="text-xl sm:text-2xl font-bold text-cyan-400">500+</p>
              <p className="text-xs sm:text-sm text-slate-400">Usuarios</p>
            </div>
            <div className="text-center">
              <p className="text-xl sm:text-2xl font-bold text-cyan-400">1000+</p>
              <p className="text-xs sm:text-sm text-slate-400">Viajes</p>
            </div>
            <div className="text-center">
              <p className="text-xl sm:text-2xl font-bold text-cyan-400">24/7</p>
              <p className="text-xs sm:text-sm text-slate-400">Soporte</p>
            </div>
          </div>

          {/* Scroll indicator */}
          <div className="pt-8 sm:pt-12 flex justify-center animate-bounce">
            <ChevronDown className="w-6 h-6 text-cyan-400" />
          </div>
        </div>
      </section>

      {/* Features Carousel */}
      <section id="carousel" className="relative py-16 sm:py-20 px-4 sm:px-6">
        <div className="max-w-3xl mx-auto">
          <div className="text-center mb-12 sm:mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-white mb-2">Características</h2>
            <p className="text-sm sm:text-base text-slate-400">
              Herramientas diseñadas para simplificar tu logística
            </p>
          </div>

          {/* Carousel Card */}
          <div className="relative h-64 sm:h-80 rounded-2xl border border-cyan-500/15 bg-gradient-to-br from-blue-950/25 to-cyan-950/10 backdrop-blur-sm overflow-hidden group">
            {/* Card content */}
            <div className="relative z-10 h-full flex flex-col items-center justify-center text-center p-5 sm:p-7">
              <div
                className="w-20 sm:w-24 h-20 sm:h-24 rounded-full mb-4 sm:mb-6 group-hover:scale-110 transition-transform duration-300"
                style={{
                  backgroundImage: `url(${currentFeatureData.icon})`,
                  backgroundSize: '320% 180%',
                  backgroundPosition: '48% 29%',
                }}
              />

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
        </div>
      </section>

      {/* CTA Section */}
      <section id="cta" className="relative py-16 sm:py-20 px-4 sm:px-6">
        <div className="max-w-2xl mx-auto">
          <div className="rounded-2xl border border-cyan-500/15 bg-gradient-to-br from-cyan-600/8 via-blue-950/20 to-green-600/5 backdrop-blur-sm p-6 sm:p-10 overflow-hidden relative">
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
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="relative border-t border-blue-900/50 bg-gradient-to-b from-gray-950 to-blue-950 py-8 sm:py-12 px-4 sm:px-6">
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
