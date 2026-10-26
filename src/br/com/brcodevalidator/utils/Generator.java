package br.com.brcodevalidator.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public final class Generator {

    public enum Type { EMAIL, CITY, NAME, CPF, CNPJ, PHONE, EVP, TXID, AMOUNT, PROFILE, PIX }

    public static final class Options {
        public String state;
        public String domain;
        public boolean formatted;
        public boolean pix;
    }

    private static final String[] FIRST_NAMES = {
        "Ana", "Beatriz", "Bruno", "Camila", "Carlos", "Daniel", "Eduardo", "Fernanda", "Gabriel", "Helena",
        "Igor", "Isabela", "João", "Julia", "Larissa", "Leonardo", "Lucas", "Luiza", "Marcos", "Mariana",
        "Mateus", "Natalia", "Paulo", "Pedro", "Rafael", "Renata", "Ricardo", "Rodrigo", "Sofia", "Thiago",
        "Vanessa", "Vinicius", "Amanda", "André", "Bianca", "Caio", "Diego", "Elisa", "Felipe", "Gustavo",
        "Heitor", "Ingrid", "Jéssica", "Kaique", "Letícia", "Murilo", "Nicole", "Otávio", "Priscila", "Sérgio"
    };

    private static final String[] LAST_NAMES = {
        "Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Alves", "Pereira", "Lima", "Gomes",
        "Costa", "Ribeiro", "Martins", "Carvalho", "Almeida", "Lopes", "Soares", "Fernandes", "Vieira", "Barbosa",
        "Rocha", "Dias", "Nascimento", "Andrade", "Moreira", "Nunes", "Marques", "Machado", "Mendes", "Freitas",
        "Cardoso", "Ramos", "Gonçalves", "Santana", "Teixeira", "Araújo", "Pinto", "Cavalcanti", "Monteiro", "Moura"
    };

    private static final String[] DOMAINS = {
        "gmail.com", "outlook.com", "hotmail.com", "yahoo.com.br", "icloud.com", "uol.com.br", "bol.com.br", "terra.com.br"
    };

    private static final int[] DDD = {
        11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 24, 27, 28, 31, 32, 33, 34, 35, 37, 38,
        41, 42, 43, 44, 45, 46, 47, 48, 49, 51, 53, 54, 55, 61, 62, 63, 64, 65, 66, 67, 68, 69,
        71, 73, 74, 75, 77, 79, 81, 82, 83, 84, 85, 86, 87, 88, 89, 91, 92, 93, 94, 95, 96, 97, 98, 99
    };

    private static final String ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final Random random;

    public Generator(Random random) {
        this.random = random;
    }

    public String generate(Type type, Options o) {
        return switch (type) {
            case EMAIL -> email(person(), o.domain);
            case CITY -> city(o);
            case NAME -> name(person(), o.pix);
            case CPF -> cpf(o.formatted);
            case CNPJ -> cnpj(o.formatted);
            case PHONE -> phone(o.formatted);
            case EVP -> evp();
            case TXID -> txid();
            case AMOUNT -> amount();
            case PROFILE -> profile(o);
            case PIX -> pix(o);
        };
    }

    public List<String> cities(int count, Options o) {
        List<Cities.City> pool = new ArrayList<>(pool(o));
        Collections.shuffle(pool, random);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(format(pool.get(i % pool.size()), o));
        }
        return result;
    }

    private record Person(String first, String last, String second) { }

    private Person person() {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        String second = pick(LAST_NAMES);
        while (second.equals(last)) {
            second = pick(LAST_NAMES);
        }
        return new Person(first, last, second);
    }

    private String name(Person p, boolean pix) {
        String full = p.first + " " + p.last + " " + p.second;
        if (!pix) {
            return full;
        }
        String ascii = Text.ascii(full).toUpperCase();
        if (ascii.length() <= 25) {
            return ascii;
        }
        return Text.ascii(p.first + " " + p.last).toUpperCase();
    }

    private String email(Person p, String domain) {
        String first = slug(p.first);
        String last = slug(p.last);
        String local = switch (random.nextInt(5)) {
            case 0 -> first + "." + last;
            case 1 -> first + last;
            case 2 -> first + "_" + last;
            case 3 -> first + "." + last + (10 + random.nextInt(90));
            default -> first.charAt(0) + "." + last + (1 + random.nextInt(999));
        };
        String host = domain != null && !domain.isBlank() ? domain.trim().toLowerCase() : pick(DOMAINS);
        return local + "@" + host;
    }

    private static String slug(String text) {
        return Text.ascii(text).toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private List<Cities.City> pool(Options o) {
        if (o.state != null && !Cities.isState(o.state)) {
            throw new IllegalArgumentException("Unknown state '" + o.state + "'. Use a two-letter code such as SP, RJ or MG");
        }
        List<Cities.City> pool = Cities.filter(o.state, o.pix);
        if (pool.isEmpty()) {
            throw new IllegalArgumentException("No city fits the Pix limit of 15 characters in state " + o.state.toUpperCase());
        }
        return pool;
    }

    private String format(Cities.City c, Options o) {
        return o.pix ? c.pixName() : c.name() + "/" + c.uf();
    }

    private String city(Options o) {
        List<Cities.City> pool = pool(o);
        return format(pool.get(random.nextInt(pool.size())), o);
    }

    private String cpf(boolean formatted) {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = random.nextInt(10);
            }
        } while (allSame(d, 9));
        d[9] = cpfDigit(d, 9);
        d[10] = cpfDigit(d, 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            if (formatted && (i == 3 || i == 6)) {
                sb.append('.');
            }
            if (formatted && i == 9) {
                sb.append('-');
            }
            sb.append(d[i]);
        }
        return sb.toString();
    }

    private static int cpfDigit(int[] d, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += d[i] * (length + 1 - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    private String cnpj(boolean formatted) {
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] d = new int[14];
        do {
            for (int i = 0; i < 8; i++) {
                d[i] = random.nextInt(10);
            }
        } while (allSame(d, 8));
        d[8] = 0;
        d[9] = 0;
        d[10] = 0;
        d[11] = 1;
        d[12] = cnpjDigit(d, w1);
        d[13] = cnpjDigit(d, w2);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            if (formatted && (i == 2 || i == 5)) {
                sb.append('.');
            }
            if (formatted && i == 8) {
                sb.append('/');
            }
            if (formatted && i == 12) {
                sb.append('-');
            }
            sb.append(d[i]);
        }
        return sb.toString();
    }

    private static int cnpjDigit(int[] d, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += d[i] * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    private static boolean allSame(int[] d, int length) {
        for (int i = 1; i < length; i++) {
            if (d[i] != d[0]) {
                return false;
            }
        }
        return true;
    }

    private String phone(boolean formatted) {
        int ddd = DDD[random.nextInt(DDD.length)];
        int first = random.nextInt(10000);
        int second = random.nextInt(10000);
        if (formatted) {
            return String.format(Locale.ROOT, "+55 (%d) 9%04d-%04d", ddd, first, second);
        }
        return String.format(Locale.ROOT, "+55%d9%04d%04d", ddd, first, second);
    }

    private String evp() {
        long most = random.nextLong();
        long least = random.nextLong();
        most = (most & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000004000L;
        least = (least & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(most, least).toString();
    }

    private String txid() {
        int length = 10 + random.nextInt(16);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ALNUM.charAt(random.nextInt(ALNUM.length())));
        }
        return sb.toString();
    }

    private String amount() {
        return String.format(Locale.ROOT, "%d.%02d", 1 + random.nextInt(2000), random.nextInt(100));
    }

    private String profile(Options o) {
        Person p = person();
        Cities.City city = pick(pool(o));
        return "name:  " + name(p, false) + "\n"
            + "email: " + email(p, o.domain) + "\n"
            + "cpf:   " + cpf(o.formatted) + "\n"
            + "phone: " + phone(o.formatted) + "\n"
            + "city:  " + format(city, o) + "\n"
            + "state: " + city.uf();
    }

    private String pix(Options o) {
        Person p = person();
        String key = switch (random.nextInt(5)) {
            case 0 -> cpf(false);
            case 1 -> cnpj(false);
            case 2 -> phone(false);
            case 3 -> email(p, o.domain);
            default -> evp();
        };
        Options pixOptions = new Options();
        pixOptions.state = o.state;
        pixOptions.pix = true;
        Cities.City city = pick(pool(pixOptions));
        return Pix.create(key, null, name(p, true), city.pixName(), amount(), "***", null);
    }

    private String pick(String[] values) {
        return values[random.nextInt(values.length)];
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }
}
