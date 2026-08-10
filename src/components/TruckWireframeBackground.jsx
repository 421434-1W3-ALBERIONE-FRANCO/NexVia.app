import { Canvas, useFrame } from '@react-three/fiber';
import { useGLTF } from '@react-three/drei';
import { useRef, useEffect, useState, useMemo } from 'react';
import * as THREE from 'three';

function PerspectiveGrid() {
  const gridRef = useRef();
  const linesGeo = useMemo(() => {
    const points = [];
    const spread = 30;
    const depth = 40;
    const count = 40;

    for (let i = 0; i <= count; i++) {
      const x = (i / count) * spread - spread / 2;
      points.push(new THREE.Vector3(x, 0, -depth / 2));
      points.push(new THREE.Vector3(x, 0, depth / 2));
    }
    for (let i = 0; i <= count; i++) {
      const z = (i / count) * depth - depth / 2;
      points.push(new THREE.Vector3(-spread / 2, 0, z));
      points.push(new THREE.Vector3(spread / 2, 0, z));
    }

    const geo = new THREE.BufferGeometry().setFromPoints(points);
    return geo;
  }, []);

  return (
    <lineSegments ref={gridRef} position={[0, -2.5, 0]} rotation={[0, 0, 0]}>
      <primitive object={linesGeo} attach="geometry" />
      <lineBasicMaterial color="#0e4d6e" transparent opacity={0.4} />
    </lineSegments>
  );
}

function FloatingParticles({ scrollProgress }) {
  const particlesRef = useRef();
  const count = 120;

  const { positions, speeds } = useMemo(() => {
    const pos = new Float32Array(count * 3);
    const spd = new Float32Array(count);
    for (let i = 0; i < count; i++) {
      pos[i * 3] = (Math.random() - 0.5) * 20;
      pos[i * 3 + 1] = (Math.random() - 0.5) * 12;
      pos[i * 3 + 2] = (Math.random() - 0.5) * 10;
      spd[i] = 0.002 + Math.random() * 0.006;
    }
    return { positions: pos, speeds: spd };
  }, []);

  useFrame(({ clock }) => {
    if (!particlesRef.current) return;
    const posArr = particlesRef.current.geometry.attributes.position.array;
    const t = clock.getElapsedTime();
    for (let i = 0; i < count; i++) {
      posArr[i * 3 + 1] += Math.sin(t * speeds[i] * 100 + i) * 0.003;
      posArr[i * 3] += Math.cos(t * speeds[i] * 80 + i * 0.5) * 0.002;
    }
    particlesRef.current.geometry.attributes.position.needsUpdate = true;
  });

  return (
    <points ref={particlesRef}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          array={positions}
          count={count}
          itemSize={3}
        />
      </bufferGeometry>
      <pointsMaterial
        color="#67e8f9"
        size={0.04}
        transparent
        opacity={0.6}
        sizeAttenuation
      />
    </points>
  );
}

function thinEdgeGeometry(edgeGeo, removeFraction) {
  const src = edgeGeo.attributes.position.array;
  const segmentCount = src.length / 6; // 2 vertices * 3 floats per line segment
  const step = Math.round(1 / removeFraction);
  const kept = [];
  for (let i = 0; i < segmentCount; i++) {
    if (step > 0 && i % step === step - 1) continue;
    const base = i * 6;
    for (let k = 0; k < 6; k++) kept.push(src[base + k]);
  }
  const thinned = new THREE.BufferGeometry();
  thinned.setAttribute('position', new THREE.Float32BufferAttribute(kept, 3));
  return thinned;
}

function WireframeTruck({ url, scrollProgress }) {
  const { scene } = useGLTF(url);
  const groupRef = useRef();
  const startY = THREE.MathUtils.degToRad(50);
  const targetRotation = useRef({ x: 0, y: startY });

  const { mainGroup, glowGroup } = useMemo(() => {
    const main = new THREE.Group();
    const glow = new THREE.Group();

    const mainMat = new THREE.LineBasicMaterial({
      color: 0xffffff,
      transparent: true,
      opacity: 0.7,
    });
    const glowMat = new THREE.LineBasicMaterial({
      color: 0x67e8f9,
      transparent: true,
      opacity: 0.25,
    });

    scene.updateMatrixWorld(true);
    scene.traverse((child) => {
      if (!child.isMesh || !child.geometry) return;
      const geo = child.geometry.clone();
      geo.applyMatrix4(child.matrixWorld);
      const fullEdgeGeo = new THREE.EdgesGeometry(geo, 1);
      const edgeGeo = thinEdgeGeometry(fullEdgeGeo, 0.1);

      main.add(new THREE.LineSegments(edgeGeo, mainMat));

      const glowEdge = edgeGeo.clone();
      const glowLine = new THREE.LineSegments(glowEdge, glowMat);
      glowLine.scale.set(1.03, 1.03, 1.03);
      glow.add(glowLine);
    });

    return { mainGroup: main, glowGroup: glow };
  }, [scene]);

  useEffect(() => {
    const maxX = THREE.MathUtils.degToRad(4);
    // Starts at a 50° diagonal on load, straightens to face front as the user scrolls
    targetRotation.current.y = startY * (1 - scrollProgress);
    targetRotation.current.x = scrollProgress * maxX;
  }, [scrollProgress, startY]);

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
    <group ref={groupRef} scale={3.5} position={[0, -0.8, 0]} rotation={[0, startY, 0]}>
      <primitive object={mainGroup} />
      <primitive object={glowGroup} />
    </group>
  );
}

export default function TruckWireframeBackground() {
  const [scrollProgress, setScrollProgress] = useState(0);

  useEffect(() => {
    const handleScroll = () => {
      // Completes the rotation within roughly the first viewport of scrolling,
      // then holds steady while the truck stays fixed behind the rest of the page.
      const progress = Math.min(window.scrollY / (window.innerHeight * 1.2), 1);
      setScrollProgress(progress);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        zIndex: 0,
        pointerEvents: 'none',
      }}
    >
      <Canvas
        camera={{ position: [0, 2, 6], fov: 50 }}
        dpr={[1, 1.5]}
        gl={{
          antialias: true,
          alpha: false,
          powerPreference: 'high-performance',
        }}
      >
        <color attach="background" args={['#030712']} />
        <PerspectiveGrid />
        <FloatingParticles scrollProgress={scrollProgress} />
        <WireframeTruck url="/truck-3d.glb" scrollProgress={scrollProgress} />
      </Canvas>
    </div>
  );
}
