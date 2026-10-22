package br.com.brcodevalidator;

import br.com.brcodevalidator.utils.Pix;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "brcv",
    version = "BRCV 1.0.0",
    description = "BR Code / Pix tools",
    mixinStandardHelpOptions = true
)
public class Main {

    @Command(name = "validate", description = "Validate a Pix payload (static or dynamic is detected automatically)")
    void validate(
        @Option(names = {"-p", "--payload"}, required = true, description = "Pix payload") String payload,
        @Option(names = {"-r", "--response-detailed"}, description = "Response detailed") boolean detailed
    ) {
        boolean valid = Pix.isValid(payload);

        if (detailed) {
            String type = Pix.isDynamic(payload) ? "Dynamic" : "Static";
            System.out.println("The payload is " + type + " and " + (valid ? "Valid" : "Invalid"));
        } else {
            System.out.println(valid);
        }
    }

    @Command(name = "decode", description = "Show the whole EMV structure as JSON")
    void decode(@Option(names = {"-p", "--payload"}, required = true, description = "Pix payload") String payload) {
        System.out.println(Pix.decode(payload));
    }

    @Command(name = "extract", description = "Show the main Pix data (key, url, name, amount...) as JSON")
    void extract(@Option(names = {"-p", "--payload"}, required = true, description = "Pix payload") String payload) {
        System.out.println(Pix.extract(payload));
    }

    @Command(name = "create", description = "Create a Pix payload (--key = static, --url = dynamic)")
    void create(
        @Option(names = "--key", description = "Pix key (static)") String key,
        @Option(names = "--url", description = "Payload URL (dynamic)") String url,
        @Option(names = "--name", required = true, description = "Merchant name (max 25)") String name,
        @Option(names = "--city", required = true, description = "Merchant city (max 15)") String city,
        @Option(names = "--amount", description = "Amount, e.g. 10.50") String amount,
        @Option(names = "--txid", defaultValue = "***", description = "Transaction id (default: ***)") String txid,
        @Option(names = "--description", description = "Description (static only)") String description
    ) {
        System.out.println(Pix.create(key, url, name, city, amount, txid, description));
    }

    @Command(name = "fix-crc", description = "Recalculate the CRC and print the fixed payload")
    void fixCrc(@Option(names = {"-p", "--payload"}, required = true, description = "Pix payload") String payload) {
        System.out.println(Pix.fixCrc(payload));
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main());

        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            System.err.println("Error: " + ex.getMessage());
            return 1;
        });

        System.exit(cmd.execute(args));
    }
}
