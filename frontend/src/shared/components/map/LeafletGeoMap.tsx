import 'leaflet/dist/leaflet.css';
import { Box, Stack, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import { visuallyHidden } from '@mui/utils';
import type { LatLngBoundsExpression, LatLngExpression } from 'leaflet';
import { useEffect, useMemo, useRef, useState } from 'react';
import { CircleMarker, MapContainer, Polygon, Polyline, Popup, TileLayer, Tooltip, useMap, useMapEvents } from 'react-leaflet';
import type { RestrictedZone } from '../../api/domain.service';
import { ZONE_TYPE_LABEL } from '../../geo/geo';
import { MapLegend } from './MapLegend';
import { INTRUSION_COLOR, TRACK_COLOR, TRACK_START_COLOR, ZONE_STYLE, type GeoMapProps, type MapPoint } from './mapStyles';

/** Colombia completa (costas Pacífica y Caribe e islas) cuando no hay nada que encuadrar. */
const COLOMBIA: LatLngBoundsExpression = [[-4.3, -82.0], [13.6, -66.8]];

type BaseLayer = 'map' | 'satellite' | 'terrain';

/**
 * Capas base sin clave de API. Las tres exigen atribución visible, que Leaflet muestra abajo a la
 * derecha; OpenStreetMap admite este uso de bajo tráfico según su política de teselas.
 */
const LAYERS: Record<BaseLayer, { label: string; url: string; attribution: string; maxZoom: number; labels?: string }> = {
  map: {
    label: 'Mapa',
    url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    maxZoom: 19,
  },
  satellite: {
    label: 'Satélite',
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    attribution: 'Imágenes &copy; Esri, Maxar, Earthstar Geographics',
    maxZoom: 18,
    // Nombres de lugares y límites encima de la imagen, para orientarse sobre el agua.
    labels: 'https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}',
  },
  terrain: {
    label: 'Relieve',
    url: 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png',
    attribution: 'Datos &copy; OpenStreetMap · Estilo &copy; <a href="https://opentopomap.org">OpenTopoMap</a> (CC-BY-SA)',
    maxZoom: 17,
  },
};

function boundsOf(zones: RestrictedZone[], track: MapPoint[]): LatLngBoundsExpression | null {
  const points: [number, number][] = [
    ...zones.flatMap((zone) => zone.polygon),
    ...track.map((point) => [point.latitude, point.longitude] as [number, number]),
  ];
  if (points.length === 0) return null;
  const lats = points.map(([lat]) => lat);
  const lons = points.map(([, lon]) => lon);
  const pad = points.length === 1 ? 0.01 : 0;
  return [[Math.min(...lats) - pad, Math.min(...lons) - pad], [Math.max(...lats) + pad, Math.max(...lons) + pad]];
}

/**
 * Reencuadra cuando cambian los datos. En modo selección no se reencuadra en cada clic: solo se
 * desplaza hacia el punto si quedó fuera de la vista (p. ej. al usar el GPS).
 */
function CameraFitter({ zones, track, picking }: { zones: RestrictedZone[]; track: MapPoint[]; picking: boolean }) {
  const map = useMap();
  const signature = picking
    ? zones.map((zone) => zone.id).join(',')
    : `${zones.map((zone) => `${zone.id}:${zone.polygon.length}`).join(',')}|${track.map((point) => point.id).join(',')}`;
  const last = track[track.length - 1];
  const latest = useRef({ zones, track, last });
  useEffect(() => {
    latest.current = { zones, track, last };
  });

  useEffect(() => {
    const data = latest.current;
    const bounds = boundsOf(data.zones, picking ? [] : data.track) ?? (picking && data.last ? boundsOf([], [data.last]) : null);
    map.fitBounds(bounds ?? COLOMBIA, { padding: [24, 24] });
  }, [map, signature, picking]);

  useEffect(() => {
    if (!picking || !last) return;
    const position: LatLngExpression = [last.latitude, last.longitude];
    if (!map.getBounds().contains(position)) {
      map.setView(position, Math.max(map.getZoom(), 11));
    }
  }, [map, picking, last]);

  return null;
}

function ClickPicker({ onPick }: { onPick?: GeoMapProps['onPick'] }) {
  useMapEvents({
    click(event) {
      onPick?.(Number(event.latlng.lat.toFixed(5)), Number(event.latlng.lng.toFixed(5)));
    },
  });
  return null;
}

/**
 * Mapa con cartografía real sin clave de API (OpenStreetMap, Esri, OpenTopoMap): costa, puertos y
 * nombres de lugares bajo las zonas restringidas, para que se distinga dónde está cada una. Si las
 * teselas no cargan (red bloqueada), avisa con `onTilesUnavailable` para usar el esquema propio.
 */
export function LeafletGeoMap({ zones, track = [], highlightZoneId, height = 360, title, onPick, onTilesUnavailable }:
  GeoMapProps & { onTilesUnavailable?: () => void }) {
  const [layer, setLayer] = useState<BaseLayer>('map');
  const [initialBounds] = useState(() => boundsOf(zones, track) ?? COLOMBIA);
  const tileStats = useRef({ loaded: 0, failed: 0 });
  const picking = !!onPick;
  const path = useMemo(() => track.map((point) => [point.latitude, point.longitude] as [number, number]), [track]);
  const intrusions = track.filter((point) => point.intrusion).length;
  const base = LAYERS[layer];

  const tileHandlers = {
    tileload: () => { tileStats.current.loaded += 1; },
    tileerror: () => {
      tileStats.current.failed += 1;
      if (tileStats.current.loaded === 0 && tileStats.current.failed >= 8) onTilesUnavailable?.();
    },
  };

  return (
    <Stack spacing={1}>
      <Box
        role="region"
        aria-label={title}
        sx={{
          position: 'relative', height, borderRadius: '14px', overflow: 'hidden', border: '1px solid rgba(11,59,92,.14)',
          bgcolor: '#aad3df', // color del mar en OpenStreetMap mientras cargan las teselas
          '& .leaflet-container': { height: '100%', width: '100%', fontFamily: 'inherit', cursor: picking ? 'crosshair' : undefined },
          '& .leaflet-control-attribution': { fontSize: 10 },
        }}
      >
        <Typography sx={visuallyHidden}>
          {`${title}: ${zones.length} zona(s) restringida(s)${track.length ? `, ${track.length} posición(es)` : ''}${intrusions ? `, ${intrusions} ingreso(s) a zona restringida` : ''}.`}
        </Typography>
        <MapContainer bounds={initialBounds} boundsOptions={{ padding: [24, 24] }} scrollWheelZoom={false} style={{ height: '100%' }}>
          <TileLayer key={layer} url={base.url} attribution={base.attribution} maxZoom={base.maxZoom} eventHandlers={tileHandlers} />
          {base.labels && <TileLayer key={`${layer}-labels`} url={base.labels} maxZoom={base.maxZoom} />}
          <CameraFitter zones={zones} track={track} picking={picking} />
          {picking && <ClickPicker onPick={onPick} />}

          {zones.map((zone) => {
            const style = ZONE_STYLE[zone.zoneType];
            const highlighted = zone.id === highlightZoneId;
            return (
              <Polygon
                key={zone.id}
                positions={zone.polygon.map(([lat, lng]) => [lat, lng] as [number, number])}
                pathOptions={{
                  color: style.stroke,
                  weight: highlighted ? 4 : 2.5,
                  opacity: zone.active ? 1 : 0.6,
                  dashArray: zone.active ? undefined : '6 4',
                  fillColor: style.stroke,
                  fillOpacity: zone.active ? style.fillOpacity : 0.1,
                }}
                // En modo selección los clics atraviesan la zona para poder marcar puntos dentro.
                interactive={!picking}
              >
                <Tooltip sticky>{zone.name}</Tooltip>
                <Popup>
                  <strong>{zone.name}</strong>
                  <div>{ZONE_TYPE_LABEL[zone.zoneType]}{zone.active ? '' : ' · inactiva'}</div>
                  <div>Resolución: {zone.resolutionNumber}</div>
                  {zone.description && <div style={{ marginTop: 4, color: '#475569' }}>{zone.description}</div>}
                </Popup>
              </Polygon>
            );
          })}

          {path.length > 1 && (
            <>
              {/* Contorno blanco debajo: el recorrido se lee igual sobre el mar o sobre tierra. */}
              <Polyline positions={path} pathOptions={{ color: '#FFFFFF', weight: 6, opacity: 0.9 }} interactive={false} />
              <Polyline positions={path} pathOptions={{ color: TRACK_COLOR, weight: 3, opacity: 1 }} interactive={false} />
            </>
          )}

          {track.map((point, index) => {
            const isEdge = index === 0 || index === track.length - 1;
            if (!point.intrusion && !isEdge) return null;
            const color = point.intrusion ? INTRUSION_COLOR : index === 0 && track.length > 1 ? TRACK_START_COLOR : TRACK_COLOR;
            const label = point.intrusion
              ? 'Ingreso a zona restringida'
              : track.length === 1 ? 'Punto seleccionado' : index === 0 ? 'Inicio del recorrido' : 'Última posición';
            return (
              <CircleMarker
                key={point.id}
                center={[point.latitude, point.longitude]}
                radius={point.intrusion || track.length === 1 ? 8 : 6}
                pathOptions={{ color: '#FFFFFF', weight: 2, fillColor: color, fillOpacity: 1 }}
                interactive={!picking}
              >
                <Tooltip>{`${label} · ${point.latitude.toFixed(4)}, ${point.longitude.toFixed(4)}`}</Tooltip>
              </CircleMarker>
            );
          })}
        </MapContainer>

        <ToggleButtonGroup
          exclusive
          size="small"
          value={layer}
          onChange={(_, value: BaseLayer | null) => value && setLayer(value)}
          aria-label="Tipo de mapa"
          sx={{
            position: 'absolute', top: 10, right: 10, zIndex: 1000, bgcolor: 'rgba(255,255,255,.94)', borderRadius: '10px',
            boxShadow: '0 2px 8px rgba(7,30,48,.25)',
            '& .MuiToggleButton-root': { px: 1.25, py: 0.4, border: 0, fontSize: 12.5, fontWeight: 700, textTransform: 'none', color: '#33495A' },
            '& .Mui-selected': { bgcolor: 'rgba(14,124,134,.16) !important', color: '#0B3B5C !important' },
          }}
        >
          {(Object.keys(LAYERS) as BaseLayer[]).map((key) => <ToggleButton key={key} value={key}>{LAYERS[key].label}</ToggleButton>)}
        </ToggleButtonGroup>
      </Box>
      <MapLegend
        zones={zones}
        track={track}
        note={picking ? 'Haz clic en el mapa para marcar el punto.' : 'Toca una zona para ver su resolución.'}
      />
    </Stack>
  );
}
