package com.oracle.dragon;

import com.oracle.bmc.model.BmcException;
import com.oracle.dragon.util.DSSession;
import com.oracle.dragon.util.exception.ConfigurationFileNotFoundException;
import com.oracle.dragon.util.exception.ConfigurationLoadException;
import com.oracle.dragon.util.exception.DSException;

import static com.oracle.dragon.util.Console.*;

/**
 * DRAGON Stack Manager - Main entry point.
 *
 * @see https://github.com/loiclefevre/dragon
 * @since 1.0.0
 */
public class DragonStack {

    public static void main(final String[] args) {
        Kernel32.init();

        final long totalDuration = System.currentTimeMillis();

        try {
            final DSSession session = new DSSession();

            session.loadLocalConfiguration(true);

            session.analyzeCommandLineParameters(args);

            session.loadConfiguration();

            session.displayInformation();

            session.work();

            session.close();
        } catch (BmcException e) {
            if (e.isClientSide()) {
                System.err.println("A problem occurred on your side that prevented the operation to succeed!");
                e.printStackTrace();
                System.exit(-1000);
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
        } finally {
            println(Style.ANSI_BRIGHT_CYAN + "duration: " + getDurationSince(totalDuration));
        }
    }
}
