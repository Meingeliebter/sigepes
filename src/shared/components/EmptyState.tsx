import InboxIcon from '@mui/icons-material/Inbox';
import { Button, Paper, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  description?: string;
  icon?: ReactNode;
  actionLabel?: string;
  onAction?: () => void;
}

export function EmptyState({ title, description, icon, actionLabel, onAction }: EmptyStateProps) {
  return (
    <Paper variant="outlined" sx={{ p: 6, textAlign: 'center', borderStyle: 'dashed' }}>
      <Stack spacing={1.5} alignItems="center">
        {icon ?? <InboxIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
        <Typography variant="h6">{title}</Typography>
        {description && <Typography color="text.secondary">{description}</Typography>}
        {actionLabel && onAction && (
          <Button variant="contained" onClick={onAction} sx={{ mt: 1 }}>
            {actionLabel}
          </Button>
        )}
      </Stack>
    </Paper>
  );
}
