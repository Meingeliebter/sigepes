/** Enlace de accesibilidad: invisible hasta recibir foco por teclado; salta al contenido principal. */
export function SkipLink({ targetId = 'main-content' }: { targetId?: string }) {
  return (
    <a href={`#${targetId}`} className="skip-link">
      Saltar al contenido principal
    </a>
  );
}
