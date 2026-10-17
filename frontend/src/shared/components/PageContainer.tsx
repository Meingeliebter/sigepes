import type { ReactNode } from 'react';
import { Box, Typography, Paper } from '@mui/material';

interface PageContainerProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  children?: ReactNode;
  /** Ancho máximo del panel; los formularios se leen mejor sin estirarse a todo el ancho. */
  maxWidth?: number;
}

export function PageContainer({
  title,
  subtitle,
  actions,
  children,
  maxWidth,
}: PageContainerProps) {
  return (
    <Box sx={{ maxWidth }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 2,
          mb: 2.5,
        }}
      >
        <Box>
          <Typography variant="h4" component="h1" sx={{ fontSize: { xs: '1.75rem', md: '2.125rem' } }}>
            {title}
          </Typography>
          {subtitle && (
            <Typography color="text.secondary" sx={{ mt: 0.5 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
        {actions}
      </Box>
      <Paper variant="outlined" sx={{ p: { xs: 2.5, md: 4 } }}>
        {children}
      </Paper>
    </Box>
  );
}
