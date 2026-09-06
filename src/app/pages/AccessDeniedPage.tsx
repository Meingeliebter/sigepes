import LockIcon from '@mui/icons-material/Lock';
import { Button, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';

export function AccessDeniedPage() {
  const navigate = useNavigate();
  return (
    <Stack alignItems="center" justifyContent="center" minHeight="70vh" spacing={2}>
      <LockIcon sx={{ fontSize: 64, color: 'error.main' }} />
      <Typography variant="h5">Acceso restringido</Typography>
      <Typography color="text.secondary">Tu rol no tiene permisos para ver esta sección.</Typography>
      <Button variant="contained" onClick={() => void navigate('/app/dashboard')}>Ir al dashboard</Button>
    </Stack>
  );
}
