package br.com.brcodevalidator;

import br.com.brcodevalidator.interfaces.AlgorithmInterface;
import br.com.brcodevalidator.strategy.BrCodeDynamic;
import br.com.brcodevalidator.strategy.BrCodeStatic;
import br.com.brcodevalidator.exceptions.InvalidPayloadException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ArgGroup;

@Command(
    name = "brcv",
    version = "BRCV 1.0.0",
    description = "BR Code validator",
    mixinStandardHelpOptions = true
)
public class Main implements Runnable {
    private AlgorithmInterface algorithm;

    @ArgGroup(exclusive = true, multiplicity = "1")
    private AlgorithmOptions algorithmOption;

    @Option(names = {"-rd", "--response-detailed"}, description = "Response Detailed")
    private boolean responseDetailed;

    static class AlgorithmOptions {
           @Option(names = {"-d", "--dynamic"}, description = "Use dynamic validation algorithm")
           boolean dynamicStrategy;

           @Option(names = {"-s", "--static"}, description = "Use static validation algorithm")
           boolean staticStrategy;
    }

    @Option(names = {"-p", "--payload"}, required = true, description = "Payload to be used")
    String payload;

    @Override
    public void run() {
        // System.out.println("BRCV running!");

        if(payload.isBlank()) {
            throw new InvalidPayloadException("Missing payload content (-p)");
        }

        if (algorithmOption.dynamicStrategy) {
            this.algorithm = BrCodeDynamic.getInstance();
        } else {
            this.algorithm = BrCodeStatic.getInstance();
        }

        boolean isValidPayload = this.algorithm.validate(this.payload);

        if(responseDetailed) {
            System.out.println(String.format(
                "The result of algorithm %s was that the payload is %s",
                algorithmOption.dynamicStrategy ? "Dynamic" : "Static",
                isValidPayload ? "Valid" : "Invalid"
            ));
        } else {
            System.out.println(isValidPayload);
        }
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main());
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            if (ex instanceof InvalidPayloadException) {
                commandLine.getErr().println(commandLine.getColorScheme().errorText("Validate error: " + ex.getMessage()));
                return 1;
            }

            ex.printStackTrace();
            return 2;
        });

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
