package com.oracle.dragon;

import com.oracle.dragon.util.Console;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.word.Pointer;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CContext(Kernel32.Directives.class)
@Platforms({Platform.WINDOWS.class, Platform.WINDOWS_AMD64.class})
public class Kernel32 {

    public static void init() {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("windows")) {
            final BigDecimal OSVersion = new BigDecimal(System.getProperty("os.version"));
            final String VMName = System.getProperty("java.vm.name");
            if (OSVersion.doubleValue() >= 10.0d || (OSVersion.doubleValue() >= 6.2d && "Substrate VM".equalsIgnoreCase(VMName))) {
//                System.out.println("os.version: OK");
                try {
                    final Pointer handle = Kernel32.getStdHandle(-11);
//                    System.out.println("Kernel32.getStdHandle: OK");
                    Kernel32.setConsoleMode(handle, 7);
//                    System.out.println("Kernel32.setConsoleMode: OK");
                    Console.ENABLE_COLORS = true;
//                    System.out.println("Console.ENABLE_COLORS: "+Console.ENABLE_COLORS);
//                    System.out.println("Colors for Windows 10+ console enabled!");
                } catch (Throwable t) {
                    Console.ENABLE_COLORS = false;
//                    t.printStackTrace();
//                    System.out.println("Console.ENABLE_COLORS: "+Console.ENABLE_COLORS);
                }
            }
        }
    }

    static final class Directives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList("<windows.h>");
        }

        @Override
        public boolean isInConfiguration() {
            return Platform.includedIn(Platform.WINDOWS.class);
        }

        @Override
        public List<String> getMacroDefinitions() {
            return Arrays.asList("_WIN64");
        }
    }

    /*
    HANDLE WINAPI GetStdHandle(
      _In_ DWORD nStdHandle
    );
    */
    @CFunction("GetStdHandle")
    static native Pointer getStdHandle(int nStdHandle);

    /*
    BOOL WINAPI SetConsoleMode(
      _In_ HANDLE hConsoleHandle,
      _In_ DWORD  dwMode
    );
     */
    @CFunction("SetConsoleMode")
    static native int setConsoleMode(Pointer hConsoleHandle, int dwMode);
}
