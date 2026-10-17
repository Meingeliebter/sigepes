import BlockIcon from '@mui/icons-material/Block';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';
import ScaleOutlinedIcon from '@mui/icons-material/ScaleOutlined';
import SailingOutlinedIcon from '@mui/icons-material/SailingOutlined';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import WrongLocationIcon from '@mui/icons-material/WrongLocation';
import {
  Alert, Box, Button, Card, CardContent, CircularProgress, Grid, LinearProgress, MenuItem, Stack, TextField,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { BarList } from '../../../shared/charts/BarList';
import { ChartCard } from '../../../shared/charts/ChartCard';
import { SplitBar } from '../../../shared/charts/SplitBar';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatCard } from '../../../shared/components/StatCard';
import {
  domainService, type IndicatorFilters, type IndicatorsResponse, type Region, type SpeciesLanding,
} from '../../../shared/api/domain.service';
import { useAuthStore } from '../../auth/store/authStore';
import { isAuthority } from '../../auth/types/auth.types';
import { REPORT_STATUS_LABEL } from '../../reports/reportLabels';

const kg = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });
const integer = new Intl.NumberFormat('es-CO');
const day = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' });
const time = new Intl.DateTimeFormat('es-CO', { timeStyle: 'short' });

const REGION_LABEL: Record<Region, string> = { PACIFICO: 'Pacífico', CARIBE: 'Caribe', OTRA: 'Otra zona' };
const PERMIT_LABEL: Record<string, string> = {
  APPROVED: 'Vigentes', PENDING: 'Pendientes', SUSPENDED: 'Suspendidos', EXPIRED: 'Vencidos', REJECTED: 'Rechazados',
};

function today() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function yearAgo() {
  const date = new Date();
  date.setFullYear(date.getFullYear() - 1);
  date.setDate(date.getDate() + 1);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

/** AC-07.4 · Umbral de la cuota con icono y texto: el color nunca es la única pista. */
function QuotaMeter({ quota }: { quota: IndicatorsResponse['quotas'][number] }) {
  const level = quota.consumedPercent >= 95 ? 'error' : quota.consumedPercent >= 80 ? 'warning' : 'success';
  const Icon = level === 'error' ? ErrorOutlineIcon : level === 'warning' ? WarningAmberIcon : null;
  return (
    <Box component="li" sx={{ py: 1 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="baseline" spacing={1}>
        <Typography variant="body2" fontWeight={700} noWrap>{quota.speciesName} · {quota.zone}</Typography>
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ flexShrink: 0 }}>
          {Icon && <Icon fontSize="inherit" sx={{ color: `${level}.main` }} />}
          <Typography variant="body2" fontWeight={700} sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {kg.format(quota.consumedPercent)} %
          </Typography>
        </Stack>
      </Stack>
      <LinearProgress
        variant="determinate" value={Math.min(100, quota.consumedPercent)} color={level} sx={{ height: 8, my: 0.5 }}
        aria-label={`${quota.speciesName} en ${quota.zone}: ${kg.format(quota.consumedPercent)} por ciento consumido`}
      />
      <Typography variant="caption" color="text.secondary">
        Quedan {kg.format(quota.remainingKg)} de {kg.format(quota.totalKg)} kg
        {level === 'error' ? ' · crítica' : level === 'warning' ? ' · atención' : ''}
      </Typography>
    </Box>
  );
}

function speciesBars(landings: SpeciesLanding[]) {
  // Hasta 8 especies; el resto se agrupa en «Otras» para que el gráfico siga siendo legible.
  const top = landings.slice(0, 8);
  const rest = landings.slice(8);
  const data = top.map((item) => ({
    key: item.speciesId, label: item.speciesName, value: item.kg,
    display: `${kg.format(item.kg)} kg`, detail: `${item.trips} faena(s)`,
  }));
  if (rest.length) {
    const total = rest.reduce((sum, item) => sum + item.kg, 0);
    data.push({ key: 'other', label: `Otras (${rest.length})`, value: total, display: `${kg.format(total)} kg`, detail: 'varias especies' });
  }
  return data;
}

function Filters({ value, onChange }: { value: IndicatorFilters; onChange: (next: IndicatorFilters) => void }) {
  const speciesQuery = useQuery({ queryKey: ['species'], queryFn: domainService.species });
  return (
    <Card sx={{ mb: 2.5 }}>
      <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
        <Grid container spacing={1.5} component="form" aria-label="Filtros de los indicadores">
          <Grid size={{ xs: 6, md: 3 }}>
            <TextField fullWidth size="small" type="date" label="Desde" value={value.from}
              onChange={(e) => onChange({ ...value, from: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={{ xs: 6, md: 3 }}>
            <TextField fullWidth size="small" type="date" label="Hasta" value={value.to}
              onChange={(e) => onChange({ ...value, to: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={{ xs: 6, md: 3 }}>
            <TextField select fullWidth size="small" label="Región" value={value.region ?? ''}
              onChange={(e) => onChange({ ...value, region: e.target.value as Region | '' })}>
              <MenuItem value="">Todas</MenuItem>
              <MenuItem value="PACIFICO">Pacífico</MenuItem>
              <MenuItem value="CARIBE">Caribe</MenuItem>
            </TextField>
          </Grid>
          <Grid size={{ xs: 6, md: 3 }}>
            <TextField select fullWidth size="small" label="Especie" value={value.speciesId ?? ''}
              onChange={(e) => onChange({ ...value, speciesId: e.target.value })}>
              <MenuItem value="">Todas</MenuItem>
              {(speciesQuery.data ?? []).map((species) => (
                <MenuItem key={species.id} value={species.id}>{species.commonName}</MenuItem>
              ))}
            </TextField>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}

function AuthorityDashboard({ filters }: { filters: IndicatorFilters }) {
  const query = useQuery({ queryKey: ['indicators', filters], queryFn: () => domainService.indicators(filters) });
  const data = query.data;
  if (query.isLoading) return <CircularProgress size={28} />;
  if (query.isError || !data) return <Alert severity="error">No fue posible calcular los indicadores. Revisa el periodo elegido.</Alert>;

  const { totals } = data;
  const regionData = data.landingsByRegion
    .filter((item) => item.region !== 'OTRA')
    .map((item) => ({ key: item.region, label: REGION_LABEL[item.region], value: item.kg, display: `${kg.format(item.kg)} kg` }));
  const permits = data.permitsByStatus.map((item) => ({
    key: item.status, label: PERMIT_LABEL[item.status] ?? item.status, value: item.count, display: integer.format(item.count),
  }));
  const reports = data.reportsByStatus.map((item) => ({
    key: item.status, label: REPORT_STATUS_LABEL[item.status as keyof typeof REPORT_STATUS_LABEL] ?? item.status,
    value: item.count, display: integer.format(item.count),
  }));
  const species = speciesBars(data.landingsBySpecies);

  return (
    <Stack spacing={2.5}>
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={1} justifyContent="space-between" alignItems={{ lg: 'center' }}>
        <Alert severity="info" icon={<InfoOutlinedIcon />} sx={{ flex: 1 }}>
          Solo se cuentan faenas cerradas y sincronizadas.
          {totals.excludedOpenTrips > 0 && ` ${totals.excludedOpenTrips} faena(s) abierta(s) del periodo quedaron por fuera hasta que se validen.`}
        </Alert>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ flexShrink: 0 }}>
          <Typography variant="caption" color="text.secondary">
            Calculado a las {time.format(new Date(data.generatedAt))} · se actualiza cada 15 min
          </Typography>
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => void query.refetch()} disabled={query.isFetching}>
            Consultar
          </Button>
        </Stack>
      </Stack>

      <Grid container spacing={2}>
        <Grid size={{ xs: 6, md: 4, lg: 2.4 }}>
          <StatCard label="Desembarcado" value={`${kg.format(totals.landedKg)} kg`} icon={<ScaleOutlinedIcon />} helper={`${totals.closedTrips} faena(s) cerradas`} />
        </Grid>
        <Grid size={{ xs: 6, md: 4, lg: 2.4 }}>
          <StatCard label="Faenas validadas" value={integer.format(totals.closedTrips)} icon={<SailingOutlinedIcon />} accent="secondary"
            helper={totals.excludedOpenTrips ? `${totals.excludedOpenTrips} abierta(s) excluidas` : 'Ninguna excluida'} />
        </Grid>
        <Grid size={{ xs: 6, md: 4, lg: 2.4 }}>
          <StatCard label="Vedas vigentes" value={integer.format(totals.activeClosures)} icon={<BlockIcon />} accent="warning" helper="Hoy" />
        </Grid>
        <Grid size={{ xs: 6, md: 6, lg: 2.4 }}>
          <StatCard label="Denuncias abiertas" value={integer.format(totals.openReports)} icon={<CampaignOutlinedIcon />} accent="info" helper="Recibidas o en investigación" />
        </Grid>
        <Grid size={{ xs: 12, md: 6, lg: 2.4 }}>
          <StatCard label="Ingresos a zonas" value={integer.format(totals.zoneEvents)} icon={<WrongLocationIcon />} accent="warning" helper="Eventos de control del periodo" />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 7 }}>
          <ChartCard
            title="Desembarcos por especie"
            subtitle={`kg validados, ${day.format(new Date(`${data.filters.from}T12:00:00`))} – ${day.format(new Date(`${data.filters.to}T12:00:00`))}`}
            empty={species.length === 0}
            table={{ columns: ['Especie', 'kg', 'Faenas'], rows: data.landingsBySpecies.map((item) => [item.speciesName, kg.format(item.kg), item.trips]) }}
          >
            <BarList data={species} />
          </ChartCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 5 }}>
          <Stack spacing={2}>
            <ChartCard
              fill={false}
              title="Desembarcos por región"
              empty={regionData.length === 0}
              table={{ columns: ['Región', 'kg'], rows: regionData.map((item) => [item.label, kg.format(item.value)]) }}
            >
              <SplitBar data={regionData} />
            </ChartCard>
            <ChartCard
              fill={false}
              title="Consumo de cuotas"
              subtitle="Cuotas activas en el periodo, de la más a la menos consumida"
              empty={data.quotas.length === 0}
              emptyText="No hay cuotas activas para estos filtros."
              table={{
                columns: ['Cuota', '% consumido', 'kg restantes'],
                rows: data.quotas.map((quota) => [`${quota.speciesName} · ${quota.zone}`, kg.format(quota.consumedPercent), kg.format(quota.remainingKg)]),
              }}
            >
              <Box component="ul" sx={{ listStyle: 'none', p: 0, m: 0 }}>
                {data.quotas.slice(0, 6).map((quota) => <QuotaMeter key={quota.id} quota={quota} />)}
              </Box>
            </ChartCard>
          </Stack>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <ChartCard title="Permisos por estado" subtitle="Situación actual"
            empty={permits.length === 0}
            table={{ columns: ['Estado', 'Permisos'], rows: permits.map((item) => [item.label, item.value]) }}>
            <BarList data={permits} />
          </ChartCard>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <ChartCard title="Denuncias por estado" subtitle="Radicadas en el periodo"
            empty={reports.every((item) => item.value === 0)} emptyText="No hubo denuncias en el periodo."
            table={{ columns: ['Estado', 'Denuncias'], rows: reports.map((item) => [item.label, item.value]) }}>
            <BarList data={reports} />
          </ChartCard>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <ChartCard title="Vedas vigentes" subtitle="Hoy, según los filtros de región y especie"
            empty={data.closures.length === 0} emptyText="No hay vedas vigentes."
            table={{ columns: ['Especie', 'Zona', 'Hasta'], rows: data.closures.map((item) => [item.speciesName, item.zone, day.format(new Date(`${item.until}T12:00:00`))]) }}>
            <Stack component="ul" spacing={1} sx={{ listStyle: 'none', p: 0, m: 0 }}>
              {data.closures.map((item) => (
                <Stack component="li" key={`${item.speciesName}-${item.zone}`} direction="row" justifyContent="space-between" spacing={1}>
                  <Typography variant="body2"><strong>{item.speciesName}</strong> · {item.zone}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ flexShrink: 0 }}>hasta {day.format(new Date(`${item.until}T12:00:00`))}</Typography>
                </Stack>
              ))}
            </Stack>
          </ChartCard>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <ChartCard title="Ingresos a zonas restringidas" subtitle="Eventos de control del periodo por zona"
            empty={data.zoneEvents.length === 0} emptyText="No hubo ingresos a zonas restringidas."
            table={{ columns: ['Zona', 'Eventos', 'Pendientes'], rows: data.zoneEvents.map((item) => [item.zoneName, item.total, item.open]) }}>
            <BarList data={data.zoneEvents.map((item) => ({
              key: item.zoneName, label: item.zoneName, value: item.total,
              display: integer.format(item.total), detail: `${item.open} pendiente(s) de revisión`,
            }))} />
          </ChartCard>
        </Grid>
      </Grid>
    </Stack>
  );
}

/** AC-12.6 · Pescadores y armadores solo ven sus propias cifras. */
function OwnDashboard({ filters }: { filters: IndicatorFilters }) {
  const query = useQuery({ queryKey: ['own-indicators', filters], queryFn: () => domainService.ownIndicators(filters) });
  const data = query.data;
  if (query.isLoading) return <CircularProgress size={28} />;
  if (query.isError || !data) return <Alert severity="error">No fue posible calcular tus cifras.</Alert>;
  const species = speciesBars(data.landingsBySpecies);
  return (
    <Stack spacing={2.5}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 4 }}>
          <StatCard label="Desembarcado" value={`${kg.format(data.landedKg)} kg`} icon={<ScaleOutlinedIcon />} helper="Faenas cerradas" />
        </Grid>
        <Grid size={{ xs: 6, sm: 4 }}>
          <StatCard label="Faenas cerradas" value={integer.format(data.closedTrips)} icon={<SailingOutlinedIcon />} accent="secondary" />
        </Grid>
        <Grid size={{ xs: 6, sm: 4 }}>
          <StatCard label="Faenas abiertas" value={integer.format(data.openTrips)} icon={<InfoOutlinedIcon />} accent="info" helper="No suman hasta cerrarlas" />
        </Grid>
      </Grid>
      <ChartCard title="Tus desembarcos por especie" empty={species.length === 0} emptyText="Aún no tienes faenas cerradas en este periodo."
        table={{ columns: ['Especie', 'kg', 'Faenas'], rows: data.landingsBySpecies.map((item) => [item.speciesName, kg.format(item.kg), item.trips]) }}>
        <BarList data={species} />
      </ChartCard>
    </Stack>
  );
}

/** RF-12 · Tablero de indicadores de sostenibilidad. */
export function IndicatorsPage() {
  const user = useAuthStore((s) => s.user);
  const authority = isAuthority(user?.role);
  const [filters, setFilters] = useState<IndicatorFilters>({ from: yearAgo(), to: today(), region: '', speciesId: '' });
  const valid = !filters.from || !filters.to || filters.from <= filters.to;

  return (
    <>
      <PageHeader
        title={authority ? 'Indicadores de sostenibilidad' : 'Mis indicadores'}
        subtitle={authority
          ? 'Desembarcos, cuotas, vedas, permisos, denuncias e ingresos a zonas restringidas, con datos validados.'
          : 'Lo que has desembarcado legalmente, por especie y periodo.'}
      />
      <Filters value={filters} onChange={setFilters} />
      {!valid
        ? <Alert severity="warning">La fecha inicial no puede ser posterior a la final.</Alert>
        : authority ? <AuthorityDashboard filters={filters} /> : <OwnDashboard filters={filters} />}
    </>
  );
}
