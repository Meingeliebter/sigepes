import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Link, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import axios from 'axios';
import { Controller, useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { authService } from '../api/auth.service';
import { AuthShell } from '../components/AuthShell';
import { registerSchema, type RegisterFormValues } from '../schemas/auth.schemas';
import { useAuthStore } from '../store/authStore';

const FIELDS = [
  { name: 'nationalId', label: 'Identificación', type: 'text' },
  { name: 'fullName', label: 'Nombre completo', type: 'text' },
  { name: 'email', label: 'Correo', type: 'text' },
  { name: 'password', label: 'Contraseña', type: 'password' },
  { name: 'confirmPassword', label: 'Confirmar contraseña', type: 'password' },
] as const;

export function RegisterPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { nationalId: '', fullName: '', email: '', role: 'FISHER', password: '', confirmPassword: '' },
  });

  const mutation = useMutation({
    mutationFn: authService.register,
    onSuccess: (session) => {
      setSession(session);
      void navigate('/app/dashboard');
    },
  });

  const apiError = axios.isAxiosError(mutation.error)
    ? String(mutation.error.response?.data?.message ?? 'No fue posible registrar el usuario')
    : null;

  return (
    <AuthShell>
      <Typography variant="h6" sx={{ mb: 2 }}>Crear cuenta</Typography>
      {apiError && (
        <Alert severity="error" sx={{ mb: 2 }}>{apiError}</Alert>
      )}
      <Stack component="form" spacing={2} onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        {FIELDS.map((f) => (
          <Controller
            key={f.name}
            name={f.name}
            control={control}
            render={({ field }) => (
              <TextField {...field} label={f.label} type={f.type} error={!!errors[f.name]} helperText={errors[f.name]?.message} />
            )}
          />
        ))}
        <Controller
          name="role"
          control={control}
          render={({ field }) => (
            <TextField {...field} select label="Rol" error={!!errors.role} helperText={errors.role?.message}>
              <MenuItem value="FISHER">Pescador</MenuItem>
              <MenuItem value="INSPECTOR">Inspector AUNAP</MenuItem>
              <MenuItem value="ADMIN">Administrador</MenuItem>
            </TextField>
          )}
        />
        <Button type="submit" variant="contained" disabled={mutation.isPending}>
          {mutation.isPending ? 'Registrando...' : 'Crear cuenta'}
        </Button>
        <Typography textAlign="center">
          ¿Ya tienes cuenta? <Link component={RouterLink} to="/login">Inicia sesión</Link>
        </Typography>
      </Stack>
    </AuthShell>
  );
}
