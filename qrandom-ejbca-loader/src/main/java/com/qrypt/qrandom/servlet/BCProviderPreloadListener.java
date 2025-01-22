package com.qrypt.qrandom.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.security.Provider;
import java.security.Security;
import java.util.Random;

import com.qrypt.qrandom.core.QryptSingleQueueRandomStore;
import com.qrypt.qrandom.core.RandomStore;

@WebListener
public class BCProviderPreloadListener implements ServletContextListener {

    private static final String STRONG_ALGS = "securerandom.strongAlgorithms";
    private static final String QRYPT_ALG = "DEFAULT:BC";
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // Force loading of your jce provider class
            Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load provider class", e);
        }

        try {
            // Force loading of your entropysource provider class
            Class.forName("com.qrypt.qrandom.bcprov.ChainedEntropySourceProvider");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load entropy provider class", e);
        }

        System.setProperty("org.bouncycastle.drbg.entropysource",
                "com.qrypt.qrandom.bcprov.ChainedEntropySourceProvider");
        Provider p = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        Security.insertProviderAt(p, 1);

        //Add BouncyCastleProvider to the list of strong algorithms
        String strongAlgs=Security.getProperty(STRONG_ALGS);
        if (strongAlgs==null)
            strongAlgs=QRYPT_ALG;
        else
            strongAlgs=QRYPT_ALG+","+strongAlgs;

        Security.setProperty("securerandom.strongAlgorithms", strongAlgs);

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        //destroy singleton instance
        RandomStore.Builder.destroy();
    }
}