import { Card, CardContent, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface StatCardProps {
  label: string;
  value: string | number;
  icon: ReactNode;
  helper?: string;
}

export function StatCard({ label, value, icon, helper }: StatCardProps) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <Typography color="text.secondary" fontWeight={700}>{label}</Typography>
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
