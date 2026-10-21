package br.com.brcodevalidator.exceptions;

public class MissingFlagsException extends RuntimeException  {
    public MissingFlagsException(String msg) {
        super(msg);
    }
}
