import { zodResolver } from '@hookform/resolvers/zod';
import HowToRegIcon from '@mui/icons-material/HowToReg';
import { Alert, Button, Divider, Link, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import axios from 'axios';
import { Controller, useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { authService } from '../api/auth.service';
import { AuthShell } from '../components/AuthShell';
import { registerSchema, type RegisterFormValues } from '../schemas/auth.schemas';
import { useAuthStore } from '../store/authStore';

function SectionLabel({ children }: { children: string }) {
  return (
    <Typography variant="subtitle2" component="h2" color="primary.dark" sx={{ display: 'block', mt: 1.5, pb: 0.75, borderBottom: '1px solid', borderColor: 'divider' }}>
      {children}
    </Typography>
  );
}

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
      <Stack spacing={0.5} sx={{ mb: 3 }}>
        <Typography variant="h5" component="h1">Crear cuenta</Typography>
        <Typography variant="body2" color="text.secondary">
          Formaliza tu registro para operar embarcaciones, solicitar permisos y registrar faenas.
          Los campos marcados con <Typography component="span" color="error.main">*</Typography> son obligatorios.
        </Typography>
      </Stack>

      {apiError && (
        <Alert severity="error" sx={{ mb: 2.5 }} role="alert">{apiError}</Alert>
      )}

      <Stack component="form" spacing={2} onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <SectionLabel>Datos de identificación</SectionLabel>

        <Controller
          name="nationalId"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Número de identificación"
              required
              autoComplete="off"
              inputProps={{ inputMode: 'numeric' }}
              error={!!errors.nationalId}
              helperText={errors.nationalId?.message ?? 'Documento sin puntos ni espacios (6 a 12 dígitos).'}
            />
          )}
        />
        <Controller
          name="fullName"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Nombre completo"
              required
              autoComplete="name"
              error={!!errors.fullName}
              helperText={errors.fullName?.message}
            />
          )}
        />

        <SectionLabel>Cuenta de acceso</SectionLabel>

        <Controller
          name="email"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Correo electrónico"
              required
              autoComplete="email"
              error={!!errors.email}
              helperText={errors.email?.message}
            />
          )}
        />
        <Controller
          name="role"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              select
              label="Tipo de actor"
              required
              error={!!errors.role}
              helperText={errors.role?.message ?? 'Define qué podrás hacer en el sistema.'}
            >
              <MenuItem value="FISHER">Pescador artesanal</MenuItem>
              <MenuItem value="INSPECTOR">Inspector AUNAP</MenuItem>
              <MenuItem value="ADMIN">Administrador</MenuItem>
            </TextField>
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
              required
              autoComplete="new-password"
              error={!!errors.password}
              helperText={errors.password?.message ?? 'Mínimo 8 caracteres, con mayúscula, número y símbolo.'}
            />
          )}
        />
        <Controller
          name="confirmPassword"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Confirmar contraseña"
              type="password"
              required
              autoComplete="new-password"
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword?.message}
            />
          )}
        />

        <Button type="submit" size="large" variant="contained" startIcon={<HowToRegIcon />} disabled={mutation.isPending} sx={{ mt: 1 }}>
          {mutation.isPending ? 'Registrando...' : 'Crear cuenta'}
        </Button>

        <Divider sx={{ my: 0.5 }} />

        <Typography textAlign="center" variant="body2" color="text.secondary">
          ¿Ya tienes cuenta?{' '}
          <Link component={RouterLink} to="/login" fontWeight={600}>
            Inicia sesión
          </Link>
        </Typography>
      </Stack>
    </AuthShell>
  );
}
