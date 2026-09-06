import { PageContainer } from '@/shared/components/PageContainer';
import { Typography } from '@mui/material';

export function NewTripPage() {
  return (
    <PageContainer title="Nueva faena" subtitle="Registro offline-first (en construcción)">
      <Typography color="text.secondary">
        Aquí irá el stepper de registro de faena: embarcación, zona GPS, especies,
        tallas, validación de vedas/cuotas y declaración de captura nula.
      </Typography>
    </PageContainer>
  );
}
