package br.com.brvalidator.exceptions;

class InvalidPayloadException extends RuntimeException  {
    public InvalidPayloadException(String msg) {
        super(msg);
    }
}
