# qrandom-entropy-provider

This project is aimed to add REST-based entropy source to existing SecureRandom providers.

One specific focus here is on bouncycastle providers. With bcprov (non-FIPs-compliant library) 
we are adding a custom ChainedEntropySourceProvider that can be configured with the standard 
org.bouncycastle.jce.provider.BouncyCastleProvider through the "org.bouncycastle.drbg.entropysource" 
system property. 

The ChainedEntropySourceProvider is using qrypt EaaS as the primary entropy source (backed by in-memory random store) and 
 ThreadedEntropySourceProvider (one of bouncycastle implementations) 
as the fallback mechanism: here avoiding /dev/urandom as being OS-specific entropy source. 