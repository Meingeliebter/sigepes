import HistoryIcon from '@mui/icons-material/History';
import { Alert, Card, CardContent, Chip, CircularProgress, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { EmptyState } from '../../../shared/components/EmptyState';
import { useQuery } from '@tanstack/react-query';
import { domainService } from '../../../shared/api/domain.service';

export function AuditPage() {
  const auditQuery = useQuery({ queryKey: ['audit'], queryFn: domainService.audit });
  const events = auditQuery.data ?? [];
  return (
    <>
      <PageHeader title="Auditoría" subtitle="Registro inmutable de eventos sensibles" />
      <Card>
        <CardContent>
          {auditQuery.isLoading && <CircularProgress size={28} />}
          {auditQuery.isError && <Alert severity="error">No fue posible cargar la bitácora de auditoría.</Alert>}
          {!auditQuery.isLoading && !auditQuery.isError && events.length === 0 ? (
            <EmptyState
              icon={<HistoryIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
              title="Sin eventos registrados"
              description="Aún no se han registrado operaciones sensibles en el sistema."
            />
          ) : (
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
                  {events.map((event) => (
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
          )}
        </CardContent>
      </Card>
    </>
  );
}
