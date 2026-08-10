import { Canvas, useFrame } from '@react-three/fiber';
import { useGLTF } from '@react-three/drei';
import { useRef, useEffect, useState, useMemo } from 'react';
import * as THREE from 'three';

function WireframeTruck({ url, scrollProgress }) {
  const { scene } = useGLTF(url);
  const groupRef = useRef();
  const targetRotation = useRef({ x: 0, y: -0.4 });

  const wireframeGroup = useMemo(() => {
    const group = new THREE.Group();
    const lineMat = new THREE.LineBasicMaterial({
      color: 0x22d3ee,
      transparent: true,
      opacity: 0.14,
    });

    scene.updateMatrixWorld(true);

    scene.traverse((child) => {
      if (!child.isMesh || !child.geometry) return;

      const geo = child.geometry.clone();
      geo.applyMatrix4(child.matrixWorld);

      const edges = new THREE.EdgesGeometry(geo, 1);
      const line = new THREE.LineSegments(edges, lineMat);
      group.add(line);
    });

    return group;
  }, [scene]);

  useEffect(() => {
    const baseY = -0.4;
    const maxY = THREE.MathUtils.degToRad(15);
    const maxX = THREE.MathUtils.degToRad(4);
    targetRotation.current.y = baseY + scrollProgress * maxY;
    targetRotation.current.x = scrollProgress * maxX;
  }, [scrollProgress]);

  useFrame(() => {
    if (!groupRef.current) return;
    groupRef.current.rotation.y = THREE.MathUtils.lerp(
      groupRef.current.rotation.y,
      targetRotation.current.y,
      0.05
    );
    groupRef.current.rotation.x = THREE.MathUtils.lerp(
      groupRef.current.rotation.x,
      targetRotation.current.x,
      0.05
    );
  });

  return (
    <group ref={groupRef} scale={2.8} position={[3.2, -1.6, 0]}>
      <primitive object={wireframeGroup} />
    </group>
  );
}

export default function TruckWireframeBackground() {
  const [scrollProgress, setScrollProgress] = useState(0);

  useEffect(() => {
    const handleScroll = () => {
      const heroHeight = window.innerHeight;
      const progress = Math.min(window.scrollY / heroHeight, 1);
      setScrollProgress(progress);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div
      className="absolute inset-0 z-0"
      style={{ pointerEvents: 'none' }}
    >
      <Canvas
        camera={{ position: [0, 1.5, 6], fov: 45 }}
        dpr={[1, 1.5]}
        gl={{
          antialias: true,
          alpha: true,
          powerPreference: 'high-performance',
        }}
        style={{ background: 'transparent' }}
      >
        <WireframeTruck url="/truck-3d.glb" scrollProgress={scrollProgress} />
      </Canvas>
    </div>
  );
}
