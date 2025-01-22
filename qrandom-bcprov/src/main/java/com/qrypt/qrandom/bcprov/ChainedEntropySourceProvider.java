package com.qrypt.qrandom.bcprov;

import com.qrypt.qrandom.core.RandomStore;
import com.qrypt.qrandom.util.Properties;
import org.bouncycastle.crypto.prng.EntropySource;
import org.bouncycastle.crypto.prng.EntropySourceProvider;
import org.bouncycastle.crypto.prng.ThreadedSeedGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;


public class ChainedEntropySourceProvider implements EntropySourceProvider {

    static final Logger logger = LoggerFactory.getLogger(ChainedEntropySourceProvider.class.getName());

    private final EntropySourceProvider primary;
    private final EntropySourceProvider fallback;
    //private final int thresholdBytes;

    public ChainedEntropySourceProvider() {
        logger.info("Initializing ChainedEntropySourceProvider....");
        this.primary = new RandomStoreEntropySourceProvider();
        this.fallback = new ThreadedEntropySourceProvider();
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
    private static class ThreadedEntropySourceProvider implements EntropySourceProvider {
        //using threaded seed generator from bcprov
        private final ThreadedSeedGenerator seedGenerator;

        ThreadedEntropySourceProvider() {
            this.seedGenerator = new ThreadedSeedGenerator();
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
                    byte[] res= seedGenerator.generateSeed(numBytes, true);
                    logger.info("got entropy from the fallback ThreadedEntropySourceProvider:" + res.length + " bytes");
                    return res;
                }

                @Override
                public int entropySize() {
                    return bitsRequired;
                }
            };
        }
    }

}
