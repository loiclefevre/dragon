package com.oracle.dragon.model;

public class LocalDragonConfiguration {
    private String redirect;

    private String databaseServiceURL;
    private String sqlDevWebAdmin;
    private String sqlDevWeb;
    private String apexURL;
    private String omlURL;
    private String graphStudioURL;
    private String sqlAPI;
    private String sodaAPI;
    private String version;
    private String apexVersion;
    private String ordsVersion;
    private String dbName;
    private String dbUserName;
    private String dbUserPassword;
    private String walletFile;
    private String extractedWallet;

    public LocalDragonConfiguration() {
    }

    public String getDatabaseServiceURL() {
        return databaseServiceURL;
    }

    public void setDatabaseServiceURL(String databaseServiceURL) {
        this.databaseServiceURL = databaseServiceURL;
    }

    public String getSqlDevWebAdmin() {
        return sqlDevWebAdmin;
    }

    public void setSqlDevWebAdmin(String sqlDevWebAdmin) {
        this.sqlDevWebAdmin = sqlDevWebAdmin;
    }

    public String getSqlDevWeb() {
        return sqlDevWeb;
    }

    public void setSqlDevWeb(String sqlDevWeb) {
        this.sqlDevWeb = sqlDevWeb;
    }

    public String getApexURL() {
        return apexURL;
    }

    public void setApexURL(String apexURL) {
        this.apexURL = apexURL;
    }

    public String getOmlURL() {
        return omlURL;
    }

    public void setOmlURL(String omlURL) {
        this.omlURL = omlURL;
    }

    public String getGraphStudioURL() {
        return graphStudioURL;
    }

    public void setGraphStudioURL(String graphStudioURL) {
        this.graphStudioURL = graphStudioURL;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getApexVersion() {
        return apexVersion;
    }

    public void setApexVersion(String apexVersion) {
        this.apexVersion = apexVersion;
    }

    public String getOrdsVersion() {
        return ordsVersion;
    }

    public void setOrdsVersion(String ordsVersion) {
        this.ordsVersion = ordsVersion;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUserName() {
        return dbUserName;
    }

    public void setDbUserName(String dbUserName) {
        this.dbUserName = dbUserName;
    }

    public String getDbUserPassword() {
        return dbUserPassword;
    }

    public void setDbUserPassword(String dbUserPassword) {
        this.dbUserPassword = dbUserPassword;
    }

    public String getSqlAPI() {
        return sqlAPI;
    }

    public void setSqlAPI(String sqlAPI) {
        this.sqlAPI = sqlAPI;
    }

    public String getSodaAPI() {
        return sodaAPI;
    }

    public void setSodaAPI(String sodaAPI) {
        this.sodaAPI = sodaAPI;
    }

    public String getWalletFile() {
        return walletFile;
    }

    public void setWalletFile(String walletFile) {
        this.walletFile = walletFile;
    }

    public String getExtractedWallet() {
        return extractedWallet;
    }

    public void setExtractedWallet(String extractedWallet) {
        this.extractedWallet = extractedWallet;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }
}
