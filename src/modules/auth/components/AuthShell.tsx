import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import GavelIcon from '@mui/icons-material/Gavel';
import PublicIcon from '@mui/icons-material/Public';
import { Box, Paper, Stack, Typography } from '@mui/material';
import type { PropsWithChildren } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Footer } from '../../../shared/components/Footer';
import { Logotype } from '../../../shared/components/Logotype';
import { SkipLink } from '../../../shared/components/SkipLink';
import { TopUtilityBar } from '../../../shared/components/TopUtilityBar';

const HIGHLIGHTS = [
  { icon: <VerifiedUserIcon fontSize="small" />, text: 'Formalización de pescadores artesanales ante la autoridad competente' },
  { icon: <GavelIcon fontSize="small" />, text: 'Control digital de permisos, vedas y tallas mínimas legales' },
  { icon: <PublicIcon fontSize="small" />, text: 'Alineado con la Ley 13 de 1990, la Ley 1851 de 2017 y el ODS 14' },
];

export function AuthShell({ children }: PropsWithChildren) {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <SkipLink />
      <TopUtilityBar />

      <Box
        component="main"
        id="main-content"
        sx={{
          flex: 1,
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: '1.1fr 1fr' },
        }}
      >
        {/* Panel institucional — oculto en móvil para priorizar el formulario */}
        <Box
          sx={{
            display: { xs: 'none', md: 'flex' },
            flexDirection: 'column',
            justifyContent: 'space-between',
            color: '#fff',
            px: 6,
            py: 6,
            position: 'relative',
            overflow: 'hidden',
            backgroundImage:
              'linear-gradient(180deg, rgba(7,30,48,.92) 0%, rgba(7,30,48,.82) 45%, rgba(7,30,48,.95) 100%), url(/images/auth-pescado.jpg)',
            backgroundSize: 'cover',
            backgroundPosition: 'center',
          }}
        >
          <RouterLink to="/" aria-label="Ir a la página de inicio de SIGEPES">
            <Logotype variant="light" />
          </RouterLink>

          <Stack spacing={3} sx={{ maxWidth: 460 }}>
            <Typography variant="h4" sx={{ fontWeight: 800, lineHeight: 1.2 }}>
              Gestión pesquera legal y sostenible
            </Typography>
            <Typography sx={{ color: 'rgba(255,255,255,.75)' }}>
              Portal institucional para el registro, control y trazabilidad de la actividad
              pesquera artesanal e industrial en Colombia.
            </Typography>
            <Stack spacing={1.75}>
              {HIGHLIGHTS.map((item) => (
                <Stack key={item.text} direction="row" spacing={1.5} alignItems="flex-start">
                  <Box sx={{ color: 'secondary.light', mt: 0.25 }}>{item.icon}</Box>
                  <Typography variant="body2" sx={{ color: 'rgba(255,255,255,.85)' }}>
                    {item.text}
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </Stack>

          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,.55)' }}>
            Foto: Tapan Kumar Choudhury / Unsplash
          </Typography>
        </Box>

        {/* Panel del formulario */}
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', p: { xs: 3, sm: 5 } }}>
          <Box sx={{ display: { xs: 'flex', md: 'none' }, justifyContent: 'center', mb: 3 }}>
            <RouterLink to="/">
              <Logotype />
            </RouterLink>
          </Box>
          <Paper
            component="section"
            variant="outlined"
            sx={{ width: '100%', maxWidth: 440, p: { xs: 3, sm: 4.5 } }}
          >
            {children}
          </Paper>
        </Box>
      </Box>

      <Footer compact />
    </Box>
  );
}
