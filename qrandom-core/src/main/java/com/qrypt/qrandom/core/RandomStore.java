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


    /**
     * Internal Builder for constructing RandomStore implementations.
     */
    class Builder {

        private static RandomStore INSTANCE = null;
        private String apiUrl;
        private String apiToken;
        private int storeSize = 1000; // Default
        private int minThreshold = 200; // Default
        private APIClient apiClient;

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder apiToken(String apiToken) {
            this.apiToken = apiToken;
            return this;
        }

        public Builder storeSize(int storeSize) {
            this.storeSize = storeSize;
            return this;
        }

        public Builder minThreshold(int minThreshold) {
            this.minThreshold = minThreshold;
            return this;
        }

        public Builder apiClient(APIClient apiClient) {
            this.apiClient = apiClient;
            return this;
        }

        public static synchronized void destroy() {
            if (INSTANCE != null) {
                INSTANCE.destroy();
                INSTANCE = null;
            }
        }

        public synchronized RandomStore build() {
            if (INSTANCE != null) {
                return INSTANCE;
            } else {
                if (apiUrl == null || apiToken == null) {
                    throw new IllegalArgumentException("API URL and Token must be specified.");
                }
                if (apiClient == null)
                    apiClient = new APIClient.DefaultImpl(apiUrl, apiToken);

                return new QryptSingleQueueRandomStore(apiClient, storeSize, minThreshold);
            }
        }

    }
}
