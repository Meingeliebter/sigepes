import { Box, Stack, Typography } from '@mui/material';
import { APP_FULL_NAME, APP_NAME } from '../constants/app';

interface LogotypeProps {
  /** 'light' = marca clara para fondos oscuros (AppBar, footer); 'dark' = para fondos blancos. */
  variant?: 'light' | 'dark';
  size?: number;
  showTagline?: boolean;
  compact?: boolean;
}

/**
 * Marca institucional: escudo con ancla y línea de sondeo (motivo marino/pesquero) + nombre de
 * la entidad. Se usa en el AppBar, el panel lateral, el pie de página y la pantalla de acceso,
 * así que toda la identidad visual pasa por un único componente.
 */
export function Logotype({ variant = 'dark', size = 40, showTagline = true, compact = false }: LogotypeProps) {
  const ink = variant === 'light' ? '#FFFFFF' : '#0B3B5C';
  const accent = '#4FA8AE';

  return (
    <Stack direction="row" spacing={1.5} alignItems="center">
      <Box
        component="svg"
        viewBox="0 0 48 48"
        sx={{ width: size, height: size, flexShrink: 0 }}
        role="img"
        aria-label={`Escudo de ${APP_NAME}`}
      >
        <path
          d="M24 3 L42 10 V22 C42 33 34.5 41.5 24 45 C13.5 41.5 6 33 6 22 V10 Z"
          fill={variant === 'light' ? 'rgba(255,255,255,0.12)' : '#E7EEF3'}
          stroke={ink}
          strokeWidth={1.5}
        />
        <g fill="none" stroke={ink} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
          <path d="M24 13 V32" />
          <circle cx="24" cy="12" r="2.2" fill={ink} stroke="none" />
          <path d="M15 20 C15 25 19 28 24 28 C29 28 33 25 33 20" />
          <path d="M24 32 L20 35" />
          <path d="M24 32 L28 35" />
        </g>
        <path
          d="M6 30 C11 27 16 33 21 30 C26 27 31 33 36 30 C39 28.3 41 29 42 29.6 V22 C42 33 34.5 41.5 24 45 C13.5 41.5 6 33 6 22 Z"
          fill={accent}
          opacity={0.5}
        />
      </Box>
      {!compact && (
        <Box>
          <Typography
            variant="h6"
            component="span"
            sx={{ display: 'block', lineHeight: 1.1, fontWeight: 800, letterSpacing: 0.5, color: ink }}
          >
            {APP_NAME}
          </Typography>
          {showTagline && (
            <Typography
              variant="caption"
              sx={{ color: variant === 'light' ? 'rgba(255,255,255,0.72)' : 'text.secondary' }}
            >
              {APP_FULL_NAME}
            </Typography>
          )}
        </Box>
      )}
    </Stack>
  );
}
