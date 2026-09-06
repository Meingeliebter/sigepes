import { Box, Typography, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';

export function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 3 }}>
      <Box sx={{ textAlign: 'center' }}>
        <Typography variant="h2" sx={{ fontWeight: 800, color: 'primary.main' }}>
          404
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 2 }}>
          La página que buscas no existe.
        </Typography>
        <Button variant="contained" onClick={() => navigate('/app/home')}>
          Volver al inicio
        </Button>
      </Box>
    </Box>
  );
}
