package br.com.brcodevalidator.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class PixValidator {

    public enum Type { STATIC, DYNAMIC, UNKNOWN }

    public static final class Result {
        public final Type type;
        public final List<String> errors;
        public final List<String> warnings;
        public final PixKey.Type keyType;

        Result(Type type, List<String> errors, List<String> warnings, PixKey.Type keyType) {
            this.type = type;
            this.errors = errors;
            this.warnings = warnings;
            this.keyType = keyType;
        }

        public boolean valid() {
            return errors.isEmpty();
        }
    }

    private static final Pattern HEX4 = Pattern.compile("[0-9A-Fa-f]{4}");
    private static final Pattern MCC = Pattern.compile("\\d{4}");
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,10}\\.\\d{2}");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");
    private static final Pattern TXID = Pattern.compile("[A-Za-z0-9]{1,25}");
    private static final Pattern POSTAL = Pattern.compile("\\d{8}");
    private static final Pattern URL = Pattern.compile(
            "(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}(?::\\d{1,5})?"
                    + "(?:/[A-Za-z0-9\\-._~%!$&'()*+,;=:@/]*)?");

    private PixValidator() {
        throw new IllegalStateException("The PixValidator class cannot/should not be instantiated");
    }

    public static Result validate(String raw) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String payload = Pix.normalize(raw);
        if (payload.isEmpty()) {
            errors.add("Payload is empty");
            return new Result(Type.UNKNOWN, errors, warnings, null);
        }

        boolean crcFormatOk = checkCrc(payload, errors, warnings);

        boolean bytes = false;
        List<Tlv> tags = null;
        String parseError = null;
        try {
            tags = Tlv.parse(payload);
        } catch (IllegalArgumentException e) {
            parseError = e.getMessage();
            if (!Text.isAscii(payload)) {
                try {
                    tags = Tlv.parse(payload, true);
                    bytes = true;
                    warnings.add("Field lengths are counted in UTF-8 bytes; the Pix manual asks for ASCII text without accents");
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (tags == null) {
            errors.add(parseError);
            return new Result(Type.UNKNOWN, errors, warnings, null);
        }

        Set<String> seen = new HashSet<>();
        Set<String> reported = new HashSet<>();
        for (Tlv t : tags) {
            if (!seen.add(t.tag) && reported.add(t.tag)) {
                errors.add("Tag " + t.tag + " appears more than once");
            }
        }

        if (crcFormatOk) {
            Tlv last = tags.get(tags.size() - 1);
            if (!last.tag.equals("63") || last.value.length() != 4) {
                errors.add("The last field must be tag 63 (CRC16) with 4 characters; check the length of the previous fields");
            }
        }

        String format = Tlv.find(tags, "00");
        if (format == null) {
            errors.add("Tag 00 (Payload Format Indicator) is missing");
        } else {
            if (!format.equals("01")) {
                errors.add("Tag 00 must be 01 (found " + format + ")");
            }
            if (!tags.get(0).tag.equals("00")) {
                errors.add("Tag 00 must be the first field");
            }
        }

        String initiation = Tlv.find(tags, "01");
        if (initiation != null && !initiation.equals("11") && !initiation.equals("12")) {
            errors.add("Tag 01 (Point of Initiation Method) must be 11 or 12 (found " + initiation + ")");
        }

        Type type = Type.UNKNOWN;
        PixKey.Type keyType = null;

        String template = Tlv.find(tags, "26");
        if (template == null) {
            errors.add("Tag 26 (Merchant Account Information) is missing");
        } else {
            List<Tlv> account = null;
            try {
                account = Tlv.parse(template, bytes);
            } catch (IllegalArgumentException e) {
                errors.add("Tag 26 is malformed: " + e.getMessage());
            }
            if (account != null) {
                String gui = Tlv.find(account, "00");
                if (gui == null) {
                    errors.add("Tag 26.00 (GUI) is missing; it must be br.gov.bcb.pix");
                } else if (!gui.equalsIgnoreCase("br.gov.bcb.pix")) {
                    errors.add("Tag 26.00 (GUI) must be br.gov.bcb.pix (found " + gui + ")");
                } else if (!gui.equals("br.gov.bcb.pix")) {
                    warnings.add("Tag 26.00 (GUI) should be lowercase: br.gov.bcb.pix");
                }

                String key = Tlv.find(account, "01");
                String url = Tlv.find(account, "25");

                if (key != null && url != null) {
                    errors.add("Tag 26 has both a Pix key (26.01) and a payload URL (26.25); use only one");
                } else if (key == null && url == null) {
                    errors.add("Tag 26 needs a Pix key (26.01) for static or a payload URL (26.25) for dynamic");
                } else if (key != null) {
                    type = Type.STATIC;
                    keyType = PixKey.detect(key);
                    if (keyType == null) {
                        errors.add(invalidKeyMessage(key));
                    }
                } else {
                    type = Type.DYNAMIC;
                    checkUrl(url, errors, warnings);
                    if (Tlv.find(account, "02") != null) {
                        warnings.add("Tag 26.02 (description) is not used in dynamic Pix");
                    }
                    if ("11".equals(initiation)) {
                        warnings.add("Dynamic Pix normally has tag 01 = 12 or no tag 01");
                    }
                }
            }
        }

        String mcc = Tlv.find(tags, "52");
        if (mcc == null) {
            errors.add("Tag 52 (Merchant Category Code) is missing");
        } else if (!MCC.matcher(mcc).matches()) {
            errors.add("Tag 52 (Merchant Category Code) must have 4 digits (found " + mcc + ")");
        }

        String currency = Tlv.find(tags, "53");
        if (currency == null) {
            errors.add("Tag 53 (Transaction Currency) is missing; Pix uses 986");
        } else if (!currency.equals("986")) {
            errors.add("Tag 53 (Transaction Currency) must be 986 (found " + currency + ")");
        }

        String amount = Tlv.find(tags, "54");
        if (amount != null) {
            checkAmount(amount, errors, warnings);
        }

        String country = Tlv.find(tags, "58");
        if (country == null) {
            errors.add("Tag 58 (Country Code) is missing; Pix uses BR");
        } else if (!country.equals("BR")) {
            errors.add("Tag 58 (Country Code) must be BR (found " + country + ")");
        }

        checkText("59", "Merchant Name", Tlv.find(tags, "59"), 25, errors, warnings);
        checkText("60", "Merchant City", Tlv.find(tags, "60"), 15, errors, warnings);

        String postal = Tlv.find(tags, "61");
        if (postal != null && !POSTAL.matcher(postal).matches()) {
            warnings.add("Tag 61 (Postal Code) should have 8 digits");
        }

        String extra = Tlv.find(tags, "62");
        if (extra == null) {
            errors.add("Tag 62 (Additional Data Field) is missing; it must carry the txid in 62.05");
        } else {
            try {
                List<Tlv> extraTags = Tlv.parse(extra, bytes);
                String txid = Tlv.find(extraTags, "05");
                if (txid == null) {
                    errors.add("Tag 62.05 (txid) is missing; use *** when there is none");
                } else {
                    checkTxid(txid, type, errors, warnings);
                }
            } catch (IllegalArgumentException e) {
                errors.add("Tag 62 is malformed: " + e.getMessage());
            }
        }

        return new Result(type, errors, warnings, keyType);
    }

    private static boolean checkCrc(String payload, List<String> errors, List<String> warnings) {
        int n = payload.length();
        if (n < 8 || !payload.substring(n - 8, n - 4).equals("6304")
                || !HEX4.matcher(payload.substring(n - 4)).matches()) {
            errors.add("The payload must end with the CRC field: 6304 followed by 4 hexadecimal digits");
            return false;
        }
        String found = payload.substring(n - 4);
        String expected = Crc16False.encode(payload.substring(0, n - 4));
        if (!expected.equalsIgnoreCase(found)) {
            errors.add("CRC16 does not match (expected " + expected + ", found " + found.toUpperCase() + ")");
        } else if (!expected.equals(found)) {
            warnings.add("CRC16 should be uppercase");
        }
        return true;
    }

    private static String invalidKeyMessage(String key) {
        if (key.length() > PixKey.MAX_LENGTH) {
            return "Tag 26.01 (Pix key) is longer than " + PixKey.MAX_LENGTH + " characters";
        }
        if (key.matches("\\d{10,13}")) {
            return "Tag 26.01 (Pix key) '" + key + "' is not a valid CPF; phone keys must use the +55DDDNUMBER format";
        }
        return "Tag 26.01 (Pix key) '" + key
                + "' is not a valid CPF, CNPJ, phone (+55...), e-mail or random key (UUID)";
    }

    private static void checkUrl(String url, List<String> errors, List<String> warnings) {
        if (url.isEmpty()) {
            errors.add("Tag 26.25 (payload URL) is empty");
            return;
        }
        String host = url;
        String lower = url.toLowerCase();
        if (lower.startsWith("http://")) {
            errors.add("Tag 26.25 (payload URL) must be HTTPS");
            return;
        }
        if (lower.startsWith("https://")) {
            warnings.add("Tag 26.25 (payload URL) should not include the https:// prefix");
            host = url.substring(8);
        }
        if (!URL.matcher(host).matches()) {
            errors.add("Tag 26.25 (payload URL) is not valid: " + url);
        }
        if (url.length() > 77) {
            warnings.add("Tag 26.25 (payload URL) is longer than the recommended 77 characters");
        }
    }

    private static void checkAmount(String amount, List<String> errors, List<String> warnings) {
        if (AMOUNT.matcher(amount).matches()) {
            if (new BigDecimal(amount).signum() <= 0) {
                errors.add("Tag 54 (Transaction Amount) must be greater than zero");
            }
        } else if (NUMBER.matcher(amount).matches()) {
            warnings.add("Tag 54 (Transaction Amount) should have exactly 2 decimals, e.g. 10.50");
            if (new BigDecimal(amount).signum() <= 0) {
                errors.add("Tag 54 (Transaction Amount) must be greater than zero");
            }
        } else {
            errors.add("Tag 54 (Transaction Amount) must use a dot as decimal separator, e.g. 10.50 (found " + amount + ")");
        }
    }

    private static void checkText(String tag, String name, String value, int max,
                                  List<String> errors, List<String> warnings) {
        if (value == null || value.isBlank()) {
            errors.add("Tag " + tag + " (" + name + ") is missing");
            return;
        }
        if (value.length() > max) {
            errors.add("Tag " + tag + " (" + name + ") has " + value.length() + " characters; the limit is " + max);
        }
        if (!Text.isAscii(value)) {
            warnings.add("Tag " + tag + " (" + name + ") should not have accents or special characters");
        }
    }

    private static void checkTxid(String txid, Type type, List<String> errors, List<String> warnings) {
        if (txid.equals("***")) {
            return;
        }
        if (txid.isEmpty() || txid.length() > 25) {
            errors.add("Tag 62.05 (txid) must have 1 to 25 characters (or ***)");
        } else if (!TXID.matcher(txid).matches()) {
            warnings.add("Tag 62.05 (txid) should have only letters and digits");
        } else if (type == Type.DYNAMIC) {
            warnings.add("Dynamic Pix normally uses *** as txid");
        }
    }
}
