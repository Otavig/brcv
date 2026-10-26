package br.com.brcodevalidator.utils;

import java.nio.charset.StandardCharsets;

public final class Crc16False {
    private static final int POLYNOMIAL = 0x1021;
    private static final int INITIAL_VALUE = 0xFFFF;

    private Crc16False() {
        throw new IllegalStateException(
                "The Crc16 class cannot/should not be instantiated"
        );
    }

    public static String encode(String payload) {
        return encode(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static String encode(byte[] data) {
        int crc = INITIAL_VALUE;

        for (byte value : data) {
            crc ^= (value & 0xFF) << 8;

            for (int bit = 0; bit < 8; bit++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ POLYNOMIAL;
                } else {
                    crc <<= 1;
                }

                crc &= 0xFFFF;
            }
        }

        return String.format("%04X", crc);
    }

}
