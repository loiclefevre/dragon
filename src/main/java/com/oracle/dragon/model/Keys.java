package com.oracle.dragon.model;

import java.io.File;

public class Keys {
    public final File publicKey;
    public final String publicKeyContent;
    public final File privateKey;
    public final String fingerprint;
    public final String passPhrase;

    public Keys(File publicKey, String publicKeyContent, File privateKey, String fingerprint, String passPhrase) {
        this.publicKey = publicKey;
        this.publicKeyContent= publicKeyContent;
        this.privateKey = privateKey;
        this.fingerprint = fingerprint;
        this.passPhrase = passPhrase;
    }
}
