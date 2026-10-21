package br.com.brvalidator.exceptions;

class MissingFlagsException extends RuntimeException  {
    public MissingFlagsException(String msg) {
        super(msg);
    }
}
