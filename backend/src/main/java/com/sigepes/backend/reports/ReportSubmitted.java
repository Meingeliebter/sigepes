package com.sigepes.backend.reports;

/**
 * Se recibió una denuncia nueva; el módulo de alertas (HU-11) avisa a los inspectores.
 *
 * <p>No lleva ningún dato del denunciante a propósito (RN-20).
 */
public record ReportSubmitted(String reportId, String trackingCode, ReportType reportType) {
}
