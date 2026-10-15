package pl.piomin.services.guga.common;

public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
