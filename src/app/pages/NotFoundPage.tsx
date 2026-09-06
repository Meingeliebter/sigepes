import { Button, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';

export function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <Stack alignItems="center" justifyContent="center" minHeight="80vh" spacing={2}>
      <Typography variant="h1" fontWeight={900} color="primary">404</Typography>
      <Typography>La página que buscas no existe.</Typography>
      <Button variant="contained" onClick={() => void navigate('/app/dashboard')}>Volver al inicio</Button>
    </Stack>
  );
}
