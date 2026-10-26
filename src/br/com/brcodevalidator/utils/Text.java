package br.com.brcodevalidator.utils;

import java.text.Normalizer;

public final class Text {
    private Text() {
        throw new IllegalStateException("The Text class cannot/should not be instantiated");
    }

    public static String ascii(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    public static boolean isAscii(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c > 126) {
                return false;
            }
        }
        return true;
    }
}
