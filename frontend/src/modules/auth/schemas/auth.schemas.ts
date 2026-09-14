import { z } from 'zod';

const password = z
  .string()
  .min(8, 'Mínimo 8 caracteres')
  .regex(/[A-Z]/, 'Incluye una mayúscula')
  .regex(/[0-9]/, 'Incluye un número')
  .regex(/[^A-Za-z0-9]/, 'Incluye un símbolo');

export const loginSchema = z.object({
  email: z.string().email('Correo inválido'),
  password: z.string().min(1, 'La contraseña es obligatoria'),
});

export const registerSchema = z
  .object({
    nationalId: z.string().regex(/^\d{6,12}$/, 'La identificación debe tener entre 6 y 12 dígitos'),
    fullName: z.string().trim().min(3, 'Ingresa el nombre completo').max(120),
    email: z.string().email('Correo inválido'),
    role: z.enum(['FISHER', 'INSPECTOR', 'ADMIN']),
    password,
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Las contraseñas no coinciden',
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
