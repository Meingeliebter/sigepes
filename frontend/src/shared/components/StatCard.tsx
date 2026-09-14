import { Card, CardContent, Stack, Typography } from '@mui/material';
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
    <Card sx={{ height: '100%', borderTop: '3px solid', borderTopColor: `${accent}.main` }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <Typography color="text.secondary" fontWeight={700} variant="body2">{label}</Typography>
          {icon}
        </Stack>
        <Typography variant="h4" sx={{ mt: 1 }}>{value}</Typography>
        {helper && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>{helper}</Typography>
        )}
      </CardContent>
    </Card>
  );
}
