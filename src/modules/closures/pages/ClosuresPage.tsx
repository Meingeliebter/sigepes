import BlockIcon from '@mui/icons-material/Block';
import { Card, CardContent, Grid, Stack, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';

interface Closure {
  id: string;
  species: string;
  zone: string;
  from: string;
  to: string;
  status: string;
}

const CLOSURES: Closure[] = [
  { id: 'c1', species: 'Camarón blanco', zone: 'Pacífico Norte', from: '01 sep 2026', to: '30 oct 2026', status: 'CLOSED' },
  { id: 'c2', species: 'Pargo rojo', zone: 'Caribe', from: '15 dic 2026', to: '31 ene 2027', status: 'EXPIRING' },
  { id: 'c3', species: 'Langosta espinosa', zone: 'San Andrés', from: '01 abr 2026', to: '30 jun 2026', status: 'OPEN' },
];

export function ClosuresPage() {
  return (
    <>
      <PageHeader title="Vedas pesqueras" subtitle="Periodos de restricción por especie y zona" />
      <Grid container spacing={2}>
        {CLOSURES.map((closure) => (
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
