import { Box, Button, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  /** Etiqueta corta sobre el título (p. ej. el módulo o la categoría). */
  eyebrow?: string;
  actionLabel?: string;
  onAction?: () => void;
  actionIcon?: ReactNode;
  secondaryAction?: ReactNode;
}

export function PageHeader({ title, subtitle, eyebrow, actionLabel, onAction, actionIcon, secondaryAction }: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      justifyContent="space-between"
      alignItems={{ xs: 'stretch', sm: 'center' }}
      spacing={2}
      sx={{ mb: 3, pl: 2, borderLeft: '4px solid', borderColor: 'secondary.main' }}
    >
      <Box>
        {eyebrow && (
          <Typography variant="overline" color="secondary.dark" sx={{ display: 'block' }}>
            {eyebrow}
          </Typography>
        )}
        <Typography variant="h4">{title}</Typography>
        {subtitle && (
          <Typography color="text.secondary" sx={{ mt: 0.5, maxWidth: 640 }}>
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
