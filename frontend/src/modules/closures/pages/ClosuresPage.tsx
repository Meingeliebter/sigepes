import BlockIcon from '@mui/icons-material/Block';
import { Alert, Card, CardContent, CircularProgress, Grid, Stack, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { EmptyState } from '../../../shared/components/EmptyState';
import { StatusChip } from '../../../shared/components/StatusChip';
import { useQuery } from '@tanstack/react-query';
import { domainService } from '../../../shared/api/domain.service';

export function ClosuresPage() {
  const closuresQuery = useQuery({ queryKey: ['closures'], queryFn: domainService.closures });
  const closures = closuresQuery.data ?? [];
  return (
    <>
      <PageHeader title="Vedas pesqueras" subtitle="Periodos de restricción por especie y zona" />
      {closuresQuery.isLoading && <CircularProgress size={28} />}
      {closuresQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar las vedas.</Alert>}
      {!closuresQuery.isLoading && !closuresQuery.isError && closures.length === 0 && (
        <EmptyState
          icon={<BlockIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
          title="No hay vedas registradas"
          description="Actualmente no existen periodos de restricción declarados por la autoridad."
        />
      )}
      <Grid container spacing={2}>
        {closures.map((closure) => (
          <Grid size={{ xs: 12, md: 4 }} key={closure.id}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <BlockIcon color="error" />
                    <Typography variant="h6">{closure.species}</Typography>
                  </Stack>
                  <StatusChip status={closure.status} />
                </Stack>
                <Typography variant="body2" color="text.secondary">Zona: {closure.zone}</Typography>
                <Typography variant="body2" color="text.secondary">Del {closure.from} al {closure.to}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
}
