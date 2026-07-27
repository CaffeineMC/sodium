package net.caffeinemc.mods.sodium.client.platform.windows.api;

import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.platform.windows.api.msgbox.MsgBoxParamSw;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.SharedLibrary;

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;
import static org.lwjgl.system.APIUtil.apiGetFunctionAddressOptional;
import static org.lwjgl.system.MemoryUtil.NULL;

public class User32 {
    private static final SharedLibrary LIBRARY;

    static {
        LIBRARY = APIUtil.apiCreateLibrary("user32");

        PFN_MessageBoxIndirectW = apiGetFunctionAddress(LIBRARY, "MessageBoxIndirectW");
        PFN_GetKeyboardLayout = apiGetFunctionAddress(LIBRARY, "GetKeyboardLayout");
    }

    private static final long PFN_MessageBoxIndirectW;
    private static final long PFN_GetKeyboardLayout;

    /**
     * @see <a href="https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-messageboxw">Winuser.h Documentation</a>
     */
    public static void callMessageBoxIndirectW(MsgBoxParamSw params) {
        JNI.callPI(params.address(), PFN_MessageBoxIndirectW);
    }

    /**
     * @see <a href="https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getkeyboardlayout">Winuser.h Documentation</a>
     */
    public static long callGetKeyboardLayout(int thread) {
        return JNI.callPI(thread, PFN_GetKeyboardLayout);
    }
}
