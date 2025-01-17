package com.qrypt.qrandom.bcprov;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

public class TestChainedEntropy {
    static final Logger logger = LoggerFactory.getLogger(TestChainedEntropy.class.getName());


    public static void main(String[] args) throws Exception {
        // 1. Set the BC DRBG Entropy Source property
        //    The value must be the fully-qualified class name of your custom provider
        System.setProperty("org.bouncycastle.drbg.entropysource",
                "com.qrypt.qrandom.bcprov.ChainedEntropySourceProvider");


        // 2. Add the Bouncy Castle Provider (bcprov)
        //    Make sure bcprov is on your classpath
        //Security.addProvider(new BouncyCastleProvider());
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        // 3. Obtain a SecureRandom instance from "DEFAULT" algorithm in "BC" provider
        SecureRandom sr = SecureRandom.getInstance("DEFAULT", "BC");

        while (true) {// 4. Generate some random bytes
            byte[] randomBytes = new byte[32];
            sr.nextBytes(randomBytes);

            logger.info("Got random bytes: " + Arrays.toString(randomBytes));
            //logger.info("Reseeding...");
            //sr.reseed();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }

        //QryptSingleQueueRandomStore.getInstance().destroy();
    }
}