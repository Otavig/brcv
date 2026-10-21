package br.com.brcodevalidator.exceptions;

public class InvalidPayloadException extends RuntimeException  {
    public InvalidPayloadException(String msg) {
        super(msg);
    }
}
