package com.qrypt.qrandom.bcfips;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

public class TestQryptProvider {
    static final Logger logger = LoggerFactory.getLogger(TestQryptProvider.class.getName());


    public static void main(String[] args) throws Exception {
        //need to add and verify that args[0] is not empty and set it to System property
        if (args.length == 0) {
            throw new IllegalArgumentException("Specify api token in the first argument");
        }
        System.setProperty("qrypt.api.token", args[0]);
        System.setProperty("qrypt.api.url", "https://api-eus.qrypt.com/api/v1/entropy");


        Security.insertProviderAt(new QryptProvider(), 1);

        // 3. Obtain a SecureRandom instance from "DEFAULT" algorithm in "BC" provider
        SecureRandom sr = SecureRandom.getInstance("QryptSecureRandom", "Qrypt");

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