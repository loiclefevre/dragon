package com.oracle.dragon.util.exception;

public enum ErrorCode {
    UnsupportedPlatform(-1),
    MissingDatabaseNameParameter(-2),
    ConfigurationFileNotFound(-3),
    ConfigurationFileLoadProblem(-4),
    ConfigurationMissesParameter(-5),
    OCIAPIAuthenticationPrivateKeyNotFound(-6),
    OCIAPIDatabase(-7),
    OCIDatabaseTerminationFailed(-8),
    OCIDatabaseWaitForTerminationFailed(-9),
    OCIAlwaysFreeDatabaseLimitReached(-10),
    OCIDatabaseNameAlreadyExists(-11),
    OCIDatabaseCreationCantProceedFurther(-12),
    OCIDatabaseCreationFailed(-13),
    DatabaseWalletSaving(-14),
    DatabaseWalletCorrupted(-15),
    DatabaseUserCreationFailed(-16),
    ObjectStorageConfigurationFailed(-17),
    ObjectStorageBucketCreationFailed(-18),
    MissingProfileNameParameter(-19),
    ConfigurationProfileNotFound(-20),
    ConfigurationParsing(-21),
    ConfigurationWrongDatabaseLicenseType(-22),
    ConfigurationWrongDatabaseType(-23),
    ConfigurationDataPathNotFound(-24),
    ConfigurationDataPathDirectory(-25),
    SecurityAlgorithmNotFound(-26),
    DataFileToLoadNotFound(-27),
    DataFileNotLoaded(-28),
    CollectionNotLoaded(-29),
    LocalConfigurationNotSaved(-30),
    AutonomousDatabaseLimitReached(-31),
    LoadLocalConfiguration(-32),
    LoadStackMetadata(-33),
    StackFileNotFound(-34),
    ConfigurationBadFingerprintParameter(-35),
    ConfigurationUnsupportedParameter(-36),
    ConfigurationMissingKeyFile(-37),
    ConfigurationKeyFileNotAFile(-38),
    UpgradeFailed(-39), UpgradeTimeout(-40),
    StackFileDownload(-41),
    DatabaseAlreadyDeployed(-42),
    UnmanagedDatabaseCantBeDestroyed(-43),
    OCIDatabaseWaitForShutdownFailed(-44),
    OCIDatabaseShutdownFailed(-45),
    OCIDatabaseWaitForStartFailed(-46),
    OCIDatabaseStartFailed(-47),
    UnknownEnvironmentRequirementForStack(-48);

    public final int internalErrorCode;

    ErrorCode(final int errorCode) {
        this.internalErrorCode = errorCode;
    }
}
