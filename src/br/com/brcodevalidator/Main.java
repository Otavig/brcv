package br.com.brcodevalidator;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "brcv",
    version = "BRCodeValidator 1.0.0",
    description = "Validador de códigos BR Code",
    mixinStandardHelpOptions = true
)
public class Main implements Runnable {

    @Override
    public void run() {
        System.out.println("Validador BR Code iniciado!");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
