package br.com.brcodevalidator;

import br.com.brcodevalidator.utils.Generator;
import br.com.brcodevalidator.utils.Pix;
import br.com.brcodevalidator.utils.PixValidator;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Random;

@Command(
    name = "brcv",
    version = "BRCV 1.1.0",
    description = "BR Code / Pix tools",
    mixinStandardHelpOptions = true
)
public class Main {

    @Command(name = "validate", description = "Validate a Pix payload (static or dynamic is detected automatically)", mixinStandardHelpOptions = true)
    void validate(
        @Option(names = {"-p", "--payload"}, required = true, description = "Pix payload") String payload,
        @Option(names = {"-r", "--response-detailed"}, description = "Response detailed (type, errors and warnings)") boolean detailed
    ) {
        PixValidator.Result result = PixValidator.validate(payload);

        if (!detailed) {
            System.out.println(result.valid());
            return;
        }

        String type = switch (result.type) {
            case STATIC -> "Static";
            case DYNAMIC -> "Dynamic";
            case UNKNOWN -> "of unknown type";
        };
        String key = result.keyType == null ? "" : " (" + result.keyType.name() + " key)";
        System.out.println("The payload is " + type + key + " and " + (result.valid() ? "Valid" : "Invalid"));
        result.errors.forEach(e -> System.out.println("  error:   " + e));
        result.warnings.forEach(w -> System.out.println("  warning: " + w));
    }

    @Command(name = "decode", description = "Show the whole EMV structure as JSON", mixinStandardHelpOptions = true)
    void decode(@Option(names = {"-p", "--payload"}, required = true, description = "Pix payload") String payload) {
        System.out.println(Pix.decode(payload));
    }

    @Command(name = "extract", description = "Show the main Pix data (key, url, name, amount...) as JSON", mixinStandardHelpOptions = true)
    void extract(@Option(names = {"-p", "--payload"}, required = true, description = "Pix payload") String payload) {
        System.out.println(Pix.extract(payload));
    }

    @Command(name = "create", description = "Create a Pix payload (--key = static, --url = dynamic)", mixinStandardHelpOptions = true)
    void create(
        @Option(names = "--key", description = "Pix key (static): CPF, CNPJ, +55 phone, e-mail or random key") String key,
        @Option(names = "--url", description = "Payload URL (dynamic)") String url,
        @Option(names = "--name", required = true, description = "Merchant name (max 25)") String name,
        @Option(names = "--city", required = true, description = "Merchant city (max 15)") String city,
        @Option(names = "--amount", description = "Amount, e.g. 10.50") String amount,
        @Option(names = "--txid", defaultValue = "***", description = "Transaction id (default: ***)") String txid,
        @Option(names = "--description", description = "Description (static only)") String description
    ) {
        System.out.println(Pix.create(key, url, name, city, amount, txid, description));
    }

    @Command(name = "fix-crc", description = "Recalculate the CRC and print the fixed payload", mixinStandardHelpOptions = true)
    void fixCrc(@Option(names = {"-p", "--payload"}, required = true, description = "Pix payload") String payload) {
        System.out.println(Pix.fixCrc(payload));
    }

    @Command(
        name = "generate",
        mixinStandardHelpOptions = true,
        description = {
            "Generate valid random data for tests.",
            "Types: email, city, name, cpf, cnpj, phone, evp, txid, amount, profile, pix"
        }
    )
    void generate(
        @Parameters(index = "0", paramLabel = "<type>", description = "What to generate: ${COMPLETION-CANDIDATES}") Generator.Type type,
        @Option(names = {"-n", "--count"}, paramLabel = "<n>", defaultValue = "1", description = "How many (default: 1)") int count,
        @Option(names = "--state", paramLabel = "<UF>", description = "Only cities of this state, e.g. SP (city, profile and pix)") String state,
        @Option(names = "--domain", paramLabel = "<domain>", description = "E-mail domain, e.g. example.com (default: a real provider)") String domain,
        @Option(names = "--formatted", description = "Use mask: 123.456.789-09, 11.222.333/0001-81, +55 (11) 91234-5678") boolean formatted,
        @Option(names = "--pix", description = "Pix format: no accents, uppercase, city up to 15 and name up to 25 characters") boolean pix,
        @Option(names = "--seed", paramLabel = "<number>", description = "Seed, to repeat the same output") Long seed
    ) {
        if (count < 1 || count > 10000) {
            throw new IllegalArgumentException("--count must be between 1 and 10000");
        }

        Generator generator = new Generator(seed == null ? new Random() : new Random(seed));
        Generator.Options options = new Generator.Options();
        options.state = state;
        options.domain = domain;
        options.formatted = formatted;
        options.pix = pix;

        if (type == Generator.Type.CITY) {
            generator.cities(count, options).forEach(System.out::println);
            return;
        }

        for (int i = 0; i < count; i++) {
            if (i > 0 && type == Generator.Type.PROFILE) {
                System.out.println();
            }
            System.out.println(generator.generate(type, options));
        }
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main());
        cmd.setCaseInsensitiveEnumValuesAllowed(true);

        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            System.err.println("Error: " + ex.getMessage());
            return 1;
        });

        System.exit(cmd.execute(args));
    }
}
