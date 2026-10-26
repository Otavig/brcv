package br.com.brcodevalidator.utils;

import java.util.regex.Pattern;

public final class PixKey {

    public enum Type { CPF, CNPJ, PHONE, EMAIL, EVP }

    public static final int MAX_LENGTH = 77;

    private static final Pattern PHONE_BR = Pattern.compile("\\+55[1-9]\\d\\d{8,9}");
    private static final Pattern PHONE_INTL = Pattern.compile("\\+[1-9]\\d{7,14}");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}");
    private static final Pattern UUID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern CNPJ = Pattern.compile("[0-9A-Z]{12}\\d{2}");

    private static final int[] CNPJ_W1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_W2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private PixKey() {
        throw new IllegalStateException("The PixKey class cannot/should not be instantiated");
    }

    public static Type detect(String key) {
        if (key == null || key.isEmpty() || key.length() > MAX_LENGTH) {
            return null;
        }
        if (key.startsWith("+")) {
            boolean ok = key.startsWith("+55") ? PHONE_BR.matcher(key).matches() : PHONE_INTL.matcher(key).matches();
            return ok ? Type.PHONE : null;
        }
        if (key.indexOf('@') >= 0) {
            return isEmail(key) ? Type.EMAIL : null;
        }
        if (key.length() == 11 && allDigits(key)) {
            return isCpf(key) ? Type.CPF : null;
        }
        if (key.length() == 14 && CNPJ.matcher(key).matches()) {
            return isCnpj(key) ? Type.CNPJ : null;
        }
        return UUID.matcher(key).matches() ? Type.EVP : null;
    }

    public static boolean isValid(String key) {
        return detect(key) != null;
    }

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String key = input.trim();
        if (key.matches("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}")) {
            return key.replaceAll("[.\\-]", "");
        }
        if (key.matches("[0-9A-Za-z]{2}\\.?[0-9A-Za-z]{3}\\.?[0-9A-Za-z]{3}/?[0-9A-Za-z]{4}-?\\d{2}")) {
            return key.replaceAll("[./\\-]", "").toUpperCase();
        }
        if ((key.startsWith("+") || key.startsWith("(")) && key.matches("[+()\\d\\s.\\-]+")) {
            String digits = key.replaceAll("[^\\d]", "");
            if (key.startsWith("+")) {
                return "+" + digits;
            }
            return "+55" + digits;
        }
        return key;
    }

    public static boolean isCpf(String cpf) {
        if (cpf == null || cpf.length() != 11 || !allDigits(cpf) || allSame(cpf)) {
            return false;
        }
        return cpfDigit(cpf, 9) == cpf.charAt(9) - '0' && cpfDigit(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int cpfDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * (length + 1 - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    public static boolean isCnpj(String cnpj) {
        if (cnpj == null || !CNPJ.matcher(cnpj).matches() || allSame(cnpj)) {
            return false;
        }
        return cnpjDigit(cnpj, CNPJ_W1) == cnpj.charAt(12) - '0'
                && cnpjDigit(cnpj, CNPJ_W2) == cnpj.charAt(13) - '0';
    }

    private static int cnpjDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (cnpj.charAt(i) - '0') * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    private static boolean isEmail(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        return local.length() <= 64
                && !local.startsWith(".") && !local.endsWith(".") && !local.contains("..")
                && EMAIL.matcher(email).matches();
    }

    private static boolean allDigits(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean allSame(String text) {
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) != text.charAt(0)) {
                return false;
            }
        }
        return true;
    }
}
