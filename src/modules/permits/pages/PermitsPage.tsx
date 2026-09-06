import AddIcon from '@mui/icons-material/Add';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import { Button, Card, CardContent, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';

interface Permit {
  id: string;
  type: string;
  holder: string;
  issued: string;
  expires: string;
  status: string;
}

const PERMITS: Permit[] = [
  { id: 'PES-0231', type: 'Pesca artesanal', holder: 'La Sirena', issued: '10 ene 2026', expires: '10 ene 2027', status: 'APPROVED' },
  { id: 'PES-0244', type: 'Pesca artesanal', holder: 'Estrella del Mar', issued: '05 sep 2026', expires: '05 sep 2027', status: 'PENDING' },
  { id: 'PES-0198', type: 'Comercialización', holder: 'Cooperativa Pacífico', issued: '01 mar 2025', expires: '01 mar 2026', status: 'EXPIRED' },
];

export function PermitsPage() {
  return (
    <>
      <PageHeader title="Permisos" subtitle="Solicitud, seguimiento y verificación de permisos de pesca" actionLabel="Solicitar permiso" actionIcon={<AddIcon />} onAction={() => undefined} />
      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Titular</TableCell>
                  <TableCell>Emisión</TableCell>
                  <TableCell>Vencimiento</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell align="right">Acción</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {PERMITS.map((permit) => (
                  <TableRow key={permit.id} hover>
                    <TableCell><Typography fontWeight={700}>{permit.id}</Typography></TableCell>
                    <TableCell>{permit.type}</TableCell>
                    <TableCell>{permit.holder}</TableCell>
                    <TableCell>{permit.issued}</TableCell>
                    <TableCell>{permit.expires}</TableCell>
                    <TableCell><StatusChip status={permit.status} /></TableCell>
                    <TableCell align="right"><Button size="small" startIcon={<QrCode2Icon />}>QR</Button></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <Stack sx={{ mt: 1 }}>
            <Typography variant="caption" color="text.secondary">Mostrando {PERMITS.length} permisos</Typography>
          </Stack>
        </CardContent>
      </Card>
    </>
  );
}
