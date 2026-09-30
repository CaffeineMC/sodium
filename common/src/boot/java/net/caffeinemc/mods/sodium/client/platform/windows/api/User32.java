package net.caffeinemc.mods.sodium.client.platform.windows.api;

import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.SharedLibrary;

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;

public class User32 {
    private static final SharedLibrary LIBRARY;

    static {
        LIBRARY = APIUtil.apiCreateLibrary("user32");

        PFN_GetKeyboardLayout = apiGetFunctionAddress(LIBRARY, "GetKeyboardLayout");
    }

    private static final long PFN_GetKeyboardLayout;

    /**
     * @see <a href="https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getkeyboardlayout">Winuser.h Documentation</a>
     */
    public static long callGetKeyboardLayout(int thread) {
        return JNI.callPI(thread, PFN_GetKeyboardLayout);
    }
}
