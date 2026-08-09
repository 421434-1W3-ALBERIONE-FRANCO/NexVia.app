import { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const originIcon = L.divIcon({
  html: `<div style="width:12px;height:12px;background:#059669;border:2px solid white;border-radius:50%;box-shadow:0 1px 2px rgba(0,0,0,.3)"></div>`,
  className: '',
  iconSize: [12, 12],
  iconAnchor: [6, 6],
});

const destIcon = L.divIcon({
  html: `<div style="width:12px;height:12px;background:#dc2626;border:2px solid white;border-radius:50%;box-shadow:0 1px 2px rgba(0,0,0,.3)"></div>`,
  className: '',
  iconSize: [12, 12],
  iconAnchor: [6, 6],
});

function FitBounds({ origen, destino }) {
  const map = useMap();
  useEffect(() => {
    if (origen && destino) {
      const bounds = L.latLngBounds([origen.lat, origen.lng], [destino.lat, destino.lng]);
      map.fitBounds(bounds, { padding: [25, 25] });
    }
  }, [origen?.lat, origen?.lng, destino?.lat, destino?.lng, map]);
  return null;
}

function RouteLine({ origen, destino }) {
  const [route, setRoute] = useState(null);

  useEffect(() => {
    if (!origen || !destino) return;
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
    return <Polyline positions={route} pathOptions={{ color: '#0f766e', weight: 3, opacity: 0.8 }} />;
  }

  return (
    <Polyline
      positions={[[origen.lat, origen.lng], [destino.lat, destino.lng]]}
      pathOptions={{ color: '#0f766e', weight: 2, dashArray: '6 4' }}
    />
  );
}

export default function MiniRuta({ origen, destino }) {
  if (!origen || !destino) return null;

  const center = [(origen.lat + destino.lat) / 2, (origen.lng + destino.lng) / 2];

  return (
    <MapContainer
      center={center}
      zoom={13}
      className="w-full rounded-xl overflow-hidden border border-stone-200"
      style={{ height: '8rem', width: '100%' }}
      dragging={false}
      scrollWheelZoom={false}
      doubleClickZoom={false}
      attributionControl={false}
      zoomControl={false}
    >
      <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
      <FitBounds origen={origen} destino={destino} />
      <Marker position={[origen.lat, origen.lng]} icon={originIcon} />
      <Marker position={[destino.lat, destino.lng]} icon={destIcon} />
      <RouteLine origen={origen} destino={destino} />
    </MapContainer>
  );
}