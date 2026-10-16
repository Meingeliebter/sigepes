import { Box, Stack, Typography } from '@mui/material';
import { useId, useMemo } from 'react';
import type { RestrictedZone } from '../api/domain.service';
import { ZONE_TYPE_LABEL } from '../geo/geo';

export interface MapPoint {
  id: string;
  latitude: number;
  longitude: number;
  /** Punto dentro de una zona restringida: se marca en rojo. */
  intrusion?: boolean;
}

interface GeoMapProps {
  zones: RestrictedZone[];
  track?: MapPoint[];
  /** Zona a resaltar (p. ej. la que se está previsualizando en un formulario). */
  highlightZoneId?: string;
  height?: number;
  title: string;
}

const WIDTH = 640;

const ZONE_STYLE: Record<RestrictedZone['zoneType'], { fill: string; stroke: string }> = {
  CLOSURE_ZONE: { fill: 'rgba(168, 90, 0, 0.16)', stroke: '#A85A00' },
  MPA: { fill: 'rgba(27, 107, 63, 0.16)', stroke: '#1B6B3F' },
  OTHER: { fill: 'rgba(61, 110, 146, 0.16)', stroke: '#3D6E92' },
};

/**
 * Esquema geográfico (sin teselas externas, funciona sin conexión) de zonas restringidas y del
 * recorrido de una faena. Proyección equirectangular corregida por la latitud media: suficiente
 * para las decenas de kilómetros de una zona costera.
 */
export function GeoMap({ zones, track = [], highlightZoneId, height = 360, title }: GeoMapProps) {
  const descId = useId();
  const view = useMemo(() => {
    const points: [number, number][] = [
      ...zones.flatMap((zone) => zone.polygon),
      ...track.map((point) => [point.latitude, point.longitude] as [number, number]),
    ];
    if (points.length === 0) return null;
    const lats = points.map(([lat]) => lat);
    const lons = points.map(([, lon]) => lon);
    let minLat = Math.min(...lats);
    let maxLat = Math.max(...lats);
    let minLon = Math.min(...lons);
    let maxLon = Math.max(...lons);
    // Un solo punto: abre una ventana de ~2 km para que el mapa no colapse.
    if (maxLat - minLat < 0.02) { minLat -= 0.01; maxLat += 0.01; }
    if (maxLon - minLon < 0.02) { minLon -= 0.01; maxLon += 0.01; }
    const padLat = (maxLat - minLat) * 0.12;
    const padLon = (maxLon - minLon) * 0.12;
    minLat -= padLat; maxLat += padLat; minLon -= padLon; maxLon += padLon;

    const cos = Math.cos((((minLat + maxLat) / 2) * Math.PI) / 180);
    const spanX = (maxLon - minLon) * cos;
    const spanY = maxLat - minLat;
    const scale = Math.min(WIDTH / spanX, height / spanY);
    const offsetX = (WIDTH - spanX * scale) / 2;
    const offsetY = (height - spanY * scale) / 2;
    const project = (lat: number, lon: number): [number, number] => [
      offsetX + (lon - minLon) * cos * scale,
      offsetY + (maxLat - lat) * scale,
    ];
    return { minLat, maxLat, minLon, maxLon, project };
  }, [zones, track, height]);

  if (!view) {
    return (
      <Box sx={{ p: 4, textAlign: 'center', borderRadius: 3, bgcolor: 'rgba(14,124,134,.06)' }}>
        <Typography color="text.secondary">Sin datos geográficos para mostrar.</Typography>
      </Box>
    );
  }

  const { project } = view;
  const toPath = (vertices: [number, number][]) =>
    vertices.map(([lat, lon], index) => `${index === 0 ? 'M' : 'L'}${project(lat, lon).join(' ')}`).join(' ') + ' Z';
  const trackPath = track.map((point, index) => `${index === 0 ? 'M' : 'L'}${project(point.latitude, point.longitude).join(' ')}`).join(' ');
  const intrusions = track.filter((point) => point.intrusion);
  const graticule = [0.25, 0.5, 0.75];

  return (
    <Stack spacing={1}>
      <Box
        component="svg"
        viewBox={`0 0 ${WIDTH} ${height}`}
        role="img"
        aria-labelledby={descId}
        sx={{ width: '100%', height: 'auto', display: 'block', borderRadius: '14px', overflow: 'hidden', border: '1px solid rgba(11,59,92,.14)' }}
      >
        <title id={descId}>
          {`${title}: ${zones.length} zona(s) restringida(s)${track.length ? `, recorrido de ${track.length} posición(es)` : ''}${intrusions.length ? `, ${intrusions.length} ingreso(s) a zona restringida` : ''}.`}
        </title>
        <defs>
          <linearGradient id={`${descId}-sea`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#EEF7F7" />
            <stop offset="100%" stopColor="#C4E3E8" />
          </linearGradient>
        </defs>
        <rect width={WIDTH} height={height} fill={`url(#${descId}-sea)`} />
        <image href="/images/isobaths.svg" width={WIDTH} height={height} preserveAspectRatio="xMidYMid slice" opacity="0.7" />

        {graticule.map((fraction) => {
          const lat = view.maxLat - (view.maxLat - view.minLat) * fraction;
          const lon = view.minLon + (view.maxLon - view.minLon) * fraction;
          const [, y] = project(lat, view.minLon);
          const [x] = project(view.minLat, lon);
          return (
            <g key={fraction} stroke="rgba(11,59,92,.18)" strokeDasharray="3 5" fontSize="10" fill="#33495A">
              <line x1={0} x2={WIDTH} y1={y} y2={y} />
              <line x1={x} x2={x} y1={0} y2={height} />
              <text x={6} y={y - 4} stroke="none">{lat.toFixed(2)}°</text>
              <text x={x + 4} y={height - 6} stroke="none">{lon.toFixed(2)}°</text>
            </g>
          );
        })}

        {zones.map((zone) => {
          const style = ZONE_STYLE[zone.zoneType];
          const highlighted = zone.id === highlightZoneId;
          return (
            <path
              key={zone.id}
              d={toPath(zone.polygon)}
              fill={style.fill}
              stroke={style.stroke}
              strokeWidth={highlighted ? 3 : 1.6}
              strokeDasharray={zone.active ? undefined : '6 4'}
              opacity={zone.active ? 1 : 0.55}
            >
              <title>{`${zone.name} · ${ZONE_TYPE_LABEL[zone.zoneType]}`}</title>
            </path>
          );
        })}

        {track.length > 1 && (
          <path d={trackPath} fill="none" stroke="#0B3B5C" strokeWidth={2.5} strokeLinejoin="round" strokeLinecap="round" />
        )}
        {track.map((point, index) => {
          const [x, y] = project(point.latitude, point.longitude);
          const isEdge = index === 0 || index === track.length - 1;
          if (point.intrusion) {
            return (
              <g key={point.id}>
                <circle cx={x} cy={y} r={11} fill="rgba(179,38,30,.18)" />
                <circle cx={x} cy={y} r={5.5} fill="#B3261E" stroke="#fff" strokeWidth={2} />
              </g>
            );
          }
          return (
            <circle key={point.id} cx={x} cy={y} r={isEdge ? 5.5 : 3} fill={index === 0 ? '#0E7C86' : '#0B3B5C'} stroke="#fff" strokeWidth={isEdge ? 2 : 1} />
          );
        })}
      </Box>

      <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap sx={{ fontSize: '.8rem', color: 'text.secondary' }}>
        {(Object.keys(ZONE_STYLE) as RestrictedZone['zoneType'][])
          .filter((type) => zones.some((zone) => zone.zoneType === type))
          .map((type) => (
            <Stack key={type} direction="row" spacing={0.75} alignItems="center">
              <Box sx={{ width: 14, height: 14, borderRadius: '4px', bgcolor: ZONE_STYLE[type].fill, border: `1.5px solid ${ZONE_STYLE[type].stroke}` }} />
              <span>{ZONE_TYPE_LABEL[type]}</span>
            </Stack>
          ))}
        {track.length > 0 && (
          <Stack direction="row" spacing={0.75} alignItems="center">
            <Box sx={{ width: 14, height: 3, borderRadius: 2, bgcolor: '#0B3B5C' }} />
            <span>Recorrido (inicio en turquesa)</span>
          </Stack>
        )}
        {intrusions.length > 0 && (
          <Stack direction="row" spacing={0.75} alignItems="center">
            <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: '#B3261E' }} />
            <span>Ingreso a zona restringida</span>
          </Stack>
        )}
        <span>Esquema sin cartografía oficial.</span>
      </Stack>
    </Stack>
  );
}
