import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import GppBadIcon from '@mui/icons-material/GppBad';
import SearchIcon from '@mui/icons-material/Search';
import VerifiedIcon from '@mui/icons-material/Verified';
import { Alert, Box, Button, CircularProgress, Grid, Stack, TextField, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { Footer } from '../../../shared/components/Footer';
import { Logotype } from '../../../shared/components/Logotype';
import { SkipLink } from '../../../shared/components/SkipLink';
import { TopUtilityBar } from '../../../shared/components/TopUtilityBar';
import { domainService } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { glass } from '../../../shared/theme/theme';
import { ChainTimeline, LegalChecksList } from '../components/LotParts';
import { formatDay } from '../lotUtils';

const kg = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 });

/**
 * AC-09.4 · Página que abre el QR del lote: origen, legalidad y cadena de custodia, sin cuenta y
 * sin datos personales del pescador.
 */
export function PublicLotPage() {
  const { code = '' } = useParams();
  const navigate = useNavigate();
  const [input, setInput] = useState(code);
  const lotQuery = useQuery({
    queryKey: ['public-lot', code],
    queryFn: () => domainService.verifyLot(code),
    enabled: !!code,
    retry: false,
  });
  const error = lotQuery.error ? toAppError(lotQuery.error) : null;
  const lot = lotQuery.data;
  const trusted = lot ? lot.legal && lot.integrityOk : false;

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <SkipLink />
      <TopUtilityBar />
      <Box component="header" sx={{ ...glass.smoked, px: { xs: 2, sm: 4 }, py: 1.5 }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ maxWidth: 960, mx: 'auto' }}>
          <RouterLink to="/" aria-label="Ir a la página de inicio de SIGEPES" style={{ textDecoration: 'none' }}>
            <Logotype variant="light" />
          </RouterLink>
          <Button component={RouterLink} to="/" startIcon={<ArrowBackIcon />} sx={{ color: '#fff', whiteSpace: 'nowrap', flexShrink: 0 }}>
            Inicio
          </Button>
        </Stack>
      </Box>

      <Box component="main" id="main-content" sx={{ flex: 1, px: { xs: 2, sm: 4 }, py: { xs: 4, md: 6 } }}>
        <Box sx={{ maxWidth: 960, mx: 'auto' }}>
          <Typography variant="h3" component="h1" sx={{ fontSize: { xs: '2rem', md: '2.5rem' }, mb: 1 }}>
            Verificar origen del pescado
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 3, maxWidth: 640 }}>
            Escanea el QR de la etiqueta o escribe el código del lote para comprobar que se pescó legalmente
            y por qué manos ha pasado.
          </Typography>

          <Stack component="form" direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mb: 4, maxWidth: 560 }}
            onSubmit={(e) => { e.preventDefault(); if (input.trim()) void navigate(`/verificar/${input.trim().toUpperCase()}`); }}>
            <TextField fullWidth label="Código del lote" placeholder="LOT-XXXXXXXX" value={input}
              onChange={(e) => setInput(e.target.value.toUpperCase())}
              slotProps={{ htmlInput: { style: { fontFamily: 'ui-monospace, Consolas, monospace' }, maxLength: 20 } }} />
            <Button type="submit" variant="contained" startIcon={<SearchIcon />} sx={{ minWidth: 140 }}>Verificar</Button>
          </Stack>

          {lotQuery.isLoading && <CircularProgress size={28} />}
          {error && (
            <Alert severity={error.status === 404 ? 'warning' : 'error'} role="alert">
              {error.status === 404 ? 'No existe un lote con ese código. Desconfía de un producto con un QR que no verifica.' : error.message}
            </Alert>
          )}

          {lot && (
            <Box sx={{ ...glass.paneStrong, borderRadius: '24px', overflow: 'hidden' }} aria-live="polite">
              <Stack
                direction="row" spacing={2} alignItems="center"
                sx={{ px: { xs: 2.5, sm: 4 }, py: 2.5, bgcolor: trusted ? 'success.main' : 'error.main', color: '#fff' }}
              >
                {trusted ? <VerifiedIcon sx={{ fontSize: 40 }} /> : <GppBadIcon sx={{ fontSize: 40 }} />}
                <Box>
                  <Typography variant="h5" component="p" fontWeight={800}>
                    {!lot.integrityOk ? 'Integridad comprometida' : lot.legal ? 'Origen legal verificado' : 'Origen legal no verificado'}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.92 }}>
                    {!lot.integrityOk
                      ? 'Los registros de este lote fueron alterados. No lo compres y repórtalo a la autoridad.'
                      : `Lote ${lot.code} registrado en SIGEPES.`}
                  </Typography>
                </Box>
              </Stack>

              <Grid container spacing={4} sx={{ p: { xs: 2.5, sm: 4 } }}>
                <Grid size={{ xs: 12, md: 6 }}>
                  <Typography variant="h5" component="h2" fontWeight={800}>{lot.speciesName}</Typography>
                  <Typography color="text.secondary" fontStyle="italic" sx={{ mb: 2 }}>{lot.scientificName}</Typography>
                  <Stack spacing={0.75} sx={{ mb: 3 }}>
                    <Typography variant="body2"><strong>Peso:</strong> {kg.format(lot.weightKg)} kg</Typography>
                    <Typography variant="body2"><strong>Desembarco:</strong> {formatDay(lot.landingDate)}</Typography>
                    <Typography variant="body2"><strong>Zona de pesca:</strong> {lot.zone}</Typography>
                    <Typography variant="body2"><strong>Embarcación:</strong> matrícula {lot.vesselRegistration}</Typography>
                  </Stack>
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>Verificaciones de legalidad</Typography>
                  <LegalChecksList checks={lot.legalChecks} />
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}>
                  <Typography variant="subtitle2" sx={{ mb: 1.5 }}>Cadena de custodia</Typography>
                  <ChainTimeline chain={lot.chain} />
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
                    Por protección de datos (Ley 1581 de 2012) no se muestran nombres ni documentos de quienes
                    pescaron o recibieron el lote.
                  </Typography>
                </Grid>
              </Grid>
            </Box>
          )}
        </Box>
      </Box>
      <Footer compact />
    </Box>
  );
}
