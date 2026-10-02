import { Box, Card, CardContent, Stack, Typography } from '@mui/material';
import { alpha } from '@mui/material/styles';
import type { ReactNode } from 'react';

type Accent = 'primary' | 'secondary' | 'success' | 'warning' | 'info';

interface StatCardProps {
  label: string;
  value: string | number;
  icon: ReactNode;
  helper?: string;
  accent?: Accent;
}

export function StatCard({ label, value, icon, helper, accent = 'primary' }: StatCardProps) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 2.5, '&:last-child': { pb: 2.5 } }}>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <Box
            sx={(t) => ({
              width: 40,
              height: 40,
              flexShrink: 0,
              display: 'grid',
              placeItems: 'center',
              borderRadius: '12px',
              color: t.palette[accent].main,
              bgcolor: alpha(t.palette[accent].main, 0.1),
              boxShadow: `inset 0 0 0 1px ${alpha(t.palette[accent].main, 0.16)}`,
              '& svg': { color: 'inherit' },
            })}
          >
            {icon}
          </Box>
          <Typography color="text.secondary" fontWeight={650} variant="body2">{label}</Typography>
        </Stack>
        <Typography sx={{ mt: 2, fontSize: '2.25rem', fontWeight: 800, lineHeight: 1, letterSpacing: '-0.02em' }}>
          {value}
        </Typography>
        {helper && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>{helper}</Typography>
        )}
      </CardContent>
    </Card>
  );
}
