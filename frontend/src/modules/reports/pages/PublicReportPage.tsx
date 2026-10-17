import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import { Box, Button, Stack, Tab, Tabs, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Footer } from '../../../shared/components/Footer';
import { Logotype } from '../../../shared/components/Logotype';
import { SkipLink } from '../../../shared/components/SkipLink';
import { TopUtilityBar } from '../../../shared/components/TopUtilityBar';
import { useOnlineStatus } from '../../../shared/hooks/useOnlineStatus';
import { flushReportOutbox } from '../../../shared/sync/reportSync';
import { glass } from '../../../shared/theme/theme';
import { useAuthStore } from '../../auth/store/authStore';
import { ReportForm } from '../components/ReportForm';
import { ReportTracker } from '../components/ReportTracker';

/**
 * RF-10 · Canal ciudadano de denuncia, sin cuenta. Cualquier persona en la costa puede reportar
 * pesca ilegal y consultar el avance con su código.
 */
export function PublicReportPage() {
  const signedIn = !!useAuthStore((s) => s.accessToken);
  const online = useOnlineStatus();
  const [tab, setTab] = useState<'report' | 'track'>('report');

  useEffect(() => {
    if (online) void flushReportOutbox();
  }, [online]);

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <SkipLink />
      <TopUtilityBar />
      <Box component="header" sx={{ ...glass.smoked, px: { xs: 2, sm: 4 }, py: 1.5 }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ maxWidth: 1080, mx: 'auto' }}>
          <RouterLink to="/" aria-label="Ir a la página de inicio de SIGEPES" style={{ textDecoration: 'none' }}>
            <Logotype variant="light" />
          </RouterLink>
          <Button
            component={RouterLink}
            to={signedIn ? '/app/dashboard' : '/'}
            startIcon={<ArrowBackIcon />}
            sx={{ color: '#fff', whiteSpace: 'nowrap', flexShrink: 0 }}
          >
            {signedIn ? 'Mi panel' : 'Inicio'}
          </Button>
        </Stack>
      </Box>

      <Box component="main" id="main-content" sx={{ flex: 1, px: { xs: 2, sm: 4 }, py: { xs: 4, md: 6 } }}>
        <Box sx={{ maxWidth: 1080, mx: 'auto', display: 'grid', gap: { xs: 3, md: 5 }, gridTemplateColumns: { xs: '1fr', md: '0.8fr 1.4fr' }, alignItems: 'start' }}>
          <Stack spacing={2} sx={{ position: { md: 'sticky' }, top: { md: 32 } }}>
            <Box sx={{ width: 52, height: 52, borderRadius: 3, display: 'grid', placeItems: 'center', bgcolor: 'secondary.main', color: '#fff' }}>
              <CampaignOutlinedIcon />
            </Box>
            <Typography variant="h3" component="h1" sx={{ fontSize: { xs: '2rem', md: '2.5rem' }, lineHeight: 1.1, textWrap: 'balance' }}>
              Denuncia la pesca ilegal
            </Typography>
            <Typography color="text.secondary" sx={{ fontSize: '1.0625rem' }}>
              Pesca en veda, tallas por debajo del mínimo, artes prohibidas o faenas dentro de un Área
              Marina Protegida. Tu reporte llega directo a los inspectores de la autoridad pesquera.
            </Typography>
            <Stack spacing={1} component="ul" sx={{ pl: 2.5, m: 0, color: 'text.secondary' }}>
              <Typography component="li" variant="body2">Puedes denunciar de forma anónima, sin cuenta.</Typography>
              <Typography component="li" variant="body2">Recibes un código para consultar el avance.</Typography>
              <Typography component="li" variant="body2">Sin señal, se guarda y se envía al reconectar.</Typography>
            </Stack>
          </Stack>

          <Box component="section" aria-label="Denuncia" sx={{ ...glass.paneStrong, borderRadius: '24px', p: { xs: 2.5, sm: 4 } }}>
            <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 3 }} aria-label="Opciones de denuncia">
              <Tab value="report" label="Radicar denuncia" />
              <Tab value="track" label="Consultar estado" />
            </Tabs>
            {tab === 'report' ? <ReportForm signedIn={signedIn} /> : <ReportTracker signedIn={signedIn} />}
          </Box>
        </Box>
      </Box>

      <Footer compact />
    </Box>
  );
}
