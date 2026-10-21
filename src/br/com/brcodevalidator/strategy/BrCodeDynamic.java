package br.com.brcodevalidator.strategy;

import br.com.brcodevalidator.interfaces.AlgorithmInterface;

public class BrCodeDynamic implements AlgorithmInterface {

    private static volatile BrCodeDynamic instance;

    private BrCodeDynamic() {}

    @Override
    public boolean validate(String payload) {
        return false;
    }

    public static BrCodeDynamic getInstance() {
        if (instance == null) {
            synchronized (BrCodeDynamic.class) {
                if (instance == null) {
                    instance = new BrCodeDynamic();
                }
            }
        }
        return instance;
    }
}
