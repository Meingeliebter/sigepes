import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import GavelIcon from '@mui/icons-material/Gavel';
import PublicIcon from '@mui/icons-material/Public';
import { Box, Stack, Typography } from '@mui/material';
import type { PropsWithChildren } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Footer } from '../../../shared/components/Footer';
import { Logotype } from '../../../shared/components/Logotype';
import { SkipLink } from '../../../shared/components/SkipLink';
import { TopUtilityBar } from '../../../shared/components/TopUtilityBar';
import { glass } from '../../../shared/theme/theme';

const HIGHLIGHTS = [
  { icon: <VerifiedUserIcon fontSize="small" />, text: 'Formalización de pescadores artesanales ante la autoridad competente' },
  { icon: <GavelIcon fontSize="small" />, text: 'Control digital de permisos, vedas y tallas mínimas legales' },
  { icon: <PublicIcon fontSize="small" />, text: 'Alineado con la Ley 13 de 1990, la Ley 1851 de 2017 y el ODS 14' },
];

/**
 * Acceso institucional: la captura sobre hielo ocupa toda la pantalla y el formulario flota
 * encima como un panel de vidrio esmerilado, de modo que el objeto que el sistema regula
 * (el pescado, su talla, su legalidad) está siempre presente detrás del trámite.
 */
export function AuthShell({ children }: PropsWithChildren) {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <SkipLink />
      <TopUtilityBar />

      <Box
        component="main"
        id="main-content"
        sx={{
          flex: 1,
          position: 'relative',
          isolation: 'isolate',
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: '1.05fr minmax(420px, 1fr)' },
          alignItems: 'center',
          gap: { md: 6 },
          px: { xs: 2, sm: 4, md: 8 },
          py: { xs: 4, md: 8 },
          backgroundImage: {
            xs: 'linear-gradient(180deg, rgba(7,30,48,.55) 0%, rgba(7,30,48,.35) 100%), url(/images/auth-pescado.jpg)',
            md: 'linear-gradient(100deg, rgba(7,30,48,.92) 0%, rgba(7,30,48,.72) 38%, rgba(7,30,48,.15) 70%, rgba(7,30,48,.05) 100%), url(/images/auth-pescado.jpg)',
          },
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      >
        {/* Mensaje institucional — oculto en móvil para priorizar el formulario */}
        <Stack
          spacing={4}
          sx={{ display: { xs: 'none', md: 'flex' }, color: '#fff', maxWidth: 520, alignSelf: 'stretch', justifyContent: 'space-between' }}
        >
          <RouterLink to="/" aria-label="Ir a la página de inicio de SIGEPES" style={{ alignSelf: 'flex-start', textDecoration: 'none' }}>
            <Logotype variant="light" />
          </RouterLink>

          <Stack spacing={3}>
            <Typography variant="h3" component="p" sx={{ fontSize: '2.5rem', lineHeight: 1.1, textWrap: 'balance' }}>
              Gestión pesquera legal y sostenible
            </Typography>
            <Typography sx={{ color: 'rgba(255,255,255,.82)', fontSize: '1.0625rem', maxWidth: 460 }}>
              Portal institucional para el registro, control y trazabilidad de la actividad
              pesquera artesanal e industrial en Colombia.
            </Typography>
            <Stack
              spacing={1.75}
              sx={{ ...glass.smoked, borderBottom: 'none', border: '1px solid rgba(255,255,255,.14)', borderRadius: '16px', p: 2.5 }}
            >
              {HIGHLIGHTS.map((item) => (
                <Stack key={item.text} direction="row" spacing={1.5} alignItems="flex-start">
                  <Box sx={{ color: 'secondary.light', mt: 0.25, display: 'flex' }}>{item.icon}</Box>
                  <Typography variant="body2" sx={{ color: 'rgba(255,255,255,.9)' }}>
                    {item.text}
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </Stack>

          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,.6)' }}>
            Foto: Tapan Kumar Choudhury / Unsplash
          </Typography>
        </Stack>

        {/* Panel del formulario */}
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifySelf: { md: 'center' }, width: '100%' }}>
          <Box sx={{ display: { xs: 'flex', md: 'none' }, justifyContent: 'center', mb: 3 }}>
            <RouterLink to="/" aria-label="Ir a la página de inicio de SIGEPES" style={{ textDecoration: 'none' }}>
              <Logotype variant="light" />
            </RouterLink>
          </Box>
          <Box
            component="section"
            sx={{
              ...glass.paneStrong,
              width: '100%',
              maxWidth: 460,
              p: { xs: 3, sm: 5 },
              borderRadius: '24px',
            }}
          >
            {children}
          </Box>
        </Box>
      </Box>

      <Footer compact />
    </Box>
  );
}
