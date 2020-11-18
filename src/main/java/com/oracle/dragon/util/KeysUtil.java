package com.oracle.dragon.util;

import com.oracle.dragon.model.Keys;
import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.option.HostedOptionKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

@AutomaticFeature
public final class KeysUtil implements Feature {
    @Override
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        return true;
    }

    @Override
    public void duringSetup(DuringSetupAccess access) {
        RuntimeClassInitializationSupport rci = ImageSingletons.lookup(RuntimeClassInitializationSupport.class);
        /*
         * The SecureRandom implementations open the /dev/random and /dev/urandom files which are
         * used as sources for entropy. These files are opened in the static initializers. That's
         * why we rerun the static initializers at runtime. We cannot completely delay the static
         * initializers execution to runtime because the SecureRandom classes are needed by the
         * native image generator too, e.g., by Files.createTempDirectory().
         */
        rci.rerunInitialization(org.bouncycastle.jcajce.provider.drbg.DRBG.Default.class, "for substitutions");
        rci.rerunInitialization(org.bouncycastle.jcajce.provider.drbg.DRBG.NonceAndIV.class, "for substitutions");

    }

/*    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        RuntimeClassInitialization.initializeAtBuildTime("org.bouncycastle");
        // see https://www.bouncycastle.org/fips-java/BCFipsIn100.pdf
        Security.addProvider(new BouncyCastleProvider());
    }
*/
    public Keys createKeys(final String passPhrase) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        final KeyPair kp = kpg.generateKeyPair();
        final Key publicKey = kp.getPublic();
        final PrivateKey privateKey = kp.getPrivate();

        final Base64.Encoder encoder = Base64.getEncoder();

        final String keyName = "dragon_ssh_key";
        final String publicKeyContent = "-----BEGIN RSA PUBLIC KEY-----\n" +
                chunk(encoder.encodeToString(publicKey.getEncoded()), 64) +
                "\n-----END RSA PUBLIC KEY-----\n";

        // check for filenames in case these already exist!
        File publicKeyFile = new File(keyName + ".pub");
        File privateKeyFile = new File(keyName);

        int fileNumber = 1;
        while (publicKeyFile.exists() || privateKeyFile.exists()) {
            publicKeyFile = new File(String.format("%s.%d.pub", keyName, fileNumber));
            privateKeyFile = new File(String.format("%s.%d", keyName, fileNumber));
            fileNumber++;
        }


        try (Writer out = new FileWriter(publicKeyFile)) {
            out.write(publicKeyContent);
        }

        MessageDigest digest = MessageDigest.getInstance("MD5");
        final byte[] fingerprintBytes = digest.digest(publicKey.getEncoded());
        final StringBuilder fingerprint = new StringBuilder();
        for (int i = 0; i < fingerprintBytes.length; i++) {
            if (i != 0) fingerprint.append(':');
            int b = fingerprintBytes[i] & 0xff;
            final String hex = Integer.toHexString(b);
            if (hex.length() == 1) fingerprint.append('0');
            fingerprint.append(hex);
        }


/*        final PKCS8EncryptedPrivateKeyInfoBuilder pkcs8Builder = new JcaPKCS8EncryptedPrivateKeyInfoBuilder(privateKey);
        final StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(pkcs8Builder.build(new JcePKCSPBEOutputEncryptorBuilder(NISTObjectIdentifiers.id_aes256_CBC).setProvider("BC").build(passPhrase.toCharArray())));
        }
*/

        final StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(privateKey,
                    new JcePEMEncryptorBuilder("AES-256-CBC").setProvider("BC").build(passPhrase.toCharArray()));
        }

        try (FileOutputStream fos = new FileOutputStream(privateKeyFile)) {
            fos.write(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
        }

        return new Keys(publicKeyFile, publicKeyContent, privateKeyFile, fingerprint.toString(), passPhrase);
    }

    private static String chunk(String encoded, int maxColumns) {
        final StringBuilder s = new StringBuilder();

        for (int i = 0; i < encoded.length(); i++) {
            s.append(encoded.charAt(i));
            if (i % maxColumns == (maxColumns - 1))
                s.append('\n');
        }

        return s.toString();
    }
}
