import AddIcon from '@mui/icons-material/Add';
import GppBadIcon from '@mui/icons-material/GppBad';
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import PrintOutlinedIcon from '@mui/icons-material/PrintOutlined';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import VerifiedOutlinedIcon from '@mui/icons-material/VerifiedOutlined';
import {
  Alert, Box, Button, Card, CardActionArea, CardContent, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, Divider, GlobalStyles, Grid, MenuItem, Radio, Stack, TextField, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { QRCodeSVG } from 'qrcode.react';
import { useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { domainService, type Lot, type PartyType } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useAuthStore } from '../../auth/store/authStore';
import { isAuthority } from '../../auth/types/auth.types';
import { ChainTimeline, LegalChecksList } from '../components/LotParts';
import { formatDay, lotVerificationUrl, PARTY_LABEL } from '../lotUtils';

const kg = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 });

function IntegrityChip({ ok }: { ok: boolean }) {
  return ok
    ? <Chip size="small" color="success" icon={<VerifiedOutlinedIcon />} label="Cadena íntegra" />
    : <Chip size="small" color="error" icon={<GppBadIcon />} label="Integridad comprometida" />;
}

/** AC-09.1 · Crear un lote eligiendo una faena cerrada y una de sus capturas legales. */
function NewLotDialog({ onClose, onCreated }: { onClose: () => void; onCreated: (lot: Lot) => void }) {
  const tripsQuery = useQuery({ queryKey: ['trips'], queryFn: domainService.trips });
  const closedTrips = (tripsQuery.data ?? []).filter((trip) => trip.status === 'CLOSED' && !trip.nullDeclaration);
  const [tripId, setTripId] = useState('');
  const [catchId, setCatchId] = useState('');
  const [weight, setWeight] = useState('');

  const catchesQuery = useQuery({
    queryKey: ['eligible-catches', tripId],
    queryFn: () => domainService.eligibleCatches(tripId),
    enabled: !!tripId,
  });
  const selected = catchesQuery.data?.find((item) => item.catchId === catchId);
  const weightValue = Number(weight.replace(',', '.'));
  const weightInvalid = weight !== '' && (!(weightValue > 0) || (selected ? weightValue > selected.availableKg : false));

  const create = useMutation({
    mutationFn: () => domainService.createLot(catchId, weightValue),
    onSuccess: onCreated,
  });
  const error = create.error ? toAppError(create.error) : null;

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm" aria-labelledby="new-lot-title">
      <DialogTitle id="new-lot-title">Nuevo lote trazable</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2.5}>
          {error && <Alert severity="error">{error.message}</Alert>}
          <TextField
            select fullWidth label="Faena cerrada" value={tripId}
            onChange={(e) => { setTripId(e.target.value); setCatchId(''); setWeight(''); }}
            helperText={tripsQuery.isSuccess && closedTrips.length === 0 ? 'No tienes faenas cerradas con capturas.' : ' '}
          >
            {closedTrips.map((trip) => (
              <MenuItem key={trip.id} value={trip.id}>{formatDay(trip.date)} · {trip.vessel} · {trip.zone}</MenuItem>
            ))}
          </TextField>

          {catchesQuery.isLoading && <CircularProgress size={24} />}
          {catchesQuery.data && (
            <Stack spacing={1.5} role="radiogroup" aria-label="Captura">
              {catchesQuery.data.map((item) => {
                const usable = item.legal && item.availableKg > 0;
                return (
                  <Box
                    key={item.catchId}
                    sx={{
                      p: 1.75, borderRadius: 3, border: '1px solid',
                      borderColor: item.catchId === catchId ? 'secondary.main' : 'divider',
                      opacity: usable ? 1 : 0.85,
                    }}
                  >
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Radio
                        checked={item.catchId === catchId} disabled={!usable}
                        onChange={() => { setCatchId(item.catchId); setWeight(String(item.availableKg)); }}
                        inputProps={{ 'aria-label': item.speciesName }}
                      />
                      <Box sx={{ flex: 1 }}>
                        <Typography fontWeight={700}>{item.speciesName}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {kg.format(item.weightKg)} kg capturados · {kg.format(item.availableKg)} kg disponibles
                        </Typography>
                      </Box>
                      <Chip size="small" color={item.legal ? 'success' : 'error'} label={item.legal ? 'Origen legal' : 'No apto'} />
                    </Stack>
                    {(!item.legal || item.catchId === catchId) && (
                      <Box sx={{ pl: 6, pt: 1 }}><LegalChecksList checks={item.checks} /></Box>
                    )}
                  </Box>
                );
              })}
            </Stack>
          )}

          {selected && (
            <TextField
              label="Peso del lote (kg)" value={weight} inputMode="decimal"
              onChange={(e) => setWeight(e.target.value)}
              error={weightInvalid}
              helperText={weightInvalid ? `Entre 0,01 y ${kg.format(selected.availableKg)} kg` : `Máximo ${kg.format(selected.availableKg)} kg`}
            />
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={() => create.mutate()} disabled={!selected || !weight || weightInvalid || create.isPending}>
          {create.isPending ? 'Creando…' : 'Crear lote'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

const EMPTY_TRANSFER = { partyType: 'BUYER' as PartyType, partyName: '', partyTaxId: '', location: '' };

/** Detalle del lote: QR imprimible, legalidad, cadena y registro de entregas (AC-09.3). */
function LotDialog({ lot, onClose }: { lot: Lot; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [current, setCurrent] = useState(lot);
  const [form, setForm] = useState(EMPTY_TRANSFER);
  const url = lotVerificationUrl(current.code);

  const transfer = useMutation({
    mutationFn: () => domainService.transferLot(current.id, {
      partyType: form.partyType,
      partyName: form.partyName.trim(),
      partyTaxId: form.partyTaxId.trim() || undefined,
      location: form.location.trim(),
    }),
    onSuccess: async (updated) => {
      setCurrent(updated);
      setForm(EMPTY_TRANSFER);
      await queryClient.invalidateQueries({ queryKey: ['lots'] });
    },
  });
  const error = transfer.error ? toAppError(transfer.error) : null;

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="md" aria-labelledby="lot-title">
      {/* Al imprimir solo sale la etiqueta con el QR, lista para pegar en la caja o la bolsa. */}
      <GlobalStyles styles={{
        '@media print': {
          'body *': { visibility: 'hidden' },
          '.lot-label, .lot-label *': { visibility: 'visible' },
          '.lot-label': { position: 'fixed', top: 0, left: 0, width: '80mm', border: 'none !important' },
        },
      }} />
      <DialogTitle id="lot-title">
        <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap>
          <Box component="span" sx={{ fontFamily: 'ui-monospace, Consolas, monospace' }}>{current.code}</Box>
          <IntegrityChip ok={current.integrityOk} />
        </Stack>
      </DialogTitle>
      <DialogContent dividers>
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 5 }}>
            <Box
              className="lot-label"
              sx={{ p: 2.5, borderRadius: 4, border: '1px solid', borderColor: 'divider', textAlign: 'center', bgcolor: '#fff' }}
            >
              <QRCodeSVG value={url} size={176} level="M" marginSize={1} title={`QR del lote ${current.code}`} />
              <Typography sx={{ mt: 1.5, fontFamily: 'ui-monospace, Consolas, monospace', fontWeight: 700, letterSpacing: '.05em' }}>
                {current.code}
              </Typography>
              <Typography variant="body2">{current.speciesName} · {kg.format(current.weightKg)} kg</Typography>
              <Typography variant="caption" color="text.secondary">Desembarco {formatDay(current.landingDate)}</Typography>
            </Box>
            <Button fullWidth startIcon={<PrintOutlinedIcon />} sx={{ mt: 1.5 }} onClick={() => window.print()}>
              Imprimir etiqueta
            </Button>
            <Typography variant="subtitle2" sx={{ mt: 2.5, mb: 1 }}>Origen legal</Typography>
            <LegalChecksList checks={current.legalChecks} />
          </Grid>
          <Grid size={{ xs: 12, md: 7 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {current.scientificName} · {current.zone} · {current.vesselName} ({current.vesselRegistration})
            </Typography>
            <Typography variant="subtitle2" sx={{ mb: 1.5 }}>Cadena de custodia</Typography>
            <ChainTimeline chain={current.chain} showParties />

            <Divider sx={{ my: 2.5 }} />
            <Typography variant="subtitle2" sx={{ mb: 1.5 }}>Registrar entrega</Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error.message}</Alert>}
            <Grid container spacing={1.5}>
              <Grid size={{ xs: 12, sm: 5 }}>
                <TextField select fullWidth size="small" label="Recibe" value={form.partyType}
                  onChange={(e) => setForm({ ...form, partyType: e.target.value as PartyType })}>
                  {(['BUYER', 'TRADER', 'PROCESSOR', 'EXPORTER'] as PartyType[]).map((type) => (
                    <MenuItem key={type} value={type}>{PARTY_LABEL[type]}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid size={{ xs: 12, sm: 7 }}>
                <TextField fullWidth size="small" label="Nombre o razón social" value={form.partyName}
                  onChange={(e) => setForm({ ...form, partyName: e.target.value })} slotProps={{ htmlInput: { maxLength: 120 } }} />
              </Grid>
              <Grid size={{ xs: 12, sm: 5 }}>
                <TextField fullWidth size="small" label="NIT (opcional)" value={form.partyTaxId}
                  onChange={(e) => setForm({ ...form, partyTaxId: e.target.value })} slotProps={{ htmlInput: { maxLength: 30 } }} />
              </Grid>
              <Grid size={{ xs: 12, sm: 7 }}>
                <TextField fullWidth size="small" label="Lugar de entrega" value={form.location}
                  onChange={(e) => setForm({ ...form, location: e.target.value })} slotProps={{ htmlInput: { maxLength: 120 } }} />
              </Grid>
            </Grid>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              El nombre y el NIT solo los ve la autoridad; la página pública muestra el tipo de receptor y el lugar.
            </Typography>
            <Button
              variant="outlined" startIcon={<LocalShippingOutlinedIcon />} sx={{ mt: 1.5 }}
              disabled={!form.partyName.trim() || !form.location.trim() || transfer.isPending}
              onClick={() => transfer.mutate()}
            >
              {transfer.isPending ? 'Registrando…' : 'Registrar entrega'}
            </Button>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button href={url} target="_blank" rel="noreferrer">Ver página pública</Button>
        <Button variant="contained" onClick={onClose}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}

/** RF-09 · Lotes trazables del pescador (o de todos, para la autoridad). */
export function LotsPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const operator = !!user && !isAuthority(user.role);
  const lotsQuery = useQuery({ queryKey: ['lots'], queryFn: domainService.lots });
  const [creating, setCreating] = useState(false);
  const [open, setOpen] = useState<Lot | null>(null);
  const lots = lotsQuery.data ?? [];

  return (
    <>
      <PageHeader
        title="Trazabilidad"
        subtitle="Convierte tu captura legal en lotes con código QR. Cada entrega queda en una cadena que nadie puede alterar sin que se note."
        actionLabel={operator ? 'Nuevo lote' : undefined}
        actionIcon={operator ? <AddIcon /> : undefined}
        onAction={operator ? () => setCreating(true) : undefined}
      />

      {lotsQuery.isLoading && <CircularProgress size={28} />}
      {lotsQuery.isError && <Alert severity="error">No fue posible cargar los lotes.</Alert>}

      {lotsQuery.isSuccess && lots.length === 0 && (
        <EmptyState
          icon={<QrCode2Icon sx={{ fontSize: 48, color: 'secondary.light' }} />}
          title="Aún no hay lotes"
          description={operator
            ? 'Cuando cierres una faena con captura legal, crea aquí el lote y pégale su QR.'
            : 'Los pescadores todavía no han registrado lotes trazables.'}
          actionLabel={operator ? 'Crear el primer lote' : undefined}
          onAction={operator ? () => setCreating(true) : undefined}
        />
      )}

      <Grid container spacing={2}>
        {lots.map((lot) => (
          <Grid key={lot.id} size={{ xs: 12, sm: 6, lg: 4 }}>
            <Card sx={{ height: '100%' }}>
              <CardActionArea onClick={() => setOpen(lot)} sx={{ height: '100%' }}>
                <CardContent>
                  <Stack direction="row" spacing={2} alignItems="flex-start">
                    <Box sx={{ p: 0.75, bgcolor: '#fff', borderRadius: 2, lineHeight: 0, boxShadow: 1 }}>
                      <QRCodeSVG value={lotVerificationUrl(lot.code)} size={64} />
                    </Box>
                    <Box sx={{ minWidth: 0, flex: 1 }}>
                      <Typography sx={{ fontFamily: 'ui-monospace, Consolas, monospace', fontWeight: 700 }}>{lot.code}</Typography>
                      <Typography fontWeight={700}>{lot.speciesName}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {kg.format(lot.weightKg)} kg · {formatDay(lot.landingDate)}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>
                        {lot.chain.length > 1 ? `En manos de ${lot.currentHolder}` : 'Sin entregas registradas'}
                        {!operator && ` · ${lot.ownerName}`}
                      </Typography>
                    </Box>
                  </Stack>
                  <Box sx={{ mt: 1.5 }}><IntegrityChip ok={lot.integrityOk} /></Box>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>

      {creating && (
        <NewLotDialog
          onClose={() => setCreating(false)}
          onCreated={async (lot) => {
            setCreating(false);
            setOpen(lot);
            await queryClient.invalidateQueries({ queryKey: ['lots'] });
          }}
        />
      )}
      {open && <LotDialog lot={open} onClose={() => setOpen(null)} />}
    </>
  );
}
