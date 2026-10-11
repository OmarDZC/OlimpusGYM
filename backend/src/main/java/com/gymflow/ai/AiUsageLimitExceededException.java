package com.gymflow.ai;

/** Límite de uso de IA alcanzado (por socio y día, o el tope global): 429 (Trozo 8a-2). */
public class AiUsageLimitExceededException extends RuntimeException {

    public AiUsageLimitExceededException(String message) {
        super(message);
    }
}
