import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { nexvia } from '@/api/client';
import { useAuth } from '@/lib/AuthContext';
import { Sprout, Truck, Loader2, ArrowLeft } from 'lucide-react';
import DatosCamionForm from '@/components/auth/DatosCamionForm';
import { Button } from '@/components/ui/button';
import { toast } from '@/components/ui/use-toast';

const LOGO_URL = '/icon-mark.png';

export default function Bienvenida() {
  const navigate = useNavigate();
  const { user, checkUserAuth } = useAuth();
  const [paso, setPaso] = useState('elegir');
  const [guardando, setGuardando] = useState(false);
  const [datosCamion, setDatosCamion] = useState({
    transporte_nombre: '',
    transporte_cuit: '',
    chofer_nombre: '',
    chofer_cuit: '',
    patente: '',
    patente_acoplado: '',
    telefono: '',
    capacidad_kg: '',
  });

  const elegirProductor = async () => {
    setGuardando(true);
    try {
      await nexvia.functions.invoke('asignarRol', { role: 'usuario' });
      await checkUserAuth();
      navigate('/', { replace: true });
    } catch (err) {
      toast({ title: 'Error', description: err.message, variant: 'destructive' });
    } finally {
      setGuardando(false);
    }
  };

  const confirmarCamionero = async () => {
    if (!datosCamion.chofer_nombre || !datosCamion.patente) {
      toast({ title: 'Faltan datos', description: 'Completá al menos el nombre del chofer y la patente', variant: 'destructive' });
      return;
    }
    setGuardando(true);
    try {
      await nexvia.functions.invoke('asignarRol', { role: 'chofer' });
      await nexvia.entities.Camion.create({
        ...datosCamion,
        chofer_nombre: datosCamion.chofer_nombre || user?.full_name || '',
        capacidad_kg: Number(datosCamion.capacidad_kg) || 0,
        user_id: user?.id || '',
        lat: -32.4341,
        lng: -63.2433,
        estado: 'disponible',
      });
      await checkUserAuth();
      navigate('/chofer', { replace: true });
    } catch (err) {
      toast({ title: 'Error', description: err.message, variant: 'destructive' });
    } finally {
      setGuardando(false);
    }
  };

  if (paso === 'elegir') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#05090f] px-4">
        <div className="w-full max-w-2xl">
          <div className="text-center mb-10">
            <img src={LOGO_URL} alt="NEXVIA" className="h-20 w-auto mx-auto rounded-2xl mb-6 object-contain" />
            <h1 className="text-3xl font-bold tracking-tight text-white">¡Bienvenido a NEXVIA!</h1>
            <p className="text-stone-400 mt-2">¿Qué tipo de usuario vas a ser?</p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <button
              onClick={elegirProductor}
              disabled={guardando}
              className="flex flex-col items-center gap-3 p-8 rounded-2xl border-2 border-stone-700 bg-stone-900/50 hover:border-emerald-500 hover:bg-emerald-500/5 transition text-left disabled:opacity-50"
            >
              <div className="w-16 h-16 rounded-2xl bg-emerald-500/10 flex items-center justify-center">
                {guardando ? <Loader2 className="w-8 h-8 text-emerald-400 animate-spin" /> : <Sprout className="w-8 h-8 text-emerald-400" />}
              </div>
              <span className="text-lg font-bold text-white">Productor</span>
              <span className="text-sm text-stone-400 text-center">
                Solicito camiones para transportar mi carga. Defino origen, destino y tarifa.
              </span>
            </button>

            <button
              onClick={() => setPaso('camionero')}
              disabled={guardando}
              className="flex flex-col items-center gap-3 p-8 rounded-2xl border-2 border-stone-700 bg-stone-900/50 hover:border-blue-500 hover:bg-blue-500/5 transition text-left disabled:opacity-50"
            >
              <div className="w-16 h-16 rounded-2xl bg-blue-500/10 flex items-center justify-center">
                <Truck className="w-8 h-8 text-blue-400" />
              </div>
              <span className="text-lg font-bold text-white">Camionero</span>
              <span className="text-sm text-stone-400 text-center">
                Cargo los datos de mi camión y acepto los viajes que me convengan según la tarifa.
              </span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#05090f] px-4 py-8">
      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <img src={LOGO_URL} alt="NEXVIA" className="h-16 w-auto mx-auto rounded-2xl mb-4 object-contain" />
          <h1 className="text-2xl font-bold text-white">Datos del camión</h1>
          <p className="text-stone-400 mt-1">Completá los datos de tu vehículo</p>
        </div>

        <div className="bg-white rounded-2xl p-6 space-y-4">
          <button
            onClick={() => setPaso('elegir')}
            className="flex items-center gap-1.5 text-sm text-stone-500 hover:text-stone-700 transition"
          >
            <ArrowLeft className="w-4 h-4" /> Volver
          </button>

          <DatosCamionForm datos={datosCamion} onChange={setDatosCamion} />

          <Button
            onClick={confirmarCamionero}
            disabled={guardando}
            className="w-full h-12 font-medium"
          >
            {guardando ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Guardando...
              </>
            ) : (
              'Comenzar a manejar'
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}