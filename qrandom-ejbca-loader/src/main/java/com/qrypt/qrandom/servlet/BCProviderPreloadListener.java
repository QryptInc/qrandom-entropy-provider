package com.qrypt.qrandom.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.security.Provider;
import java.security.Security;

import com.qrypt.qrandom.bcfips.QryptProvider;
import com.qrypt.qrandom.core.RandomStore;
import com.qrypt.qrandom.util.Properties;

@WebListener
public class BCProviderPreloadListener implements ServletContextListener {

    private static final String STRONG_ALGS = "securerandom.strongAlgorithms";
    private static final String QRYPT_BCPROV_ALG = "DEFAULT:BC";
    private static final String QRYPT_BCFIPS_ALG = "QryptSecureRandom:Qrypt";
    @Override
    public void contextInitialized(ServletContextEvent sce) {

        //FORGET about FIPS mode for now
//        boolean fips_mode = Properties.getProperty("qrypt.fips.mode", false, Boolean::parseBoolean);
//        if (fips_mode) {
//            Provider p = new QryptProvider();
//            Security.insertProviderAt(p, 1);
//
//            //Add BouncyCastleProvider to the list of strong algorithms
//            String strongAlgs = Security.getProperty(STRONG_ALGS);
//            if (strongAlgs == null)
//                strongAlgs = QRYPT_BCPROV_ALG;
//            else
//                strongAlgs = QRYPT_BCFIPS_ALG + "," + strongAlgs;
//
//            Security.setProperty("securerandom.strongAlgorithms", strongAlgs);
//        } else {
            System.setProperty("org.bouncycastle.drbg.entropysource",
                    "com.qrypt.qrandom.bcprov.ChainedEntropySourceProvider");
            Provider p = new org.bouncycastle.jce.provider.BouncyCastleProvider();
            Security.insertProviderAt(p, 1);

            //Add BouncyCastleProvider to the list of strong algorithms
            String strongAlgs = Security.getProperty(STRONG_ALGS);
            if (strongAlgs == null)
                strongAlgs = QRYPT_BCPROV_ALG;
            else
                strongAlgs = QRYPT_BCPROV_ALG + "," + strongAlgs;

            Security.setProperty("securerandom.strongAlgorithms", strongAlgs);
//        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        //destroy singleton instance
        RandomStore.Builder.destroy();
    }
}