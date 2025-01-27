package com.qrypt.qrandom.bcfips;

import java.security.Provider;
import java.security.AccessController;

import java.security.PrivilegedAction;

/**
 * Qrypt Provider
 * The code adopted from BouncyCastleProvider
 */
public class QryptProvider extends Provider {

    public QryptProvider() {
        super("Qrypt", 1.0, "Qrypt provider");
        putService(new Service(this, "SecureRandom", "QryptSecureRandom", QryptSecureRandomSpi.class.getName(), null, null));

    }

}
