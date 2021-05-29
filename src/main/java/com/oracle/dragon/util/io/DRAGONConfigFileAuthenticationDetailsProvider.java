package com.oracle.dragon.util.io;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.*;
import com.oracle.bmc.http.ClientConfigurator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DRAGONConfigFileAuthenticationDetailsProvider extends ConfigFileAuthenticationDetailsProvider implements BasicAuthenticationDetailsProvider {
    private DRAGONConfigFile.ConfigFile configFile;
    private BasicConfigFileAuthenticationProvider delegate;

    public DRAGONConfigFileAuthenticationDetailsProvider(String profile) throws IOException {
        super(profile);
    }

    public DRAGONConfigFileAuthenticationDetailsProvider(String configurationFilePath, String profile) throws IOException {
        super(configurationFilePath, profile);
    }

    public DRAGONConfigFileAuthenticationDetailsProvider(ConfigFileReader.ConfigFile configFile) {
        super(configFile);
    }

    public DRAGONConfigFileAuthenticationDetailsProvider(String configurationFilePath, String profile, DRAGONConfigFile.ConfigFile configFile) throws IOException {
        this(configurationFilePath, profile);

        this.configFile = configFile;
        this.delegate = new DRAGONConfigFileAuthenticationDetailsProvider.DRAGONConfigFileSimpleAuthenticationDetailsProvider(configFile);
    }

    public String getFingerprint() {
        return this.delegate.getFingerprint();
    }

    public String getTenantId() {
        return this.delegate.getTenantId();
    }

    public String getUserId() {
        return this.delegate.getUserId();
    }

    public List<ClientConfigurator> getClientConfigurators() {
        return this.delegate.getClientConfigurators();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getPassPhrase() {
        return this.delegate.getPassPhrase();
    }

    public char[] getPassphraseCharacters() {
        return this.delegate.getPassphraseCharacters();
    }

    public InputStream getPrivateKey() {
        return this.delegate.getPrivateKey();
    }

    public String getKeyId() {
        return this.delegate.getKeyId();
    }

    public String getPemFilePath() {
        return this.delegate.getPemFilePath();
    }

    public String toString() {
        return "ConfigFileAuthenticationDetailsProvider(delegate=" + this.delegate + ", region=" + this.getRegion() + ")";
    }

    private static class DRAGONConfigFileSimpleAuthenticationDetailsProvider implements BasicConfigFileAuthenticationProvider {
        private final SimpleAuthenticationDetailsProvider delegate;
        private final String pemFilePath;
        private final List<ClientConfigurator> clientConfigurators;

        private DRAGONConfigFileSimpleAuthenticationDetailsProvider(DRAGONConfigFile.ConfigFile configFile) {
            String fingerprint = (String) Preconditions.checkNotNull(configFile.get("fingerprint"), "missing fingerprint in config");
            String tenantId = (String) Preconditions.checkNotNull(configFile.get("tenancy"), "missing tenancy in config");
            String userId = (String) Preconditions.checkNotNull(configFile.get("user"), "missing user in config");
            String pemFilePath = (String) Preconditions.checkNotNull(configFile.get("key_file"), "missing key_file in config");
            String passPhrase = configFile.get("pass_phrase");
            Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier(pemFilePath);
            SimpleAuthenticationDetailsProvider.SimpleAuthenticationDetailsProviderBuilder builder = SimpleAuthenticationDetailsProvider.builder().privateKeySupplier(privateKeySupplier).fingerprint(fingerprint).userId(userId).tenantId(tenantId);
            if (passPhrase != null) {
                builder = builder.passphraseCharacters(passPhrase.toCharArray());
            }

            this.delegate = builder.build();
            this.pemFilePath = pemFilePath;
            this.clientConfigurators = new ArrayList();
        }

        public String getFingerprint() {
            return this.delegate.getFingerprint();
        }

        public String getTenantId() {
            return this.delegate.getTenantId();
        }

        public String getUserId() {
            return this.delegate.getUserId();
        }

        /**
         * @deprecated
         */
        @Deprecated
        public String getPassPhrase() {
            return this.delegate.getPassPhrase();
        }

        /**
         * @deprecated
         */
        @Deprecated
        public char[] getPassphraseCharacters() {
            return this.delegate.getPassphraseCharacters();
        }

        public InputStream getPrivateKey() {
            return this.delegate.getPrivateKey();
        }

        public String getKeyId() {
            return this.delegate.getKeyId();
        }

        public String getPemFilePath() {
            return this.pemFilePath;
        }

        public List<ClientConfigurator> getClientConfigurators() {
            return this.clientConfigurators;
        }
    }

}
