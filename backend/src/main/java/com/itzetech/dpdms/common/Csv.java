package com.itzetech.dpdms.common;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Builds a CSV download. Starts with a byte order mark so Excel reads accented names correctly. */
public final class Csv {

    private final StringBuilder out = new StringBuilder("﻿");

    public Csv row(Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(cell(cells[i]));
        }
        out.append("\r\n");
        return this;
    }

    /** Quotes the cell when needed and defuses spreadsheet formulas (a cell starting with = + - @). */
    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        boolean number = value instanceof Number;
        if (!number && !s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public ResponseEntity<byte[]> download(String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(out.toString().getBytes(StandardCharsets.UTF_8));
    }
}
