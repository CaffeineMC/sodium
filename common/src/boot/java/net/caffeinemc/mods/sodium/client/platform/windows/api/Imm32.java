package net.caffeinemc.mods.sodium.client.platform.windows.api;

public class Imm32 {
    public static boolean checkIMEStatus() {
        long hkl = User32.callGetKeyboardLayout(0);

        if (hkl == 0) {
            // Assume English keyboard layout.
            return false;
        }

        int langId = (int)(hkl & 0xFFFF);

        // ImmIsIME does not return a sensible result for this, sadly. Maybe it will some day. But it returns true almost all the time right now.
        return isImeLanguage(langId);
    }

    private static boolean isImeLanguage(int langId) {
        return  langId == 2052 /* zh-CN */ ||
                langId == 1028 /* zh-TW */ ||
                langId == 3076 /* zh-HK */ ||
                langId == 4100 /* zh-SG */ ||
                langId == 1041 /* ja-JP */ ||
                langId == 1042 /* ko-KR */;
    }
}
