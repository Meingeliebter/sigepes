import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import GavelIcon from '@mui/icons-material/Gavel';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { Alert, Box, Card, CardContent, Divider, Grid, List, ListItem, ListItemText, Stack, Typography } from '@mui/material';
import { StatCard } from '../../../shared/components/StatCard';
import { StatusChip } from '../../../shared/components/StatusChip';
import { useAuthStore } from '../../../modules/auth/store/authStore';

const RECENT = [
  { title: 'Faena cerrada en Bahía Solano', status: 'SYNCHRONIZED', date: '05 sep 2026' },
  { title: 'Permiso PES-0231 aprobado', status: 'APPROVED', date: '04 sep 2026' },
  { title: 'Veda de camarón activada', status: 'CLOSED', date: '02 sep 2026' },
  { title: 'Faena pendiente de sincronizar', status: 'PENDING_SYNC', date: '01 sep 2026' },
];

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h4">Hola, {user?.fullName ?? 'usuario'}</Typography>
        <Typography color="text.secondary">Resumen operativo de SIGEPES</Typography>
      </Box>

      <Alert severity="info">Tienes 1 faena por sincronizar y 1 permiso próximo a vencer.</Alert>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard label="Embarcaciones" value={2} icon={<DirectionsBoatIcon color="primary" />} helper="1 en mantenimiento" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard label="Permisos vigentes" value={1} icon={<GavelIcon color="success" />} helper="1 por vencer" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard label="Faenas del mes" value={8} icon={<MenuBookIcon color="secondary" />} helper="7 sincronizadas" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard label="Alertas activas" value={3} icon={<WarningAmberIcon color="warning" />} helper="2 vedas y 1 zona" />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Actividad reciente</Typography>
              <Divider sx={{ mb: 1 }} />
              <List disablePadding>
                {RECENT.map((item) => (
                  <ListItem key={item.title} secondaryAction={<StatusChip status={item.status} />} disableGutters>
                    <ListItemText primary={item.title} secondary={item.date} />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 5 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Cumplimiento normativo</Typography>
              <Divider sx={{ mb: 2 }} />
              <Stack spacing={1.5}>
                <Stack direction="row" justifyContent="space-between"><Typography>Permisos al día</Typography><StatusChip status="VALID" /></Stack>
                <Stack direction="row" justifyContent="space-between"><Typography>Tallas mínimas</Typography><StatusChip status="ACTIVE" /></Stack>
                <Stack direction="row" justifyContent="space-between"><Typography>Veda de camarón</Typography><StatusChip status="CLOSED" /></Stack>
                <Stack direction="row" justifyContent="space-between"><Typography>Sincronización</Typography><StatusChip status="PENDING_SYNC" /></Stack>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Stack>
  );
}
