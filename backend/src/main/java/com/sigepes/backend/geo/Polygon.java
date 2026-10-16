package com.sigepes.backend.geo;

import java.util.List;

import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;

/**
 * Polígono geográfico simple (lista de vértices {@code [lat, lon]}) con las operaciones que necesita
 * RF-04.
 *
 * <p>Las zonas costeras de interés son pequeñas (decenas de km), así que se trabaja en coordenadas
 * planas lat/lon: el error de no proyectar es despreciable frente a la precisión del GPS de un
 * teléfono.
 */
public record Polygon(List<double[]> vertices) {

    public static Polygon validated(List<double[]> vertices) {
        if (vertices == null || vertices.size() < 3) {
            throw ApiException.of(ErrorCode.INVALID_ZONE_POLYGON, "El polígono necesita al menos 3 vértices.");
        }
        for (double[] vertex : vertices) {
            if (vertex == null || vertex.length != 2 || Math.abs(vertex[0]) > 90 || Math.abs(vertex[1]) > 180) {
                throw ApiException.of(ErrorCode.INVALID_ZONE_POLYGON,
                        "Cada vértice debe ser [latitud, longitud] con valores válidos.");
            }
        }
        Polygon polygon = new Polygon(List.copyOf(vertices));
        if (polygon.selfIntersects()) {
            throw ApiException.of(ErrorCode.INVALID_ZONE_POLYGON, "Los lados del polígono no pueden cruzarse.");
        }
        return polygon;
    }

    public double minLat() {
        return vertices.stream().mapToDouble(v -> v[0]).min().orElseThrow();
    }

    public double maxLat() {
        return vertices.stream().mapToDouble(v -> v[0]).max().orElseThrow();
    }

    public double minLon() {
        return vertices.stream().mapToDouble(v -> v[1]).min().orElseThrow();
    }

    public double maxLon() {
        return vertices.stream().mapToDouble(v -> v[1]).max().orElseThrow();
    }

    /** Prueba de punto en polígono por cruce de rayos (Jordan). */
    public boolean contains(double lat, double lon) {
        boolean inside = false;
        int n = vertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double latI = vertices.get(i)[0];
            double lonI = vertices.get(i)[1];
            double latJ = vertices.get(j)[0];
            double lonJ = vertices.get(j)[1];
            boolean crosses = (latI > lat) != (latJ > lat)
                    && lon < (lonJ - lonI) * (lat - latI) / (latJ - latI) + lonI;
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** ¿Algún par de lados no adyacentes se cruza? (n es pequeño: O(n²) basta). */
    boolean selfIntersects() {
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            double[] a1 = vertices.get(i);
            double[] a2 = vertices.get((i + 1) % n);
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(i - j) <= 1 || (i == 0 && j == n - 1)) {
                    continue;
                }
                double[] b1 = vertices.get(j);
                double[] b2 = vertices.get((j + 1) % n);
                if (segmentsCross(a1, a2, b1, b2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean segmentsCross(double[] p1, double[] p2, double[] q1, double[] q2) {
        double d1 = orientation(q1, q2, p1);
        double d2 = orientation(q1, q2, p2);
        double d3 = orientation(p1, p2, q1);
        double d4 = orientation(p1, p2, q2);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double orientation(double[] a, double[] b, double[] c) {
        return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
    }
}
