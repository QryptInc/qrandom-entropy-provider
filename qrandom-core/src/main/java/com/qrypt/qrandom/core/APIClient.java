package com.qrypt.qrandom.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public interface APIClient {
    byte[] getRandom(int totalBytes);

    class DefaultImpl implements APIClient {
        private static final Logger logger = LoggerFactory.getLogger(DefaultImpl.class.getName());
        private static final int MAX_REQUEST_BLOCK_SIZE = 1024;
        private static final int MAX_REQUEST_BLOCK_COUNT = 512;
        private static final int MAX_RETRY_COUNT = 3; // Retry limit

        private final String apiUrl;
        private final String token;
        private HttpClient client;

        public DefaultImpl(final String apiUrl, final String token) {
            this.apiUrl = apiUrl;
            this.token = token;
        }

        private HttpClient getHttpClient() {
            if (client == null) {
                client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
            }
            return client;
        }

        @Override
        public byte[] getRandom(int totalBytes) {
            if (totalBytes <= 0) {
                throw new IllegalArgumentException("Total bytes must be greater than 0");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // To collect and aggregate
            int bytesRemaining = totalBytes;

            while (bytesRemaining > 0) {
                int blockSize = Math.min(MAX_REQUEST_BLOCK_SIZE, bytesRemaining);
                int blockCount = Math.min(MAX_REQUEST_BLOCK_COUNT, (int) Math.ceil((double) bytesRemaining / blockSize));

                byte[] data = fetchBytesWithRetry(blockSize, blockCount);
                outputStream.writeBytes(data); // Collect the output

                bytesRemaining -= data.length;
            }

            logger.info("Generated total of {} bytes", outputStream.size());
            return outputStream.toByteArray();
        }

        private byte[] fetchBytesWithRetry(int blockSize, int blockCount) {
            int retryCount = 0;
            while (retryCount < MAX_RETRY_COUNT) {
                try {
                    logger.info("Fetching random bytes: blockSize={}, blockCount={}, retry={}", blockSize, blockCount, retryCount);
                    return callApi(blockSize, blockCount);
                } catch (RestAPIClientException e) {
                    logger.error("API call failed (attempt {}): {}", retryCount + 1, e.getMessage());
                    retryCount++;
                    resetHttpClient(); // Reset and retry
                }
            }
            throw new RestAPIClientException("Failed to fetch random bytes after " + MAX_RETRY_COUNT + " attempts");
        }

        private void resetHttpClient() {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }

        private byte[] callApi(int blockSize, int blockCount) throws RestAPIClientException {
            if (blockSize <= 0 || blockCount <= 0) {
                return new byte[0];
            }

            byte[] returnValue = new byte[blockSize * blockCount];
            try {
                String tokenHeader = "Bearer " + this.token;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.apiUrl))
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(
                                String.format("{\"block_size\":%d,\"block_count\":%d}", blockSize, blockCount)))
                        .header("Accept", "application/json")
                        .header("Authorization", tokenHeader)
                        .build();

                HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new RestAPIClientException("Unexpected status code: " + response.statusCode());
                }

                byte[] data = extractBytesFromResponse(response.body(), blockCount);
                logger.info("Successfully fetched {} bytes from API", data.length);

                return data;

            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RestAPIClientException("Error during API call", e);
            }
        }

        private byte[] extractBytesFromResponse(String responseBody, int expectedBlockCount) {
            Gson gson = new Gson();
            JsonObject responseObject = gson.fromJson(responseBody, JsonObject.class);
            JsonArray jsonArray = responseObject.getAsJsonArray("entropy");

            if (jsonArray.size() != expectedBlockCount) {
                throw new RestAPIClientException("Mismatch in expected block count: " + expectedBlockCount);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (int i = 0; i < jsonArray.size(); i++) {
                String base64Entry = jsonArray.get(i).getAsString();
                byte[] decodedBytes = Base64.getDecoder().decode(base64Entry);
                outputStream.writeBytes(decodedBytes);
            }

            return outputStream.toByteArray();
        }
    }

    class RestAPIClientException extends RuntimeException {
        public RestAPIClientException(String message) {
            super(message);
        }

        public RestAPIClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}