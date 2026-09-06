import { Card, CardContent, Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';

interface AuditEvent {
  id: string;
  timestamp: string;
  actor: string;
  action: string;
  entity: string;
}

const EVENTS: AuditEvent[] = [
  { id: 'a1', timestamp: '05 sep 2026 14:32', actor: 'Ana Inspectora', action: 'Aprobó permiso', entity: 'PES-0231' },
  { id: 'a2', timestamp: '05 sep 2026 11:10', actor: 'Pescador Demo', action: 'Cerró faena', entity: 'T-1042' },
  { id: 'a3', timestamp: '04 sep 2026 09:05', actor: 'Carlos Admin', action: 'Activó veda', entity: 'Camarón blanco' },
  { id: 'a4', timestamp: '03 sep 2026 16:48', actor: 'Pescador Demo', action: 'Registró embarcación', entity: 'Estrella del Mar' },
];

export function AuditPage() {
  return (
    <>
      <PageHeader title="Auditoría" subtitle="Registro inmutable de eventos sensibles" />
      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Fecha y hora</TableCell>
                  <TableCell>Responsable</TableCell>
                  <TableCell>Acción</TableCell>
                  <TableCell>Entidad</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {EVENTS.map((event) => (
                  <TableRow key={event.id} hover>
                    <TableCell><Typography variant="body2" color="text.secondary">{event.timestamp}</Typography></TableCell>
                    <TableCell>{event.actor}</TableCell>
                    <TableCell><Chip label={event.action} size="small" variant="outlined" /></TableCell>
                    <TableCell><Typography fontWeight={700}>{event.entity}</Typography></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </>
  );
}
