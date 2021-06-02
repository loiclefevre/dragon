package com.oracle.dragon.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.database.DatabaseClient;
import com.oracle.bmc.database.DatabaseWaiters;
import com.oracle.bmc.database.model.*;
import com.oracle.bmc.database.requests.*;
import com.oracle.bmc.database.responses.*;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.requests.GetUserRequest;
import com.oracle.bmc.identity.responses.GetUserResponse;
import com.oracle.bmc.limits.LimitsClient;
import com.oracle.bmc.limits.requests.GetResourceAvailabilityRequest;
import com.oracle.bmc.limits.responses.GetResourceAvailabilityResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import com.oracle.bmc.workrequests.WorkRequestClient;
import com.oracle.bmc.workrequests.model.WorkRequestError;
import com.oracle.bmc.workrequests.requests.GetWorkRequestRequest;
import com.oracle.bmc.workrequests.requests.ListWorkRequestErrorsRequest;
import com.oracle.bmc.workrequests.responses.GetWorkRequestResponse;
import com.oracle.bmc.workrequests.responses.ListWorkRequestErrorsResponse;
import com.oracle.dragon.model.Keys;
import com.oracle.dragon.model.LocalDragonConfiguration;
import com.oracle.dragon.model.Version;
import com.oracle.dragon.stacks.CodeGenerator;
import com.oracle.dragon.stacks.StackType;
import com.oracle.dragon.util.exception.*;
import com.oracle.dragon.util.io.*;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.*;

import static com.oracle.dragon.DragonStack.displayHowToReportIssue;
import static com.oracle.dragon.util.Console.*;
import static com.oracle.dragon.util.Console.Style.*;

/**
 * DRAGON Stack session.
 */
public class DSSession {

    /**
     * Current version.
     */
    public static final String VERSION = "2.2.0";

    public static final String CONFIGURATION_FILENAME = "dragon.config";
    public static final String LOCAL_CONFIGURATION_FILENAME = "local_dragon.config.json";

    private static final int OCI_ALWAYS_FREE_DATABASE_NUMBER_LIMIT = 2;
    public static final String CONFIG_REGION = "region";
    public static final String CONFIG_FINGERPRINT = "fingerprint";
    private static final String CONFIG_DATABASE_TYPE = "database_type";
    private static final String CONFIG_DATABASE_VERSION = "database_version";
    private static final String CONFIG_DATABASE_USER_NAME = "database_user_name";
    private static final String CONFIG_DATABASE_PASSWORD = "database_password";
    private static final String CONFIG_DATABASE_LICENSE_TYPE = "database_license_type";
    private static final String CONFIG_COLLECTIONS = "database_collections";
    private static final String CONFIG_TABLES = "database_tables";
    public static final String CONFIG_COMPARTMENT_ID = "compartment_id";
    public static final String CONFIG_TENANCY_ID = "tenancy";
    public static final String CONFIG_KEY_FILE = "key_file";
    public static final String CONFIG_PASS_PHRASE = "pass_phrase";
    public static final String CONFIG_USER = "user";
    public static final String CONFIG_AUTH_TOKEN = "auth_token";
    private static final String CONFIG_DATA_PATH = "data_path";

    // Code generation
    private boolean createStack;
    private StackType stackType;
    private String stackName = "frontend";
    private String stackOverride = null;

    private LocalDragonConfiguration localConfiguration;

    public String getCompartmentId() {
        return this.configFile.get(CONFIG_COMPARTMENT_ID);
    }

    public enum Platform {
        Windows,
        Linux,
        MacOS,
        LinuxARM,
        Unsupported
    }

    public enum Operation {
        CreateDatabase,
        DestroyDatabase,
        LoadDataJSON,
        LoadDataCSV,
        UpgradeDragon,
        StopDatabase,
        StartDatabase
    }

    public enum Section {
        CommandLineParameters("Command line parameters"),
        OCIConfiguration("Oracle Cloud Infrastructure configuration"),
        OCIConnection("OCI API endpoints"),
        DatabaseTermination("Database termination"),
        DatabaseCreation("Database creation"),
        DatabaseWalletDownload("Database wallet download"),
        DatabaseConfiguration("Database configuration"),
        ObjectStorageConfiguration("Object storage configuration"),
        LoadDataIntoCollections("JSON data loading"),
        LoadDataIntoTables("CSV data loading"),
        LocalConfiguration("Local configuration"),
        CreateStack("Stack creation"),
        Upgrade("DRAGON upgrade"),
        PostProcessingStack("Stack post processing"),
        CreateKeys("Keys creation"),
        DatabaseShutdown("Database shutdown"),
        DatabaseStart("Database startup"),
        StackEnvironmentValidation("Stack environment validation");

        private final String name;


        Section(String name) {
            this.name = name;
        }

        public void printlnKO() {
            System.out.print(Style.ANSI_RED);
            printBoundedln(name, "ko");
        }

        public void printlnOK() {
            System.out.print(Style.ANSI_BRIGHT_GREEN);
            printBoundedln(name, "ok");
        }

        public void print(String msg) {
            //System.out.print(Style.ANSI_BLUE_BACKGROUND);
            printBounded(name, msg);
        }

        public void printlnKO(String msg) {
            System.out.print(Style.ANSI_RED);
            printBoundedln(name, String.format("ko [%s]", msg));
        }

        public void printlnOK(String msg) {
            System.out.print(Style.ANSI_BRIGHT_GREEN);
            printBoundedln(name, String.format("ok [%s]", msg));
        }
    }

    public static final Platform platform;
    public static final boolean OCICloudShell;
    public static boolean vscode;

    private Section section;
    private DRAGONConfigFile.ConfigFile configFile;
    private AuthenticationDetailsProvider provider;
    private DatabaseClient dbClient;
    private WorkRequestClient workRequestClient;
    private ObjectStorageClient objectStorageClient;
    private IdentityClient identityClient;
    private LimitsClient limitsClient;

    private String databaseUserName = "dragon";

    /**
     * The database name to create.
     */
    private String dbName = "DRAGON";

    private String dbVersion = "19c";

    private String profileName = "DEFAULT";

    /**
     * The OCI region to manage.
     */
    private String region = "";

    private Operation operation = Operation.CreateDatabase;

    enum DatabaseType {
        AJDFree(true),
        ATPFree(true),
        ADWFree(true),
        ApexFree(true),
        AJD(false),
        ATP(false),
        ADW(false),
        Apex(false);

        private final boolean free;

        DatabaseType(boolean free) {
            this.free = free;
        }

        public boolean isFree() {
            return free;
        }
    }

    private DatabaseType databaseType = DatabaseType.ATPFree;

    enum LicenseType {
        LicenseIncluded,
        BYOL
    }

    private LicenseType licenseType = LicenseType.LicenseIncluded;

    /**
     * Load data into collections.
     */
    private boolean loadJSON = false;

    /**
     * Load data into tables.
     */
    private boolean loadCSV = false;

    /**
     * Display information about region, compartment, user...
     */
    private boolean info = false;

    private File dataPath = new File(".");

    private File workingDirectory = new File(".");

    public static final String EXECUTABLE_NAME;

    static {
        final ProcessHandle processHandle = ProcessHandle.current();
        EXECUTABLE_NAME = processHandle.info().command().get();

        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("windows")) {
            platform = Platform.Windows;
            OCICloudShell = false;

            try {

                final String shell = processHandle.parent().get().info().command().get();
                vscode = processHandle.parent().get().parent().get().parent().get().info().command().get().toLowerCase().endsWith("code.exe");
            } catch (NoSuchElementException ignored) {
                //ignored.printStackTrace();
            }
        } else if (osName.startsWith("linux")) {
            System.setProperty("java.awt.headless", "true");

            final String osArchitecture = System.getProperty("os.arch").toLowerCase();

            if (osArchitecture.equals("aarch64")) {
                platform = Platform.LinuxARM;

                OCICloudShell = false;
            } else {
                platform = Platform.Linux;

                if (System.getenv("CLOUD_SHELL_TOOL_SET") != null && System.getenv("OCI_REGION") != null && System.getenv("OCI_TENANCY") != null) {
                    OCICloudShell = true;
                } else {
                    OCICloudShell = false;
                }
            }
        } else if (osName.startsWith("mac os")) {
            platform = Platform.MacOS;
            System.setProperty("java.awt.headless", "true");
            OCICloudShell = false;
        } else {
            platform = Platform.Unsupported;
            OCICloudShell = false;
        }
    }

    private static void banner() {
        if (ENABLE_COLORS && platform == Platform.Windows && !vscode) {
            printGradient(new Console.Color(199, 52, 46), new Console.Color(255, 255, 0), String.format("DRAGON Stack manager v%s", VERSION), true, true);
        } else {
            print(String.format("%sDRAGON Stack manager v%s", Style.ANSI_TITLE, VERSION));
        }
        println();
        println();
    }

    public DSSession() throws UnsupportedPlatformException {
        banner();

        if (platform == Platform.Unsupported) {
            throw new UnsupportedPlatformException(System.getProperty("os.name"));
        }
    }

    public String getProfileName() {
        return profileName;
    }

    public void analyzeCommandLineParameters(String[] args) throws DSException {
        section = Section.CommandLineParameters;
        section.print("analyzing");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();

            boolean hasAnchor = false;
            String anchor = null;

            if (arg.startsWith("-") && arg.contains("#")) {
                hasAnchor = true;
                anchor = arg.substring(arg.indexOf('#') + 1);
                arg = arg.substring(0, arg.indexOf('#'));
            }

            switch (arg) {
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
                    if (localConfiguration != null) {
                        if (operation == Operation.CreateDatabase) {
                            operation = Operation.DestroyDatabase;
                        } else {
                            section.printlnKO("conflicting command: " + arg);
                            displayUsage();
                            System.exit(-9998);
                        }
                    }
                    break;

                case "-loadjson":
                case "--loadjson":
                    loadJSON = true;
                    if (localConfiguration != null) {
                        if (operation == Operation.CreateDatabase) {
                            operation = Operation.LoadDataJSON;
                        } else {
                            section.printlnKO("conflicting command: " + arg);
                            displayUsage();
                            System.exit(-9999);
                        }
                    }
                    break;

                case "-loadcsv":
                case "--loadcsv":
                    loadCSV = true;
                    if (localConfiguration != null) {
                        if (operation == Operation.CreateDatabase) {
                            operation = Operation.LoadDataCSV;
                        } else {
                            section.printlnKO("conflicting command: " + arg);
                            displayUsage();
                            System.exit(-9999);
                        }
                    }
                    break;

                case "-stop-db":
                case "--stop-db":
                    if (localConfiguration != null) {
                        if (operation == Operation.CreateDatabase) {
                            operation = Operation.StopDatabase;
                        } else {
                            section.printlnKO("conflicting command: " + arg);
                            displayUsage();
                            System.exit(-9999);
                        }
                    }
                    break;

                case "-start-db":
                case "--start-db":
                    if (localConfiguration != null) {
                        if (operation == Operation.CreateDatabase) {
                            operation = Operation.StartDatabase;
                        } else {
                            section.printlnKO("conflicting command: " + arg);
                            displayUsage();
                            System.exit(-9999);
                        }
                    }
                    break;

                case "-info":
                case "--info":
                    info = true;
                    break;

                case "-create-keys":
                case "--create-keys":
                case "-ck":
                case "--ck":
                    break;

                case "-config-template":
                case "--config-template":
                case "-ct":
                case "--ct":
                    section.printlnOK();
                    final boolean hasToCreateKeys = checkForArgument(args, new String[]{"-create-keys", "--create-keys", "-ck", "--ck"});
                    printlnConfigurationTemplate(hasToCreateKeys, Section.CreateKeys);
                    System.exit(0);
                    break;

                case "-create-react-app":
                case "--create-react-app":
                case "-cra":
                case "--cra":
                    createStack = true;
                    stackType = StackType.REACT;
                    if (hasAnchor) {
                        stackOverride = anchor;
                    }

                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        i++;
                        stackName = args[i];
                    }
                    break;

                case "-create-micro-service":
                case "--create-micro-service":
                case "-cms":
                case "--cms":
                    createStack = true;
                    stackType = StackType.MICRO_SERVICE;
                    stackName = "backend";

                    if (hasAnchor) {
                        stackOverride = anchor;
                    }

                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        i++;
                        stackName = args[i];
                    }
                    break;

                case "-create-jet-app":
                case "--create-jet-app":
                case "-cja":
                case "--cja":
                    createStack = true;
                    stackType = StackType.JET;
                    if (hasAnchor) {
                        stackOverride = anchor;
                    }

                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        i++;
                        stackName = args[i];
                    }
                    break;

                case "-create-spring-boot-petclinic":
                case "--create-spring-boot-petclinic":
                case "-csbp":
                case "--csbp":
                    createStack = true;
                    stackType = StackType.SPRINGBOOTPETCLINIC;
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        i++;
                        stackName = args[i];
                    } else {
                        stackName = "petclinic";
                    }
                    break;

                case "-upgrade":
                case "--upgrade":
                    operation = Operation.UpgradeDragon;
                    break;

                case "-h":
                case "--h":
                case "-?":
                case "--?":
                case "/?":
                case "/h":
                case "-help":
                case "--help":
                    section.printlnOK();
                    displayUsage();
                    System.exit(0);
                    break;

                default:
                    section.printlnKO("bad command: " + arg);
                    displayUsage();
                    System.exit(-10000);
            }
        }
        section.printlnOK();
    }

    private boolean checkForArgument(String[] args, String[] possibleValues) {
        for (String arg : args) {
            for (String possibleArgument : possibleValues) {
                if (arg.toLowerCase().equals(possibleArgument)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void displayUsage() {
        println(ANSI_UNDERLINE + "Usage:");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "config" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "template" + ANSI_RESET + "                    \tdisplays a configuration file template");
        println(ANSI_VSC_DASH + "  -" + ANSI_VSC_BLUE + "create" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "keys" + ANSI_RESET + "                      \tcreate the user's OCI API Key pair (use with -config-template)");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "profile" + ANSI_RESET + " <profile name>             \tto choose the given profile name from " + CONFIGURATION_FILENAME + " (default profile name: DEFAULT)");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "db" + ANSI_RESET + " <database name>                 \tto denote the database name to create or destroy");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "loadjson" + ANSI_RESET + "                           \tloads " + ANSI_BRIGHT + "{JSON}" + ANSI_RESET + " data corresponding to collections (default: no data loaded)");
        println("                                    \t . use with configuration parameters database_collections and data_path");
        println("                                    \t . loading JSON data can be done during and/or after database provisioning");
        println("                                    \t . JSON file names must match <collection name>[_[0-9]+].json");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "loadcsv" + ANSI_RESET + "                            \tloads " + ANSI_BRIGHT + "CSV" + ANSI_RESET + " data corresponding to tables (default: no data loaded)");
        println("                                    \t . use with configuration parameters database_tables and data_path");
        println("                                    \t . loading CSV data can be done during and/or after database provisioning");
        println("                                    \t . CSV file names must match <table name>[_[0-9]+].csv");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "create" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "react" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "app" + ANSI_RESET + " [name]            \tcreates a " + ANSI_VSC_BLUE + "React" + ANSI_RESET + " frontend (default name: frontend, overrides supported)");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "create" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "jet" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "app" + ANSI_RESET + " [name]                \tcreates an " + ANSI_BRIGHT_RED + "Oracle JET" + ANSI_RESET + " frontend (default name: frontend)");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "create" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "spring" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "boot" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "petclinic" + ANSI_RESET + " [name]\tcreates the " + ANSI_BRIGHT_GREEN + "Spring Boot" + ANSI_RESET + " Petclinic (default name: petclinic)");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "create" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "micro" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "service" + ANSI_VSC_DASH + ANSI_RESET + " [name]        \tcreates a " + ANSI_BRIGHT_WHITE + "Microservice" + ANSI_RESET + " (default name: backend, overrides supported)");
        println("                                    \t . If supported, overrides default stack using #<extension name>, examples:");
        println("                                    \t   . -create-react-app#lab2");
        println("                                    \t   . -create-micro-service#json-po-generator <name>");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "stop" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "db" + ANSI_RESET + "                            \t" + ANSI_RED + "stops" + ANSI_RESET + " the database");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "start" + ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "db" + ANSI_RESET + "                           \t" + ANSI_BRIGHT_GREEN + "starts" + ANSI_RESET + " the database");
        println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "destroy" + ANSI_RESET + "                            \tto destroy the database");

        try {
            final String latestVersion = upgradeOrGetLastVersion(false);
            if (!VERSION.equals(latestVersion) && Version.isAboveVersion(latestVersion, VERSION)) {
                println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "upgrade" + ANSI_RESET + "                            \tto download the " + ANSI_BRIGHT + "latest version for your platform: v" + latestVersion + ANSI_RESET);
            } else {
                println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "upgrade" + ANSI_RESET + "                            \tto download the latest version for your platform... but you're already up to date :)");
            }
        } catch (DSException dse) {
            println(ANSI_VSC_DASH + "-" + ANSI_VSC_BLUE + "upgrade" + ANSI_RESET + "                            \tto download the latest version for your platform... (if available)");
        }

        displayHowToReportIssue();
    }

    public static void printlnConfigurationTemplate(final boolean hasToCreateKeys, final Section section) {
        Keys keys = null;
        if (hasToCreateKeys) {
            println("Entering keys generation process...");
            println("These keys (public and private) will be used for future connection to Oracle Cloud Infrastructure API endpoints.");
            String passPhrase = null;
            while (passPhrase == null || passPhrase.trim().length() == 0) {
                print("Please enter a passphrase: ");
                try {
                    System.in.reset();
                } catch (IOException ignored) {
                }
                passPhrase = new Scanner(System.in).next();
            }

            section.print("pending");

            try {
                // Key generation will work solely in the case of a native image!
                keys = new KeysUtil().createKeys(passPhrase);
                section.printlnOK("Upload the Public Key");
                println("Please upload this " + ANSI_YELLOW + "public" + ANSI_RESET + " key to your Oracle Cloud Infrastructure user's API Keys:");
                println();
                println(keys.publicKeyContent);
                println("(instructions: " + ANSI_UNDERLINE + "https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#three" + ANSI_RESET + ")");
                println("- public key saved in file: " + ANSI_BRIGHT + keys.publicKey.getAbsolutePath());
                println("- private key saved in file: " + ANSI_BRIGHT + keys.privateKey.getAbsolutePath());
                println();
            } catch (Exception e) {
                section.printlnKO();
                //e.printStackTrace();
            }
        }

        println("Configuration template (save the content in a file named " + ANSI_YELLOW + "\"" + CONFIGURATION_FILENAME + "\"" + ANSI_RESET + "):");
        println("---" + ANSI_YELLOW + "8<" + ANSI_RESET + "-----------------------------------------------------------------------------------------");
        println(" # DEFAULT profile " + ANSI_UNDERLINE + "(case sensitive)" + ANSI_RESET + ", you can define others: ASHBURN_REGION or TEST_ENVIRONMENT");
        println(" # You can choose a profile using the -profile command line argument");
        println(" # This configuration file must have at least one profile named DEFAULT");
        println(" # WARNING: any property not defined inside the selected profile will use the one from the DEFAULT profile");
        println(" #          if found, hence the name of the profile: DEFAULT :)");
        println("[DEFAULT]");
        println();
        println(" # OCID of the user connecting to Oracle Cloud Infrastructure APIs. To get the value, see:");
        println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#five");
        println("user=ocid1.user.oc1..<unique_ID>");
        println();
        println(" # Full path and filename of the SSH private key (use *solely* forward slashes).");
        println(" # /!\\ Warning: The key pair must be in PEM format (2048 bits). For instructions on generating a key pair in PEM format, see:");
        println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#Required_Keys_and_OCIDs");
        if (keys != null) {
            println("key_file=" + keys.privateKey.getAbsolutePath().replace('\\', '/'));
        } else {
            println("key_file=<full path to SSH private key file>");
        }
        println();
        println(" # Uncomment in the case your SSH private key needs a pass phrase.");
        if (keys != null) {
            println("pass_phrase=" + keys.passPhrase);
        } else {
            println("# pass_phrase=<pass phrase to use with your SSH private key>");
        }
        println();
        println(" # Fingerprint for the SSH *public* key that was added to the user mentioned above. To get the value, see:");
        println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#four");
        if (keys != null) {
            println("fingerprint=" + keys.fingerprint);
        } else {
            println("fingerprint=<fingerprint associated with the corresponding SSH *public* key>");
        }
        println();
        println(" # OCID of your tenancy. To get the value, see:");
        println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#five");
        println("tenancy=ocid1.tenancy.oc1..<unique_ID>");
        println();
        println(" # An Oracle Cloud Infrastructure region identifier. For a list of possible region identifiers, check here:");
        println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm#top");
        println("region=eu-frankfurt-1");
        println();
        println(" # OCID of the compartment to use for resources creation. to get more information about compartments, see:");
        println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/Identity/Tasks/managingcompartments.htm?Highlight=compartment%20ocid#Managing_Compartments");
        println("compartment_id=ocid1.compartment.oc1..<unique_ID>");
        println();
        println(" # Authentication token that will be used for OCI Object Storage configuration, see:");
        println(" # https://docs.cloud.oracle.com/en-us/iaas/Content/Registry/Tasks/registrygettingauthtoken.htm?Highlight=user%20auth%20tokens");
        println("auth_token=<authentication token>");
        println();
        println(" # Autonomous Database Types:");
        println(" # - atpfree : Always Free Autonomous Transaction Processing (default)");
        println(" # - ajdfree : Always Free Autonomous JSON Database");
        println(" # - apexfree: Always Free Autonomous Application Express");
        println(" # - adwfree : Always Free Autonomous Data Warehouse");
        println(" # - atp     : Autonomous Transaction Processing");
        println(" # - ajd     : Autonomous JSON Database");
        println(" # - apex    : Autonomous Application Express");
        println(" # - adw     : Autonomous Data Warehouse");
        println("# database_type=atpfree");
        println();
        println(" # Uncomment to specify another database user name than dragon (default)");
        println("# database_user_name=<your database user name>");
        println();
        println(" # The database password used for database creation and dragon user");
        println(" # - 12 chars minimum and 30 chars maximum");
        println(" # - can't contain the database user name word");
        println(" # - contains 1 digit minimum");
        println(" # - contains 1 lower case char");
        println(" # - contains 1 upper case char");
        println("database_password=<database password>");
        println();
        println(" # Uncomment to ask for Bring Your Own Licenses model (doesn't work for Always Free and AJD)");
        println("# database_license_type=byol");
        println();
        println(" # Path to a folder where data to load into collections can be found (default to current directory)");
        println("data_path=.");
        println();
        println(" # A list of coma separated JSON collection name(s) that you wish to get right after database creation");
        println("# database_collections=");
        println();
        println(" # A list of coma separated table name(s) that you wish to get right after database creation");
        println(" # These table must have corresponding CSV file(s) so that table structure (DDL) is deduced from the files");
        println("# database_tables=");
        println();
        println();
    }

    public void loadLocalConfiguration(boolean displaySection) throws DSException {
        File localConfigurationFile = new File(LOCAL_CONFIGURATION_FILENAME);

        if (localConfigurationFile.exists() && localConfigurationFile.isFile()) {
            if (displaySection) {
                section = Section.LocalConfiguration;
                section.print("parsing");
            }

            final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                localConfiguration = mapper.readValue(localConfigurationFile, LocalDragonConfiguration.class);
            } catch (IOException e) {
                throw new LoadLocalConfigurationException(LOCAL_CONFIGURATION_FILENAME, e);
            }

            if (!Strings.isNullOrEmpty(localConfiguration.getRedirect())) {
                workingDirectory = new File(localConfiguration.getRedirect());

                localConfigurationFile = new File(localConfiguration.getRedirect(), LOCAL_CONFIGURATION_FILENAME);

                if (localConfigurationFile.exists() && localConfigurationFile.isFile()) {
                    try {
                        localConfiguration = mapper.readValue(localConfigurationFile, LocalDragonConfiguration.class);
                    } catch (IOException e) {
                        throw new LoadLocalConfigurationException(LOCAL_CONFIGURATION_FILENAME, e);
                    }
                }
            }

            if (displaySection) {
                section.printlnOK();
            }
        }
    }

    /**
     * Loads the dragon.config file and analyze the parameters referenced for a given profile.
     *
     * @throws DSException in case of problems (unknown parameter, wrong value, missing mandatory parameter...)
     */
    public void loadConfigurationFile() throws DSException {
        section = Section.OCIConfiguration;
        section.print("parsing");

        try {
            this.configFile = DRAGONConfigFile.parse(workingDirectory, CONFIGURATION_FILENAME, profileName);

            for (String key : this.configFile.getAllKeys()) {
                switch (key) {
                    case CONFIG_REGION:
                    case CONFIG_KEY_FILE:
                    case CONFIG_TENANCY_ID:
                    case CONFIG_COMPARTMENT_ID:
                    case CONFIG_DATABASE_PASSWORD:
                    case CONFIG_USER:
                    case CONFIG_AUTH_TOKEN:
                    case CONFIG_FINGERPRINT:
                    case CONFIG_DATABASE_USER_NAME:
                    case CONFIG_DATABASE_LICENSE_TYPE:
                    case CONFIG_DATABASE_TYPE:
                    case CONFIG_DATA_PATH:
                    case CONFIG_COLLECTIONS:
                    case CONFIG_TABLES:
                    case CONFIG_PASS_PHRASE:
                    case CONFIG_DATABASE_VERSION:
                        break;

                    default:
                        section.printlnKO();
                        throw new ConfigurationUnsupportedParameterException(key, profileName);
                }
            }

            if (Strings.isNullOrEmpty(region = this.configFile.get(CONFIG_REGION))) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_REGION);
            }

            region = region.toUpperCase().replaceAll("-", "_");

            String keyFilename = this.configFile.get(CONFIG_KEY_FILE);
            if (Strings.isNullOrEmpty(keyFilename)) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_KEY_FILE);
            } else {
                final File keyFile = new File(keyFilename);
                if (!keyFile.exists()) {
                    section.printlnKO();
                    throw new ConfigurationMissingKeyFileException(CONFIG_KEY_FILE, keyFilename);
                }
                if (!keyFile.isFile()) {
                    section.printlnKO();
                    throw new ConfigurationKeyFileNotAFileException(CONFIG_KEY_FILE, keyFilename);
                }
            }

            if (Strings.isNullOrEmpty(this.configFile.get(CONFIG_TENANCY_ID))) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_TENANCY_ID);
            }
            if (Strings.isNullOrEmpty(this.configFile.get(CONFIG_COMPARTMENT_ID))) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_COMPARTMENT_ID);
            }
            if (Strings.isNullOrEmpty(this.configFile.get(CONFIG_DATABASE_PASSWORD))) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_DATABASE_PASSWORD);
            }
            if (Strings.isNullOrEmpty(this.configFile.get(CONFIG_USER))) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_USER);
            }
            if (Strings.isNullOrEmpty(this.configFile.get(CONFIG_AUTH_TOKEN))) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_AUTH_TOKEN);
            }

            final String fingerprintValue = this.configFile.get(CONFIG_FINGERPRINT);
            if (Strings.isNullOrEmpty(fingerprintValue)) {
                section.printlnKO();
                throw new ConfigurationMissesParameterException(CONFIG_FINGERPRINT);
            } else {
                if (fingerprintValue.length() != 47) {
                    section.printlnKO();
                    throw new ConfigurationBadFingerprintParameterException(CONFIG_FINGERPRINT, CONFIGURATION_FILENAME, fingerprintValue);
                }
            }

            // Optional config file parameters
            if (!Strings.isNullOrEmpty(this.configFile.get(CONFIG_DATABASE_USER_NAME))) {
                databaseUserName = this.configFile.get(CONFIG_DATABASE_USER_NAME);
            }

            if (!Strings.isNullOrEmpty(this.configFile.get(CONFIG_DATABASE_VERSION))) {
                final String wantedVersion = this.configFile.get(CONFIG_DATABASE_VERSION);
                switch (wantedVersion) {
                    case "19c":
                    case "21c":
                        dbVersion = this.configFile.get(CONFIG_DATABASE_VERSION);
                        break;

                    default:
                        section.printlnKO();
                        throw new ConfigurationWrongDatabaseVersionException(this.configFile.get(CONFIG_DATABASE_VERSION));
                }
            }


            if (!Strings.isNullOrEmpty(this.configFile.get(CONFIG_DATABASE_LICENSE_TYPE))) {
                if (LicenseType.BYOL.toString().equalsIgnoreCase(this.configFile.get(CONFIG_DATABASE_LICENSE_TYPE))) {
                    licenseType = LicenseType.BYOL;
                } else {
                    section.printlnKO();
                    throw new ConfigurationWrongDatabaseLicenseTypeException(this.configFile.get(CONFIG_DATABASE_LICENSE_TYPE));
                }
            } else {
                licenseType = LicenseType.LicenseIncluded;
            }

            if (!Strings.isNullOrEmpty(this.configFile.get(CONFIG_DATABASE_TYPE))) {
                if (DatabaseType.AJD.toString().equalsIgnoreCase(this.configFile.get(CONFIG_DATABASE_TYPE))) {
                    databaseType = DatabaseType.AJD;
                } else if (DatabaseType.ATP.toString().equalsIgnoreCase(this.configFile.get(CONFIG_DATABASE_TYPE))) {
                    databaseType = DatabaseType.ATP;
                } else if (DatabaseType.Apex.toString().equalsIgnoreCase(this.configFile.get(CONFIG_DATABASE_TYPE))) {
                    databaseType = DatabaseType.Apex;
                } else if (DatabaseType.ADW.toString().equalsIgnoreCase(this.configFile.get(CONFIG_DATABASE_TYPE))) {
                    databaseType = DatabaseType.ADW;
                } else if (DatabaseType.AJDFree.toString().equalsIgnoreCase(this.configFile.get(CONFIG_DATABASE_TYPE))) {
                    databaseType = DatabaseType.AJDFree;
                } else if (DatabaseType.ApexFree.toString().equalsIgnoreCase(this.configFile.get(CONFIG_DATABASE_TYPE))) {
                    databaseType = DatabaseType.ApexFree;
                } else if (DatabaseType.ATPFree.toString().equalsIgnoreCase(this.configFile.get(CONFIG_DATABASE_TYPE))) {
                    databaseType = DatabaseType.ATPFree;
                } else if (DatabaseType.ADWFree.toString().equalsIgnoreCase(this.configFile.get(CONFIG_DATABASE_TYPE))) {
                    databaseType = DatabaseType.ADWFree;
                } else {
                    section.printlnKO();
                    throw new ConfigurationWrongDatabaseTypeException(this.configFile.get(CONFIG_DATABASE_TYPE));
                }
            }

            if (loadJSON || loadCSV) {
                if (!Strings.isNullOrEmpty(this.configFile.get(CONFIG_DATA_PATH))) {
                    final File tempPath = new File(this.configFile.get(CONFIG_DATA_PATH));

                    if (!tempPath.exists()) {
                        section.printlnKO();
                        throw new ConfigurationDataPathNotFoundException(this.configFile.get(CONFIG_DATA_PATH));
                    }

                    if (!tempPath.isDirectory()) {
                        section.printlnKO();
                        throw new ConfigurationDataPathDirectoryException(this.configFile.get(CONFIG_DATA_PATH));
                    }

                    dataPath = tempPath;
                }
            }

        } catch (java.io.FileNotFoundException fnfe) {
            section.printlnKO();
            throw new ConfigurationFileNotFoundException();
        } catch (IOException ioe) {
            section.printlnKO();
            throw new ConfigurationLoadException(ioe);
        } catch (IllegalArgumentException iae) {
            if (iae.getMessage().startsWith("No profile named")) {
                section.printlnKO("profile " + profileName + " not found");
                throw new ConfigurationProfileNotFoundException(profileName);
            }

            throw new ConfigurationParsingException(iae);
        }

        section.printlnOK();
    }

    private void initializeClients() throws DSException {
        try {
            section = Section.OCIConnection;
            section.print("authentication pending");
            provider = new DRAGONConfigFileAuthenticationDetailsProvider(configFile.getConfigurationFilePath(), configFile.getProfile(), configFile);

            section.print("database pending");

            dbClient = new DatabaseClient(provider);
            dbClient.setRegion(region);

            limitsClient = new LimitsClient(provider);
            limitsClient.setRegion(region);

			/*
		ListLimitDefinitionsRequest listLimitDefinitionsRequest =
				ListLimitDefinitionsRequest.builder()
						.compartmentId(configFile.get(CONFIG_TENANCY_ID))
						.build();

		ListLimitDefinitionsResponse listLimitDefinitionsResponse =
				client.listLimitDefinitions(listLimitDefinitionsRequest);
		for (LimitDefinitionSummary summary : listLimitDefinitionsResponse.getItems()) {
			System.out.println("Service Name: " + summary.getServiceName());
			System.out.println("Limit Name: " + summary.getName());
			System.out.println("Limit Description: " + summary.getDescription());
		}*/

        } catch (IOException ioe) {
            section.printlnKO();
            throw new ConfigurationLoadException(ioe);
        } catch (IllegalArgumentException iae) {
            if (iae.getMessage().startsWith("Could not find private key")) {
                section.printlnKO("private key not found");
                iae.printStackTrace();
                throw new OCIAPIAuthenticationPrivateKeyNotFoundException(configFile.get(CONFIG_KEY_FILE));
            }

            throw new OCIAPIDatabaseException(iae);
        }
    }

    public void work() throws DSException {
        switch (operation) {
            case CreateDatabase:
                // prevent creating a new database if one already exists!
                // TODO: check if the one inside the local config do really exists...
                if (localConfiguration == null) {
                    initializeClients();
                    createADB();
                } else if (!createStack) {
                    throw new DatabaseAlreadyDeployedException(localConfiguration.getDbName());
                }
                break;

            case DestroyDatabase:
                // prevent to destroy any database that weren't created using DRAGON
                if (localConfiguration != null && localConfiguration.getDbName().equals(dbName)) {
                    initializeClients();
                    destroyDatabase();
                } else {
                    if (localConfiguration == null) {
                        throw new UnmanagedDatabaseCantBeDestroyedException();
                    } else {
                        throw new UnmanagedDatabaseCantBeDestroyedException(dbName);
                    }
                }
                break;

            case LoadDataJSON:
                if (localConfiguration != null && localConfiguration.getDbName().equals(dbName)) {
                    initializeClients();
                    loadDataIntoCollections();
                }
                break;

            case LoadDataCSV:
                if (localConfiguration != null && localConfiguration.getDbName().equals(dbName)) {
                    initializeClients();
                    loadDataIntoTables();
                }
                break;

            case UpgradeDragon:
                upgradeOrGetLastVersion(true);
                break;

            case StopDatabase:
                if (localConfiguration != null && localConfiguration.getDbName().equals(dbName)) {
                    initializeClients();
                    stopDatabase();
                }
                break;

            case StartDatabase:
                if (localConfiguration != null && localConfiguration.getDbName().equals(dbName)) {
                    initializeClients();
                    startDatabase();
                }
                break;
        }

        if ((operation == Operation.CreateDatabase || operation == Operation.LoadDataJSON) && createStack) {
            final CodeGenerator c = new CodeGenerator(stackType, stackName, stackOverride, localConfiguration, profileName, this.configFile.getConfigFilename());
            c.work();
        }
    }

    private String upgradeOrGetLastVersion(boolean proceedUpgrade) throws DSException {
        section = Section.Upgrade;
        if (proceedUpgrade) {
            section.print("gathering metadata");
        }

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .timeout(Duration.ofSeconds(proceedUpgrade ? 10L : 2L))
                    .uri(new URI("https://github.com/loiclefevre/dragon/releases/latest"))
                    .setHeader("Pragma", "no-cache")
                    .setHeader("Cache-Control", "no-store")
                    .GET()
                    .build();

            final CookieManager cm = new CookieManager();
            CookieHandler.setDefault(cm);

            final HttpResponse<String> response = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.getDefault())
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .cookieHandler(CookieHandler.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                if (proceedUpgrade) {
                    section.printlnKO();
                }
                throw new UpgradeFailedException(response.statusCode());
            }

            final String page = response.body();

            final String tagPattern = "<a href=\"/loiclefevre/dragon/releases/tag/";
            final int tagStart = page.indexOf(tagPattern);

            if (tagStart != -1) {
                String latestVersion = page.substring(tagStart + tagPattern.length(), page.indexOf("\">", tagStart + tagPattern.length()));

                if (latestVersion.startsWith("v")) {
                    latestVersion = latestVersion.substring(1);
                }

                if (!proceedUpgrade) {
                    return latestVersion;
                }

                // a newer version available?
                if (!VERSION.equals(latestVersion) && Version.isAboveVersion(latestVersion, VERSION)) {
                    section.print("downloading v" + latestVersion + " ...");

                    int searchFromPos = tagStart;
                    final String downloadLinkPattern = "<a href=\"/loiclefevre/dragon/releases/download/v" + latestVersion + "/dragon-";
                    boolean downloaded = false;
                    String link = "";

                    while (!downloaded) {
                        int downloadLinkStartPos = page.indexOf(downloadLinkPattern, searchFromPos);

                        if (downloadLinkStartPos == -1) {
                            break;
                        }

                        link = page.substring(downloadLinkStartPos + "<a href=\"".length(), page.indexOf("\"", downloadLinkStartPos + downloadLinkPattern.length()));

                        switch (platform) {
                            case Windows:
                                if (link.contains("windows")) {
                                    downloaded = downloadRelease(link);
                                }
                                break;

                            case Linux:
                                if (link.contains("linux-x86_64")) {
                                    downloaded = downloadRelease(link);
                                }
                                break;

                            case LinuxARM:
                                if (link.contains("linux-aarch_64")) {
                                    downloaded = downloadRelease(link);
                                }
                                break;

                            case MacOS:
                                if (link.contains("osx")) {
                                    downloaded = downloadRelease(link);
                                }
                                break;
                        }

                        searchFromPos = downloadLinkStartPos + downloadLinkPattern.length();
                    }

                    if (downloaded) {
                        final String fileName = link.substring(link.lastIndexOf('/') + 1);

                        if (platform == Platform.Linux || platform == Platform.LinuxARM || platform == Platform.MacOS) {
                            final File release = new File(".", fileName);

                            // make it executable!
                            final Set<PosixFilePermission> perms = new HashSet<>();
                            perms.add(PosixFilePermission.OWNER_READ);
                            perms.add(PosixFilePermission.OWNER_WRITE);
                            perms.add(PosixFilePermission.OWNER_EXECUTE);

                            Files.setPosixFilePermissions(release.toPath(), perms);
                        }

                        if (proceedUpgrade) {
                            section.printlnOK(fileName);
                        }
                    } else {
                        if (proceedUpgrade) {
                            section.printlnKO("no new release for your platform");
                        }
                    }
                } else {
                    if (proceedUpgrade) {
                        section.printlnOK("you are up to date :)");
                    }
                }

                return latestVersion;
            } else {
                if (proceedUpgrade) {
                    section.printlnKO();
                }
                throw new UpgradeFailedException("metadata integrity");
            }
        } catch (InterruptedException e) {
            if (proceedUpgrade) {
                section.printlnKO();
            }
            throw new UpgradeTimeoutException(10);
        } catch (URISyntaxException e) {
            if (proceedUpgrade) {
                section.printlnKO();
            }
            throw new UpgradeFailedException(e);
        } catch (IOException e) {
            if (proceedUpgrade) {
                section.printlnKO();
            }
            throw new UpgradeFailedException(e);
        }
    }

    private boolean downloadRelease(String link) throws URISyntaxException, IOException, InterruptedException, UpgradeFailedException {
        final HttpRequest requestDownload = HttpRequest.newBuilder()
                .uri(new URI("https://github.com" + link))
                .setHeader("Pragma", "no-cache")
                .setHeader("Cache-Control", "no-store")
                .GET()
                .build();

        final HttpResponse<Path> responseDownload = HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .proxy(ProxySelector.getDefault())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(CookieHandler.getDefault())
                .build()
                .send(requestDownload, HttpResponse.BodyHandlers.ofFileDownload(new File(".").toPath(), new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE}));

        if (responseDownload.statusCode() != 200) {
            section.printlnKO();
            throw new UpgradeFailedException(link, responseDownload.statusCode());
        }

        return true;
    }


    private void stopDatabase() throws DSException {
        section = Section.DatabaseShutdown;
        section.print("checking existing databases");

        try {
            final ListAutonomousDatabasesRequest listADB = ListAutonomousDatabasesRequest.builder().compartmentId(configFile.get(CONFIG_COMPARTMENT_ID)).build();
            final ListAutonomousDatabasesResponse listADBResponse = dbClient.listAutonomousDatabases(listADB);

            boolean dbNameExists = false;
            String adbId = null;
            AutonomousDatabaseSummary.LifecycleState currentLifecycleState = null;
            for (AutonomousDatabaseSummary adb : listADBResponse.getItems()) {
                //System.out.println(adb.getLifecycleState()+", "+adb.getIsFreeTier()+", "+dbName);

                if (adb.getLifecycleState() != AutonomousDatabaseSummary.LifecycleState.Terminated) {
                    if (adb.getDbName().equals(dbName)) {
                        if (databaseType.isFree() && !adb.getIsFreeTier()) {
                            continue;
                        }
                        dbNameExists = true;
                        adbId = adb.getId();
                        currentLifecycleState = adb.getLifecycleState();
                        break;
                    }
                }
            }

            if (!dbNameExists) {
                section.printlnOK("nothing to do");
            } else {
                section.print("pending");

                if (currentLifecycleState == AutonomousDatabaseSummary.LifecycleState.Stopped) {
                    section.printlnOK("already stopped");
                    return;
                }

                workRequestClient = new WorkRequestClient(provider);
                StopAutonomousDatabaseResponse responseTerminate = dbClient.stopAutonomousDatabase(StopAutonomousDatabaseRequest.builder().autonomousDatabaseId(adbId).build());
                String workRequestId = responseTerminate.getOpcWorkRequestId();

                GetWorkRequestRequest getWorkRequestRequest = GetWorkRequestRequest.builder().workRequestId(workRequestId).build();
                boolean exit = false;
                long startTime = System.currentTimeMillis();
                float pendingProgressMove = 0f;
                do {
                    GetWorkRequestResponse getWorkRequestResponse = workRequestClient.getWorkRequest(getWorkRequestRequest);
                    switch (getWorkRequestResponse.getWorkRequest().getStatus()) {
                        case Succeeded:
                            section.printlnOK(getDurationSince(startTime));
                            exit = true;
                            break;
                        case Failed:
                            section.printlnKO();
                            final ListWorkRequestErrorsResponse response = workRequestClient.listWorkRequestErrors(ListWorkRequestErrorsRequest.builder().workRequestId(workRequestId).opcRequestId(getWorkRequestResponse.getOpcRequestId()).build());
                            final StringBuilder errors = new StringBuilder();
                            int i = 0;
                            for (WorkRequestError e : response.getItems()) {
                                if (i > 0) {
                                    errors.append("\n");
                                }
                                errors.append(e.getMessage());
                                i++;
                            }
                            throw new OCIDatabaseShutdownFailedException(dbName, errors.toString());
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
                            new AutonomousDatabase.LifecycleState[]{AutonomousDatabase.LifecycleState.Stopped}).execute();
                } catch (Exception e) {
                    section.printlnKO();
                    throw new OCIDatabaseWaitForShutdownFailedException(e);
                }
            }
        } catch (BmcException be) {
            section.printlnKO();
            throw be;
        }
    }

    private void startDatabase() throws DSException {
        section = Section.DatabaseStart;
        section.print("checking existing databases");

        try {
            final ListAutonomousDatabasesRequest listADB = ListAutonomousDatabasesRequest.builder().compartmentId(configFile.get(CONFIG_COMPARTMENT_ID)).build();
            final ListAutonomousDatabasesResponse listADBResponse = dbClient.listAutonomousDatabases(listADB);

            boolean dbNameExists = false;
            String adbId = null;
            AutonomousDatabaseSummary.LifecycleState currentLifecycleState = null;
            for (AutonomousDatabaseSummary adb : listADBResponse.getItems()) {
                //System.out.println(adb.getLifecycleState()+", "+adb.getIsFreeTier()+", "+dbName);

                if (adb.getLifecycleState() != AutonomousDatabaseSummary.LifecycleState.Terminated) {
                    if (adb.getDbName().equals(dbName)) {
                        if (databaseType.isFree() && !adb.getIsFreeTier()) {
                            continue;
                        }
                        dbNameExists = true;
                        adbId = adb.getId();
                        currentLifecycleState = adb.getLifecycleState();
                        break;
                    }
                }
            }

            if (!dbNameExists) {
                section.printlnOK("nothing to do");
            } else {
                section.print("pending");

                if (currentLifecycleState == AutonomousDatabaseSummary.LifecycleState.Available) {
                    section.printlnOK("already started");
                    return;
                }

                workRequestClient = new WorkRequestClient(provider);
                StartAutonomousDatabaseResponse responseTerminate = dbClient.startAutonomousDatabase(StartAutonomousDatabaseRequest.builder().autonomousDatabaseId(adbId).build());
                String workRequestId = responseTerminate.getOpcWorkRequestId();

                GetWorkRequestRequest getWorkRequestRequest = GetWorkRequestRequest.builder().workRequestId(workRequestId).build();
                boolean exit = false;
                long startTime = System.currentTimeMillis();
                float pendingProgressMove = 0f;
                do {
                    GetWorkRequestResponse getWorkRequestResponse = workRequestClient.getWorkRequest(getWorkRequestRequest);
                    switch (getWorkRequestResponse.getWorkRequest().getStatus()) {
                        case Succeeded:
                            section.printlnOK(getDurationSince(startTime));
                            exit = true;
                            break;
                        case Failed:
                            section.printlnKO();
                            final ListWorkRequestErrorsResponse response = workRequestClient.listWorkRequestErrors(ListWorkRequestErrorsRequest.builder().workRequestId(workRequestId).opcRequestId(getWorkRequestResponse.getOpcRequestId()).build());
                            final StringBuilder errors = new StringBuilder();
                            int i = 0;
                            for (WorkRequestError e : response.getItems()) {
                                if (i > 0) {
                                    errors.append("\n");
                                }
                                errors.append(e.getMessage());
                                i++;
                            }
                            throw new OCIDatabaseStartFailedException(dbName, errors.toString());
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
                            new AutonomousDatabase.LifecycleState[]{AutonomousDatabase.LifecycleState.Available}).execute();
                } catch (Exception e) {
                    section.printlnKO();
                    throw new OCIDatabaseWaitForStartFailedException(e);
                }
            }
        } catch (BmcException be) {
            section.printlnKO();
            throw be;
        }
    }

    private void createADB() throws DSException {
        section = Section.DatabaseCreation;
        section.print("checking existing databases");

        try {
            // Verify we didn't reach the limit for Always Free Databases
            if (databaseType.isFree()) {
                GetResourceAvailabilityRequest getResourceAvailabilityRequest =
                        GetResourceAvailabilityRequest.builder()
                                .compartmentId(provider.getTenantId())
                                .serviceName("database")
                                .limitName("adb-free-count")
                                .build();
                GetResourceAvailabilityResponse resourceAvailabilityResponse = limitsClient.getResourceAvailability(getResourceAvailabilityRequest);

                if (resourceAvailabilityResponse.getResourceAvailability().getAvailable() <= 0) {
                    section.printlnKO("limit reached");
                    throw new AlwaysFreeDatabaseLimitReachedException(resourceAvailabilityResponse.getResourceAvailability().getUsed().intValue());
                }
            }

            // Verify no existing database already exists with the provided name
            final ListAutonomousDatabasesRequest listADB = ListAutonomousDatabasesRequest.builder().compartmentId(configFile.get(CONFIG_COMPARTMENT_ID)).build();
            final ListAutonomousDatabasesResponse listADBResponse = dbClient.listAutonomousDatabases(listADB);
            boolean dbNameAlreadyExists = false;

            for (AutonomousDatabaseSummary adb : listADBResponse.getItems()) {
                if (adb.getLifecycleState() != AutonomousDatabaseSummary.LifecycleState.Terminated) {
                    if (adb.getDbName().equals(dbName)) {
                        dbNameAlreadyExists = true;
                    }
                }
            }

            if (dbNameAlreadyExists) {
                section.printlnKO("duplicate name");
                throw new DatabaseNameAlreadyExistsException(dbName);
            }

            section.print("pending");
            CreateAutonomousDatabaseDetails createFreeRequest = CreateAutonomousDatabaseDetails.builder()
                    .dbVersion(dbVersion)
                    .cpuCoreCount(1)
                    .dataStorageSizeInTBs(1)
                    .displayName(dbName + " Database")
                    .adminPassword(configFile.get(CONFIG_DATABASE_PASSWORD))
                    .dbName(dbName)
                    .compartmentId(configFile.get(CONFIG_COMPARTMENT_ID))
                    .dbWorkload(databaseType == DatabaseType.ATPFree || databaseType == DatabaseType.ATP ? CreateAutonomousDatabaseBase.DbWorkload.Oltp :
                            (databaseType == DatabaseType.AJDFree || databaseType == DatabaseType.AJD ? CreateAutonomousDatabaseBase.DbWorkload.Ajd :
                                    (databaseType == DatabaseType.ApexFree || databaseType == DatabaseType.Apex ? CreateAutonomousDatabaseBase.DbWorkload.Apex : CreateAutonomousDatabaseBase.DbWorkload.Dw)))
                    .isAutoScalingEnabled(databaseType == DatabaseType.ATP || databaseType == DatabaseType.AJD || databaseType == DatabaseType.ADW)
                    .licenseModel(databaseType.isFree() || databaseType == DatabaseType.AJD ? CreateAutonomousDatabaseBase.LicenseModel.LicenseIncluded :
                            (licenseType == LicenseType.LicenseIncluded ? CreateAutonomousDatabaseBase.LicenseModel.LicenseIncluded : CreateAutonomousDatabaseBase.LicenseModel.BringYourOwnLicense))
                    .isPreviewVersionWithServiceTermsAccepted(Boolean.FALSE)
                    .isFreeTier(databaseType.isFree() ? Boolean.TRUE : Boolean.FALSE)
                    .build();

            String workRequestId = null;
            AutonomousDatabase autonomousDatabase = null;
            workRequestClient = new WorkRequestClient(provider);

            BmcException creationException = null;

            try {
                CreateAutonomousDatabaseResponse responseCreate = dbClient.createAutonomousDatabase(CreateAutonomousDatabaseRequest.builder().createAutonomousDatabaseDetails(createFreeRequest).build());
                autonomousDatabase = responseCreate.getAutonomousDatabase();
                workRequestId = responseCreate.getOpcWorkRequestId();
            } catch (BmcException e) {
                //e.printStackTrace();
                if (e.getStatusCode() == 400 && e.getServiceCode().equals("LimitExceeded")) {
                    section.printlnKO("limit reached");
                    if (e.getMessage().startsWith("Tenancy has reached maximum limit for Free Tier Autonomous Database")) {
                        throw new AlwaysFreeDatabaseLimitReachedException(OCI_ALWAYS_FREE_DATABASE_NUMBER_LIMIT);
                    } else {
                        throw new AutonomousDatabaseLimitReachedException(e.getMessage());
                    }
                } else if (e.getStatusCode() == 400 && e.getServiceCode().equals("InvalidParameter") &&
                        e.getMessage().contains(dbName) && e.getMessage().contains("already exists")) {
                    section.printlnKO("duplicate name");
                    throw new DatabaseNameAlreadyExistsException(dbName);
                }

                creationException = e;
            }

            if (autonomousDatabase == null) {
                section.printlnKO();
                throw new OCIDatabaseCreationCantProceedFurtherException(creationException);
            }

            GetWorkRequestRequest getWorkRequestRequest = GetWorkRequestRequest.builder().workRequestId(workRequestId).build();
            boolean exit = false;
            long startTime = System.currentTimeMillis();
            float pendingProgressMove = 0f;
            boolean probe = true;
            GetWorkRequestResponse getWorkRequestResponse = null;
            do {
                if (probe) {
                    getWorkRequestResponse = workRequestClient.getWorkRequest(getWorkRequestRequest);
                }
                switch (getWorkRequestResponse.getWorkRequest().getStatus()) {
                    case Succeeded:
                        section.printlnOK(dbName + " " + dbVersion + ": " + getDurationSince(startTime));
                        exit = true;
                        break;
                    case Failed:
                        section.printlnKO();

                        final ListWorkRequestErrorsResponse response = workRequestClient.listWorkRequestErrors(ListWorkRequestErrorsRequest.builder().workRequestId(workRequestId).opcRequestId(getWorkRequestResponse.getOpcRequestId()).build());
                        final StringBuilder errors = new StringBuilder();
                        int i = 0;
                        for (WorkRequestError e : response.getItems()) {
                            if (i > 0) {
                                errors.append("\n");
                            }
                            errors.append(e.getMessage());
                            i++;
                        }

                        throw new OCIDatabaseCreationFaileDException(dbName, errors.toString());
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
                GetAutonomousDatabaseResponse responseGet = waiter.forAutonomousDatabase(GetAutonomousDatabaseRequest.builder().autonomousDatabaseId(autonomousDatabase.getId()).build(),
                        new AutonomousDatabase.LifecycleState[]{AutonomousDatabase.LifecycleState.Available}).execute();
                autonomousDatabase = responseGet.getAutonomousDatabase();
            } catch (Exception e) {
                section.printlnKO();
                throw new OCIDatabaseWaitForTerminationFailedException(e);
            }

            // The free autonomous database should now be available!

            File walletFile = null;

            // No wallet for Apex
            if (!(databaseType == DatabaseType.ApexFree || databaseType == DatabaseType.Apex)) {

                section = Section.DatabaseWalletDownload;
                section.print("pending");
                GenerateAutonomousDatabaseWalletDetails atpWalletDetails = GenerateAutonomousDatabaseWalletDetails.builder().password(configFile.get(CONFIG_DATABASE_PASSWORD)).generateType(GenerateAutonomousDatabaseWalletDetails.GenerateType.Single).build();
                GenerateAutonomousDatabaseWalletResponse atpWalletResponse =
                        dbClient.generateAutonomousDatabaseWallet(
                                GenerateAutonomousDatabaseWalletRequest.builder()
                                        .generateAutonomousDatabaseWalletDetails(atpWalletDetails)
                                        .autonomousDatabaseId(autonomousDatabase.getId())
                                        .build());
                section.print("saving");

                final String walletFileName = dbName.toLowerCase() + ".zip";
                walletFile = new File(walletFileName);
                try {
                    Files.copy(atpWalletResponse.getInputStream(), walletFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ioe) {
                    throw new DatabaseWalletSavingException(walletFile.getAbsolutePath());
                }

                if (!ZipUtil.isValid(walletFile)) {
                    section.printlnKO(String.format("%s is corrupted", walletFileName));
                    throw new DatabaseWalletCorruptedException(walletFile.getAbsolutePath());
                }

                ZipUtil.unzipFile(walletFile, new File(".", "wallet_" + dbName.toLowerCase()));

                section.printlnOK(walletFileName);
            }

            final ADBRESTService rSQLS = new ADBRESTService(autonomousDatabase.getConnectionUrls().getSqlDevWebUrl(), databaseUserName.toUpperCase(), configFile.get(CONFIG_DATABASE_PASSWORD));

            // Save the local config file as early as possible in case of problems afterward so that one can destroy it
            section = Section.LocalConfiguration;
            try (PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(LOCAL_CONFIGURATION_FILENAME)))) {
                out.println(getConfigurationAsJSON(autonomousDatabase, rSQLS, true, walletFile));
            } catch (IOException e) {
                throw new LocalConfigurationNotSavedException(e);
            }
            section.printlnOK();

            section = Section.DatabaseConfiguration;

            section.print(String.format("creating %s user", databaseUserName));
            createSchema(autonomousDatabase);

            if (configFile.get(CONFIG_COLLECTIONS) != null) {
                createCollections(rSQLS, autonomousDatabase, walletFile);

                if (!databaseType.isFree()) {
                    section.print("search index setup");

                    final ADBRESTService adminRSQLS = new ADBRESTService(autonomousDatabase.getConnectionUrls().getSqlDevWebUrl(), "ADMIN", configFile.get(CONFIG_DATABASE_PASSWORD));

                    try {
                        adminRSQLS.execute("BEGIN\n" +
                                "    CTXSYS.CTX_ADM.SET_PARAMETER('default_index_memory','2147483648');\n" +
                                "END;\n" +
                                "/");
                    } catch (RuntimeException re) {
                        section.printlnKO();
                        throw new SearchIndexConfigurationFailedException("default_index_memory");
                    }
                }
            }

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
            boolean backupBucketExist = false;
            boolean dragonBucketExist = false;
            final String backupBucketName = "backup_" + dbName.toLowerCase();
            final String dragonBucketName = "dragon";
            do {
                listBucketsBuilder.page(nextToken);
                ListBucketsResponse listBucketsResponse = objectStorageClient.listBuckets(listBucketsBuilder.build());
                for (BucketSummary bucket : listBucketsResponse.getItems()) {
                    if (!backupBucketExist && backupBucketName.equals(bucket.getName())) {
                        backupBucketExist = true;
                    }
                    if (!dragonBucketExist && dragonBucketName.equals(bucket.getName())) {
                        dragonBucketExist = true;
                    }
                }
                nextToken = listBucketsResponse.getOpcNextPage();
            } while (nextToken != null);

            if (!dragonBucketExist) {
                section.print("creating dragon bucket");
                createManualBucket(namespaceName, dragonBucketName, true);
            }

            identityClient = new IdentityClient(provider);
            GetUserResponse userResponse = identityClient.getUser(GetUserRequest.builder().userId(configFile.get(CONFIG_USER)).build());

            section.print("database setup");

            try {
                rSQLS.execute(String.format(
                        "BEGIN\n" +
                                "    DBMS_CLOUD.CREATE_CREDENTIAL(credential_name => 'DRAGON_CREDENTIAL_NAME', username => '%s', password => '%s');\n" +
                                "    COMMIT;\n" +
                                "END;\n" +
                                "/", userResponse.getUser().getName(), configFile.get(CONFIG_AUTH_TOKEN)));
            } catch (RuntimeException re) {
                section.printlnKO();
                throw new ObjectStorageConfigurationFailedException();
            }

            if (!databaseType.isFree()) {
                if (!backupBucketExist) {
                    section.print("creating manual backup bucket");
                    createManualBucket(namespaceName, backupBucketName, false);
                }

                section.print("database backup setup");

                final ADBRESTService adminRSQLS = new ADBRESTService(autonomousDatabase.getConnectionUrls().getSqlDevWebUrl(), "ADMIN", configFile.get(CONFIG_DATABASE_PASSWORD));

                try {
                    adminRSQLS.execute(String.format(
                            "ALTER DATABASE PROPERTY SET default_bucket='https://swiftobjectstorage." + getRegionForURL() + ".oraclecloud.com/v1/" + namespaceName + "';\n" +
                                    "BEGIN\n" +
                                    "    DBMS_CLOUD.CREATE_CREDENTIAL(credential_name => 'BACKUP_CREDENTIAL_NAME', username => '%s', password => '%s');\n" +
                                    "    COMMIT;\n" +
                                    "END;\n" +
                                    "/\n" +
                                    "ALTER DATABASE PROPERTY SET default_credential='ADMIN.BACKUP_CREDENTIAL_NAME'", userResponse.getUser().getName(), configFile.get(CONFIG_AUTH_TOKEN)));
                } catch (RuntimeException re) {
                    section.printlnKO();
                    throw new ObjectStorageConfigurationFailedException();
                }
            }

            section.printlnOK();

            if (loadJSON) {
                section = Section.LoadDataIntoCollections;
                loadDataIntoCollections(namespaceName, rSQLS);
                section.printlnOK();
            }

            if (loadCSV) {
                section = Section.LoadDataIntoTables;
                loadDataIntoTables(namespaceName, rSQLS);
                section.printlnOK();
            }

            // reload just saved JSON local configuration as POJO for further processing (create stack...)
            loadLocalConfiguration(false);

            Console.println("You can connect to your database using SQL Developer Web:");
            final String url = rSQLS.getUrlPrefix() + "sign-in/?username=" + databaseUserName.toUpperCase() + "&r=_sdw%2F";
            Console.println("- URL  : " + ANSI_UNDERLINE + url);
            Console.println("- login: " + ANSI_BRIGHT + databaseUserName.toLowerCase());
        } catch (BmcException be) {
            section.printlnKO();
            throw be;
        }
    }

    private void loadDataIntoCollections() throws DSException {
        section = Section.LoadDataIntoCollections;

        objectStorageClient = new ObjectStorageClient(provider);
        objectStorageClient.setRegion(region);

        section.print("checking existing buckets");
        final GetNamespaceResponse namespaceResponse = objectStorageClient.getNamespace(GetNamespaceRequest.builder().build());
        final String namespaceName = namespaceResponse.getValue();

        final ADBRESTService rSQLS = new ADBRESTService(localConfiguration.getSqlDevWeb(), databaseUserName.toUpperCase(), configFile.get(CONFIG_DATABASE_PASSWORD));

        loadDataIntoCollections(namespaceName, rSQLS);

        section.printlnOK();
    }

    private void loadDataIntoTables() throws DSException {
        section = Section.LoadDataIntoTables;

        objectStorageClient = new ObjectStorageClient(provider);
        objectStorageClient.setRegion(region);

        section.print("checking existing buckets");
        final GetNamespaceResponse namespaceResponse = objectStorageClient.getNamespace(GetNamespaceRequest.builder().build());
        final String namespaceName = namespaceResponse.getValue();

        final ADBRESTService rSQLS = new ADBRESTService(localConfiguration.getSqlDevWeb(), databaseUserName.toUpperCase(), configFile.get(CONFIG_DATABASE_PASSWORD));

        loadDataIntoTables(namespaceName, rSQLS);

        section.printlnOK();
    }

    private void loadDataIntoTables(final String namespaceName, final ADBRESTService rSQLS) throws DSException {
        UploadConfiguration uploadConfiguration =
                UploadConfiguration.builder()
                        .allowMultipartUploads(true)
                        .allowParallelUploads(true)
                        .build();

        UploadManager uploadManager = new UploadManager(objectStorageClient, uploadConfiguration);

        for (String tableName : configFile.get(CONFIG_TABLES).split(",")) {
            section.print("table " + tableName);

            // find all names starting by <table name>_XXX.csv and stored in some data folder (specified in CONFIGURATION_FILENAME)
            final File[] dataFiles = dataPath.listFiles(new CSVTableFilenameFilter(tableName));

            if (dataFiles == null || dataFiles.length == 0) {
                continue;
            }

            Map<String, String> metadata = null;

            // upload them in parallel to OCI Object Storage
            int nb = 1;
            final CSVAnalyzerInputStream csvAnalyzerInputStream = new CSVAnalyzerInputStream(false);
            String tableDDL;
            for (File file : dataFiles) {
                section.print(String.format("table %s: uploading file %d/%d", tableName, nb, dataFiles.length));

                PutObjectRequest request =
                        PutObjectRequest.builder()
                                .bucketName("dragon")
                                .namespaceName(namespaceName)
                                .objectName(dbName + "/" + tableName + "/" + file.getName())
                                .contentType("application/csv")
                                //.contentLanguage(contentLanguage)
                                //.contentEncoding("UTF-8")
                                //.opcMeta(metadata)
                                .build();

                // old version:                     UploadManager.UploadRequest uploadDetails = UploadManager.UploadRequest.builder(file).allowOverwrite(true).build(request);
                try {
                    UploadManager.UploadRequest uploadDetails = UploadManager.UploadRequest.builder(csvAnalyzerInputStream.analyze(new InputStreamReader(new BufferedInputStream(new FileInputStream(file), 1024 * 1024))), file.length()).allowOverwrite(true).build(request);
                    UploadManager.UploadResponse response = uploadManager.upload(uploadDetails);
                } catch (FileNotFoundException ignored) {
                    // should not happen!
                }

                //System.out.println("https://objectstorage."+getRegionForURL()+".oraclecloud.com/n/"+namespaceName+"/b/"+"dragon"+"/o/"+(dbName+"/"+collectionName+"/"+file.getName()).replaceAll("/", "%2F"));


                //System.out.println(response);
                nb++;
            }

            tableDDL = csvAnalyzerInputStream.getTableDDL(tableName);

            //System.out.println("table DDL: "+tableDDL);

            section.print(String.format("table %s: loading %d row(s)...", tableName, csvAnalyzerInputStream.getRows()));


            // if (databaseType == DatabaseType.AlwaysFreeATP) {
            try {
                final String sql = String.format(
                        "DECLARE\n" +
                                "l_tableExist number;\n" +
                                "l_externalColumns varchar2(32000);\n" +
                                "i number;\n" +
                                "BEGIN\n" +
                                "    select COUNT(*) into l_tableExist from user_tables where table_name = '%s';\n" +
                                "    IF l_tableExist = 0 THEN\n" +
                                "    	execute immediate '%s';\n" +
                                "    END IF;\n\n" +
                                "    l_externalColumns := '';\n" +
                                "    i := 0;\n" +
                                "    for cur in (select column_name||' '||data_type||case when data_scale is not null and data_scale > 0 then (case when data_type like 'TIMESTAMP%%' then '' else '('||data_precision||', '||data_scale||')' end) when data_scale is null then decode(data_type,'DATE','','(' || data_length || ')') else '' end DB_COL, \n" +
                                "                       column_name||' CHAR'|| decode(case when data_type like 'TIMESTAMP%%' then 'TIMESTAMP' else data_type end,'DATE',' date_format DATE mask \\\"yyyy-mm-dd\\\"','TIMESTAMP',' timestamp_format TIMESTAMP mask \\\"yyyy-mm-dd hh-mi-ss\\\"','') EXT_TAB_COL \n" +
                                "                  from user_tab_cols where table_name='%s' and virtual_column='NO' order by column_id)\n" +
                                "    loop\n" +
                                "        l_externalColumns := l_externalColumns || case i when 0 then '' else ',' end || cur.EXT_TAB_COL;\n" +
                                "        i := i + 1;\n" +
                                "    end loop;\n" +
                                "        \n" +
                                "    DBMS_CLOUD.COPY_DATA (table_name => '%s', credential_name => 'DRAGON_CREDENTIAL_NAME', file_uri_list => 'https://objectstorage.%s.oraclecloud.com/n/%s/b/dragon/o/%s/%s/*', schema_name => null, field_list => l_externalColumns, format => json_object('delimiter' value '%s', 'rejectlimit' value 'unlimited', 'skipheaders' value '1', 'dateformat' value 'yyyy-mm-dd', 'timestampformat' value 'yyyy-mm-dd hh:mi:ss', 'blankasnull' value 'true', 'ignoreblanklines' value 'true', 'removequotes' value 'true', 'recorddelimiter' value '''%s''' ) );\n" +
                                "    COMMIT;\n" +
                                "END;\n" +
                                "/", tableName.toUpperCase(), tableDDL, tableName.toUpperCase(), tableName.toUpperCase(), getRegionForURL(), namespaceName, dbName, tableName, csvAnalyzerInputStream.getFieldSeparator(), csvAnalyzerInputStream.getRecordDelimiter());

                //System.out.println(sql);

                rSQLS.execute(sql);
            } catch (RuntimeException re) {
                section.printlnKO();
                throw new TableNotLoadedException(tableName, re);
            }
                /*} else {
                    // use DBMS_SCHEDULER with class HIGH...
                    try {
                        // TODO: Check for progress of load... using view USER_LOAD_OPERATIONS
                        rSQLS.execute(String.format(
                                "BEGIN\n" +
                                        "    DBMS_SCHEDULER.CREATE_JOB (\n" +
                                        "     job_name => 'LOAD_%s',\n" +
                                        "     job_type => 'PLSQL_BLOCK',\n" +
                                        "     job_action => 'BEGIN DBMS_CLOUD.COPY_COLLECTION(collection_name => ''%s'', credential_name => ''DRAGON_CREDENTIAL_NAME'', file_uri_list => ''https://objectstorage.%s.oraclecloud.com/n/%s/b/dragon/o/%s/%s/*'', format => JSON_OBJECT(''recorddelimiter'' value ''''''\\n'''''', ''ignoreblanklines'' value ''true'')); END;',\n" +
                                        "     start_date => SYSTIMESTAMP,\n" +
                                        "     enabled => TRUE,\n" +
                                        "     auto_drop => FALSE,\n" +
                                        "     job_class => 'HIGH',\n" +
                                        "     comments => 'load %s collection');\n" +
                                        "    COMMIT;\n" +
                                        "END;\n" +
                                        "/\n", collectionName, collectionName, getRegionForURL(), namespaceName, dbName, collectionName, collectionName));

                        // TODO: Check for progress of load... using view USER_LOAD_OPERATIONS
                    } catch (RuntimeException re) {
                        section.printlnKO();
                        throw new CollectionNotLoadedException(collectionName, re);
                    }
                }*/
        }
    }

    private void createSchema(AutonomousDatabase adb) throws DatabaseUserCreationFailedException {
        final ADBRESTService rSQLS = new ADBRESTService(adb.getConnectionUrls().getSqlDevWebUrl(), "ADMIN", configFile.get(CONFIG_DATABASE_PASSWORD));

        try {
            rSQLS.execute(String.format("create user %s identified by \"%s\" DEFAULT TABLESPACE DATA TEMPORARY TABLESPACE TEMP;\n" +
                    "alter user %s quota unlimited on data;\n" +
                    "grant dwrole, create session, soda_app, alter session to %s;\n" +
                    "grant execute on CTX_DDL to %s;\n" +
                    "grant select on sys.v_$mystat to %s;\n" +
                    "grant select on dba_rsrc_consumer_group_privs to %s;\n" +
                    "grant execute on dbms_session to %s;\n" +
                    "grant select on sys.v_$services to %s;\n" +
                    "grant alter session to %s;" +
                    "grant execute on DBMS_AUTO_INDEX to %s;\n" +
                    "BEGIN\n" +
                    "    ords_admin.enable_schema(p_enabled => TRUE, p_schema => '%s', p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => '%s', p_auto_rest_auth => TRUE);\n" +
                    "END;\n" +
                    "/", databaseUserName, configFile.get(CONFIG_DATABASE_PASSWORD), databaseUserName, databaseUserName, databaseUserName, databaseUserName, databaseUserName, databaseUserName, databaseUserName, databaseUserName, databaseUserName, databaseUserName.toUpperCase(), databaseUserName.toLowerCase()));
        } catch (RuntimeException re) {
            section.printlnKO();
            throw new DatabaseUserCreationFailedException(re);
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

    private void createCollections(ADBRESTService rSQLS, AutonomousDatabase adb, File walletFile) {
        section.print("creating dragon collections");
        rSQLS.createSODACollection("dragon");
        section.print("storing dragon information");
        rSQLS.insertDocument("dragon", getConfigurationAsJSON(adb, rSQLS, walletFile));

        for (String collectionName : configFile.get(CONFIG_COLLECTIONS).split(",")) {
            if (!"dragon".equals(collectionName)) {
                section.print("creating collection " + collectionName);
                rSQLS.createSODACollection(collectionName);
            }
        }
    }

    private String getConfigurationAsJSON(AutonomousDatabase adb, ADBRESTService rSQLS, File walletFile) {
        return getConfigurationAsJSON(adb, rSQLS, false, walletFile);
    }

    private String getConfigurationAsJSON(AutonomousDatabase adb, ADBRESTService rSQLS, boolean local, File walletFile) {
        return String.format("{\n" +
                        "\"ocid\": \"%s\",\n" +
                        "\"databaseServiceURL\": \"%s\",\n" +
                        "\"sqlDevWebAdmin\": \"%s\",\n" +
                        "\"sqlDevWeb\": \"%s\",\n" +
                        "\"apexURL\": \"%s\",\n" +
                        "\"omlURL\": \"%s\",\n" +
                        "\"graphStudioURL\": \"%s\",\n" +
                        "\"sqlAPI\": \"%s\",\n" +
                        "\"sodaAPI\": \"%s\",\n" +
                        "\"version\": \"%s\",\n" +
                        "\"apexVersion\": \"%s\",\n" +
                        "\"ordsVersion\": \"%s\"" +
                        (local ? ",\n\"dbName\": \"%s\",\n\"dbUserName\": \"%s\",\n\"dbUserPassword\": \"%s\"" + (walletFile != null ? ",\n\"walletFile\": \"%s\",\n\"extractedWallet\": \"%s\"" : "%s%s")
                                : "") +
                        "}",
                adb.getId(),
                adb.getServiceConsoleUrl(),
                adb.getConnectionUrls().getSqlDevWebUrl(),
                adb.getConnectionUrls().getSqlDevWebUrl().replaceAll("admin", databaseUserName.toLowerCase()),
                adb.getConnectionUrls().getApexUrl(),
                adb.getConnectionUrls().getMachineLearningUserManagementUrl(),
                adb.getConnectionUrls().getGraphStudioUrl(),
                rSQLS.getUrlSQLService(),
                rSQLS.getUrlSODAService(),
                adb.getDbVersion(),
                adb.getApexDetails().getApexVersion(),
                adb.getApexDetails().getOrdsVersion(),
                dbName, databaseUserName, configFile.get(CONFIG_DATABASE_PASSWORD),
                walletFile == null ? "" :
                        walletFile.getAbsolutePath().replace('\\', '/'),
                walletFile == null ? "" :
                        new File(walletFile.getAbsoluteFile().getParent(), "wallet_" + dbName.toLowerCase()).getAbsolutePath().replace('\\', '/')
        );
    }

    private void loadDataIntoCollections(String namespaceName, ADBRESTService rSQLS) throws DSException {
        UploadConfiguration uploadConfiguration =
                UploadConfiguration.builder()
                        .allowMultipartUploads(true)
                        .allowParallelUploads(true)
                        .build();

        UploadManager uploadManager = new UploadManager(objectStorageClient, uploadConfiguration);


        for (String collectionName : configFile.get(CONFIG_COLLECTIONS).split(",")) {
            if (!"dragon".equals(collectionName)) {
                section.print("collection " + collectionName);

                // find all names starting by <collection name>_XXX.json and stored in some data folder (specified in CONFIGURATION_FILENAME)
                final File[] dataFiles = dataPath.listFiles(new JSONCollectionFilenameFilter(collectionName));

                if (dataFiles == null || dataFiles.length == 0) {
                    continue;
                }

                Map<String, String> metadata = null;

                // upload them in parallel to OCI Object Storage
                int nb = 1;
                for (File file : dataFiles) {
                    section.print(String.format("collection %s: uploading file %d/%d", collectionName, nb, dataFiles.length));

                    PutObjectRequest request =
                            PutObjectRequest.builder()
                                    .bucketName("dragon")
                                    .namespaceName(namespaceName)
                                    .objectName(dbName + "/" + collectionName + "/" + file.getName())
                                    .contentType("application/json")
                                    //.contentLanguage(contentLanguage)
                                    //.contentEncoding("UTF-8")
                                    //.opcMeta(metadata)
                                    .build();

                    // old version:                     UploadManager.UploadRequest uploadDetails = UploadManager.UploadRequest.builder(file).allowOverwrite(true).build(request);
                    try {
                        final UploadManager.UploadRequest uploadDetails = UploadManager.UploadRequest.builder(new JSONFlattenerInputStream(new BufferedInputStream(new FileInputStream(file), 1024 * 1024)), file.length()).allowOverwrite(true).build(request);

						/*
						final File tempFile = File.createTempFile("dragon-", "", null);
						Files.copy(new JSONFlattenerInputStream(new BufferedInputStream(new FileInputStream(file), 1024 * 1024)), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						file = tempFile;
						// TODO: uncomment code at the end of  @Override
						//    public int read(byte[] b, int off, int len) throws IOException {
						//        int result = super.read(b, off, len);
						//        if(result == -1) { return -1;
						///*            if(strippedBytes >= len) {

						UploadManager.UploadRequest uploadDetails = UploadManager.UploadRequest.builder(new BufferedInputStream(new FileInputStream(file), 1024 * 1024), file.length()).allowOverwrite(true).build(request);
						 */
                        UploadManager.UploadResponse response = uploadManager.upload(uploadDetails);
                    } catch (IOException ignored) {
                        // should not happen!
                        ignored.printStackTrace();
                    }


                    //System.out.println("https://objectstorage."+getRegionForURL()+".oraclecloud.com/n/"+namespaceName+"/b/"+"dragon"+"/o/"+(dbName+"/"+collectionName+"/"+file.getName()).replaceAll("/", "%2F"));


                    //System.out.println(response);
                    nb++;
                }

                section.print(String.format("collection %s: loading...", collectionName));

                // if (databaseType == DatabaseType.AlwaysFreeATP) {
                try {
                    rSQLS.execute(String.format(
                            "BEGIN\n" +
                                    "    DBMS_CLOUD.COPY_COLLECTION(\n" +
                                    "        collection_name => '%s',\n" +
                                    "        credential_name => 'DRAGON_CREDENTIAL_NAME',\n" +
                                    "        file_uri_list => 'https://objectstorage.%s.oraclecloud.com/n/%s/b/dragon/o/%s/%s/*',\n" +
                                    "        format => JSON_OBJECT('recorddelimiter' value '''\\n''', 'ignoreblanklines' value 'true') );\n" +
                                    "END;\n" +
                                    "/", collectionName, getRegionForURL(), namespaceName, dbName, collectionName));
                } catch (RuntimeException re) {
                    section.printlnKO();
                    throw new CollectionNotLoadedException(collectionName, re);
                }
                /*} else {
                    // use DBMS_SCHEDULER with class HIGH...
                    try {
                        // TODO: Check for progress of load... using view USER_LOAD_OPERATIONS
                        rSQLS.execute(String.format(
                                "BEGIN\n" +
                                        "    DBMS_SCHEDULER.CREATE_JOB (\n" +
                                        "     job_name => 'LOAD_%s',\n" +
                                        "     job_type => 'PLSQL_BLOCK',\n" +
                                        "     job_action => 'BEGIN DBMS_CLOUD.COPY_COLLECTION(collection_name => ''%s'', credential_name => ''DRAGON_CREDENTIAL_NAME'', file_uri_list => ''https://objectstorage.%s.oraclecloud.com/n/%s/b/dragon/o/%s/%s/*'', format => JSON_OBJECT(''recorddelimiter'' value ''''''\\n'''''', ''ignoreblanklines'' value ''true'')); END;',\n" +
                                        "     start_date => SYSTIMESTAMP,\n" +
                                        "     enabled => TRUE,\n" +
                                        "     auto_drop => FALSE,\n" +
                                        "     job_class => 'HIGH',\n" +
                                        "     comments => 'load %s collection');\n" +
                                        "    COMMIT;\n" +
                                        "END;\n" +
                                        "/\n", collectionName, collectionName, getRegionForURL(), namespaceName, dbName, collectionName, collectionName));

                        // TODO: Check for progress of load... using view USER_LOAD_OPERATIONS
                    } catch (RuntimeException re) {
                        section.printlnKO();
                        throw new CollectionNotLoadedException(collectionName, re);
                    }
                }*/
            }
        }
    }

    private void destroyDatabase() throws OCIDatabaseTerminationFailedException, OCIDatabaseWaitForTerminationFailedException {
        section = Section.DatabaseTermination;
        section.print("checking existing databases");

        try {
            final ListAutonomousDatabasesRequest listADB = ListAutonomousDatabasesRequest.builder().compartmentId(configFile.get(CONFIG_COMPARTMENT_ID)).build();
            final ListAutonomousDatabasesResponse listADBResponse = dbClient.listAutonomousDatabases(listADB);

            boolean dbNameExists = false;
            String adbId = null;
            for (AutonomousDatabaseSummary adb : listADBResponse.getItems()) {
                //System.out.println(adb.getLifecycleState()+", "+adb.getIsFreeTier()+", "+dbName);

                if (adb.getLifecycleState() != AutonomousDatabaseSummary.LifecycleState.Terminated) {
                    if (adb.getDbName().equals(dbName)) {
                        if (databaseType.isFree() && !adb.getIsFreeTier()) {
                            continue;
                        }
                        dbNameExists = true;
                        adbId = adb.getId();
                        break;
                    }
                }
            }

            if (!dbNameExists) {
                section.printlnOK("nothing to do");

                // deleting local configuration!
                final File toDelete = new File(LOCAL_CONFIGURATION_FILENAME);
                if (toDelete.exists()) {
                    toDelete.delete();
                }
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
                            section.printlnOK(getDurationSince(startTime));
                            exit = true;
                            break;
                        case Failed:
                            section.printlnKO();
                            final ListWorkRequestErrorsResponse response = workRequestClient.listWorkRequestErrors(ListWorkRequestErrorsRequest.builder().workRequestId(workRequestId).opcRequestId(getWorkRequestResponse.getOpcRequestId()).build());
                            final StringBuilder errors = new StringBuilder();
                            int i = 0;
                            for (WorkRequestError e : response.getItems()) {
                                if (i > 0) {
                                    errors.append("\n");
                                }
                                errors.append(e.getMessage());
                                i++;
                            }
                            throw new OCIDatabaseTerminationFailedException(dbName, errors.toString());
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
                } finally {
                    // deleting local configuration!
                    final File toDelete = new File(LOCAL_CONFIGURATION_FILENAME);
                    if (toDelete.exists()) {
                        toDelete.delete();
                    }
                }
            }
        } catch (BmcException be) {
            section.printlnKO();
            throw be;
        }
    }

    private void sleep(final long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignored) {
        }
    }

    public void close() {
        if (dbClient != null) {
            dbClient.close();
        }
        if (workRequestClient != null) {
            workRequestClient.close();
        }
        if (objectStorageClient != null) {
            objectStorageClient.close();
        }
        if (identityClient != null) {
            identityClient.close();
        }
        if (limitsClient != null) {
            limitsClient.close();
        }
    }

    public void displayInformation() {
        if (!info) {
            return;
        }

        Console.println("  . OCI profile    : " + profileName);
        Console.println("  . OCI region     : " + getRegionForURL());
        Console.println("  . OCI tenant     : " + configFile.get(CONFIG_TENANCY_ID));
        Console.println("  . OCI compartment: " + configFile.get(CONFIG_COMPARTMENT_ID));
        Console.println("  . OCI user       : " + configFile.get(CONFIG_USER));
    }
}
