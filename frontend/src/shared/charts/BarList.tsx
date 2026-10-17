import { Box, Stack, Tooltip, Typography } from '@mui/material';
import { BAR_COLOR } from './chartColors';

export interface BarDatum {
  key: string;
  label: string;
  value: number;
  /** Texto del valor ya formateado (p. ej. "1.250 kg"). */
  display: string;
  /** Detalle adicional para el tooltip. */
  detail?: string;
}

/**
 * Barras horizontales de una sola serie (magnitud). Sin leyenda: el título nombra la serie.
 * Cada fila es enfocable y muestra su tooltip con el teclado igual que con el mouse; el área
 * activa es la fila completa, más grande que la barra.
 */
export function BarList({ data, max: maxProp, color = BAR_COLOR }: { data: BarDatum[]; max?: number; color?: string }) {
  const max = maxProp ?? Math.max(...data.map((item) => item.value), 0);
  return (
    <Stack component="ul" spacing={0.5} sx={{ listStyle: 'none', p: 0, m: 0 }}>
      {data.map((item) => {
        const width = max > 0 ? Math.max((item.value / max) * 100, item.value > 0 ? 1.5 : 0) : 0;
        return (
          <Tooltip
            key={item.key}
            placement="top"
            title={<><strong>{item.label}</strong><br />{item.display}{item.detail ? ` · ${item.detail}` : ''}</>}
          >
            <Box
              component="li"
              tabIndex={0}
              aria-label={`${item.label}: ${item.display}${item.detail ? `, ${item.detail}` : ''}`}
              sx={{
                display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'minmax(120px, 40%) 1fr' }, gap: { xs: 0.5, sm: 1.5 },
                alignItems: 'center', py: 0.75, px: 1, borderRadius: 2, outline: 'none',
                '&:hover, &:focus-visible': { bgcolor: 'rgba(11,59,92,.05)' },
                '&:focus-visible': { boxShadow: '0 0 0 2px #0B3B5C' },
              }}
            >
              <Typography variant="body2" sx={{ overflowWrap: 'anywhere', lineHeight: 1.3 }}>{item.label}</Typography>
              <Stack direction="row" alignItems="center" spacing={1}>
                <Box sx={{ flex: 1, height: 14, position: 'relative', bgcolor: 'rgba(11,59,92,.06)', borderRadius: '0 4px 4px 0' }}>
                  <Box sx={{ position: 'absolute', inset: 0, width: `${width}%`, bgcolor: color, borderRadius: '0 4px 4px 0' }} />
                </Box>
                <Typography variant="body2" sx={{ minWidth: 72, textAlign: 'right', fontVariantNumeric: 'tabular-nums', color: 'text.primary' }}>
                  {item.display}
                </Typography>
              </Stack>
            </Box>
          </Tooltip>
        );
      })}
    </Stack>
  );
}
