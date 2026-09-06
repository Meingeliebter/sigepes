import AddIcon from '@mui/icons-material/Add';
import CloudOffIcon from '@mui/icons-material/CloudOff';
import { Alert, Card, CardContent, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';

interface Trip {
  id: string;
  vessel: string;
  zone: string;
  date: string;
  catchKg: number;
  status: string;
}

const TRIPS: Trip[] = [
  { id: 'T-1042', vessel: 'La Sirena', zone: 'Bahía Solano', date: '05 sep 2026', catchKg: 120, status: 'SYNCHRONIZED' },
  { id: 'T-1041', vessel: 'La Sirena', zone: 'Cabo Marzo', date: '03 sep 2026', catchKg: 0, status: 'DRAFT' },
  { id: 'T-1040', vessel: 'Estrella del Mar', zone: 'Málaga', date: '01 sep 2026', catchKg: 85, status: 'PENDING_SYNC' },
];

export function TripsPage() {
  return (
    <>
      <PageHeader title="Bitácora de faenas" subtitle="Registro de faenas y capturas con soporte offline" actionLabel="Nueva faena" actionIcon={<AddIcon />} onAction={() => undefined} />
      <Alert severity="warning" icon={<CloudOffIcon />} sx={{ mb: 2 }}>
        1 faena está pendiente de sincronizar. Se enviará automáticamente al recuperar conexión.
      </Alert>
      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Folio</TableCell>
                  <TableCell>Embarcación</TableCell>
                  <TableCell>Zona</TableCell>
                  <TableCell>Fecha</TableCell>
                  <TableCell align="right">Captura (kg)</TableCell>
                  <TableCell>Estado</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {TRIPS.map((trip) => (
                  <TableRow key={trip.id} hover>
                    <TableCell><Typography fontWeight={700}>{trip.id}</Typography></TableCell>
                    <TableCell>{trip.vessel}</TableCell>
                    <TableCell>{trip.zone}</TableCell>
                    <TableCell>{trip.date}</TableCell>
                    <TableCell align="right">{trip.catchKg}</TableCell>
                    <TableCell><StatusChip status={trip.status} /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <Stack sx={{ mt: 1 }}>
            <Typography variant="caption" color="text.secondary">Mostrando {TRIPS.length} faenas recientes</Typography>
          </Stack>
        </CardContent>
      </Card>
    </>
  );
}
