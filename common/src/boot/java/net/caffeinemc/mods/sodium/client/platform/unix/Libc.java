package net.caffeinemc.mods.sodium.client.platform.unix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.*;

public class Libc {
    private static final SharedLibrary LIBRARY = APIUtil.apiCreateLibrary("libc.so.6");

    private static final long PFN_setenv;
    private static final long PFN_unsetenv;

    static {
        PFN_setenv = APIUtil.apiGetFunctionAddress(LIBRARY, "setenv");
        PFN_unsetenv = APIUtil.apiGetFunctionAddress(LIBRARY, "unsetenv");
    }

    public static void setEnvironmentVariable(String name, @Nullable String value) {
        if (value != null) {
            setenv(name, value);
        } else {
            unsetenv(name);
        }
    }

    private static void setenv(String name, @NotNull String value) {
        int result;

        try (var stack = MemoryStack.stackPush()) {
            result = JNI.callPPI(
                    MemoryUtil.memAddress(stack.UTF8(name)),
                    MemoryUtil.memAddress(stack.UTF8(value)),
                    1 /* replace */,
                    PFN_setenv);
        }

        if (result != 0) {
            throw new RuntimeException("setenv() failed: %d".formatted(result));
        }
    }

    private static void unsetenv(@NotNull String name) {
        int result;

        try (var stack = MemoryStack.stackPush()) {
            result = JNI.callPI(MemoryUtil.memAddress(stack.UTF8(name)), PFN_unsetenv);
        }

        if (result != 0) {
            throw new RuntimeException("unsetenv() failed: %d".formatted(result));
        }
    }
}
