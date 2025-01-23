package com.qrypt.qrandom.bcfips;

import java.security.SecureRandom;
import java.security.SecureRandomSpi;
import org.bouncycastle.crypto.fips.FipsDRBG;
import org.bouncycastle.util.Strings;

public class QryptSecureRandomSpi extends SecureRandomSpi {


    private static final SecureRandom random = createBaseRandom();

    public QryptSecureRandomSpi() {}

    protected void engineSetSeed(byte[] bytes)
    {
        random.setSeed(bytes);
    }

    protected void engineNextBytes(byte[] bytes)
    {
        random.nextBytes(bytes);
    }

    protected byte[] engineGenerateSeed(int numBytes)
    {
        return random.generateSeed(numBytes);
    }


    private static SecureRandom createBaseRandom() {
        return FipsDRBG.SHA512_HMAC.fromEntropySource(new ChainedEntropySourceProvider())
                .setSecurityStrength(256)
                .setEntropyBitsRequired(256)
                .build(Strings.toByteArray("number only used once"), true);
    }

}
