package com.qrypt.qrandom.core;

public interface RandomStore {

    /**
     * request numBytes from the random store
     * @param numBytes
     * @return a byte array of entropy that is <= numBytes requests
     */
    byte[] getBytes(int numBytes);

    void destroy();

    class StorePopulationException extends RuntimeException {
        public StorePopulationException(String message) {
            super(message);
        }
        public StorePopulationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
