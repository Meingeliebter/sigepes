import { zodResolver } from '@hookform/resolvers/zod';
import LoginIcon from '@mui/icons-material/Login';
import { Alert, Button, Divider, Link, Stack, TextField, Typography } from '@mui/material';
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
    defaultValues: { email: '', password: '' },
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
      <Stack spacing={0.5} sx={{ mb: 3 }}>
        <Typography variant="h5" component="h1">Iniciar sesión</Typography>
        <Typography variant="body2" color="text.secondary">
          Accede con tu cuenta institucional para operar el sistema.
        </Typography>
      </Stack>

      {message && (
        <Alert severity="error" sx={{ mb: 2.5 }} role="alert">{message}</Alert>
      )}

      <Stack component="form" spacing={2.5} onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <Controller
          name="email"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Correo institucional"
              autoComplete="email"
              autoFocus
              required
              error={!!errors.email}
              helperText={errors.email?.message}
            />
          )}
        />
        <Controller
          name="password"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Contraseña"
              type="password"
              autoComplete="current-password"
              required
              error={!!errors.password}
              helperText={errors.password?.message}
            />
          )}
        />
        <Button type="submit" size="large" variant="contained" startIcon={<LoginIcon />} disabled={mutation.isPending}>
          {mutation.isPending ? 'Ingresando...' : 'Ingresar'}
        </Button>

        <Divider sx={{ my: 0.5 }} />

        <Typography textAlign="center" variant="body2" color="text.secondary">
          ¿No tienes cuenta?{' '}
          <Link component={RouterLink} to="/register" fontWeight={600}>
            Regístrate como pescador o armador
          </Link>
        </Typography>
      </Stack>
    </AuthShell>
  );
}
