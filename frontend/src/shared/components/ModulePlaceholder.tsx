import { Card, CardContent, Stack, Typography } from '@mui/material';

interface ModulePlaceholderProps {
  title: string;
  description?: string;
}

export function ModulePlaceholder({ title, description }: ModulePlaceholderProps) {
  return (
    <Stack spacing={3}>
      <div>
        <Typography variant="h4">{title}</Typography>
        <Typography color="text.secondary">Módulo en construcción</Typography>
      </div>
      <Card>
        <CardContent>
          <Typography color="text.secondary">
            {description ?? `Contenido del módulo "${title}" pendiente de implementación.`}
          </Typography>
        </CardContent>
      </Card>
    </Stack>
  );
}