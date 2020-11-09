package com.oracle.dragon.util;

import com.oracle.dragon.model.Keys;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;
import java.util.Random;

public final class KeysUtil {
    public static Keys createKeys(final String passPhrase) throws Exception {
        // see https://www.bouncycastle.org/fips-java/BCFipsIn100.pdf
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

    private static byte[] encryptWithPassPhrase(byte[] encodedprivkey, String passPhrase) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidParameterSpecException, IOException {
        // We must use a PasswordBasedEncryption algorithm in order to encrypt the private key, you may use any common algorithm supported by openssl, you can check them in the openssl documentation http://www.openssl.org/docs/apps/pkcs8.html
        String MYPBEALG = "PBEWithSHA1AndDESede";

        final int count = 10000 + new Random().nextInt(1000);// hash iteration count
        final SecureRandom random = new SecureRandom();
        final byte[] salt = new byte[8];
        random.nextBytes(salt);

        // Create PBE parameter set
        final PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
        final PBEKeySpec pbeKeySpec = new PBEKeySpec(passPhrase.toCharArray());
        final SecretKeyFactory keyFac = SecretKeyFactory.getInstance(MYPBEALG);
        final SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        final Cipher pbeCipher = Cipher.getInstance(MYPBEALG);

        // Initialize PBE Cipher with key and parameters
        pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

        // Encrypt the encoded Private Key with the PBE key
        byte[] ciphertext = pbeCipher.doFinal(encodedprivkey);

        // Now construct  PKCS #8 EncryptedPrivateKeyInfo object
        AlgorithmParameters algparms = AlgorithmParameters.getInstance(MYPBEALG);
        algparms.init(pbeParamSpec);
        EncryptedPrivateKeyInfo encinfo = new EncryptedPrivateKeyInfo(algparms, ciphertext);

        // and here we have it! a DER encoded PKCS#8 encrypted key!
        return encinfo.getEncoded();
    }

}
