package com.logsense.rca.ai;

public class AiProviderException extends RuntimeException {

    private final String providerName;

    public AiProviderException(String providerName, String message) {
        super("[" + providerName + "] " + message);
        this.providerName = providerName;
    }

    public AiProviderException(String providerName, String message, Throwable cause) {
        super("[" + providerName + "] " + message, cause);
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
