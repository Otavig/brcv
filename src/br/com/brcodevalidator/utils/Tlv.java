package br.com.brcodevalidator.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Tlv {
    public final String tag;
    public final String value;
    public final int start;
    public final int end;

    public Tlv(String tag, String value) {
        this(tag, value, -1, -1);
    }

    public Tlv(String tag, String value, int start, int end) {
        this.tag = tag;
        this.value = value;
        this.start = start;
        this.end = end;
    }

    public static List<Tlv> parse(String payload) {
        return parseWire(payload);
    }

    public static List<Tlv> parse(String payload, boolean utf8Bytes) {
        if (!utf8Bytes) {
            return parseWire(payload);
        }
        List<Tlv> decoded = new ArrayList<>();
        for (Tlv t : parseWire(toWire(payload))) {
            decoded.add(new Tlv(t.tag, fromWire(t.value), t.start, t.end));
        }
        return decoded;
    }

    public static String toWire(String text) {
        return new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    public static String fromWire(String wire) {
        return new String(wire.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    private static List<Tlv> parseWire(String payload) {
        List<Tlv> list = new ArrayList<>();
        int i = 0;

        while (i < payload.length()) {
            if (i + 4 > payload.length() || !isDigits(payload, i, i + 4)) {
                throw new IllegalArgumentException("Invalid EMV at position " + i
                        + " (expected 2 digits for the tag and 2 digits for the length)");
            }

            String tag = payload.substring(i, i + 2);
            int size = Integer.parseInt(payload.substring(i + 2, i + 4));
            int end = i + 4 + size;

            if (end > payload.length()) {
                throw new IllegalArgumentException("Length of tag " + tag + " goes past the end of the payload");
            }

            list.add(new Tlv(tag, payload.substring(i + 4, end), i, end));
            i = end;
        }

        return list;
    }

    private static boolean isDigits(String text, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static String build(String tag, String value) {
        if (value.length() > 99) {
            throw new IllegalArgumentException("Value of tag " + tag + " is too long ("
                    + value.length() + " characters, the limit is 99)");
        }
        return tag + String.format("%02d", value.length()) + value;
    }

    public static String find(List<Tlv> list, String tag) {
        for (Tlv t : list) {
            if (t.tag.equals(tag)) {
                return t.value;
            }
        }
        return null;
    }
}
