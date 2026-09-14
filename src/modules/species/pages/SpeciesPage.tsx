import PhishingIcon from '@mui/icons-material/Phishing';
import { Alert, Card, CardContent, CircularProgress, Grid, Stack, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { EmptyState } from '../../../shared/components/EmptyState';
import { StatusChip } from '../../../shared/components/StatusChip';
import { useQuery } from '@tanstack/react-query';
import { domainService } from '../../../shared/api/domain.service';

export function SpeciesPage() {
  const speciesQuery = useQuery({ queryKey: ['species'], queryFn: domainService.species });
  const species = speciesQuery.data ?? [];
  return (
    <>
      <PageHeader title="Especies" subtitle="Catálogo de especies con tallas mínimas y estado de veda" />
      {speciesQuery.isLoading && <CircularProgress size={28} />}
      {speciesQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar el catálogo de especies.</Alert>}
      {!speciesQuery.isLoading && !speciesQuery.isError && species.length === 0 && (
        <EmptyState
          icon={<PhishingIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
          title="El catálogo de especies está vacío"
          description="La autoridad aún no ha registrado especies con su talla mínima legal."
        />
      )}
      <Grid container spacing={2}>
        {species.map((sp) => (
          <Grid size={{ xs: 12, sm: 6, lg: 3 }} key={sp.id}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Typography variant="h6">{sp.commonName}</Typography>
                  <StatusChip status={sp.status} />
                </Stack>
                <Typography variant="body2" color="text.secondary" fontStyle="italic" sx={{ mb: 1.5 }}>{sp.scientificName}</Typography>
                <Typography variant="body2">Talla mínima: {sp.minSizeCm} cm</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
}
