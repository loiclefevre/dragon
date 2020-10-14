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
import com.oracle.dragon.util.Console;
import com.oracle.dragon.util.DSSession;
import com.oracle.dragon.util.ZipUtil;
import com.oracle.dragon.util.exception.DSException;
import com.oracle.dragon.util.exception.MissingDatabaseNameParameterException;
import com.oracle.dragon.util.exception.UnsupportedPlatformException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;
import java.util.TreeSet;

import static com.oracle.dragon.util.Console.*;

/**
 * DRAGON Stack Manager - Main entry point.
 *
 * @since 1.0.0
 */
public class DragonStack {

    public static void main(final String[] args) {
        final long totalDuration = System.currentTimeMillis();

        try {
            final DSSession session = new DSSession();

            session.analyzeCommandLineParameters(args);

            session.loadConfiguration();

            session.displayInformation();

            session.initializeClients();

            session.work();

            session.close();
        } catch (BmcException e) {
            if (e.isClientSide()) {
                System.err.println("A problem occurred on your side that prevented the operation to succeed!");
                e.printStackTrace();
                System.exit(-1000);
            } else {
                println("Status: " + e.getStatusCode());
                println("Service: " + e.getServiceCode());
                println("RequestId: " + e.getOpcRequestId());
                println("Timeout: " + e.isTimeout());
                println("Client Side: " + e.isClientSide());
                System.err.printf("ERROR: %s\n", e.getLocalizedMessage());
            }
        }
        catch( DSException e ) {
            e.displayMessageAndExit(Style.ANSI_BLUE + "duration: " + getDurationSince(totalDuration)+Style.ANSI_RESET);
        }
        catch (Exception e) {
            println(Style.ANSI_RED+"================================================================================");
            println(Style.ANSI_RED+"Unhandled exception:");
            e.printStackTrace(System.err);
        }
        finally {
            println(Style.ANSI_BLUE + "duration: " + getDurationSince(totalDuration));
        }
    }
}
