package br.com.brcodevalidator.utils;

import java.util.ArrayList;
import java.util.List;

public class Tlv {
    public final String tag;
    public final String value;

    public Tlv(String tag, String value) {
        this.tag = tag;
        this.value = value;
    }

    public static List<Tlv> parse(String payload) {
        List<Tlv> list = new ArrayList<>();
        int i = 0;

        while (i < payload.length()) {
            if (i + 4 > payload.length() || !payload.substring(i, i + 4).matches("\\d{4}")) {
                throw new IllegalArgumentException("Invalid EMV at position " + i);
            }

            String tag = payload.substring(i, i + 2);
            int size = Integer.parseInt(payload.substring(i + 2, i + 4));
            int end = i + 4 + size;

            if (end > payload.length()) {
                throw new IllegalArgumentException("Length of tag " + tag + " goes past the end of the payload");
            }

            list.add(new Tlv(tag, payload.substring(i + 4, end)));
            i = end;
        }

        return list;
    }

    public static String build(String tag, String value) {
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
