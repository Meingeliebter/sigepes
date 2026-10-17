import CancelIcon from '@mui/icons-material/Cancel';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { Box, Stack, Typography } from '@mui/material';
import type { ChainEvent, LegalCheck } from '../../../shared/api/domain.service';
import { formatDay, PARTY_LABEL } from '../lotUtils';

/** RN-18 · Verificaciones de origen legal, con icono y texto (el color nunca es la única pista). */
export function LegalChecksList({ checks }: { checks: LegalCheck[] }) {
  return (
    <Stack component="ul" spacing={1.25} sx={{ listStyle: 'none', p: 0, m: 0 }}>
      {checks.map((check) => (
        <Stack component="li" key={check.key} direction="row" spacing={1.25} alignItems="flex-start">
          {check.passed
            ? <CheckCircleIcon fontSize="small" sx={{ color: 'success.main', mt: 0.25 }} aria-label="Cumple" />
            : <CancelIcon fontSize="small" sx={{ color: 'error.main', mt: 0.25 }} aria-label="No cumple" />}
          <Box>
            <Typography variant="body2" fontWeight={700}>{check.label}</Typography>
            <Typography variant="caption" color="text.secondary">{check.detail}</Typography>
          </Box>
        </Stack>
      ))}
    </Stack>
  );
}

/** AC-09.3 · Cadena de custodia en orden, del desembarco al último receptor. */
export function ChainTimeline({ chain, showParties = false }: { chain: ChainEvent[]; showParties?: boolean }) {
  return (
    <Box component="ol" sx={{ listStyle: 'none', p: 0, m: 0 }} aria-label="Cadena de custodia">
      {chain.map((event, index) => {
        const last = index === chain.length - 1;
        return (
          <Box component="li" key={event.sequence} sx={{ display: 'flex', gap: 1.5 }}>
            <Stack alignItems="center" sx={{ pt: 0.4 }}>
              <Box sx={{
                width: 14, height: 14, borderRadius: '50%', border: '3px solid',
                borderColor: last ? 'secondary.main' : 'primary.main', bgcolor: last ? 'secondary.main' : '#fff',
              }} />
              {!last && <Box sx={{ width: 2, flex: 1, minHeight: 28, bgcolor: 'rgba(11,59,92,.2)', my: 0.5 }} />}
            </Stack>
            <Box sx={{ pb: last ? 0 : 2 }}>
              <Typography variant="body2" fontWeight={700}>
                {event.type === 'CREATED' ? 'Desembarco y creación del lote' : `Entrega a ${PARTY_LABEL[event.partyType].toLowerCase()}`}
              </Typography>
              {showParties && event.partyName && (
                <Typography variant="body2">
                  {event.partyName}{event.partyTaxId ? ` · NIT ${event.partyTaxId}` : ''}
                </Typography>
              )}
              <Typography variant="caption" color="text.secondary">
                {event.location} · {formatDay(event.occurredOn)}
              </Typography>
              {showParties && event.hash && (
                <Typography variant="caption" color="text.disabled" sx={{ display: 'block', fontFamily: 'ui-monospace, Consolas, monospace' }}>
                  hash {event.hash.slice(0, 16)}…
                </Typography>
              )}
            </Box>
          </Box>
        );
      })}
    </Box>
  );
}
