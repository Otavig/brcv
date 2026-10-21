package br.com.brcodevalidator.strategy;

import br.com.brcodevalidator.interfaces.AlgorithmInterface;

public class BrCodeStatic implements AlgorithmInterface {

    private static volatile BrCodeStatic instance;

    private BrCodeStatic() {}

    @Override
    public boolean validate(String payload) {
        return false;
    }

    public static BrCodeStatic getInstance() {
        if (instance == null) {
            synchronized (BrCodeStatic.class) {
                if (instance == null) {
                    instance = new BrCodeStatic();
                }
            }
        }
        return instance;
    }
}
