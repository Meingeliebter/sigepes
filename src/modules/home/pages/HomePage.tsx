import { Grid, Card, CardActionArea, CardContent, Typography, Box } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import AssignmentIcon from '@mui/icons-material/Assignment';
import MapIcon from '@mui/icons-material/Map';
import InsightsIcon from '@mui/icons-material/Insights';
import { PageContainer } from '@/shared/components/PageContainer';
import { useSessionStore } from '@/shared/storage/sessionStore';
import { hasPermission } from '@/shared/permissions/permissions';

export function HomePage() {
  const navigate = useNavigate();
  const user = useSessionStore((s) => s.user);
  const roles = user?.roles ?? [];

  const cards = [
    {
      title: 'Registrar faena',
      description: 'Bitácora electrónica offline-first',
      icon: <DirectionsBoatIcon fontSize="large" color="secondary" />,
      to: '/app/operations/trips/new',
      show: hasPermission(roles, 'trip:create'),
    },
    {
      title: 'Mis permisos',
      description: 'Solicitud y estado de licencias',
      icon: <AssignmentIcon fontSize="large" color="secondary" />,
      to: '/app/operations/permits',
      show: hasPermission(roles, 'permit:request') || hasPermission(roles, 'permit:review'),
    },
    {
      title: 'Mapa y vedas',
      description: 'Zonas, áreas protegidas y restricciones',
      icon: <MapIcon fontSize="large" color="secondary" />,
      to: '/app/map',
      show: true,
    },
    {
      title: 'Indicadores',
      description: 'Tablero de sostenibilidad',
      icon: <InsightsIcon fontSize="large" color="secondary" />,
      to: '/app/dashboard',
      show: hasPermission(roles, 'dashboard:read'),
    },
  ].filter((c) => c.show);

  return (
    <PageContainer
      title={`Hola, ${user?.fullName ?? ''}`}
      subtitle="Accesos rápidos según tu rol"
    >
      <Grid container spacing={2}>
        {cards.map((card) => (
          <Grid key={card.title} size={{ xs: 12, sm: 6, md: 3 }}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardActionArea
                onClick={() => navigate(card.to)}
                sx={{ height: '100%' }}
              >
                <CardContent>
                  <Box sx={{ mb: 1 }}>{card.icon}</Box>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    {card.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {card.description}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </PageContainer>
  );
}
