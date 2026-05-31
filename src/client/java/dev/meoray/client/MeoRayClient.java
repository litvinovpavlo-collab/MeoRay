package dev.meoray.client;

import dev.meoray.client.account.AccountManager;
import dev.meoray.client.core.ModuleManager;
import dev.meoray.client.gui.screen.MeoRayClickGUI;
//import dev.nova.client.gui.ClickGUI;
import dev.meoray.client.gui.theme.ThemeManager;
import dev.meoray.client.hud.CoordsHUD;
import dev.meoray.client.hud.InfoHUD;
import dev.meoray.client.util.MeoRayRPC;
import dev.meoray.client.util.MeoRayRPCUpdater;
import dev.meoray.client.util.WindowTitleAnimator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import dev.meoray.client.core.Module;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.stb.STBImage;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class MeoRayClient implements ClientModInitializer {
    public static MeoRayClient INSTANCE;
    public static float mainScale = 1.0f;
    public final ModuleManager moduleManager = new ModuleManager();
    private final ThemeManager themeManager = new ThemeManager();
    private KeyBinding toggleKey;
    private final BitSet prevKeys = new BitSet(256);

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        AccountManager.load();
        System.out.println("[MeoRay] Loaded " + AccountManager.getAccounts().size() + " accounts");

        moduleManager.init();
        MeoRayRPC.start();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open MeoRay GUI",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "MeoRay"
        ));

        final boolean[] iconLoaded = {false};
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!iconLoaded[0] && client.getWindow() != null) {
                try {
                    loadWindowIcon();
                    iconLoaded[0] = true;
                } catch (Exception e) {
                    System.err.println("[MeoRay] Failed to load window icon: " + e.getMessage());
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MeoRayClickGUI());
                }
            }

            if (client.currentScreen == null && client.player != null) {
                long handle = client.getWindow().getHandle();
                BitSet curKeys = new BitSet(256);
                for (Module mod : moduleManager.getModules()) {
                    int k = mod.getKey();
                    if (k == 0 || k >= 256) continue;
                    boolean down = GLFW.glfwGetKey(handle, k) == GLFW.GLFW_PRESS;
                    curKeys.set(k, down);
                    if (down && !prevKeys.get(k)) {
                        mod.toggle();
                    }
                }
                prevKeys.clear();
                prevKeys.or(curKeys);
            }

            moduleManager.onTick();
            WindowTitleAnimator.tick();
            MeoRayRPCUpdater.tick();
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            Module iface = moduleManager.getByName("Interface");
            boolean wmOn = false;
            boolean coordsOn = false;
            if (iface != null && iface.isEnabled()) {
                for (dev.meoray.client.core.setting.Setting<?> s : iface.getSettings()) {
                    if (s.getName().equals("Watermark")) wmOn = (boolean) s.getValue();
                    if (s.getName().equals("Coordinates")) coordsOn = (boolean) s.getValue();
                }
            }
            context.getMatrices().push();
            float ms = MeoRayClient.mainScale;
            context.getMatrices().scale(ms, ms, 1.0f);
            if (wmOn) InfoHUD.render(context);
            if (coordsOn) CoordsHUD.render(context);
            context.getMatrices().pop();
        });

        System.out.println("MeoRay client initialized");
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    private void loadWindowIcon() throws Exception {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.getWindow() == null) return;

        long window = client.getWindow().getHandle();

        try (var stream = getClass().getResourceAsStream("/assets/meoray/icon32.png")) {
            if (stream != null) {
                byte[] data = stream.readAllBytes();
                ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
                buffer.put(data);
                buffer.flip();

                STBImage.stbi_set_flip_vertically_on_load(false);
                int[] width = new int[1];
                int[] height = new int[1];
                int[] channels = new int[1];

                ByteBuffer pixels = STBImage.stbi_load_from_memory(
                        buffer, width, height, channels, 4
                );

                if (pixels != null) {
                    try (var iconBuf = org.lwjgl.glfw.GLFWImage.malloc(1)) {
                        iconBuf.get(0).set(width[0], height[0], pixels);
                        GLFW.glfwSetWindowIcon(window, iconBuf);
                    }
                    STBImage.stbi_image_free(pixels);
                    System.out.println("[MeoRay] Window icon loaded successfully");
                }
            }
        }
    }
}
