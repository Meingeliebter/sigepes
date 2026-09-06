import { Box, Paper, Typography } from '@mui/material';
import type { PropsWithChildren } from 'react';

export function AuthShell({ children }: PropsWithChildren) {
  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 2, background: 'linear-gradient(135deg,#0A2540,#0A6572)' }}>
      <Paper component="main" elevation={8} sx={{ width: '100%', maxWidth: 460, p: { xs: 3, sm: 5 } }}>
        <Typography variant="h4" color="primary" textAlign="center">SIGEPES</Typography>
        <Typography color="text.secondary" textAlign="center" sx={{ mb: 3 }}>
          Gestión pesquera legal y sostenible
        </Typography>
        {children}
      </Paper>
    </Box>
  );
}
