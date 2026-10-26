package br.com.brcodevalidator.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Pix {

    private static final Map<String, String> NAMES = Map.ofEntries(
        Map.entry("00", "Payload Format Indicator"),
        Map.entry("01", "Point of Initiation Method"),
        Map.entry("26", "Merchant Account Information"),
        Map.entry("26.00", "GUI"),
        Map.entry("26.01", "Pix Key"),
        Map.entry("26.02", "Description"),
        Map.entry("26.25", "Payload URL"),
        Map.entry("52", "Merchant Category Code"),
        Map.entry("53", "Transaction Currency"),
        Map.entry("54", "Transaction Amount"),
        Map.entry("58", "Country Code"),
        Map.entry("59", "Merchant Name"),
        Map.entry("60", "Merchant City"),
        Map.entry("61", "Postal Code"),
        Map.entry("62", "Additional Data Field"),
        Map.entry("62.01", "Bill Number"),
        Map.entry("62.02", "Mobile Number"),
        Map.entry("62.03", "Store Label"),
        Map.entry("62.04", "Loyalty Number"),
        Map.entry("62.05", "Reference Label (TxId)"),
        Map.entry("62.06", "Customer Label"),
        Map.entry("62.07", "Terminal Label"),
        Map.entry("62.08", "Purpose of Transaction"),
        Map.entry("62.09", "Additional Consumer Data Request"),
        Map.entry("63", "CRC16"),
        Map.entry("64", "Merchant Information - Language Template"),
        Map.entry("64.00", "Language Preference"),
        Map.entry("64.01", "Alternate Merchant Name"),
        Map.entry("64.02", "Alternate Merchant City")
    );

    private static final Pattern TXID = Pattern.compile("[A-Za-z0-9]{1,25}");

    private record Parsed(List<Tlv> tags, boolean bytes) { }

    public static String normalize(String payload) {
        if (payload == null) {
            return "";
        }
        String text = payload.replace("\uFEFF", "").replace("\u200B", "").replaceAll("[\\r\\n\\t]+", "");
        text = text.replaceAll("^[\\s\\u00A0]+|[\\s\\u00A0]+$", "");
        while (text.length() >= 2 && isQuote(text.charAt(0)) && isQuote(text.charAt(text.length() - 1))) {
            text = text.substring(1, text.length() - 1).replaceAll("^[\\s\\u00A0]+|[\\s\\u00A0]+$", "");
        }
        return text;
    }

    private static boolean isQuote(char c) {
        return c == '"' || c == '\'' || c == '\u201C' || c == '\u201D' || c == '\u2018' || c == '\u2019';
    }

    private static Parsed parseAuto(String payload) {
        try {
            return new Parsed(Tlv.parse(payload), false);
        } catch (IllegalArgumentException e) {
            if (!Text.isAscii(payload)) {
                try {
                    return new Parsed(Tlv.parse(payload, true), true);
                } catch (IllegalArgumentException ignored) {
                }
            }
            throw e;
        }
    }

    public static boolean isDynamic(String payload) {
        return PixValidator.validate(payload).type == PixValidator.Type.DYNAMIC;
    }

    public static boolean isValid(String payload) {
        return PixValidator.validate(payload).valid();
    }

    public static boolean crcOk(String payload) {
        String p = normalize(payload);
        int n = p.length();
        if (n < 8 || !p.substring(n - 8, n - 4).equals("6304")) {
            return false;
        }
        String calculated = Crc16False.encode(p.substring(0, n - 4));
        return calculated.equalsIgnoreCase(p.substring(n - 4));
    }

    private static List<Tlv> account(List<Tlv> tags, boolean bytes) {
        String value = Tlv.find(tags, "26");
        if (value == null) {
            return List.of();
        }
        try {
            return Tlv.parse(value, bytes);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }


    public static String fixCrc(String raw) {
        String payload = normalize(raw);
        String wire = payload;
        boolean bytes = false;
        List<Tlv> tags;
        try {
            tags = Tlv.parse(payload);
        } catch (IllegalArgumentException e) {
            if (Text.isAscii(payload)) {
                throw e;
            }
            wire = Tlv.toWire(payload);
            bytes = true;
            tags = Tlv.parse(wire);
        }

        int cut = wire.length();
        for (Tlv t : tags) {
            if (t.tag.equals("63")) {
                cut = t.start;
                break;
            }
        }

        String body = wire.substring(0, cut) + "6304";
        if (bytes) {
            return Tlv.fromWire(body + Crc16False.encode(body.getBytes(StandardCharsets.ISO_8859_1)));
        }
        return body + Crc16False.encode(body);
    }


    public static String create(String key, String url, String name, String city,
                                String amount, String txid, String description) {
        if ((key == null) == (url == null)) {
            throw new IllegalArgumentException("Provide only one of --key (static) or --url (dynamic)");
        }
        if (url != null && description != null) {
            throw new IllegalArgumentException("--description only works with --key (static Pix)");
        }

        name = Text.ascii(name.trim());
        city = Text.ascii(city.trim());
        if (name.isEmpty() || name.length() > 25 || !Text.isAscii(name)) {
            throw new IllegalArgumentException("Name must have 1 to 25 characters (letters, digits and basic symbols)");
        }
        if (city.isEmpty() || city.length() > 15 || !Text.isAscii(city)) {
            throw new IllegalArgumentException("City must have 1 to 15 characters (letters, digits and basic symbols)");
        }

        if (txid == null || txid.isBlank()) {
            txid = "***";
        }
        if (!txid.equals("***") && !TXID.matcher(txid).matches()) {
            throw new IllegalArgumentException("txid must have 1 to 25 letters or digits (or ***)");
        }

        String account = Tlv.build("00", "br.gov.bcb.pix");
        if (key != null) {
            key = PixKey.normalize(key);
            if (!PixKey.isValid(key)) {
                throw new IllegalArgumentException("'" + key
                        + "' is not a valid Pix key (CPF, CNPJ, phone +55..., e-mail or random key)");
            }
            account += Tlv.build("01", key);
            if (description != null && !description.isBlank()) {
                account += Tlv.build("02", Text.ascii(description.trim()));
            }
        } else {
            url = url.trim();
            if (url.toLowerCase().startsWith("http://")) {
                throw new IllegalArgumentException("The payload URL must be HTTPS");
            }
            if (url.toLowerCase().startsWith("https://")) {
                url = url.substring(8);
            }
            if (!txid.equals("***")) {
                throw new IllegalArgumentException("Dynamic Pix uses *** as txid");
            }
            account += Tlv.build("25", url);
        }

        String payload = Tlv.build("00", "01")
            + Tlv.build("01", key != null ? "11" : "12")
            + Tlv.build("26", account)
            + Tlv.build("52", "0000")
            + Tlv.build("53", "986");

        if (amount != null) {
            payload += Tlv.build("54", parseAmount(amount));
        }

        payload += Tlv.build("58", "BR")
            + Tlv.build("59", name)
            + Tlv.build("60", city)
            + Tlv.build("62", Tlv.build("05", txid));

        String result = fixCrc(payload);

        PixValidator.Result check = PixValidator.validate(result);
        if (!check.valid()) {
            throw new IllegalArgumentException("The generated payload is not valid: " + String.join("; ", check.errors));
        }
        return result;
    }

    private static String parseAmount(String amount) {
        String text = amount.trim();
        if (text.contains(",")) {
            text = text.replace(".", "").replace(',', '.');
        }
        BigDecimal value;
        try {
            value = new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount '" + amount + "'; use something like 10.50");
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        String plain = value.toPlainString();
        if (plain.length() > 13) {
            throw new IllegalArgumentException("Amount is too large");
        }
        return plain;
    }


    public static String extract(String raw) {
        String payload = normalize(raw);
        Parsed parsed = parseAuto(payload);
        List<Tlv> tags = parsed.tags();
        List<Tlv> account = account(tags, parsed.bytes());
        String extra = Tlv.find(tags, "62");
        List<Tlv> extraTags = List.of();
        if (extra != null) {
            try {
                extraTags = Tlv.parse(extra, parsed.bytes());
            } catch (IllegalArgumentException ignored) {
            }
        }

        PixValidator.Result check = PixValidator.validate(payload);
        String key = Tlv.find(account, "01");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", check.type == PixValidator.Type.DYNAMIC ? "dynamic" : "static");
        result.put("key", key);
        result.put("keyType", key == null ? null : (PixKey.detect(key) == null ? null : PixKey.detect(key).name().toLowerCase()));
        result.put("url", Tlv.find(account, "25"));
        result.put("description", Tlv.find(account, "02"));
        result.put("merchantName", Tlv.find(tags, "59"));
        result.put("merchantCity", Tlv.find(tags, "60"));
        result.put("amount", Tlv.find(tags, "54"));
        result.put("txid", Tlv.find(extraTags, "05"));
        result.put("crcValid", crcOk(payload));
        result.put("valid", check.valid());

        StringBuilder sb = new StringBuilder("{\n");
        for (Map.Entry<String, Object> e : result.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            Object v = e.getValue();
            sb.append("  ").append(quote(e.getKey())).append(": ");
            sb.append(v instanceof String ? quote((String) v) : v).append(",\n");
        }
        sb.setLength(sb.length() - 2);
        return sb.append("\n}").toString();
    }


    public static String decode(String raw) {
        Parsed parsed = parseAuto(normalize(raw));
        return toJson(parsed.tags(), "", "", parsed.bytes());
    }

    private static boolean hasSubTags(String tag) {
        int number = Integer.parseInt(tag);
        return (number >= 26 && number <= 51) || number == 62 || number == 64 || (number >= 80 && number <= 99);
    }

    private static String nameOf(String path) {
        String name = NAMES.get(path);
        if (name != null) {
            return name;
        }
        int dot = path.indexOf('.');
        if (dot < 0) {
            int number = Integer.parseInt(path);
            if (number >= 2 && number <= 25) {
                return "Reserved";
            }
            if (number >= 27 && number <= 51) {
                return "Merchant Account Information";
            }
            if (number >= 65 && number <= 79) {
                return "RFU for EMVCo";
            }
            if (number >= 80 && number <= 99) {
                return "Unreserved Template";
            }
            return "Unknown";
        }
        return path.endsWith(".00") ? "GUI" : "Unknown";
    }

    private static String toJson(List<Tlv> tags, String parent, String indent, boolean bytes) {
        StringBuilder sb = new StringBuilder("{\n");

        for (int i = 0; i < tags.size(); i++) {
            Tlv t = tags.get(i);
            String path = parent.isEmpty() ? t.tag : parent + "." + t.tag;

            List<Tlv> children = null;
            if (parent.isEmpty() && hasSubTags(t.tag)) {
                try {
                    children = Tlv.parse(t.value, bytes);
                } catch (IllegalArgumentException e) {
                    children = null;
                }
            }

            sb.append(indent).append("  ").append(quote(t.tag)).append(": {\n");
            sb.append(indent).append("    \"name\": ").append(quote(nameOf(path))).append(",\n");
            sb.append(indent).append("    \"length\": ").append(t.value.length()).append(",\n");
            sb.append(indent).append("    \"value\": ");
            if (children != null) {
                sb.append(toJson(children, t.tag, indent + "    ", bytes));
            } else {
                sb.append(quote(t.value));
            }
            sb.append("\n").append(indent).append("  }");
            sb.append(i < tags.size() - 1 ? ",\n" : "\n");
        }

        return sb.append(indent).append("}").toString();
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
