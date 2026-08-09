import { Canvas } from '@react-three/fiber';
import { useGLTF, OrbitControls, Stage } from '@react-three/drei';
import { useEffect, useRef } from 'react';

function TruckModel({ url }) {
  const { scene } = useGLTF(url);
  const groupRef = useRef();

  useEffect(() => {
    if (!groupRef.current) return;

    const handleWheel = (e) => {
      e.preventDefault();
      const deltaY = e.deltaY;
      groupRef.current.rotation.y += deltaY * 0.001;
    };

    window.addEventListener('wheel', handleWheel, { passive: false });
    return () => window.removeEventListener('wheel', handleWheel);
  }, []);

  return (
    <group ref={groupRef}>
      <primitive object={scene} scale={2.5} position={[0, -1, 0]} />
    </group>
  );
}

export default function TruckViewer3D() {
  return (
    <div className="w-full h-full bg-gradient-to-b from-slate-950 via-blue-950 to-slate-900 relative overflow-hidden">
      <Canvas
        camera={{ position: [0, 1, 4], fov: 50 }}
        style={{ width: '100%', height: '100%' }}
        dpr={[1, 2]}
      >
        <color attach="background" args={['#0f172a']} />

        {/* Lighting */}
        <ambientLight intensity={1.2} />
        <directionalLight position={[5, 8, 5]} intensity={1.5} castShadow />
        <directionalLight position={[-5, 4, -5]} intensity={0.8} />
        <pointLight position={[0, 3, 0]} intensity={0.6} />

        {/* Grid background */}
        <gridHelper args={[20, 20, '#1e293b', '#334155']} position={[0, -2, 0]} />

        {/* Truck model */}
        <TruckModel url="/truck-3d.glb" />

        {/* Orbit controls with restrictions */}
        <OrbitControls
          enableZoom={true}
          enablePan={true}
          autoRotate={true}
          autoRotateSpeed={2}
          minDistance={3}
          maxDistance={8}
          minPolarAngle={Math.PI / 3}
          maxPolarAngle={Math.PI * 0.7}
        />
      </Canvas>

      {/* Scroll hint */}
      <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 pointer-events-none">
        <div className="animate-bounce text-center">
          <svg
            className="w-6 h-6 text-blue-400 mx-auto"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
          </svg>
          <p className="text-xs text-blue-300 mt-2 font-medium">Scroll para girar</p>
        </div>
      </div>

      {/* Gradient overlay edges */}
      <div className="absolute inset-0 pointer-events-none bg-gradient-to-t from-slate-900 via-transparent to-slate-900 opacity-30" />
    </div>
  );
}
