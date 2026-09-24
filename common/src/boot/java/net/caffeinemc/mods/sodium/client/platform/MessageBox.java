package net.caffeinemc.mods.sodium.client.platform;

import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLMisc;
import org.lwjgl.sdl.SDL_MessageBoxButtonData;
import org.lwjgl.sdl.SDL_MessageBoxData;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

import static org.lwjgl.sdl.SDLMessageBox.*;

public class MessageBox {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-MessageBox");

    private static final int BUTTON_OK = 0;
    private static final int BUTTON_HELP = 1;

    /**
     * Shows the message box and blocks until dismissal. Choosing the help button opens {@code helpUrl} and shows the message
     * box again.
     *
     * @param pWindow the parent window, or {@code NULL} if there is none
     */
    public static void showMessageBox(long pWindow,
                                      IconType icon, String title,
                                      String description,
                                      @Nullable String helpUrl)
    {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var buttons = SDL_MessageBoxButtonData.calloc(helpUrl != null ? 2 : 1, stack);
            buttons.get(0)
                    .flags(SDL_MESSAGEBOX_BUTTON_RETURNKEY_DEFAULT | SDL_MESSAGEBOX_BUTTON_ESCAPEKEY_DEFAULT)
                    .buttonID(BUTTON_OK)
                    .text(stack.UTF8("OK"));

            if (helpUrl != null) {
                buttons.get(1)
                        .buttonID(BUTTON_HELP)
                        .text(stack.UTF8("Help"));
            }

            var data = SDL_MessageBoxData.calloc(stack)
                    .flags(icon.flag | SDL_MESSAGEBOX_BUTTONS_LEFT_TO_RIGHT)
                    .window(pWindow)
                    .title(stack.UTF8(title))
                    .message(stack.UTF8(description))
                    .buttons(buttons);

            IntBuffer pButtonId = stack.mallocInt(1);

            while (SDL_ShowMessageBox(data, pButtonId)) {
                if (helpUrl == null || pButtonId.get(0) != BUTTON_HELP) {
                    return;
                }

                SDLMisc.SDL_OpenURL(helpUrl);
            }

            LOGGER.error("Failed to show message box: {}", SDLError.SDL_GetError());
        }
    }

    public enum IconType {
        INFO(SDL_MESSAGEBOX_INFORMATION),
        WARNING(SDL_MESSAGEBOX_WARNING),
        ERROR(SDL_MESSAGEBOX_ERROR);

        private final int flag;

        IconType(int flag) {
            this.flag = flag;
        }
    }
}
