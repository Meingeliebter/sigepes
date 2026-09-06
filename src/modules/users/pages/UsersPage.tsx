import AddIcon from '@mui/icons-material/Add';
import { Avatar, Card, CardContent, Chip, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { ROLE_LABEL, type Role } from '../../../modules/auth/types/auth.types';

interface Row {
  id: string;
  fullName: string;
  email: string;
  nationalId: string;
  role: Role;
  status: string;
}

const USERS: Row[] = [
  { id: 'u1', fullName: 'Pescador Demo', email: 'pescador@sigepes.gov.co', nationalId: '1001001001', role: 'FISHER', status: 'Activo' },
  { id: 'u2', fullName: 'Ana Inspectora', email: 'inspector@sigepes.gov.co', nationalId: '2002002002', role: 'INSPECTOR', status: 'Activo' },
  { id: 'u3', fullName: 'Carlos Admin', email: 'admin@sigepes.gov.co', nationalId: '3003003003', role: 'ADMIN', status: 'Activo' },
];

const initials = (name: string) =>
  name
    .split(' ')
    .map((part) => part[0])
    .slice(0, 2)
    .join('');

export function UsersPage() {
  return (
    <>
      <PageHeader title="Usuarios" subtitle="Gestión de cuentas y roles del sistema" actionLabel="Nuevo usuario" actionIcon={<AddIcon />} onAction={() => undefined} />
      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Usuario</TableCell>
                  <TableCell>Correo</TableCell>
                  <TableCell>Identificación</TableCell>
                  <TableCell>Rol</TableCell>
                  <TableCell>Estado</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {USERS.map((user) => (
                  <TableRow key={user.id} hover>
                    <TableCell>
                      <Stack direction="row" spacing={1.5} alignItems="center">
                        <Avatar sx={{ bgcolor: 'primary.main', width: 34, height: 34, fontSize: 14 }}>{initials(user.fullName)}</Avatar>
                        <Typography fontWeight={700}>{user.fullName}</Typography>
                      </Stack>
                    </TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>{user.nationalId}</TableCell>
                    <TableCell><Chip label={ROLE_LABEL[user.role]} size="small" color="secondary" variant="outlined" /></TableCell>
                    <TableCell><Chip label={user.status} size="small" color="success" /></TableCell>
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
