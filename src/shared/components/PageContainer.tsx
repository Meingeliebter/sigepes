import type { ReactNode } from 'react';
import { Box, Typography, Paper } from '@mui/material';

interface PageContainerProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  children?: ReactNode;
}

export function PageContainer({
  title,
  subtitle,
  actions,
  children,
}: PageContainerProps) {
  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 2,
          mb: 2,
          pl: 2,
          borderLeft: '4px solid',
          borderColor: 'secondary.main',
        }}
      >
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="body2" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
        {actions}
      </Box>
      <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 } }}>
        {children}
      </Paper>
    </Box>
  );
}
