package com.oracle.dragon.util;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.database.DatabaseClient;
import com.oracle.bmc.database.DatabaseWaiters;
import com.oracle.bmc.database.model.*;
import com.oracle.bmc.database.requests.*;
import com.oracle.bmc.database.responses.*;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.requests.GetUserRequest;
import com.oracle.bmc.identity.responses.GetUserResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.workrequests.WorkRequestClient;
import com.oracle.bmc.workrequests.requests.GetWorkRequestRequest;
import com.oracle.bmc.workrequests.responses.GetWorkRequestResponse;
import com.oracle.dragon.util.exception.*;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.TreeSet;

import static com.oracle.dragon.util.Console.*;

/**
 * DRAGON Stack session.
 */
public class DSSession {
    /**
     * Current version.
     */
    public static final String VERSION = "1.0.1";

    private static final int OCI_ALWAYS_FREE_DATABASE_NUMBER_LIMIT = 2;
    private static final String CONFIG_REGION = "region";
    private static final String CONFIG_FINGERPRINT = "fingerprint";
    private static final String CONFIG_DATABASE_PASSWORD = "database_password";
    private static final String CONFIG_COLLECTIONS = "collections";
    private static final String CONFIG_COMPARTMENT_ID = "compartment_id";
    private static final String CONFIG_KEY_FILE = "key_file";
    private static final String CONFIG_USER = "user";
    private static final String CONFIG_AUTH_TOKEN = "auth_token";

    public enum Platform {
        Windows,
        Linux,
        Unsupported
    }

    public enum Operation {
        CreateDatabase,
        DestroyDatabase
    }

    public enum Section {
        CommandLineParameters("Command line parameters"),
        OCIConfiguration("Oracle Cloud Infrastructure configuration"),
        OCIConnection("OCI API endpoints"),
        DatabaseTermination("Database termination"),
        DatabaseCreation("Database creation"),
        DatabaseWalletDownload("Database wallet download"),
        DatabaseConfiguration("Database configuration"),
        ObjectStorageConfiguration("Object storage configuration");

        private final String name;


        Section(String name) {
            this.name = name;
        }

        public void printlnKO() {
            System.out.print(Style.ANSI_RED);
            print80ln(name, "ko");
        }

        public void printlnOK() {
            System.out.print(Style.ANSI_GREEN);
            print80ln(name, "ok");
        }

        public void print(String msg) {
            //System.out.print(Style.ANSI_BLUE_BACKGROUND);
            print80(name, msg);
        }

        public void printlnKO(String msg) {
            System.out.print(Style.ANSI_RED);
            print80ln(name, String.format("ko [%s]", msg));
        }

        public void printlnOK(String msg) {
            System.out.print(Style.ANSI_GREEN);
            print80ln(name, String.format("ok [%s]", msg));
        }
    }

    public static final Platform platform;

    private Section section;
    private ConfigFileReader.ConfigFile configFile;
    private AuthenticationDetailsProvider provider;
    private DatabaseClient dbClient;
    private WorkRequestClient workRequestClient;
    private ObjectStorageClient objectStorageClient;
    private IdentityClient identityClient;

    /**
     * The database name to create.
     */
    private String dbName = "DRAGON";

    private String profileName = "DEFAULT";

    /**
     * The OCI region to manage.
     */
    private String region = "";

    private Operation operation = Operation.CreateDatabase;

    static {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("windows")) {
            platform = Platform.Windows;
            Console.ENABLE_COLORS = false;
        } else if (osName.startsWith("linux")) {
            platform = Platform.Linux;
            System.setProperty("java.awt.headless", "true");
        } else {
            platform = Platform.Unsupported;
        }
    }

    private static void banner() {
        print(String.format("%sDRAGON Stack manager v%s", Style.ANSI_YELLOW,VERSION));
        println();
        println();
    }

    public DSSession() throws UnsupportedPlatformException {
        banner();

        if(platform == Platform.Unsupported) {
            throw new UnsupportedPlatformException(System.getProperty("os.name"));
        }
    }

    public void analyzeCommandLineParameters(String[] args) throws MissingDatabaseNameParameterException, MissingProfileNameParameterException {
        section = Section.CommandLineParameters;
        section.print("analyzing");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-db":
                    if (i + 1 < args.length) {
                        dbName = args[++i].toUpperCase();
                    } else {
                        section.printlnKO();
                        throw new MissingDatabaseNameParameterException();
                    }
                    break;

                case "-p":
                case "-profile":
                case "--profile":
                    if (i + 1 < args.length) {
                        profileName = args[++i].toUpperCase();
                    } else {
                        section.printlnKO();
                        throw new MissingProfileNameParameterException();
                    }
                    break;

                case "-destroy":
                case "--destroy":
                    operation = Operation.DestroyDatabase;
                    break;

                case "-config-template":
                case "--config-template":
                    section.printlnOK();
                    println("Configuration template (save the content in a file named \"config.txt\"):");
                    println();
                    println();
                    println(" # DEFAULT profile (case sensitive), you can define others: ASHBURN_REGION or TEST_ENVIRONMENT");
                    println(" # You can choose a profile using the -profile command line argument");
                    println("[DEFAULT]");
                    println();
                    println(" # OCID of the user connecting to Oracle Cloud Infrastructure APIs. To get the value, see:" );
                    println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#five");
                    println("user=ocid1.user.oc1..<unique_ID>" );
                    println();
                    println(" # Full path and filename of the SSH private key (use *solely* forward slashes).");
                    println(" # /!\\ Warning: The key pair must be in PEM format. For instructions on generating a key pair in PEM format, see:" );
                    println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#Required_Keys_and_OCIDs");
                    println("key_file=<full path to SSH private key file>");
                    println();
                    println(" # Fingerprint for the SSH *public* key that was added to the user mentioned above. To get the value, see:" );
                    println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#four");
                    println("fingerprint=<full path to private SSH key file>");
                    println();
                    println(" # OCID of your tenancy. To get the value, see:" );
                    println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#five");
                    println("tenancy=ocid1.tenancy.oc1..<unique_ID>");
                    println();
                    println(" # An Oracle Cloud Infrastructure region identifier. For a list of possible region identifiers, check here:" );
                    println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm#top" );
                    println("region=eu-frankfurt-1" );
                    println();
                    println(" # OCID of the compartment to use for resources creation. to get more information about compartments, see:" );
                    println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/Identity/Tasks/managingcompartments.htm?Highlight=compartment%20ocid#Managing_Compartments");
                    println("compartment_id=ocid1.compartment.oc1..<unique_ID>");
                    println();
                    println(" # Authentication token that will be used for OCI Object Storage configuration, see:");
                    println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/Registry/Tasks/registrygettingauthtoken.htm?Highlight=user%20auth%20tokens");
                    println("auth_token=<authentication token>");
                    println();
                    println(" # The database password used for database creation and dragon user");
                    println(" # - 12 chars minimum and 30 chars maximum");
                    println(" # - can't contain the \"dragon\" word");
                    println(" # - contains 1 digit minimum");
                    println(" # - contains 1 lower case char");
                    println(" # - contains 1 upper case char");
                    println("database_password=<database password>");
                    println();
                    println(" # A list of coma separated JSON collection name(s) that you wish to get right after database creation");
                    println("# collections=");
                    System.exit(0);

                    break;

                case "-h":
                case "-?":
                case "/?":
                case "/h":
                case "-help":
                case "--help":
                    section.printlnOK();
                    println("Usage:");
                    println("  -config-template       \tdisplay a configuration file template");
                    println("  -db <database name>    \tdenotes the database name to create");
                    println("  -profile <profile name>\tchoose the given profile name from config.txt (instead of DEFAULT)");
                    println("  -destroy               \task to destroy the database");
                    System.exit(0);
                    break;
            }
        }
        section.printlnOK();
    }

    public void loadConfiguration() throws DSException {
        section = Section.OCIConfiguration;
        section.print("parsing");

        try {
            configFile = ConfigFileReader.parse("config.txt", profileName);

            if ((region = configFile.get(CONFIG_REGION)) == null) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_REGION);
            }

            region = region.toUpperCase().replaceAll("-", "_");

            if (configFile.get(CONFIG_KEY_FILE) == null) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_KEY_FILE);
            }
            if (configFile.get(CONFIG_COMPARTMENT_ID) == null) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_COMPARTMENT_ID);
            }
            if (configFile.get(CONFIG_DATABASE_PASSWORD) == null) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_DATABASE_PASSWORD);
            }
            if (configFile.get(CONFIG_USER) == null) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_USER);
            }
            if (configFile.get(CONFIG_AUTH_TOKEN) == null) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_AUTH_TOKEN);
            }
            if (configFile.get(CONFIG_FINGERPRINT) == null) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_FINGERPRINT);
            }
        } catch (java.io.FileNotFoundException fnfe) {
            section.printlnKO();
            throw new ConfigurationFileNotFoundException();
        } catch (IOException ioe) {
            section.printlnKO();
            throw new ConfigurationLoadException(ioe);
        }
        catch(IllegalArgumentException iae) {
            if (iae.getMessage().startsWith("No profile named")) {
                section.printlnKO("profile "+profileName+" not found");
                throw new ConfigurationProfileNotFoundException(profileName);
            }

            throw new ConfigurationParsingException(iae);
        }

        section.printlnOK();
    }

    public void initializeClients() throws OCIAPIAuthenticationPrivateKeyNotFoundException, OCIAPIDatabaseException {
        section = Section.OCIConnection;
        section.print("authentication pending");
        provider = new ConfigFileAuthenticationDetailsProvider(configFile);

        section.print("database pending");
        try {
            dbClient = new DatabaseClient(provider);
            dbClient.setRegion(region);
        } catch (IllegalArgumentException iae) {
            if (iae.getMessage().startsWith("Could not find private key")) {
                section.printlnKO("private key not found");
                throw new OCIAPIAuthenticationPrivateKeyNotFoundException(configFile.get(CONFIG_KEY_FILE));
            }

            throw new OCIAPIDatabaseException(iae);
        }
    }

    public void work() throws DSException {
        switch (operation) {
            case CreateDatabase:
                createADB();
                break;

            case DestroyDatabase:
                destroyDatabase();
                break;
        }
    }

    private void createADB() throws DSException {
        section = Section.DatabaseCreation;
        section.print("checking existing databases");

        final ListAutonomousDatabasesRequest listADB = ListAutonomousDatabasesRequest.builder().compartmentId(configFile.get("compartment_id")).build();
        final ListAutonomousDatabasesResponse listADBResponse = dbClient.listAutonomousDatabases(listADB);
        final Set<String> existingFreeADB = new TreeSet<>();
        boolean dbNameAlreadyExists = false;

        for (AutonomousDatabaseSummary adb : listADBResponse.getItems()) {
            if (adb.getLifecycleState() != AutonomousDatabaseSummary.LifecycleState.Terminated) {
                if (adb.getDbName().equals(dbName)) {
                    dbNameAlreadyExists = true;
                }
                if (adb.getIsFreeTier()) {
                    existingFreeADB.add(adb.getDbName());
                }
            }
        }

        if (existingFreeADB.size() == OCI_ALWAYS_FREE_DATABASE_NUMBER_LIMIT) {
            section.printlnKO("limit reached");
            throw new AlwaysFreeDatabaseLimitReachedException(OCI_ALWAYS_FREE_DATABASE_NUMBER_LIMIT);
        }

        if (dbNameAlreadyExists) {
            section.printlnKO("duplicate name");
            throw new DatabaseNameAlreadyExistsException(dbName);
        }

        section.print("pending");
        CreateAutonomousDatabaseDetails createFreeRequest = CreateAutonomousDatabaseDetails.builder()
                .cpuCoreCount(1)
                .dataStorageSizeInTBs(1)
                .displayName(dbName + " Database")
                .adminPassword(configFile.get(CONFIG_DATABASE_PASSWORD))
                .dbName(dbName)
                .compartmentId(configFile.get("compartment_id"))
                .dbWorkload(CreateAutonomousDatabaseBase.DbWorkload.Oltp)
                .isAutoScalingEnabled(Boolean.FALSE)
                .licenseModel(CreateAutonomousDatabaseBase.LicenseModel.LicenseIncluded)
                .isPreviewVersionWithServiceTermsAccepted(Boolean.FALSE)
                .isFreeTier(Boolean.TRUE)
                .build();

        AutonomousDatabase freeAtpShared = null;
        String workRequestId = null;
        workRequestClient = new WorkRequestClient(provider);

        try {
            CreateAutonomousDatabaseResponse responseCreate = dbClient.createAutonomousDatabase(CreateAutonomousDatabaseRequest.builder().createAutonomousDatabaseDetails(createFreeRequest).build());
            freeAtpShared = responseCreate.getAutonomousDatabase();
            workRequestId = responseCreate.getOpcWorkRequestId();
        } catch (BmcException e) {
            if (e.getStatusCode() == 400 && e.getServiceCode().equals("LimitExceeded")) {
                section.printlnKO("limit reached");
                throw new AlwaysFreeDatabaseLimitReachedException(OCI_ALWAYS_FREE_DATABASE_NUMBER_LIMIT);
            } else if (e.getStatusCode() == 400 && e.getServiceCode().equals("InvalidParameter") &&
                    e.getMessage().contains(dbName) && e.getMessage().contains("already exists")) {
                section.printlnKO("duplicate name");
                throw new DatabaseNameAlreadyExistsException(dbName);
            }
        }

        if (freeAtpShared == null) {
            section.printlnKO();
            throw new OCIDatabaseCreationCantProceedFurtherException();
        }

        GetWorkRequestRequest getWorkRequestRequest = GetWorkRequestRequest.builder().workRequestId(workRequestId).build();
        boolean exit = false;
        long startTime = System.currentTimeMillis();
        float pendingProgressMove = 0f;
        boolean probe = true;
        GetWorkRequestResponse getWorkRequestResponse = null;
        do {
            if (probe) getWorkRequestResponse = workRequestClient.getWorkRequest(getWorkRequestRequest);
            switch (getWorkRequestResponse.getWorkRequest().getStatus()) {
                case Succeeded:
                    section.printlnOK( getDurationSince(startTime));
                    exit = true;
                    break;
                case Failed:
                    section.printlnKO();
                    throw new OCIDatabaseCreationFaileDException(dbName, getWorkRequestResponse.getOpcRequestId());
                case Accepted:
                    section.print(String.format("accepted [%s]", getDurationSince(startTime)));
                    break;
                case InProgress:
                    section.print(String.format("in progress %.0f%% [%s]", Math.min(getWorkRequestResponse.getWorkRequest().getPercentComplete() + pendingProgressMove, 90f), getDurationSince(startTime)));
                    pendingProgressMove += Math.random() * 1.5f;
                    break;
            }

            sleep(500L);
            probe = !probe;
        } while (!exit);

        DatabaseWaiters waiter = dbClient.getWaiters();
        try {
            GetAutonomousDatabaseResponse responseGet = waiter.forAutonomousDatabase(GetAutonomousDatabaseRequest.builder().autonomousDatabaseId(freeAtpShared.getId()).build(),
                    new AutonomousDatabase.LifecycleState[]{AutonomousDatabase.LifecycleState.Available}).execute();
            freeAtpShared = responseGet.getAutonomousDatabase();
        } catch (Exception e) {
            section.printlnKO();
            throw new OCIDatabaseWaitForTerminationFailedException(e);
        }

        // The free ATP should now be available!

        section = Section.DatabaseWalletDownload;
        section.print("pending");
        GenerateAutonomousDatabaseWalletDetails atpWalletDetails = GenerateAutonomousDatabaseWalletDetails.builder().password(configFile.get(CONFIG_DATABASE_PASSWORD)).generateType(GenerateAutonomousDatabaseWalletDetails.GenerateType.Single).build();
        GenerateAutonomousDatabaseWalletResponse atpWalletResponse =
                dbClient.generateAutonomousDatabaseWallet(
                        GenerateAutonomousDatabaseWalletRequest.builder()
                                .generateAutonomousDatabaseWalletDetails(atpWalletDetails)
                                .autonomousDatabaseId(freeAtpShared.getId())
                                .build());
        section.print("saving");

        final String walletFileName = dbName.toLowerCase() + ".zip";
        final File walletFile = new File(walletFileName);
        try {
            Files.copy(atpWalletResponse.getInputStream(), walletFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new DatabaseWalletSavingException(walletFile.getAbsolutePath());
        }

        if (!ZipUtil.isValid(walletFile)) {
            section.printlnKO(String.format("%s is corrupted", walletFileName));
            throw new DatabaseWalletCorruptedException(walletFile.getAbsolutePath());
        }

        section.printlnOK(walletFileName);

        section = Section.DatabaseConfiguration;

        section.print("creating dragon user");
        createSchema(freeAtpShared);

        final ADBRESTService rSQLS = new ADBRESTService(freeAtpShared.getConnectionUrls().getSqlDevWebUrl(), "DRAGON", configFile.get(CONFIG_DATABASE_PASSWORD));

        createCollections(rSQLS, freeAtpShared);

        section.printlnOK();

        // download Oracle Instant Client? (https://www.oracle.com/database/technologies/instant-client/downloads.html)
        //
        // Not for Always Free Tiers
        // Create backup bucket
        // - configure backup bucket

        section = Section.ObjectStorageConfiguration;
        section.print("pending");
        objectStorageClient = new ObjectStorageClient(provider);
        objectStorageClient.setRegion(region);

        section.print("checking existing buckets");
        final GetNamespaceResponse namespaceResponse = objectStorageClient.getNamespace(GetNamespaceRequest.builder().build());
        final String namespaceName = namespaceResponse.getValue();

        final ListBucketsRequest.Builder listBucketsBuilder = ListBucketsRequest.builder().namespaceName(namespaceName).compartmentId(configFile.get(CONFIG_COMPARTMENT_ID));

        String nextToken = null;
        //boolean backupBucketExist = false;
        boolean dragonBucketExist = false;
        //final String backupBucketName = "backup_"+dbName.toLowerCase();
        final String dragonBucketName = "dragon";
        do {
            listBucketsBuilder.page(nextToken);
            ListBucketsResponse listBucketsResponse = objectStorageClient.listBuckets(listBucketsBuilder.build());
            for (BucketSummary bucket : listBucketsResponse.getItems()) {
                //if(!backupBucketExist && backupBucketName.equals(bucket.getName())) backupBucketExist = true;
                if (!dragonBucketExist && dragonBucketName.equals(bucket.getName())) dragonBucketExist = true;
            }
            nextToken = listBucketsResponse.getOpcNextPage();
        } while (nextToken != null);

            /*if(!backupBucketExist) {
                print80("OCI object storage configuration", "creating manual backup bucket");
                createManualBucket(objectStorageClient,namespaceName,backupBucketName,configFile.get("compartment_id"),false);
            }*/

        if (!dragonBucketExist) {
            section.print("creating dragon bucket");
            createManualBucket(namespaceName, dragonBucketName, true);
        }

        //print80("OCI DRAGON database backup configuration", "pending");
        identityClient = new IdentityClient(provider);
        GetUserResponse userResponse = identityClient.getUser(GetUserRequest.builder().userId(configFile.get(CONFIG_USER)).build());

/*            print80("Database backup configuration", "default bucket");
            try(Connection c=pdsADMIN.getConnection()) {
                try(Statement s=c.createStatement()) {
                    s.execute("ALTER DATABASE PROPERTY SET default_bucket='https://swiftobjectstorage." + getRegionForURL(region) + ".oraclecloud.com/v1/" + namespaceName + "'");
                }

                print80("Database backup configuration", "database credential to bucket");
                try(CallableStatement cs=c.prepareCall("{call DBMS_CLOUD.CREATE_CREDENTIAL(credential_name => 'BACKUP_CRED_NAME', username => '"+userResponse.getUser().getEmail()+"', password => '"+authTokenResponse.getAuthToken().getToken()+"')}")) {
                    cs.execute();
                    c.commit();
                }

                print80("Database backup configuration", "database default credential");
                try(Statement s=c.createStatement()) {
                    s.execute("ALTER DATABASE PROPERTY SET default_credential='ADMIN.BACKUP_CRED_NAME'");
                }
            }

            print80ln("Database backup configuration", "ok");
*/
        section.print("database setup");

        try {
            rSQLS.execute(String.format("BEGIN\n" +
                    "    DBMS_CLOUD.CREATE_CREDENTIAL(credential_name => 'DRAGON_CREDENTIAL_NAME', username => '%s', password => '%s');\n" +
                    "    COMMIT;\n" +
                    "END;\n" +
                    "/", userResponse.getUser().getEmail(), configFile.get(CONFIG_AUTH_TOKEN)));
        } catch (RuntimeException re) {
            section.printlnKO();
            throw new ObjectStorageConfigurationFailedException();
        }

        section.printlnOK();
    }

    private void createSchema(AutonomousDatabase adb) throws DatabaseUserCreationFailedException {
        final ADBRESTService rSQLS = new ADBRESTService(adb.getConnectionUrls().getSqlDevWebUrl(), "ADMIN", configFile.get(CONFIG_DATABASE_PASSWORD));

        try {
            rSQLS.execute(String.format("create user dragon identified by %s DEFAULT TABLESPACE DATA TEMPORARY TABLESPACE TEMP;\n" +
                    "alter user dragon quota unlimited on data;\n" +
                    "grant dwrole, create session, soda_app to dragon;\n" +
                    "grant select on v$mystat to dragon;" +
                    "BEGIN\n" +
                    "    ords_admin.enable_schema(p_enabled => TRUE, p_schema => 'DRAGON', p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => 'dragon', p_auto_rest_auth => TRUE);\n" +
                    "END;\n" +
                    "/", configFile.get(CONFIG_DATABASE_PASSWORD)));
        } catch (RuntimeException re) {
            section.printlnKO();
            throw new DatabaseUserCreationFailedException();
        }
    }

    private String getRegionForURL() {
        return region.replaceAll("_", "-").toLowerCase();
    }

    private void createManualBucket(String namespaceName, String bucketName, boolean events) throws ObjectStorageBucketCreationFailedException {
        CreateBucketRequest request = CreateBucketRequest.builder().namespaceName(namespaceName).createBucketDetails(
                CreateBucketDetails.builder().compartmentId(configFile.get(CONFIG_COMPARTMENT_ID)).name(bucketName).objectEventsEnabled(events).build()
        ).build();

        CreateBucketResponse response = objectStorageClient.createBucket(request);

        if (response.getBucket() == null || !response.getBucket().getName().equals(bucketName)) {
            section.printlnKO();
            throw new ObjectStorageBucketCreationFailedException(bucketName);
        }
    }

    private void createCollections(ADBRESTService rSQLS, AutonomousDatabase adb) {
        section.print("creating dragon collections");
        rSQLS.createSODACollection("dragon");
        section.print("storing dragon information");
        rSQLS.insertDocument("dragon", String.format("{\"databaseServiceURL\": \"%s\", " +
                        "\"sqlDevWebAdmin\": \"%s\", " +
                        "\"sqlDevWeb\": \"%s\", " +
                        "\"apexURL\": \"%s\", " +
                        "\"omlURL\": \"%s\", " +
                        "\"version\": \"%s\" " +
                        "}",
                adb.getServiceConsoleUrl(),
                adb.getConnectionUrls().getSqlDevWebUrl(),
                adb.getConnectionUrls().getSqlDevWebUrl().replaceAll("admin", "dragon"),
                adb.getConnectionUrls().getApexUrl(),
                adb.getConnectionUrls().getMachineLearningUserManagementUrl(),
                adb.getDbVersion()
        ));

        for (String collectionName : configFile.get(CONFIG_COLLECTIONS).split(", ")) {
            if (!"dragon".equals(collectionName)) {
                section.print("creating collection " + collectionName);
                rSQLS.createSODACollection(collectionName);
            }
        }
    }

    private void destroyDatabase() throws OCIDatabaseTerminationFailedException, OCIDatabaseWaitForTerminationFailedException {
        section = Section.DatabaseTermination;
        section.print("checking existing databases");

        final ListAutonomousDatabasesRequest listADB = ListAutonomousDatabasesRequest.builder().compartmentId(configFile.get("compartment_id")).build();
        final ListAutonomousDatabasesResponse listADBResponse = dbClient.listAutonomousDatabases(listADB);

        boolean dbNameExists = false;
        String adbId = null;
        for (AutonomousDatabaseSummary adb : listADBResponse.getItems()) {
            if (adb.getLifecycleState() != AutonomousDatabaseSummary.LifecycleState.Terminated) {
                if (adb.getDbName().equals(dbName) && adb.getIsFreeTier()) {
                    dbNameExists = true;
                    adbId = adb.getId();
                    break;
                }
            }
        }

        if (!dbNameExists) {
            section.printlnOK("nothing to do");
        } else {
            section.print("pending");

            workRequestClient = new WorkRequestClient(provider);
            DeleteAutonomousDatabaseResponse responseTerminate = dbClient.deleteAutonomousDatabase(DeleteAutonomousDatabaseRequest.builder().autonomousDatabaseId(adbId).build());
            String workRequestId = responseTerminate.getOpcWorkRequestId();

            GetWorkRequestRequest getWorkRequestRequest = GetWorkRequestRequest.builder().workRequestId(workRequestId).build();
            boolean exit = false;
            long startTime = System.currentTimeMillis();
            float pendingProgressMove = 0f;
            do {
                GetWorkRequestResponse getWorkRequestResponse = workRequestClient.getWorkRequest(getWorkRequestRequest);
                switch (getWorkRequestResponse.getWorkRequest().getStatus()) {
                    case Succeeded:
                        section.printlnOK( getDurationSince(startTime));
                        exit = true;
                        break;
                    case Failed:
                        section.printlnKO();
                        throw new OCIDatabaseTerminationFailedException(dbName, getWorkRequestResponse.getOpcRequestId());
                    case Accepted:
                        section.print(String.format("accepted [%s]", getDurationSince(startTime)));
                        break;
                    case InProgress:
                        section.print(String.format("in progress %.0f%% [%s]", Math.min(getWorkRequestResponse.getWorkRequest().getPercentComplete() + pendingProgressMove, 99f), getDurationSince(startTime)));
                        pendingProgressMove += Math.random() * 2f;
                        break;
                }

                sleep(1000L);

            } while (!exit);

            DatabaseWaiters waiter = dbClient.getWaiters();
            try {
                final GetAutonomousDatabaseResponse responseGet = waiter.forAutonomousDatabase(GetAutonomousDatabaseRequest.builder().autonomousDatabaseId(adbId).build(),
                        new AutonomousDatabase.LifecycleState[]{AutonomousDatabase.LifecycleState.Terminated}).execute();
            } catch (Exception e) {
                section.printlnKO();
                throw new OCIDatabaseWaitForTerminationFailedException(e);
            }
        }
    }

    private void sleep(final long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignored) {
        }
    }

    public void close() {
        if (dbClient != null) dbClient.close();
        if (workRequestClient != null) workRequestClient.close();
        if (objectStorageClient != null) objectStorageClient.close();
        if (identityClient != null) identityClient.close();
    }
}
