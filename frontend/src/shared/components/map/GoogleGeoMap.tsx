import { Box, Stack, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import { visuallyHidden } from '@mui/utils';
import {
  AdvancedMarker, APILoadingStatus, APIProvider, ControlPosition, InfoWindow, Map, MapControl, Polygon, Polyline,
  useApiLoadingStatus, useMap,
} from '@vis.gl/react-google-maps';
import { useEffect, useMemo, useRef, useState } from 'react';
import type { RestrictedZone } from '../../api/domain.service';
import { ZONE_TYPE_LABEL } from '../../geo/geo';
import { MapLegend } from './MapLegend';
import { markTilesUnavailable, useGoogleMapsAuthFailed } from './mapsAuth';
import { LeafletGeoMap } from './LeafletGeoMap';
import {
  INTRUSION_COLOR, TRACK_COLOR, TRACK_START_COLOR, ZONE_STYLE, type GeoMapProps, type MapPoint,
} from './mapStyles';

/** Colombia completa (costas Pacífica y Caribe e islas) cuando no hay nada que encuadrar. */
const COLOMBIA: google.maps.LatLngBoundsLiteral = { north: 13.6, south: -4.3, east: -66.8, west: -82.0 };
/**
 * Los marcadores avanzados exigen un Map ID. `DEMO_MAP_ID` es el que Google publica para
 * desarrollo; en producción se configura uno propio en Google Cloud.
 */
const MAP_ID = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID || 'DEMO_MAP_ID';

type MapType = 'hybrid' | 'roadmap' | 'terrain';
const MAP_TYPES: { id: MapType; label: string }[] = [
  { id: 'hybrid', label: 'Satélite' },
  { id: 'roadmap', label: 'Mapa' },
  { id: 'terrain', label: 'Relieve' },
];

function boundsOf(zones: RestrictedZone[], track: MapPoint[]): google.maps.LatLngBoundsLiteral | null {
  const points: [number, number][] = [
    ...zones.flatMap((zone) => zone.polygon),
    ...track.map((point) => [point.latitude, point.longitude] as [number, number]),
  ];
  if (points.length === 0) return null;
  const lats = points.map(([lat]) => lat);
  const lons = points.map(([, lon]) => lon);
  // Un solo punto: ventana de ~2 km para no acercar al máximo.
  const pad = points.length === 1 ? 0.01 : 0;
  return {
    north: Math.max(...lats) + pad,
    south: Math.min(...lats) - pad,
    east: Math.max(...lons) + pad,
    west: Math.min(...lons) - pad,
  };
}

/**
 * Reencuadra la cámara cuando cambian los datos. En modo selección (`onPick`) no se reencuadra en
 * cada clic: solo se desplaza hacia el punto si quedó fuera de la vista (p. ej. al usar el GPS).
 */
function CameraFitter({ zones, track, picking }: { zones: RestrictedZone[]; track: MapPoint[]; picking: boolean }) {
  const map = useMap();
  const signature = picking
    ? zones.map((zone) => zone.id).join(',')
    : `${zones.map((zone) => `${zone.id}:${zone.polygon.length}`).join(',')}|${track.map((point) => point.id).join(',')}`;
  const last = track[track.length - 1];
  // Los datos más recientes, leídos por el efecto de encuadre que solo se dispara con la firma.
  const latest = useRef({ zones, track, last });
  useEffect(() => {
    latest.current = { zones, track, last };
  });

  useEffect(() => {
    if (!map) return;
    const data = latest.current;
    const bounds = boundsOf(data.zones, picking ? [] : data.track) ?? (picking && data.last ? boundsOf([], [data.last]) : null);
    map.fitBounds(bounds ?? COLOMBIA, 32);
  }, [map, signature, picking]);

  useEffect(() => {
    if (!map || !picking || !last) return;
    const position = { lat: last.latitude, lng: last.longitude };
    if (!map.getBounds()?.contains(position)) {
      map.panTo(position);
      if ((map.getZoom() ?? 0) < 9) map.setZoom(11);
    }
  }, [map, picking, last]);

  return null;
}

function Dot({ color, size, halo }: { color: string; size: number; halo?: boolean }) {
  return (
    <Box
      sx={{
        width: size, height: size, borderRadius: '50%', bgcolor: color, border: '2px solid #fff',
        boxShadow: halo ? `0 0 0 6px ${color}40, 0 1px 4px rgba(0,0,0,.45)` : '0 1px 4px rgba(0,0,0,.45)',
        transform: 'translateY(50%)',
      }}
    />
  );
}

function MapContents({ zones, track = [], highlightZoneId, height = 360, title, onPick }: GeoMapProps) {
  const status = useApiLoadingStatus();
  const authFailed = useGoogleMapsAuthFailed();
  const [selected, setSelected] = useState<{ zone: RestrictedZone; position: google.maps.LatLngLiteral } | null>(null);
  const [initialBounds] = useState(() => boundsOf(zones, track) ?? COLOMBIA);
  const path = useMemo(() => track.map((point) => ({ lat: point.latitude, lng: point.longitude })), [track]);
  const intrusions = track.filter((point) => point.intrusion);
  const picking = !!onPick;
  // Satélite con etiquetas por defecto: se distinguen costa, arrecifes y nombres de lugares.
  const [mapType, setMapType] = useState<MapType>('hybrid');

  // Clave inválida, sin facturación o red bloqueada: se pasa a OpenStreetMap (y, si tampoco carga, al esquema).
  if (authFailed || status === APILoadingStatus.FAILED || status === APILoadingStatus.AUTH_FAILURE) {
    return <LeafletGeoMap zones={zones} track={track} highlightZoneId={highlightZoneId} height={height} title={title} onPick={onPick} onTilesUnavailable={markTilesUnavailable} />;
  }

  return (
    <Stack spacing={1}>
      <Box
        role="region"
        aria-label={title}
        sx={{
          height, borderRadius: '14px', overflow: 'hidden', border: '1px solid rgba(11,59,92,.14)',
          bgcolor: 'rgba(14,124,134,.08)', '& .gm-style': { fontFamily: 'inherit' },
        }}
      >
        <Typography sx={visuallyHidden}>
          {`${title}: ${zones.length} zona(s) restringida(s)${track.length ? `, ${track.length} posición(es)` : ''}${intrusions.length ? `, ${intrusions.length} ingreso(s) a zona restringida` : ''}.`}
        </Typography>
        <Map
          mapId={MAP_ID}
          defaultBounds={{ ...initialBounds, padding: 32 }}
          mapTypeId={mapType}
          mapTypeControl={false}
          streetViewControl={false}
          fullscreenControl
          gestureHandling="cooperative"
          clickableIcons={false}
          draggableCursor={picking ? 'crosshair' : undefined}
          onClick={(event) => {
            const latLng = event.detail.latLng;
            if (picking && latLng) onPick?.(Number(latLng.lat.toFixed(5)), Number(latLng.lng.toFixed(5)));
            else setSelected(null);
          }}
          style={{ width: '100%', height: '100%' }}
        >
          <CameraFitter zones={zones} track={track} picking={picking} />
          <MapControl position={ControlPosition.TOP_LEFT}>
            <ToggleButtonGroup
              exclusive
              size="small"
              value={mapType}
              onChange={(_, value: MapType | null) => value && setMapType(value)}
              aria-label="Tipo de mapa"
              sx={{
                m: 1.25, bgcolor: 'rgba(255,255,255,.92)', backdropFilter: 'blur(8px)', borderRadius: '10px',
                boxShadow: '0 2px 8px rgba(7,30,48,.25)',
                '& .MuiToggleButton-root': { px: 1.25, py: 0.4, border: 0, fontSize: 12.5, fontWeight: 700, textTransform: 'none', color: '#33495A' },
                '& .Mui-selected': { bgcolor: 'rgba(14,124,134,.16) !important', color: '#0B3B5C !important' },
              }}
            >
              {MAP_TYPES.map((type) => <ToggleButton key={type.id} value={type.id}>{type.label}</ToggleButton>)}
            </ToggleButtonGroup>
          </MapControl>

          {zones.map((zone) => {
            const style = ZONE_STYLE[zone.zoneType];
            const highlighted = zone.id === highlightZoneId;
            return (
              <Polygon
                key={zone.id}
                paths={zone.polygon.map(([lat, lng]) => ({ lat, lng }))}
                strokeColor={style.stroke}
                strokeOpacity={zone.active ? 1 : 0.6}
                strokeWeight={highlighted ? 4 : 2.5}
                fillColor={style.stroke}
                fillOpacity={zone.active ? style.fillOpacity : 0.1}
                // En modo selección los clics atraviesan la zona para poder marcar puntos dentro.
                clickable={!picking}
                zIndex={highlighted ? 2 : 1}
                onClick={(event) => {
                  const latLng = event.latLng;
                  if (latLng) setSelected({ zone, position: { lat: latLng.lat(), lng: latLng.lng() } });
                }}
              />
            );
          })}

          {path.length > 1 && (
            <>
              {/* Contorno blanco debajo: el recorrido se lee igual sobre agua oscura o tierra clara. */}
              <Polyline path={path} strokeColor="#FFFFFF" strokeOpacity={0.9} strokeWeight={6} clickable={false} zIndex={3} />
              <Polyline path={path} strokeColor={TRACK_COLOR} strokeOpacity={1} strokeWeight={3} clickable={false} zIndex={4} />
            </>
          )}

          {track.map((point, index) => {
            const isEdge = index === 0 || index === track.length - 1;
            if (!point.intrusion && !isEdge) return null;
            const label = point.intrusion
              ? 'Ingreso a zona restringida'
              : track.length === 1 ? 'Punto seleccionado' : index === 0 ? 'Inicio del recorrido' : 'Última posición';
            return (
              <AdvancedMarker
                key={point.id}
                position={{ lat: point.latitude, lng: point.longitude }}
                title={`${label} · ${point.latitude.toFixed(4)}, ${point.longitude.toFixed(4)}`}
                zIndex={point.intrusion ? 10 : 5}
              >
                {point.intrusion
                  ? <Dot color={INTRUSION_COLOR} size={16} halo />
                  : <Dot color={index === 0 && track.length > 1 ? TRACK_START_COLOR : TRACK_COLOR} size={track.length === 1 ? 18 : 14} halo={track.length === 1} />}
              </AdvancedMarker>
            );
          })}

          {selected && (
            <InfoWindow position={selected.position} onCloseClick={() => setSelected(null)} headerContent={<strong>{selected.zone.name}</strong>}>
              <Box sx={{ fontSize: 13, lineHeight: 1.5, maxWidth: 260 }}>
                <div>{ZONE_TYPE_LABEL[selected.zone.zoneType]}{selected.zone.active ? '' : ' · inactiva'}</div>
                <div>Resolución: {selected.zone.resolutionNumber}</div>
                {selected.zone.description && <Box sx={{ mt: 0.5, color: '#475569' }}>{selected.zone.description}</Box>}
              </Box>
            </InfoWindow>
          )}
        </Map>
      </Box>
      <MapLegend
        zones={zones}
        track={track}
        note={picking ? 'Haz clic en el mapa para marcar el punto.' : 'Toca una zona para ver su resolución.'}
      />
    </Stack>
  );
}

/** Mapa de Google Maps: satélite con etiquetas por defecto, con cambio a mapa o relieve. */
export function GoogleGeoMap(props: GeoMapProps & { apiKey: string }) {
  const { apiKey, ...mapProps } = props;
  return (
    <APIProvider apiKey={apiKey} language="es" region="CO">
      <MapContents {...mapProps} />
    </APIProvider>
  );
}
