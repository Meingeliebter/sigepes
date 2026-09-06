import { Button, Card, CardContent, Divider, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../auth/store/authStore';

const roleLabels: Record<string, string> = {
  FISHER: 'Pescador',
  INSPECTOR: 'Inspector AUNAP',
  ADMIN: 'Administrador',
};

function Row({ label, value }: { label: string; value?: string }) {
  return (
    <Stack direction="row" justifyContent="space-between">
      <Typography color="text.secondary">{label}</Typography>
      <Typography fontWeight={600}>{value ?? '-'}</Typography>
    </Stack>
  );
}

export function ProfilePage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  return (
    <Stack spacing={3}>
      <div>
        <Typography variant="h4">Perfil</Typography>
        <Typography color="text.secondary">Datos de la sesión actual</Typography>
      </div>
      <Card>
        <CardContent>
          <Stack spacing={1.5}>
            <Row label="Nombre" value={user?.fullName} />
            <Divider />
            <Row label="Identificación" value={user?.nationalId} />
            <Divider />
            <Row label="Correo" value={user?.email} />
            <Divider />
            <Row label="Rol" value={user ? roleLabels[user.role] : undefined} />
          </Stack>
        </CardContent>
      </Card>
      <Button variant="outlined" color="error" sx={{ alignSelf: 'flex-start' }} onClick={() => { logout(); void navigate('/login'); }}>
        Cerrar sesión
      </Button>
    </Stack>
  );
}