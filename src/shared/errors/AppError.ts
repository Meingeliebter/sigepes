import { AxiosError } from 'axios';

export interface FieldError {
  field: string;
  message: string;
}

export interface AppErrorShape {
  code: string;
  message: string;
  status?: number;
  fieldErrors?: FieldError[];
  correlationId?: string;
  retryable: boolean;
}

export class AppError extends Error implements AppErrorShape {
  code: string;
  status?: number;
  fieldErrors?: FieldError[];
  correlationId?: string;
  retryable: boolean;

  constructor(shape: AppErrorShape) {
    super(shape.message);
    this.name = 'AppError';
    this.code = shape.code;
    this.status = shape.status;
    this.fieldErrors = shape.fieldErrors;
    this.correlationId = shape.correlationId;
    this.retryable = shape.retryable;
  }
}

/** Normaliza cualquier error (Axios, red, etc.) a un AppError uniforme. */
export function toAppError(error: unknown): AppError {
  if (error instanceof AppError) return error;

  if (error instanceof AxiosError) {
    // Error de red / sin respuesta del servidor
    if (!error.response) {
      return new AppError({
        code: 'NETWORK_ERROR',
        message:
          'No hay conexión con el servidor. La operación se conservará para sincronizar más tarde.',
        retryable: true,
      });
    }

    const status = error.response.status;
    const data = error.response.data as
      | { message?: string; code?: string; fieldErrors?: FieldError[] }
      | undefined;

    const messageByStatus: Record<number, string> = {
      400: 'La solicitud no es válida.',
      401: 'Tu sesión expiró. Inicia sesión nuevamente.',
      403: 'No tienes permisos para realizar esta acción.',
      404: 'El recurso solicitado no existe.',
      409: 'Conflicto: el registro ya existe o fue modificado.',
      422: 'Hay errores de validación en el formulario.',
      429: 'Demasiadas solicitudes. Intenta de nuevo en unos momentos.',
    };

    return new AppError({
      code: data?.code ?? `HTTP_${status}`,
      message:
        data?.message ??
        messageByStatus[status] ??
        (status >= 500
          ? 'Error interno del servidor. Intenta más tarde.'
          : 'Ocurrió un error inesperado.'),
      status,
      fieldErrors: data?.fieldErrors,
      correlationId: error.response.headers['x-correlation-id'] as
        | string
        | undefined,
      retryable: status >= 500 || status === 429,
    });
  }

  return new AppError({
    code: 'UNKNOWN_ERROR',
    message:
      error instanceof Error ? error.message : 'Ocurrió un error inesperado.',
    retryable: false,
  });
}
