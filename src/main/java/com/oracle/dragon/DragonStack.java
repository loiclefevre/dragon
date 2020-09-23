package com.oracle.dragon;

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
import com.oracle.bmc.objectstorage.ObjectStorage;
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
import com.oracle.dragon.util.ADBRESTService;
import com.oracle.dragon.util.ZipUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;
import java.util.TreeSet;

/**
 * DRAGON Stack Manager - Main entry point.
 *
 * @since 1.0.0
 */
public class DragonStack {
    /**
     * The database name to create.
     */
    static String dbName = "DRAGON";

    /**
     * The OCI region to manage.
     */
    static String region = "";

    /**
     * A flag denoting if we are in a creation process or a termination process.
     */
    static boolean destroy = false;

    public static void main(final String[] args) {
        final long totalDuration = System.currentTimeMillis();

        print("DRAGON Stack manager v1.0.0");
        println();
        println();

        try {
            print80("Command line parameters", "analyzing");
            analyzeCommandLineParameters(args);
            print80ln("Command line parameters", "ok");

            print80("Oracle Cloud Infrastructure configuration", "parsing");
            ConfigFileReader.ConfigFile configFile = null;

            try {
                configFile = ConfigFileReader.parse("config.txt", "DEFAULT");
            } catch (java.io.FileNotFoundException fnfe) {
                print80ln("Oracle Cloud Infrastructure configuration", "ko");
                System.err.println("The expected configuration file named \"config.txt\" is not present in this directory.");
                System.exit(-6);
            }
            print80ln("Oracle Cloud Infrastructure configuration", "ok");

            print80("OCI authentication", "pending");
            AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

            print80("OCI database API connection", "pending");
            DatabaseClient dbClient = new DatabaseClient(provider);
            region = configFile.get("region");
            dbClient.setRegion(region);

            if (destroy) {
                destroyADB(configFile, provider, dbClient);
            } else {
                final String databasePassword = configFile.get("database_password");
                createADB(configFile, provider, dbClient, databasePassword);
            }

            dbClient.close();
        } catch (BmcException e) {
            if (e.isClientSide()) {
                System.err.println("A problem occurred on your side that prevented the operation to succeed!");
                e.printStackTrace();
                System.exit(-97);
            } else {
                println("Status: " + e.getStatusCode());
                println("Service: " + e.getServiceCode());
                println("RequestId: " + e.getOpcRequestId());
                println("Timeout: " + e.isTimeout());
                println("Client Side: " + e.isClientSide());
                System.err.printf("ERROR: %s\n", e.getLocalizedMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        println("duration: " + getDurationSince(totalDuration));
    }

    private static void destroyADB(ConfigFileReader.ConfigFile configFile, AuthenticationDetailsProvider provider, DatabaseClient dbClient) throws Exception {
        print80("Database termination", "checking existing databases");

        ListAutonomousDatabasesRequest listADB = ListAutonomousDatabasesRequest.builder().compartmentId(configFile.get("compartment_id")).build();
        ListAutonomousDatabasesResponse listADBResponse = dbClient.listAutonomousDatabases(listADB);

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
            print80ln("Database termination", "ko [free tiers database not found]");
            System.err.println("The Always Free database " + dbName + " doesn't exist!");
            System.exit(-5);
        }

        print80("Database termination", "pending");

        WorkRequestClient workRequestClient = new WorkRequestClient(provider);
        DeleteAutonomousDatabaseResponse responseTerminate = dbClient.deleteAutonomousDatabase(DeleteAutonomousDatabaseRequest.builder().autonomousDatabaseId(adbId).build());
        String workRequestId = responseTerminate.getOpcWorkRequestId();

        GetWorkRequestRequest getWorkRequestRequest = GetWorkRequestRequest.builder().workRequestId(workRequestId).build();
        boolean exit = false;
        long startTime = System.currentTimeMillis();
        do {
            GetWorkRequestResponse getWorkRequestResponse = workRequestClient.getWorkRequest(getWorkRequestRequest);
            switch (getWorkRequestResponse.getWorkRequest().getStatus()) {
                case Succeeded:
                    print80ln("Database deletion", String.format("ok [%s]", getDurationSince(startTime)));
                    exit = true;
                    break;
                case Failed:
                    print80ln("Database deletion", "ko");
                    System.err.println("The deletion of your " + dbName + " database failed, see the request Id " + getWorkRequestResponse.getOpcRequestId());
                    System.exit(-99);
                    exit = true;
                    break;
                case Accepted:
                    print80("Database deletion", String.format("accepted [%s]", getDurationSince(startTime)));
                    break;
                case InProgress:
                    print80("Database deletion", String.format("in progress %.0f%% [%s]", getWorkRequestResponse.getWorkRequest().getPercentComplete(), getDurationSince(startTime)));
                    break;
            }

            Thread.sleep(1000L);
        } while (!exit);

        DatabaseWaiters waiter = dbClient.getWaiters();
        GetAutonomousDatabaseResponse responseGet = waiter.forAutonomousDatabase(GetAutonomousDatabaseRequest.builder().autonomousDatabaseId(adbId).build(),
                new AutonomousDatabase.LifecycleState[]{AutonomousDatabase.LifecycleState.Terminated}).execute();

    }

    private static void createADB(ConfigFileReader.ConfigFile configFile, AuthenticationDetailsProvider provider, DatabaseClient dbClient, String databasePassword) throws Exception {
        print80("Database creation", "checking existing databases");

        ListAutonomousDatabasesRequest listADB = ListAutonomousDatabasesRequest.builder().compartmentId(configFile.get("compartment_id")).build();
        ListAutonomousDatabasesResponse listADBResponse = dbClient.listAutonomousDatabases(listADB);
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

        if (existingFreeADB.size() == 2) {
            print80ln("Database creation", "ko [limit reached]");
            System.err.println("You've reached the maximum limit of 2 databases for OCI Free Tier Autonomous Database.");
            System.exit(-1);
        }

        if (dbNameAlreadyExists) {
            print80ln("Database creation", "ko [duplicate name]");
            System.err.println("The name of the database you are requesting (" + dbName + ") already exists, please retry with another name (-db <database name>).");
            System.exit(-2);
        }

        print80("Database creation", "pending");
        CreateAutonomousDatabaseDetails createFreeRequest = CreateAutonomousDatabaseDetails.builder()
                .cpuCoreCount(1)
                .dataStorageSizeInTBs(1)
                .displayName(dbName + " Database")
                .adminPassword(databasePassword)
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
        WorkRequestClient workRequestClient = new WorkRequestClient(provider);

        try {
            CreateAutonomousDatabaseResponse responseCreate = dbClient.createAutonomousDatabase(CreateAutonomousDatabaseRequest.builder().createAutonomousDatabaseDetails(createFreeRequest).build());
            freeAtpShared = responseCreate.getAutonomousDatabase();
            workRequestId = responseCreate.getOpcWorkRequestId();
        } catch (BmcException e) {
            if (e.getStatusCode() == 400 && e.getServiceCode().equals("LimitExceeded")) {
                print80ln("Database creation", "ko");
                System.err.println("You've reached the maximum limit of 2 databases for OCI Free Tier Autonomous Database.");
                System.exit(-1);
            } else if (e.getStatusCode() == 400 && e.getServiceCode().equals("InvalidParameter") &&
                    e.getMessage().contains(dbName) && e.getMessage().contains("already exists")) {
                print80ln("Database creation", "ko");
                System.err.println("The name of the database you are requesting (" + dbName + ") already exists, please retry with another name (-db <database name>).");
                System.exit(-2);
            }
        }

        if (freeAtpShared == null) {
            print80ln("Database creation", "ko");
            System.err.println("The OCI API response provided doesn't allow to proceed further.");
            System.exit(-2);
        }

        GetWorkRequestRequest getWorkRequestRequest = GetWorkRequestRequest.builder().workRequestId(workRequestId).build();
        boolean exit = false;
        long startTime = System.currentTimeMillis();
        do {
            GetWorkRequestResponse getWorkRequestResponse = workRequestClient.getWorkRequest(getWorkRequestRequest);
            switch (getWorkRequestResponse.getWorkRequest().getStatus()) {
                case Succeeded:
                    print80ln("Database creation", String.format("ok [%s]", getDurationSince(startTime)));
                    exit = true;
                    break;
                case Failed:
                    print80ln("Database creation", "ko");
                    System.err.println("The creation of your " + dbName + " database failed, see the request Id " + getWorkRequestResponse.getOpcRequestId());
                    System.exit(-99);
                    exit = true;
                    break;
                case Accepted:
                    print80("Database creation", String.format("accepted [%s]", getDurationSince(startTime)));
                    break;
                case InProgress:
                    print80("Database creation", String.format("in progress %.0f%% [%s]", getWorkRequestResponse.getWorkRequest().getPercentComplete(), getDurationSince(startTime)));
                    break;
            }

            Thread.sleep(1000L);
        } while (!exit);

        DatabaseWaiters waiter = dbClient.getWaiters();
        GetAutonomousDatabaseResponse responseGet = waiter.forAutonomousDatabase(GetAutonomousDatabaseRequest.builder().autonomousDatabaseId(freeAtpShared.getId()).build(),
                new AutonomousDatabase.LifecycleState[]{AutonomousDatabase.LifecycleState.Available}).execute();
        freeAtpShared = responseGet.getAutonomousDatabase();

        // The free ATP should now be available!

        print80("Database wallet download", "pending");
        GenerateAutonomousDatabaseWalletDetails atpWalletDetails = GenerateAutonomousDatabaseWalletDetails.builder().password(databasePassword).generateType(GenerateAutonomousDatabaseWalletDetails.GenerateType.Single).build();
        GenerateAutonomousDatabaseWalletResponse atpWalletResponse =
                dbClient.generateAutonomousDatabaseWallet(
                        GenerateAutonomousDatabaseWalletRequest.builder()
                                .generateAutonomousDatabaseWalletDetails(atpWalletDetails)
                                .autonomousDatabaseId(freeAtpShared.getId())
                                .build());
        print80("Database wallet download", "saving");

        final String walletFileName = dbName.toLowerCase() + ".zip";
        final File walletFile = new File(walletFileName);
        Files.copy(atpWalletResponse.getInputStream(), walletFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        if (!ZipUtil.isValid(walletFile)) {
            print80ln("Database wallet download", String.format("ko [%s is corrupted]", walletFileName));
            System.err.println(String.format("Error downloading database wallet, the file %s is corrupted.", walletFileName));
            System.exit(-98);
        }

        print80ln("Database wallet download", String.format("ok [%s]", walletFileName));

        print80("Database configuration", "creating dragon user");
        createSchema(freeAtpShared, databasePassword);

        final ADBRESTService rSQLS = new ADBRESTService(freeAtpShared.getConnectionUrls().getSqlDevWebUrl(), "DRAGON", databasePassword);

        createCollections(rSQLS, freeAtpShared, configFile.get("collections").split(", "));
        println();

        print80ln("Database configuration", "ok");

        // download Oracle Instant Client? (https://www.oracle.com/database/technologies/instant-client/downloads.html)
        //
        // Not for Always Free Tiers
        // Create backup bucket
        // - configure backup bucket

        print80("Object storage configuration", "pending");
        final ObjectStorage objectStorageClient = new ObjectStorageClient(provider);
        objectStorageClient.setRegion(region);

        print80("Object storage configuration", "checking existing buckets");
        GetNamespaceResponse namespaceResponse = objectStorageClient.getNamespace(GetNamespaceRequest.builder().build());
        String namespaceName = namespaceResponse.getValue();

        ListBucketsRequest.Builder listBucketsBuilder = ListBucketsRequest.builder().namespaceName(namespaceName).compartmentId(configFile.get("compartment_id"));

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
            print80("Object storage configuration", "creating dragon bucket");
            createManualBucket(objectStorageClient, namespaceName, dragonBucketName, configFile.get("compartment_id"), true);
        }

        //print80("OCI DRAGON database backup configuration", "pending");
        IdentityClient identityClient = new IdentityClient(provider);
        GetUserResponse userResponse = identityClient.getUser(GetUserRequest.builder().userId(configFile.get("user")).build());
        //System.out.println(userResponse.getUser().getEmail());


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
        print80("Object storage configuration", "database setup");

        try {
            rSQLS.execute("BEGIN\n" +
                    "    DBMS_CLOUD.CREATE_CREDENTIAL(credential_name => 'DRAGON_CREDENTIAL_NAME', username => '" + userResponse.getUser().getEmail() + "', password => '" + configFile.get("auth_token") + "');\n" +
                    "    COMMIT;\n" +
                    "END;\n" +
                    "/");
        } catch (RuntimeException re) {
            print80ln("Object storage configuration", "ko");
            System.err.println("The object storage credential for the database couldn't be configured.");
            System.exit(-8);
        }

        print80ln("Object storage configuration", "ok");

        identityClient.close();
        workRequestClient.close();
        objectStorageClient.close();
    }

    private static void createSchema(AutonomousDatabase adb, String databasePassword) {
        final ADBRESTService rSQLS = new ADBRESTService(adb.getConnectionUrls().getSqlDevWebUrl(), "ADMIN", databasePassword);

        try {
            rSQLS.execute("create user dragon identified by " + databasePassword + " DEFAULT TABLESPACE DATA TEMPORARY TABLESPACE TEMP;\n" +
                    "alter user dragon quota unlimited on data;\n" +
                    "grant dwrole, create session, soda_app to dragon;\n" +
                    "grant select on v$mystat to dragon;" +
                    "BEGIN\n" +
                    "    ords_admin.enable_schema(p_enabled => TRUE, p_schema => 'DRAGON', p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => 'dragon', p_auto_rest_auth => TRUE);\n" +
                    "END;\n" +
                    "/");
        } catch (RuntimeException re) {
            print80ln("Database configuration", "ko");
            System.err.println("The DRAGON user couldn't be created!");
            System.exit(-7);
        }
    }

    private static String getRegionForURL(String region) {
        return region.replaceAll("_", "-").toLowerCase();
    }

    private static void createManualBucket(ObjectStorage objectStorageClient, String namespaceName, String bucketName, String compartmentId, boolean events) {
        CreateBucketRequest request = CreateBucketRequest.builder().namespaceName(namespaceName).createBucketDetails(
                CreateBucketDetails.builder().compartmentId(compartmentId).name(bucketName).objectEventsEnabled(events).build()
        ).build();

        CreateBucketResponse response = objectStorageClient.createBucket(request);

        if (response.getBucket() == null || !response.getBucket().getName().equals(bucketName)) {
            print80ln("Object storage configuration", "ko");
            System.err.println("Error creating bucket " + bucketName);
            System.exit(-98);
        }
    }

    private static void createCollections(ADBRESTService rSQLS, AutonomousDatabase adb, String[] collections) {
        print80("Database configuration", "creating dragon collections");
        rSQLS.createSODACollection("dragon");
        print80("Database configuration", "storing dragon information");
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

        for (String collectionName : collections) {
            if (!"dragon".equals(collectionName)) {
                print80("Database configuration", "creating collection " + collectionName);
                rSQLS.createSODACollection(collectionName);
            }
        }
    }

    private static String getDurationSince(long startTime) {
        final long durationMillis = System.currentTimeMillis() - startTime;
        if (durationMillis < 1000) {
            return String.format("0.%03ds", durationMillis);
        } else {
            final Duration duration = Duration.ofMillis(durationMillis);
            return duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").replaceAll("\\.\\d+", "").toLowerCase();
        }
    }

    private static void analyzeCommandLineParameters(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-db":
                    if (i + 1 < args.length) {
                        dbName = args[++i].toUpperCase();
                    } else {
                        print80("Command line parameters", "ko");
                        println();
                        System.err.println("Please provide a valid name for your DRAGON database: -db <database name>");
                        System.exit(-100);
                    }
                    break;

                case "-destroy":
                    destroy = true;
                    break;

                case "-h":
                case "-?":
                case "/?":
                case "/h":
                case "-help":
                case "--help":
                    print80ln("Command line parameters", "ok");
                    println("Usage:");
                    println("\t-db <database name>\t\tdenotes the database name to create");
                    println("\t-destroy           \t\task to destroy the database");
                    System.exit(0);
                    break;
            }
        }
    }

    private static void print80(final String section, final String msg) {
        final int total = section.length() + msg.length() + 2 + 1;
        int spaces = 80 - total + 1;
        final StringBuilder sb = new StringBuilder("> ").append(section);

        for (int i = 0; i < spaces; i++) {
            if (i == 0 || i == spaces - 1) {
                sb.append(' ');
            } else {
                sb.append('.');
            }
        }

        sb.append(msg);

        print(sb.toString());
    }

    private static void print80ln(final String section, final String msg) {
        print80(section, msg);
        println();
    }

    /**
     * Print a carriage return in the terminal.
     */
    private static void println() {
        println("");
    }

    /**
     * Print a message to the terminal including a carriage return.
     *
     * @param msg the message to display
     */
    private static void println(final String msg) {
        System.out.println(msg);
    }

    /**
     * Print a message to the terminal erasing the previous line.
     *
     * @param msg the message to display
     */
    private static void print(final String msg) {
        System.out.print("\r");
        System.out.print(msg);
        System.out.flush();
    }
}
