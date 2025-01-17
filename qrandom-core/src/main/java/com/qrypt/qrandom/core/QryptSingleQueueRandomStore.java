package com.qrypt.qrandom.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;


public class QryptSingleQueueRandomStore implements RandomStore {
    private static final Logger logger = LoggerFactory.getLogger(QryptSingleQueueRandomStore.class.getName());

    private static final String DEFAULT_API_URL = "https://api-eus.qrypt.com/api/v1/entropy";
    private static final String DEFAULT_TOKEN = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImY0NjEzODdkOTg2ZjQ2OTliOTQzOGI5MTA1MTYwYTliIn0.eyJleHAiOjE3NTUxOTM5NjUsIm5iZiI6MTcyMzY1Nzk2NSwiaXNzIjoiQVVUSCIsImlhdCI6MTcyMzY1Nzk2NSwiZ3JwcyI6WyJQVUIiXSwiYXVkIjpbIlJQUyJdLCJybHMiOlsiUk5EVVNSIl0sImNpZCI6IjBqX2N0cFF3UW9YT0NkLVhaeEZvRiIsImR2YyI6IjNlM2NlZTBlYjVlMDRjNmZiNjM0OWViZDIxNjFmNGE1IiwianRpIjoiMzM5ZWMzNmVkMTlmNGE2YWI3ZWZkMTFiNGI1YzcxMWMiLCJ0eXAiOjN9.Tr_0vh4u0GpnRUFYjsy0Adg_VckMrhssrzfCrS9wmjNZ6PSk8B0xhinO4TCIKVW3xYn7ztssthmWYCj-pA3_NA";
    private static final int DEFAULT_STORE_SIZE = 1000;
    private static final int DEFAULT_MIN_THRESHOLD = 200;

    private final ConcurrentLinkedQueue<Byte> randomQueue = new ConcurrentLinkedQueue<>();
    private final APIClient apiClient;
    private final int storeSize;
    private final int minThreshold;

    /*
     * execute population scheduled
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    //the store has to absolutely be SINGLETON
    private static QryptSingleQueueRandomStore instance;

    private static <T> T getSystemProperty(String property, T defaultValue, Function<String,T> converter) {
        String value = System.getProperty(property);
        if (value == null) {
            return defaultValue;
        }

        try {
            return converter.apply(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * return singleton instance of this class
     * @return singleton instance of this store
     */
    public static QryptSingleQueueRandomStore getInstance() {
        if (instance == null) {
            //thread safety
            synchronized (QryptSingleQueueRandomStore.class) {
                if (instance == null) {
                    String apiUrl = getSystemProperty("qrypt.api.url", DEFAULT_API_URL, Function.identity());
                    String token = getSystemProperty("qrypt.api.token",DEFAULT_TOKEN,Function.identity());
                    int storeSize = getSystemProperty("qrypt.store.size",DEFAULT_STORE_SIZE, Integer::valueOf);
                    int storeMinThreshold = getSystemProperty("qrypt.store.min_threshold",DEFAULT_MIN_THRESHOLD,Integer::valueOf);

                    APIClient apiClient = new APIClient.DefaultImpl(apiUrl, token);
                    instance = new QryptSingleQueueRandomStore(apiClient, storeSize, storeMinThreshold);
                }
            }
        }
        return instance;
    }

    /**
     * non-public accessor for testing purposes
     * @param client
     * @param storeSize
     * @param storeMinThreshold
     * @return
     */
    static QryptSingleQueueRandomStore getInstance(APIClient client, int storeSize, int storeMinThreshold) {
        return new QryptSingleQueueRandomStore(client, storeSize, storeMinThreshold);
    }

    private QryptSingleQueueRandomStore(APIClient apiClient,
                                        int storeSize,
                                        int minThreshold) {
        logger.info("Initializing and scheduling random Store....");
        this.apiClient = apiClient;
        this.storeSize=storeSize;
        this.minThreshold=minThreshold;

        startScheduledPopulation();
    }


    private void startScheduledPopulation() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkOrPopulateStoreScheduled();
            } catch (Exception e) {
                logger.error("Scheduled task encountered an exception. It will continue to run on the next iteration.", e);
            }
        }, 1, 1, TimeUnit.MINUTES); //Initial delay: 1 min, Period: 1 minute
    }


    @Override
    public byte[] getBytes(int numBytes) {
        //so far the best dynamic structure to later convert to appropriate size array,
        //note that the result array could be <= numBytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < numBytes; i++) {
            if (!pollAndWriteNextByte(baos)) {
                break;
            }
        }
        return baos.toByteArray();
    }

    @Override
    public void destroy() {
        logger.info("RandomStore scheduler shutdown...");
        shutdown();
    }


    private boolean pollAndWriteNextByte(ByteArrayOutputStream baos) {
        Byte nextByte = randomQueue.poll();
        if (nextByte == null) {
            return false;
        }
        baos.write(nextByte);
        return true;
    }

    private void checkOrPopulateStoreScheduled() {
        if (randomQueue.size() <= this.minThreshold) {
            //no locking here since it's only accessed from single-threaded scheduler
            logger.info("checkOrPopulateStoreScheduled: min criteria met (queueSize <= " + randomQueue.size() +
                    "), starting population process...");

            // Call the populate method directly as we are already in a custom scheduler thread
            try {
                for (byte b : apiClient.getRandom(storeSize)) {
                    randomQueue.offer(b);
                }
                logger.info("checkOrPopulateStoreScheduled: population process completed.");
            } catch (Exception e) {
                logger.error("checkOrPopulateStoreScheduled: population process failed", e);
                //TODO: add monitoring signal
            }

        }
    }

    private void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
