import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import GavelIcon from '@mui/icons-material/Gavel';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { Alert, Box, Card, CardContent, CircularProgress, Divider, Grid, List, ListItem, ListItemText, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { StatCard } from '../../../shared/components/StatCard';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService } from '../../../shared/api/domain.service';
import { useAuthStore } from '../../../modules/auth/store/authStore';

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const summaryQuery = useQuery({ queryKey: ['summary'], queryFn: domainService.summary });
  const summary = summaryQuery.data;

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h4" component="h1" sx={{ fontSize: { xs: '1.75rem', md: '2.25rem' } }}>
          Hola, {summary?.fullName ?? user?.fullName ?? 'usuario'}
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 0.5 }}>Resumen operativo de SIGEPES</Typography>
      </Box>

      {summaryQuery.isLoading && <CircularProgress size={28} />}
      {summaryQuery.isError && <Alert severity="error">No fue posible cargar el resumen.</Alert>}
      {summary?.alerts.map((alert) => (
        <Alert key={alert} severity="info">{alert}</Alert>
      ))}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard label="Embarcaciones" value={summary?.vessels.value ?? 0} icon={<DirectionsBoatIcon color="primary" />} helper={summary?.vessels.helper ?? ''} accent="primary" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard label="Permisos vigentes" value={summary?.permits.value ?? 0} icon={<GavelIcon color="success" />} helper={summary?.permits.helper ?? ''} accent="success" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard label="Faenas del mes" value={summary?.trips.value ?? 0} icon={<MenuBookIcon color="secondary" />} helper={summary?.trips.helper ?? ''} accent="secondary" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard label="Alertas activas" value={summary?.warnings.value ?? 0} icon={<WarningAmberIcon color="warning" />} helper={summary?.warnings.helper ?? ''} accent="warning" />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Actividad reciente</Typography>
              <Divider sx={{ mb: 1 }} />
              <List disablePadding>
                {(summary?.recent ?? []).map((item) => (
                  <ListItem key={item.id} secondaryAction={<StatusChip status={item.status} />} disableGutters>
                    <ListItemText primary={item.title} secondary={item.date} />
                  </ListItem>
                ))}
                {summary && summary.recent.length === 0 && (
                  <Typography color="text.secondary" variant="body2">Todavía no hay actividad registrada.</Typography>
                )}
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
                {(summary?.compliance ?? []).map((row) => (
                  <Stack key={row.label} direction="row" justifyContent="space-between">
                    <Typography>{row.label}</Typography>
                    <StatusChip status={row.status} />
                  </Stack>
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Stack>
  );
}
