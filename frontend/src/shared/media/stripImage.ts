/** Lado mayor máximo tras el reescalado: suficiente como evidencia y liviano para enviar desde el mar. */
const MAX_SIDE = 2048;

export const ACCEPTED_PHOTO_TYPES = ['image/jpeg', 'image/png'];
export const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

/**
 * AC-10.5 (defensa en profundidad): vuelve a dibujar la foto en un canvas antes de enviarla, lo
 * que descarta el EXIF (GPS, modelo del teléfono) ya en el dispositivo. El servidor repite la
 * limpieza por su cuenta, así que si el navegador no soporta canvas se envía el original.
 */
export async function stripImage(file: File): Promise<Blob> {
  if (typeof createImageBitmap === 'undefined') return file;
  try {
    const bitmap = await createImageBitmap(file);
    const ratio = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(bitmap.width * ratio);
    canvas.height = Math.round(bitmap.height * ratio);
    const context = canvas.getContext('2d');
    if (!context) return file;
    context.fillStyle = '#fff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    bitmap.close();
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.85));
    return blob ?? file;
  } catch {
    return file;
  }
}
