import { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, useMapEvents, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

function RouteLine({ origen, destino }) {
  const [route, setRoute] = useState(null);

  useEffect(() => {
    if (!origen || !destino) {
      setRoute(null);
      return;
    }
    let cancelled = false;
    fetch(
      `https://router.project-osrm.org/route/v1/driving/${origen.lng},${origen.lat};${destino.lng},${destino.lat}?overview=full&geometries=geojson`
    )
      .then((res) => res.json())
      .then((data) => {
        if (!cancelled && data.routes?.[0]?.geometry?.coordinates) {
          const coords = data.routes[0].geometry.coordinates.map(([lng, lat]) => [lat, lng]);
          setRoute(coords);
        }
      })
      .catch(() => setRoute(null));
    return () => {
      cancelled = true;
    };
  }, [origen?.lat, origen?.lng, destino?.lat, destino?.lng]);

  if (!origen || !destino) return null;

  if (route) {
    return <Polyline positions={route} pathOptions={{ color: '#0f766e', weight: 4, opacity: 0.8 }} />;
  }

  return (
    <Polyline
      positions={[[origen.lat, origen.lng], [destino.lat, destino.lng]]}
      pathOptions={{ color: '#0f766e', weight: 3, dashArray: '8 6' }}
    />
  );
}

function MapClickHandler({ onClick }) {
  useMapEvents({
    click: (e) => onClick(e.latlng),
  });
  return null;
}

function MapRecenter({ userLocation, recenterOn }) {
  const map = useMap();
  const target = recenterOn || userLocation;
  useEffect(() => {
    if (target) {
      map.setView([target.lat, target.lng], recenterOn ? 16 : 14, { animate: true });
    }
  }, [target?.lat, target?.lng, map]);
  return null;
}

const userIcon = L.divIcon({
  html: `<div style="position:relative;width:20px;height:20px;">
    <div style="position:absolute;inset:0;background:#3b82f6;border-radius:50%;border:3px solid white;box-shadow:0 0 0 2px rgba(59,130,246,.3),0 2px 6px rgba(0,0,0,.3);"></div>
    <div style="position:absolute;inset:-6px;background:rgba(59,130,246,.2);border-radius:50%;animation:pulse 2s ease-out infinite;"></div>
  </div>
  <style>@keyframes pulse{0%{transform:scale(.8);opacity:.8}100%{transform:scale(2);opacity:0}}</style>`,
  className: '',
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

const truckIcon = L.divIcon({
  html: `<div style="font-size: 26px; filter: drop-shadow(0 2px 4px rgba(0,0,0,.3));">🚚</div>`,
  className: '',
  iconSize: [30, 30],
  iconAnchor: [15, 15],
});

const originIcon = L.divIcon({
  html: `<div style="width:18px;height:18px;background:#059669;border:3px solid white;border-radius:50%;box-shadow:0 2px 6px rgba(0,0,0,.35)"></div>`,
  className: '',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

const destIcon = L.divIcon({
  html: `<div style="width:18px;height:18px;background:#dc2626;border:3px solid white;border-radius:50%;box-shadow:0 2px 6px rgba(0,0,0,.35)"></div>`,
  className: '',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

export default function MapaCamiones({ centro, camiones, origen, destino, onMapClick, userLocation, recenterOn }) {
  return (
    <MapContainer
      center={[centro.centro_lat, centro.centro_lng]}
      zoom={14}
      className="w-full h-full"
      style={{ height: '100%', width: '100%' }}
    >
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; OpenStreetMap'
      />
      <MapClickHandler onClick={onMapClick} />
      <MapRecenter userLocation={userLocation} recenterOn={recenterOn} />
      {userLocation && <Marker position={[userLocation.lat, userLocation.lng]} icon={userIcon} />}
      {camiones.map((c) => (
        <Marker key={c.id} position={[c.lat, c.lng]} icon={truckIcon} />
      ))}
      {origen && <Marker position={[origen.lat, origen.lng]} icon={originIcon} />}
      {destino && <Marker position={[destino.lat, destino.lng]} icon={destIcon} />}
      {origen && destino && <RouteLine origen={origen} destino={destino} />}
    </MapContainer>
  );
}