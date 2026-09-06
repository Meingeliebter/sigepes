import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Link, Stack, TextField, Typography } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import axios from 'axios';
import { Controller, useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { AuthShell } from '../components/AuthShell';
import { authService } from '../api/auth.service';
import { loginSchema, type LoginFormValues } from '../schemas/auth.schemas';
import { useAuthStore } from '../store/authStore';

export function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: 'pescador@sigepes.gov.co', password: 'Pesca#2026' },
  });

  const mutation = useMutation({
    mutationFn: authService.login,
    onSuccess: (session) => {
      setSession(session);
      void navigate('/app/dashboard');
    },
  });

  const message = axios.isAxiosError(mutation.error)
    ? String(mutation.error.response?.data?.message ?? 'No fue posible iniciar sesión')
    : null;

  return (
    <AuthShell>
      <Typography variant="h6" sx={{ mb: 2 }}>Iniciar sesión</Typography>
      {message && (
        <Alert severity="error" sx={{ mb: 2 }}>{message}</Alert>
      )}
      <Stack component="form" spacing={2} onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <Controller
          name="email"
          control={control}
          render={({ field }) => (
            <TextField {...field} label="Correo" autoComplete="email" error={!!errors.email} helperText={errors.email?.message} />
          )}
        />
        <Controller
          name="password"
          control={control}
          render={({ field }) => (
            <TextField {...field} label="Contraseña" type="password" autoComplete="current-password" error={!!errors.password} helperText={errors.password?.message} />
          )}
        />
        <Button type="submit" variant="contained" disabled={mutation.isPending}>
          {mutation.isPending ? 'Ingresando...' : 'Ingresar'}
        </Button>
        <Typography textAlign="center">
          ¿No tienes cuenta? <Link component={RouterLink} to="/register">Regístrate</Link>
        </Typography>
      </Stack>
    </AuthShell>
  );
}
