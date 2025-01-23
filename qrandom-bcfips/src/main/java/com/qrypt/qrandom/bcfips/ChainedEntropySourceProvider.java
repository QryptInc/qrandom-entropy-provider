package com.qrypt.qrandom.bcfips;

import com.qrypt.qrandom.core.RandomStore;
import com.qrypt.qrandom.util.Properties;
import org.bouncycastle.crypto.EntropySource;
import org.bouncycastle.crypto.EntropySourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Function;


public class ChainedEntropySourceProvider implements EntropySourceProvider {

    static final Logger logger = LoggerFactory.getLogger(ChainedEntropySourceProvider.class.getName());

    private final EntropySourceProvider primary;
    private final EntropySourceProvider fallback;
    //private final int thresholdBytes;

    public ChainedEntropySourceProvider() {
        logger.info("Initializing ChainedEntropySourceProvider....");
        this.primary = new RandomStoreEntropySourceProvider();
        this.fallback = new SystemEntropySourceProvider();
    }

    @Override
    public EntropySource get(final int bitsRequired) {
        final int bytesRequired = (bitsRequired + 7) / 8;

        // We create a "chained" EntropySource that checks primary vs. fallback
        return new EntropySource() {
            @Override
            public boolean isPredictionResistant() {
                return true;
            }

            @Override
            public byte[] getEntropy() {
                // 1. Check if the primary has enough data
                byte[] primaryBytes = primary.get(bitsRequired).getEntropy();
                if (primaryBytes != null && primaryBytes.length >= bytesRequired) {
                    return primaryBytes;
                }
                // 2. If primary is insufficient, fallback
                return fallback.get(bitsRequired).getEntropy();
            }

            @Override
            public int entropySize() {
                // Typically just bitsRequired from the DRBG's perspective
                return bitsRequired;
            }
        };
    }


    static class RandomStoreEntropySourceProvider implements EntropySourceProvider {
        private final RandomStore store; // your custom buffer of random bytes

        public RandomStoreEntropySourceProvider() {
            String apiUrl = Properties.getProperty("qrypt.api.url", null, Function.identity());
            String apiToken = Properties.getProperty("qrypt.api.token",null,Function.identity());

            this.store = new RandomStore.Builder()
                    .apiToken(apiToken)
                    .apiUrl(apiUrl)
                    .build();
        }

        @Override
        public EntropySource get(int bitsRequired) {
            final int numBytes = (bitsRequired + 7) / 8;
            return new EntropySource() {
                @Override
                public boolean isPredictionResistant() {
                    return true;
                }

                @Override
                public byte[] getEntropy() {
                    byte[] res= store.getBytes(numBytes);
                    logger.info("got entropy from the RandomStoreSourceProvider: " + res.length + " bytes");
                    return res;
                }

                @Override
                public int entropySize() {
                    return bitsRequired;
                }
            };
        }
    }

    /**
     * Fallback entropy source provider
     */
    private static class SystemEntropySourceProvider implements EntropySourceProvider {
        //using threaded seed generator from bcprov
        private final SeedGenerator seedGenerator;

        SystemEntropySourceProvider() {
            this.seedGenerator = new SeedGenerator();
        }

        @Override
        public EntropySource get(int bitsRequired) {
            final int numBytes = (bitsRequired + 7) / 8;
            return new EntropySource() {

                @Override
                public boolean isPredictionResistant() {
                    return true; //is it?
                }

                @Override
                public byte[] getEntropy() {
                    byte[] res= null;
                    try {
                        res = seedGenerator.generateSeed(numBytes);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    logger.info("got entropy from the fallback SystemEntropySourceProvider:" + res.length + " bytes");
                    return res;
                }

                @Override
                public int entropySize() {
                    return bitsRequired;
                }
            };
        }
    }

    static class SeedGenerator {

        // Path to the urandom device on Unix-like systems
        private static final String URANDOM_PATH = "/dev/urandom";

        /**
         * Generate an array of seed bytes of length numBytes.
         *
         * If running on a Unix-like system, this method attempts to read directly
         * from /dev/urandom. Otherwise, it falls back to SecureRandom.
         *
         * @param numBytes number of random bytes needed
         * @return a byte array containing numBytes of random data
         * @throws IOException if an IO error occurs while reading from /dev/urandom
         */
        public byte[] generateSeed(int numBytes) throws IOException {
            // Decide if we're on Unix-like (Linux, macOS, etc.)
            String osName = System.getProperty("os.name").toLowerCase();
            byte[] seed = new byte[numBytes];

            // A simple OS check: if it contains "nix", "nux", "aix", or "mac", we try /dev/urandom
            if (osName.contains("nix") || osName.contains("nux")
                    || osName.contains("aix") || osName.contains("mac")) {

                try (FileInputStream fis = new FileInputStream(URANDOM_PATH)) {
                    int bytesRead = 0;
                    while (bytesRead < numBytes) {
                        int read = fis.read(seed, bytesRead, numBytes - bytesRead);
                        if (read == -1) {
                            throw new IOException("Unexpected end of stream while reading /dev/urandom.");
                        }
                        bytesRead += read;
                    }
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(URANDOM_PATH + "is not found", e);
                }

            } else {
                // Fallback for non-Unix-like systems
                //SecureRandom sr = new SecureRandom();
                //sr.nextBytes(seed);
                throw new UnsupportedOperationException("this os is not supported yet...");
            }

            return seed;
        }
    }

}
