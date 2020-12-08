package com.oracle.dragon;

import com.oracle.bmc.model.BmcException;
import com.oracle.dragon.util.DSSession;
import com.oracle.dragon.util.exception.*;

import static com.oracle.dragon.util.Console.*;
import static com.oracle.dragon.util.Console.Style.ANSI_RESET;
import static com.oracle.dragon.util.Console.Style.ANSI_UNDERLINE;

/**
 * DRAGON Stack Manager - Main entry point.
 *
 * @see https://github.com/loiclefevre/dragon
 * @since 1.0.0
 */
public class DragonStack {

    static {
        System.setProperty("java.net.useSystemProxies","true");
    }

    public static void main(final String[] args) {
        // will run only on Windows OS
        Kernel32.init();

        final long totalDuration = System.currentTimeMillis();

        DSSession session = null;

        try {
            session = new DSSession();

            session.loadLocalConfiguration(true);

            // may override dbName...
            session.analyzeCommandLineParameters(args);

            session.loadConfigurationFile();

            session.displayInformation();

            session.work();

            session.close();
        } catch (BmcException e) {
            if (e.isClientSide()) {
                System.err.println("A problem occurred on your side that prevented the operation to succeed!");
                e.printStackTrace(System.err);
                displayHowToReportIssue();
                System.exit(-1000);
            } else {
                if(e.getStatusCode() == 401 && e.getServiceCode().equals("NotAuthenticated") ) {
                    new NotAuthenticatedException(session != null ? session.getProfileName() : null).displayMessageAndExit(Style.ANSI_BRIGHT_CYAN + "duration: " + getDurationSince(totalDuration) + Style.ANSI_RESET);
                }
                else if(e.getStatusCode() == 404 && e.getServiceCode().equals("NotAuthorizedOrNotFound") ) {
                    new NotAuthorizedOrNotFoundException(session != null ? session.getProfileName() : null, session != null ? session.getCompartmentId() : null).displayMessageAndExit(Style.ANSI_BRIGHT_CYAN + "duration: " + getDurationSince(totalDuration) + Style.ANSI_RESET);
                } else {
                    println("Status     : " + e.getStatusCode());
                    println("Service    : " + e.getServiceCode());
                    println("RequestId  : " + e.getOpcRequestId());
                    println("Timeout    : " + e.isTimeout());
                    println("Client Side: " + e.isClientSide());
                    System.err.printf("ERROR: %s\n", e.getLocalizedMessage());
                    println(Style.ANSI_RED + "\n================================================================================");
                    println(Style.ANSI_RED + "Unhandled exception:");
                    e.printStackTrace(System.err);
                    System.err.flush();
                    displayHowToReportIssue();
                }
            }
        } catch( ConfigurationFileNotFoundException | ConfigurationLoadException ce ) {
            ce.displayMessageAndExit(Style.ANSI_BRIGHT_CYAN + "duration: " + getDurationSince(totalDuration) + Style.ANSI_RESET, true);
        }
        catch (DSException e) {
            e.displayMessageAndExit(Style.ANSI_BRIGHT_CYAN + "duration: " + getDurationSince(totalDuration) + Style.ANSI_RESET);
        } catch (Exception e) {
            println(Style.ANSI_RED + "\n================================================================================");
            println(Style.ANSI_RED + "Unhandled exception:");
            e.printStackTrace(System.err);
            displayHowToReportIssue();
        } finally {
            println(Style.ANSI_BRIGHT_CYAN + "duration: " + getDurationSince(totalDuration));
        }
    }

    public static void displayHowToReportIssue() {
        System.err.flush();
        println();
        println(ANSI_UNDERLINE + "Reporting issues:");
        println("Please report any issue (bug, enhancement request, documentation needs...) at " + ANSI_UNDERLINE + "http://bit.ly/DragonStack" + ANSI_RESET + " in the \"Issues\" tab.");
    }
}
