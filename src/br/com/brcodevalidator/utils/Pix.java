package br.com.brcodevalidator.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Map.entry("62", "Additional Data Field"),
        Map.entry("62.05", "Reference Label (TxId)"),
        Map.entry("63", "CRC16")
    );


    public static boolean isDynamic(String payload) {
        try {
            List<Tlv> account = account(payload);
            return Tlv.find(account, "25") != null && Tlv.find(account, "01") == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isValid(String payload) {
        try {
            if (!payload.startsWith("000201") || !crcOk(payload)) {
                return false;
            }
            List<Tlv> account = account(payload);
            if (isDynamic(payload)) {
                return !Tlv.find(account, "25").isBlank();
            }
            return Tlv.find(account, "01") != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean crcOk(String payload) {
        int n = payload.length();
        if (n < 8 || !payload.substring(n - 8, n - 4).equals("6304")) {
            return false;
        }
        String calculated = Crc16False.encode(payload.substring(0, n - 4));
        return calculated.equalsIgnoreCase(payload.substring(n - 4));
    }

    private static List<Tlv> account(String payload) {
        String value = Tlv.find(Tlv.parse(payload), "26");
        return value == null ? List.of() : Tlv.parse(value);
    }


    public static String fixCrc(String payload) {
        StringBuilder sb = new StringBuilder();
        for (Tlv t : Tlv.parse(payload)) {
            if (t.tag.equals("63")) {
                break;
            }
            sb.append(Tlv.build(t.tag, t.value));
        }
        sb.append("6304");
        return sb + Crc16False.encode(sb.toString());
    }


    public static String create(String key, String url, String name, String city,
                                String amount, String txid, String description) {
        if ((key == null) == (url == null)) {
            throw new IllegalArgumentException("Provide only one of --key (static) or --url (dynamic)");
        }
        if (name.length() > 25 || city.length() > 15) {
            throw new IllegalArgumentException("Name accepts up to 25 characters and city up to 15");
        }

        String account = Tlv.build("00", "br.gov.bcb.pix");
        if (key != null) {
            account += Tlv.build("01", key);
            if (description != null) {
                account += Tlv.build("02", description);
            }
        } else {
            account += Tlv.build("25", url);
        }

        String payload = Tlv.build("00", "01")
            + Tlv.build("01", key != null ? "11" : "12")
            + Tlv.build("26", account)
            + Tlv.build("52", "0000")
            + Tlv.build("53", "986");

        if (amount != null) {
            BigDecimal value = new BigDecimal(amount.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
            payload += Tlv.build("54", value.toPlainString());
        }

        payload += Tlv.build("58", "BR")
            + Tlv.build("59", name)
            + Tlv.build("60", city)
            + Tlv.build("62", Tlv.build("05", txid));

        return fixCrc(payload);
    }


    public static String extract(String payload) {
        List<Tlv> tags = Tlv.parse(payload);
        List<Tlv> account = account(payload);
        String extra = Tlv.find(tags, "62");
        List<Tlv> extraTags = extra == null ? List.of() : Tlv.parse(extra);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", isDynamic(payload) ? "dynamic" : "static");
        result.put("key", Tlv.find(account, "01"));
        result.put("url", Tlv.find(account, "25"));
        result.put("description", Tlv.find(account, "02"));
        result.put("merchantName", Tlv.find(tags, "59"));
        result.put("merchantCity", Tlv.find(tags, "60"));
        result.put("amount", Tlv.find(tags, "54"));
        result.put("txid", Tlv.find(extraTags, "05"));
        result.put("crcValid", crcOk(payload));

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


    public static String decode(String payload) {
        return toJson(Tlv.parse(payload), "", "");
    }

    private static String toJson(List<Tlv> tags, String parent, String indent) {
        StringBuilder sb = new StringBuilder("{\n");

        for (int i = 0; i < tags.size(); i++) {
            Tlv t = tags.get(i);
            String path = parent.isEmpty() ? t.tag : parent + "." + t.tag;
            int number = Integer.parseInt(t.tag);
            boolean hasSubTags = parent.isEmpty() && ((number >= 26 && number <= 51) || number == 62);

            sb.append(indent).append("  ").append(quote(t.tag)).append(": {\n");
            sb.append(indent).append("    \"name\": ").append(quote(NAMES.getOrDefault(path, "Unknown"))).append(",\n");
            sb.append(indent).append("    \"length\": ").append(t.value.length()).append(",\n");
            sb.append(indent).append("    \"value\": ");
            if (hasSubTags) {
                sb.append(toJson(Tlv.parse(t.value), t.tag, indent + "    "));
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
