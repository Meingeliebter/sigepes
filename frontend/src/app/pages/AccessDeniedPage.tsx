import LockIcon from '@mui/icons-material/Lock';
import { Button, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';

export function AccessDeniedPage() {
  const navigate = useNavigate();
  return (
    <Stack alignItems="center" justifyContent="center" minHeight="70vh" spacing={2} sx={{ textAlign: 'center', px: 3 }}>
      <LockIcon sx={{ fontSize: 56, color: 'error.main' }} />
      <Typography variant="h5">Acceso restringido</Typography>
      <Typography color="text.secondary" sx={{ maxWidth: 420 }}>
        Tu rol institucional no tiene permisos para ver esta sección. Si crees que es un error,
        contacta al administrador del sistema.
      </Typography>
      <Button variant="contained" onClick={() => void navigate('/app/dashboard')}>Ir al panel</Button>
    </Stack>
  );
}
