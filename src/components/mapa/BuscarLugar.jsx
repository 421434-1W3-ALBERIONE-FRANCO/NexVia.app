import { useState, useEffect, useRef } from 'react';
import { Search, Loader2, MapPin, Star, Plus, X, Mic } from 'lucide-react';
import { base44 } from '@/api/base44Client';

export default function BuscarLugar({ label, onSelect, puntoActual }) {
  const [query, setQuery] = useState('');
  const [resultados, setResultados] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [mostrar, setMostrar] = useState(false);
  const [guardados, setGuardados] = useState([]);
  const [guardando, setGuardando] = useState(false);
  const [nombreGuardar, setNombreGuardar] = useState('');
  const [mostrarGuardar, setMostrarGuardar] = useState(false);
  const [escuchando, setEscuchando] = useState(false);
  const debounceRef = useRef(null);
  const containerRef = useRef(null);

  const iniciarVoz = () => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return;
    const recognition = new SpeechRecognition();
    recognition.lang = 'es-AR';
    recognition.interimResults = false;
    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      setQuery(transcript);
      setMostrar(true);
      buscar(transcript);
    };
    recognition.onend = () => setEscuchando(false);
    recognition.onerror = () => setEscuchando(false);
    recognition.start();
    setEscuchando(true);
  };

  useEffect(() => {
    cargarGuardados();
  }, []);

  useEffect(() => {
    const handler = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setMostrar(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const cargarGuardados = async () => {
    try {
      const data = await base44.entities.LugarGuardado.list('-updated_date', 50);
      setGuardados(data);
    } catch {
      // ignore
    }
  };

  const buscar = async (q) => {
    if (q.trim().length < 3) {
      setResultados([]);
      return;
    }
    setBuscando(true);
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(q)}&limit=5&addressdetails=1`
      );
      const data = await res.json();
      setResultados(data);
    } catch {
      setResultados([]);
    } finally {
      setBuscando(false);
    }
  };

  const handleInput = (val) => {
    setQuery(val);
    setMostrar(true);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => buscar(val), 500);
  };

  const seleccionarResultado = (r) => {
    const point = { lat: parseFloat(r.lat), lng: parseFloat(r.lon), nombre: r.display_name.split(',')[0] };
    onSelect(point);
    setQuery(point.nombre);
    setMostrar(false);
    setResultados([]);
  };

  const seleccionarGuardado = (g) => {
    const point = { lat: g.lat, lng: g.lng, nombre: g.nombre };
    onSelect(point);
    setQuery(g.nombre);
    setMostrar(false);
  };

  const guardarLugar = async () => {
    if (!nombreGuardar.trim() || !puntoActual) return;
    setGuardando(true);
    try {
      await base44.entities.LugarGuardado.create({
        nombre: nombreGuardar.trim(),
        lat: puntoActual.lat,
        lng: puntoActual.lng,
        tipo: 'otro',
      });
      setNombreGuardar('');
      setMostrarGuardar(false);
      cargarGuardados();
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div ref={containerRef} className="relative">
      <div className="flex items-center gap-2 px-3 py-2 rounded-lg border border-stone-300 bg-white focus-within:border-emerald-500 focus-within:ring-2 focus-within:ring-emerald-100">
        <Search className="w-4 h-4 text-stone-400 shrink-0" />
        <input
          type="text"
          placeholder={`Buscar ${label.toLowerCase()}...`}
          value={query}
          onChange={(e) => handleInput(e.target.value)}
          onFocus={() => setMostrar(true)}
          className="flex-1 outline-none text-sm bg-transparent min-w-0"
        />
        {buscando ? (
          <Loader2 className="w-4 h-4 animate-spin text-stone-400 shrink-0" />
        ) : escuchando ? (
          <Mic className="w-4 h-4 text-red-500 animate-pulse shrink-0" />
        ) : (
          <button
            onClick={iniciarVoz}
            title="Buscar por voz"
            className="text-stone-400 hover:text-emerald-600 transition shrink-0"
          >
            <Mic className="w-4 h-4" />
          </button>
        )}
      </div>

      {mostrar && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl border border-stone-200 shadow-lg z-50 max-h-72 overflow-y-auto">
          {resultados.length > 0 && (
            <div className="py-1">
              {resultados.map((r, i) => (
                <button
                  key={i}
                  onClick={() => seleccionarResultado(r)}
                  className="w-full flex items-start gap-2 px-3 py-2 hover:bg-stone-50 transition text-left"
                >
                  <MapPin className="w-4 h-4 text-stone-400 shrink-0 mt-0.5" />
                  <span className="text-sm text-stone-700 line-clamp-2">{r.display_name}</span>
                </button>
              ))}
            </div>
          )}

          {!buscando && query.trim().length >= 3 && resultados.length === 0 && (
            <div className="px-3 py-3 text-center">
              <p className="text-sm text-stone-500 mb-2">No se encontró "{query}"</p>
              <button
                onClick={() => setMostrarGuardar(true)}
                className="text-sm text-emerald-600 font-medium hover:underline"
              >
                Guardar como lugar personalizado
              </button>
            </div>
          )}

          {query.trim().length < 3 && (
            <div className="px-3 py-2">
              <p className="text-xs text-stone-400 mb-1">Escribí al menos 3 letras para buscar</p>
            </div>
          )}

          {guardados.length > 0 && (
            <div className="border-t border-stone-100 py-1">
              <p className="px-3 py-1 text-xs font-semibold text-stone-400 uppercase tracking-wide">Mis lugares guardados</p>
              {guardados.map((g) => (
                <button
                  key={g.id}
                  onClick={() => seleccionarGuardado(g)}
                  className="w-full flex items-center gap-2 px-3 py-2 hover:bg-stone-50 transition text-left"
                >
                  <Star className="w-4 h-4 text-amber-400 shrink-0" />
                  <span className="text-sm text-stone-700">{g.nombre}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {mostrarGuardar && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl border border-stone-200 shadow-lg z-50 p-4 space-y-3">
          <div className="flex items-center justify-between">
            <p className="text-sm font-semibold text-stone-900">Guardar lugar personalizado</p>
            <button onClick={() => setMostrarGuardar(false)} className="text-stone-400 hover:text-stone-600">
              <X className="w-4 h-4" />
            </button>
          </div>
          <input
            type="text"
            placeholder="Nombre del lugar (ej: Hacienda La Esperanza)"
            value={nombreGuardar}
            onChange={(e) => setNombreGuardar(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-stone-300 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 outline-none text-sm"
            autoFocus
          />
          {puntoActual ? (
            <p className="text-xs text-stone-500">
              Se guardará en: {puntoActual.lat.toFixed(4)}, {puntoActual.lng.toFixed(4)}
              <br />
              <span className="text-stone-400">(Elegí el punto en el mapa primero)</span>
            </p>
          ) : (
            <p className="text-xs text-amber-600">
              Tocá el mapa para elegir la ubicación del lugar antes de guardarlo
            </p>
          )}
          <button
            onClick={guardarLugar}
            disabled={!nombreGuardar.trim() || !puntoActual || guardando}
            className="w-full py-2 rounded-lg bg-emerald-600 text-white text-sm font-semibold disabled:opacity-40 hover:bg-emerald-700 transition flex items-center justify-center gap-2"
          >
            {guardando ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            Guardar lugar
          </button>
        </div>
      )}
    </div>
  );
}