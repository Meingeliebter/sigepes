interface WaveDividerProps {
  /** Color de relleno de la ola; normalmente el color de fondo de la sección siguiente. */
  fill?: string;
}

/** Divisor ondulado entre el hero institucional y el contenido, motivo marino discreto. */
export function WaveDivider({ fill = '#F6F8FA' }: WaveDividerProps) {
  return (
    <svg
      viewBox="0 0 1440 80"
      preserveAspectRatio="none"
      style={{ display: 'block', width: '100%', height: 56 }}
      aria-hidden="true"
    >
      <path
        d="M0 32 C 240 80, 480 0, 720 24 C 960 48, 1200 8, 1440 40 L1440 80 L0 80 Z"
        fill={fill}
      />
    </svg>
  );
}
