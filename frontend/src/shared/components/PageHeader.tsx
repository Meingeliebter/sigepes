import { Box, Button, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  /**
   * Módulo o categoría. Ya no se pinta sobre el título (las migas de pan indican el módulo);
   * se conserva para no romper las páginas que lo pasan.
   */
  eyebrow?: string;
  actionLabel?: string;
  onAction?: () => void;
  actionIcon?: ReactNode;
  secondaryAction?: ReactNode;
}

export function PageHeader({ title, subtitle, actionLabel, onAction, actionIcon, secondaryAction }: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      justifyContent="space-between"
      alignItems={{ xs: 'stretch', sm: 'center' }}
      spacing={2}
      sx={{ mb: 3.5 }}
    >
      <Box>
        <Typography variant="h4" component="h1" sx={{ fontSize: { xs: '1.75rem', md: '2.125rem' }, textWrap: 'balance' }}>
          {title}
        </Typography>
        {subtitle && (
          <Typography color="text.secondary" sx={{ mt: 0.75, maxWidth: 680 }}>
            {subtitle}
          </Typography>
        )}
      </Box>
      {(actionLabel || secondaryAction) && (
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ alignSelf: { xs: 'stretch', sm: 'auto' } }}>
          {secondaryAction}
          {actionLabel && onAction && (
            <Button variant="contained" startIcon={actionIcon} onClick={onAction}>
              {actionLabel}
            </Button>
          )}
        </Stack>
      )}
    </Stack>
  );
}
