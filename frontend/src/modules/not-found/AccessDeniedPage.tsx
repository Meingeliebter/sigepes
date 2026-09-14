import { Box, Typography, Button } from '@mui/material';
import LockIcon from '@mui/icons-material/Lock';
import { useNavigate } from 'react-router-dom';

export function AccessDeniedPage() {
  const navigate = useNavigate();
  return (
    <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
      <Box sx={{ textAlign: 'center' }}>
        <LockIcon sx={{ fontSize: 64, color: 'error.main', mb: 1 }} />
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          Acceso denegado
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 2 }}>
          No tienes permisos para ver esta sección.
        </Typography>
        <Button variant="contained" onClick={() => navigate('/app/home')}>
          Volver al inicio
        </Button>
      </Box>
    </Box>
  );
}
