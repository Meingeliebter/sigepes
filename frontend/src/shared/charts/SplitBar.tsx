import { Box, Stack, Tooltip, Typography } from '@mui/material';
import { SPLIT_COLORS } from './chartColors';

export interface SplitDatum {
  key: string;
  label: string;
  value: number;
  display: string;
}

/**
 * Barra apilada al 100 % para comparar partes de un total (p. ej. Pacífico frente a Caribe).
 * Separación de 2 px entre segmentos, leyenda siempre visible y etiqueta directa con el porcentaje:
 * la identidad nunca depende solo del color.
 */
export function SplitBar({ data }: { data: SplitDatum[] }) {
  const total = data.reduce((sum, item) => sum + item.value, 0);
  const pct = (value: number) => (total > 0 ? Math.round((value / total) * 100) : 0);
  return (
    <Stack spacing={1.5}>
      <Stack direction="row" sx={{ height: 28, gap: '2px' }} aria-hidden="true">
        {data.filter((item) => item.value > 0).map((item, index) => (
          <Tooltip key={item.key} title={`${item.label}: ${item.display} (${pct(item.value)} %)`}>
            <Box sx={{
              flex: item.value, bgcolor: SPLIT_COLORS[index % SPLIT_COLORS.length],
              borderRadius: index === 0 ? '4px 0 0 4px' : '0 4px 4px 0', minWidth: 4,
            }} />
          </Tooltip>
        ))}
      </Stack>
      <Stack direction="row" spacing={2.5} flexWrap="wrap" useFlexGap component="ul" sx={{ listStyle: 'none', p: 0, m: 0 }}>
        {data.map((item, index) => (
          <Stack key={item.key} component="li" direction="row" spacing={1} alignItems="center">
            <Box sx={{ width: 12, height: 12, borderRadius: '3px', bgcolor: SPLIT_COLORS[index % SPLIT_COLORS.length] }} />
            <Typography variant="body2">
              {item.label} · <strong style={{ fontVariantNumeric: 'tabular-nums' }}>{item.display}</strong> ({pct(item.value)} %)
            </Typography>
          </Stack>
        ))}
      </Stack>
    </Stack>
  );
}
