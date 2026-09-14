import AddIcon from '@mui/icons-material/Add';
import PeopleIcon from '@mui/icons-material/People';
import {
  Alert, Avatar, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, MenuItem, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { ROLE_LABEL, type Role } from '../../../modules/auth/types/auth.types';

const initials = (name: string) =>
  name
    .split(' ')
    .map((part) => part[0])
    .slice(0, 2)
    .join('');

const EMPTY = { nationalId: '', fullName: '', email: '', role: 'FISHER' as Role, password: '' };

export function UsersPage() {
  const queryClient = useQueryClient();
  const usersQuery = useQuery({ queryKey: ['users'], queryFn: domainService.users });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(EMPTY);

  const createMutation = useMutation({
    mutationFn: domainService.createUser,
    onSuccess: async () => {
      setOpen(false);
      setForm(EMPTY);
      await queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const users = usersQuery.data ?? [];
  const error = createMutation.error ? toAppError(createMutation.error) : null;

  return (
    <>
      <PageHeader
        title="Usuarios"
        subtitle="Gestión de cuentas y roles del sistema"
        actionLabel="Nuevo usuario"
        actionIcon={<AddIcon />}
        onAction={() => setOpen(true)}
      />
      {usersQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar los usuarios.</Alert>}
      <Card>
        <CardContent>
          {usersQuery.isLoading && <CircularProgress size={28} />}
          {!usersQuery.isLoading && !usersQuery.isError && users.length === 0 ? (
            <EmptyState
              icon={<PeopleIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
              title="No hay usuarios registrados"
              description="Crea el primer usuario del sistema."
              actionLabel="Nuevo usuario"
              onAction={() => setOpen(true)}
            />
          ) : (
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
                {users.map((user) => (
                  <TableRow key={user.id} hover>
                    <TableCell>
                      <Stack direction="row" spacing={1.5} alignItems="center">
                        <Avatar sx={{ bgcolor: 'primary.main', width: 34, height: 34, fontSize: 14 }}>{initials(user.fullName)}</Avatar>
                        <Typography fontWeight={700}>{user.fullName}</Typography>
                      </Stack>
                    </TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>{user.nationalId}</TableCell>
                    <TableCell><Chip label={ROLE_LABEL[user.role] ?? user.role} size="small" color="secondary" variant="outlined" /></TableCell>
                    <TableCell><StatusChip status={user.status} /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Nuevo usuario</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error.message}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Identificación" value={form.nationalId} onChange={(e) => setForm({ ...form, nationalId: e.target.value })} />
            <TextField label="Nombre completo" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            <TextField label="Correo" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            <TextField select label="Rol" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
              {(Object.keys(ROLE_LABEL) as Role[]).map((role) => (
                <MenuItem key={role} value={role}>{ROLE_LABEL[role]}</MenuItem>
              ))}
            </TextField>
            <TextField label="Contraseña" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancelar</Button>
          <Button
            variant="contained"
            disabled={createMutation.isPending || !form.nationalId || !form.fullName || !form.email || !form.password}
            onClick={() => createMutation.mutate(form)}
          >
            {createMutation.isPending ? 'Creando...' : 'Crear usuario'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
