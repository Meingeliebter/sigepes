import { Card, CardContent, Grid, Stack, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';

interface Species {
  id: string;
  commonName: string;
  scientificName: string;
  minSizeCm: number;
  status: string;
}

const SPECIES: Species[] = [
  { id: 's1', commonName: 'Pargo rojo', scientificName: 'Lutjanus purpureus', minSizeCm: 35, status: 'ACTIVE' },
  { id: 's2', commonName: 'Camarón blanco', scientificName: 'Litopenaeus vannamei', minSizeCm: 12, status: 'CLOSED' },
  { id: 's3', commonName: 'Atún aleta amarilla', scientificName: 'Thunnus albacares', minSizeCm: 50, status: 'ACTIVE' },
  { id: 's4', commonName: 'Sierra', scientificName: 'Scomberomorus sierra', minSizeCm: 40, status: 'ACTIVE' },
];

export function SpeciesPage() {
  return (
    <>
      <PageHeader title="Especies" subtitle="Catálogo de especies con tallas mínimas y estado de veda" />
      <Grid container spacing={2}>
        {SPECIES.map((sp) => (
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
