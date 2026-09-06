import AddIcon from '@mui/icons-material/Add';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import { Card, CardContent, Chip, Grid, Stack, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';

interface Vessel {
  id: string;
  name: string;
  registration: string;
  gears: string[];
  status: string;
  port: string;
}

const VESSELS: Vessel[] = [
  { id: 'v1', name: 'La Sirena', registration: 'DIMAR-CO-10231', gears: ['Línea de mano', 'Nasa'], status: 'ACTIVE', port: 'Bahía Solano' },
  { id: 'v2', name: 'Estrella del Mar', registration: 'DIMAR-CO-11875', gears: ['Palangre'], status: 'UNDER_MAINTENANCE', port: 'Buenaventura' },
];

export function VesselsPage() {
  return (
    <>
      <PageHeader title="Embarcaciones" subtitle="Registro y estado de tus embarcaciones y artes de pesca" actionLabel="Nueva embarcación" actionIcon={<AddIcon />} onAction={() => undefined} />
      <Grid container spacing={2}>
        {VESSELS.map((vessel) => (
          <Grid size={{ xs: 12, md: 6 }} key={vessel.id}>
            <Card>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                  <Stack direction="row" spacing={1.5} alignItems="center">
                    <DirectionsBoatIcon color="primary" />
                    <Typography variant="h6">{vessel.name}</Typography>
                  </Stack>
                  <StatusChip status={vessel.status} />
                </Stack>
                <Typography color="text.secondary" variant="body2">Matrícula {vessel.registration}</Typography>
                <Typography color="text.secondary" variant="body2" sx={{ mb: 1.5 }}>Puerto base: {vessel.port}</Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  {vessel.gears.map((gear) => (
                    <Chip key={gear} label={gear} size="small" variant="outlined" />
                  ))}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
}
