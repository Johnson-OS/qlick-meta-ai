package dev.qlick.meta.core;

public final class MetaApiException extends RuntimeException {
    private final int status;
    private final String errorType;

    public MetaApiException(int status, String errorType, String message) {
        super(message);
        this.status = status;
        this.errorType = errorType;
    }

    public int status() { return status; }
    public String errorType() { return errorType; }
    public boolean retryable() { return status == 429 || status >= 500; }
}
