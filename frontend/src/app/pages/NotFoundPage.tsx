import { Button, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { Logotype } from '../../shared/components/Logotype';

export function NotFoundPage() {
  return (
    <Stack alignItems="center" justifyContent="center" minHeight="100vh" spacing={3} sx={{ px: 3, textAlign: 'center' }}>
      <Logotype showTagline={false} />
      <Typography variant="h1" fontWeight={800} color="primary.main">404</Typography>
      <Typography variant="h6">La página que buscas no existe</Typography>
      <Typography color="text.secondary" sx={{ maxWidth: 420 }}>
        Verifica la dirección o vuelve al inicio del portal institucional.
      </Typography>
      <Button component={RouterLink} to="/" variant="contained">Volver al inicio</Button>
    </Stack>
  );
}
