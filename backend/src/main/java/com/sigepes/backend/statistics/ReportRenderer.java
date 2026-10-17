package com.sigepes.backend.statistics;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * AC-18.3 · Exporta un {@link StatTable} a CSV o PDF con el mismo encabezado: SIGEPES, título,
 * fecha de generación, filtros usados y quién lo generó; las notas de registros excluidos van al
 * pie (RN-36).
 */
@Component
class ReportRenderer {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final Color NAVY = new Color(0x0B, 0x3B, 0x5C);
    private static final Color ROW_TINT = new Color(0xEE, 0xF5, 0xF7);

    // ------------------------------------------------------------------
    // CSV
    // ------------------------------------------------------------------

    /**
     * CSV con BOM UTF-8 y punto y coma, como lo abre Excel en configuración regional de Colombia. Las
     * celdas que empiezan por = + - @ se neutralizan para evitar inyección de fórmulas.
     */
    byte[] csv(StatTable table) {
        StringBuilder out = new StringBuilder("﻿");
        line(out, List.of("SIGEPES · Sistema de Gestión de Pesca Sostenible"));
        line(out, List.of(table.title()));
        line(out, List.of("Generado: " + STAMP.format(table.generatedAt()) + " por " + table.generatedBy()));
        for (String filter : table.filters()) {
            line(out, List.of(filter));
        }
        out.append("\r\n");
        line(out, table.columns());
        for (List<String> row : table.rows()) {
            line(out, row);
        }
        out.append("\r\n");
        for (String note : table.footnotes()) {
            line(out, List.of("Nota: " + note));
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder out, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                out.append(';');
            }
            out.append(cell(cells.get(i)));
        }
        out.append("\r\n");
    }

    static String cell(String raw) {
        String value = raw == null ? "" : raw;
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 && !looksNumeric(value)) {
            value = "'" + value;
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Un número negativo (p. ej. una longitud "-77.52") es un dato, no una fórmula. */
    private static boolean looksNumeric(String value) {
        return value.matches("-?\\d+(\\.\\d+)?(, -?\\d+(\\.\\d+)?)?");
    }

    // ------------------------------------------------------------------
    // PDF
    // ------------------------------------------------------------------

    byte[] pdf(StatTable table) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(table.columns().size() > 5 ? PageSize.A4.rotate() : PageSize.A4, 36, 36, 40, 40);
        PdfWriter.getInstance(document, out);
        document.addTitle("SIGEPES · " + table.title());
        document.addAuthor(table.generatedBy());
        document.addCreator("SIGEPES");
        document.open();

        Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NAVY);
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, NAVY);
        Font meta = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);
        Font note = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.DARK_GRAY);

        document.add(new Paragraph("SIGEPES · Sistema de Gestión de Pesca Sostenible", brand));
        Paragraph heading = new Paragraph(table.title(), title);
        heading.setSpacingAfter(4);
        document.add(heading);
        document.add(new Paragraph("Generado el " + STAMP.format(table.generatedAt()) + " por " + table.generatedBy(), meta));
        for (String filter : table.filters()) {
            document.add(new Paragraph(filter, meta));
        }

        PdfPTable grid = new PdfPTable(table.columns().size());
        grid.setWidthPercentage(100);
        grid.setSpacingBefore(12);
        grid.setHeaderRows(1);
        for (String column : table.columns()) {
            PdfPCell cell = new PdfPCell(new Phrase(column, header));
            cell.setBackgroundColor(NAVY);
            cell.setPadding(5);
            cell.setBorderColor(NAVY);
            grid.addCell(cell);
        }
        if (table.rows().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Sin registros para los filtros elegidos.", body));
            empty.setColspan(table.columns().size());
            empty.setPadding(6);
            grid.addCell(empty);
        }
        for (int r = 0; r < table.rows().size(); r++) {
            for (String value : table.rows().get(r)) {
                PdfPCell cell = new PdfPCell(new Phrase(value, body));
                cell.setPadding(4);
                cell.setBorderColor(new Color(0xD5, 0xDE, 0xE3));
                if (r % 2 == 1) {
                    cell.setBackgroundColor(ROW_TINT);
                }
                if (value.matches("-?\\d+(\\.\\d+)?")) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                grid.addCell(cell);
            }
        }
        document.add(grid);

        for (String footnote : table.footnotes()) {
            Paragraph paragraph = new Paragraph("Nota: " + footnote, note);
            paragraph.setSpacingBefore(4);
            document.add(paragraph);
        }
        document.close();
        return out.toByteArray();
    }
}
