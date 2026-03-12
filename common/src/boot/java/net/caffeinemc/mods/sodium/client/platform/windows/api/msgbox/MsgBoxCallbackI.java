package net.caffeinemc.mods.sodium.client.platform.windows.api.msgbox;

import org.jspecify.annotations.NonNull;
import org.lwjgl.system.Callback;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.libffi.FFICIF;

import java.lang.invoke.MethodHandles;

import static org.lwjgl.system.APIUtil.apiCreateCIF;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.libffi.LibFFI.*;

@FunctionalInterface
@NativeType("MSGBOXCALLBACK")
public interface MsgBoxCallbackI extends CallbackI {
    Callback.Descriptor CIF = new Callback.Descriptor(
            MethodHandles.lookup(), apiCreateCIF(
            FFI_DEFAULT_ABI,
            ffi_type_void,
            ffi_type_pointer
    ));

    @Override
    default Callback.Descriptor getDescriptor() {
        return CIF;
    }

    @Override
    default void callback(long ret, long args) {
        this.invoke(
                memGetAddress(memGetAddress(args)) /* lpHelpInfo */
        );
    }

    void invoke(@NativeType("LPHELPINFO *") long lpHelpInfo);
}
