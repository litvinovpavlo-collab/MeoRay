package dev.meoray.client;

import dev.meoray.client.account.AccountManager;
import dev.meoray.client.core.ModuleManager;
import dev.meoray.client.managers.ConfigManager;
import dev.meoray.client.gui.screen.MeoRayClickGUI;
//import dev.nova.client.gui.ClickGUI;
import dev.meoray.client.gui.theme.ThemeManager;
import dev.meoray.client.hud.CoordsHUD;
import dev.meoray.client.hud.HudElement;
import dev.meoray.client.hud.InfoHUD;
import dev.meoray.client.hud.draggable.DraggableManager;
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
    public static float mainScale = 1.00f;
    public static boolean snowOn = false;
    public static boolean starsOn = false;
    public static boolean bubblesOn = false;
    public final ModuleManager moduleManager = new ModuleManager();
    private final ThemeManager themeManager = new ThemeManager();
    private final DraggableManager draggableManager = new DraggableManager();
    private ConfigManager configManager;
    private MeoRayClickGUI clickGUI;
    private final InfoHUD infoHUD = new InfoHUD();
    private final CoordsHUD coordsHUD = new CoordsHUD();
    private KeyBinding toggleKey;
    private final BitSet prevKeys = new BitSet(256);
    private int saveTimer = 0;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        AccountManager.load();
        System.out.println("[MeoRay] Loaded " + AccountManager.getAccounts().size() + " accounts");

        moduleManager.init();
        draggableManager.add(infoHUD);
        draggableManager.add(coordsHUD);
        MeoRayRPC.start();

        configManager = new ConfigManager(moduleManager, draggableManager, themeManager);
        configManager.loadAll();
        System.out.println("[MeoRay] Config loaded");

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
            // Open ClickGUI on Right Shift
            while (toggleKey.wasPressed()) {
                if (client.currentScreen instanceof MeoRayClickGUI) {
                    client.setScreen(null);
                } else {
                    clickGUI = new MeoRayClickGUI();
                    client.setScreen(clickGUI);
                }
            }
            // Update sky color from current theme
            int accentRgb = themeManager.getRenderTheme().accent() & 0x00FFFFFF;
            dev.meoray.client.util.render.SkyConfig.setBaseColorRgb(accentRgb);
            moduleManager.onTick();
            WindowTitleAnimator.tick();
            MeoRayRPCUpdater.tick();

            saveTimer++;
            if (saveTimer >= 1200) {
                saveTimer = 0;
                if (configManager != null) configManager.saveAll();
            }
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
            // Update draggable positions every frame for any editable screen
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen
                    || mc.currentScreen instanceof MeoRayClickGUI) {
                double mx = mc.mouse.getX();
                double my = mc.mouse.getY();
                float ms = MeoRayClient.mainScale;
                if (draggableManager != null) {
                    draggableManager.updatePositions((float) (mx / ms), (float) (my / ms));
                }
            }
            context.getMatrices().push();
            float ms = MeoRayClient.mainScale;
            context.getMatrices().scale(ms, ms, 1.0f);
            if (wmOn) infoHUD.render(context);
            if (coordsOn) coordsHUD.render(context);
            context.getMatrices().pop();
        });

        System.out.println("MeoRay client initialized");
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public DraggableManager getDraggableManager() {
        return draggableManager;
    }

    public MeoRayClickGUI getClickGUI() {
        return clickGUI;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public void onShutdown() {
        if (configManager != null) {
            configManager.saveAll();
            System.out.println("[MeoRay] Config saved");
        }
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
