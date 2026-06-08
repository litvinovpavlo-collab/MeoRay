package dev.meoray.client;

import dev.meoray.client.account.AccountManager;
import dev.meoray.client.account.altmanager.AltManagerScreen;
import dev.meoray.client.account.altmanager.NickNameManager;
import dev.meoray.client.core.ModuleManager;
import dev.meoray.client.managers.ConfigManager;
import dev.meoray.client.managers.FriendManager;
import dev.meoray.client.gui.screen.MeoRayClickGUI;
import dev.meoray.client.gui.theme.ThemeManager;
import dev.meoray.client.util.other.NameGen;
import dev.meoray.client.hud.CoordsHUD;
import dev.meoray.client.hud.HudElement;
import dev.meoray.client.hud.ArmorHUD;
import dev.meoray.client.hud.InfoHUD;
import dev.meoray.client.hud.KeyBindHUD;
import dev.meoray.client.hud.InventoryHUD;
import dev.meoray.client.hud.KeyListHUD;
import dev.meoray.client.hud.PotionsHUD;
import dev.meoray.client.hud.TargetHUD;
import dev.meoray.client.hud.draggable.DraggableManager;
import dev.meoray.client.util.MeoRayRPC;
import dev.meoray.client.util.MeoRayRPCUpdater;
import dev.meoray.client.util.cape.CapeGenerator;
import dev.meoray.client.util.WindowTitleAnimator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import dev.meoray.client.core.Module;
import dev.meoray.client.feature.render.ESP;
import dev.meoray.client.feature.render.HitEffect;
import dev.meoray.client.feature.render.JumpCircles;
import dev.meoray.client.feature.render.ItemESP;
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
    private final PotionsHUD potionsHUD = new PotionsHUD();
    private final InventoryHUD inventoryHUD = new InventoryHUD();
    private final FriendManager friendManager = new FriendManager();
    private final TargetHUD targetHUD = new TargetHUD();
    private final KeyListHUD keyListHUD = new KeyListHUD();
    private final ArmorHUD armorHUD = new ArmorHUD();
    private final KeyBindHUD keyBindHUD = new KeyBindHUD();
    private final NickNameManager nickNameManager = new NickNameManager();
    private final NameGen nameGen = new NameGen();
    private AltManagerScreen altManagerScreen;
    private KeyBinding toggleKey;
    private final BitSet prevKeys = new BitSet(256);
    private int saveTimer = 0;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        // Pre-init GLFW before Minecraft's GLX._initGlfw() to work around Fabric Loom dev env bug
        GLFW.glfwInit();

        AccountManager.load();
        System.out.println("[MeoRay] Loaded " + AccountManager.getAccounts().size() + " accounts");

        moduleManager.init();
        draggableManager.add(infoHUD);
        draggableManager.add(coordsHUD);
        draggableManager.add(potionsHUD);
        draggableManager.add(inventoryHUD);
        draggableManager.add(targetHUD);
        draggableManager.add(keyListHUD);
        draggableManager.add(armorHUD);
        draggableManager.add(keyBindHUD);
        CapeGenerator.registerCapeTexture();
        MeoRayRPC.start();

        configManager = new ConfigManager(moduleManager, draggableManager, themeManager);
        configManager.loadAll();
        configManager.loadNickNames();
        System.out.println("[MeoRay] Config loaded (" + nickNameManager.getNickNames().size() + " nicknames)");

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
            // Module keybinds
            if (client.getWindow() != null && client.getWindow().getHandle() != 0) {
                long handle = client.getWindow().getHandle();
                for (Module mod : moduleManager.getModules()) {
                    int k = mod.getKey();
                    if (k <= 0 || k >= 256) continue;
                    boolean pressed = GLFW.glfwGetKey(handle, k) == GLFW.GLFW_PRESS;
                    boolean wasPressed = prevKeys.get(k);
                    if (pressed && !wasPressed) mod.toggle();
                    prevKeys.set(k, pressed);
                }
                if (client.currentScreen != null) prevKeys.clear();
            }
            // Update sky color from current theme
            int accentRgb = themeManager.getRenderTheme().accent() & 0x00FFFFFF;
            dev.meoray.client.util.render.SkyConfig.setBaseColorRgb(accentRgb);
            moduleManager.onTick();
            moduleManager.onTickMovement();
            moduleManager.onMoveInput();
            Module heMod = moduleManager.getByName("HitEffect");
            if (heMod != null && heMod.isEnabled() && heMod instanceof HitEffect he) {
                he.tick();
            }
            WindowTitleAnimator.tick();
            dev.meoray.client.hud.CustomHotbar.tick();
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
            boolean potionsOn = false;
            boolean invOn = false;
            boolean targetOn = false;
            boolean keyListOn = false;
            boolean armorOn = false;
            boolean keyBindsOn = false;
            boolean hotbarOn = false;
            boolean notifyOn = false;

            if (iface != null && iface.isEnabled()) {
                for (dev.meoray.client.core.setting.Setting<?> s : iface.getSettings()) {
                    switch (s.getName()) {
                        case "Watermark" -> wmOn = (boolean) s.getValue();
                        case "Coordinates" -> coordsOn = (boolean) s.getValue();
                        case "Potions" -> potionsOn = (boolean) s.getValue();
                        case "InventoryHUD" -> invOn = (boolean) s.getValue();
                        case "TargetHUD" -> targetOn = (boolean) s.getValue();
                        case "KeyList" -> keyListOn = (boolean) s.getValue();
                        case "ArmorHUD" -> armorOn = (boolean) s.getValue();
                        case "KeyBinds" -> keyBindsOn = (boolean) s.getValue();
                        case "Hotbar" -> hotbarOn = (boolean) s.getValue();
                        case "Notifications" -> notifyOn = (boolean) s.getValue();
                    }
                }
            }

            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            float ms = MeoRayClient.mainScale;

            // Правильно конвертируем raw pixel mouse -> scaled coords -> делим на ms
            double rawMx = mc.mouse.getX();
            double rawMy = mc.mouse.getY();
            int winW = mc.getWindow().getWidth();
            int winH = mc.getWindow().getHeight();
            int scW = mc.getWindow().getScaledWidth();
            int scH = mc.getWindow().getScaledHeight();
            float mx = (float) (rawMx * scW / winW / ms);
            float my = (float) (rawMy * scH / winH / ms);

            if (draggableManager != null) {
                draggableManager.updatePositions(mx, my);
            }

            context.getMatrices().push();
            context.getMatrices().scale(ms, ms, 1.0f);
            if (wmOn) infoHUD.render(context);
            if (coordsOn) coordsHUD.render(context);
            if (potionsOn) potionsHUD.render(context);
            if (invOn) inventoryHUD.render(context);
            if (targetOn) targetHUD.render(context);
            if (keyListOn) keyListHUD.render(context);
            if (armorOn) armorHUD.render(context);
            if (keyBindsOn) keyBindHUD.render(context);
            if (hotbarOn) dev.meoray.client.hud.CustomHotbar.render(context);

            Module arrows = moduleManager.getByName("Arrows");
            if (arrows instanceof dev.meoray.client.feature.render.Arrows a) {
                try { context.draw(); a.render(context); context.draw(); } catch (Throwable ignored) {}
            }

            context.getMatrices().pop();

            // Notifications — рендерятся вне scale, всегда поверх
            if (notifyOn) {
                dev.meoray.client.notify.NotificationRenderer.render(context);
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickDeltaManager) -> {
            float tickDelta = tickDeltaManager.getTickDelta(false);
            Module esp = INSTANCE.moduleManager.getByName("ESP");
            if (esp != null && esp.isEnabled() && esp instanceof ESP espModule) {
                try { ctx.draw(); espModule.onRender2D(ctx, tickDelta); ctx.draw(); } catch (Throwable ignored) {}
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickDeltaManager) -> {
            float tickDelta = tickDeltaManager.getTickDelta(false);
            Module ie = INSTANCE.moduleManager.getByName("ItemESP");
            if (ie != null && ie.isEnabled() && ie instanceof ItemESP itemESP) {
                try { ctx.draw(); itemESP.onHudRender(ctx, tickDelta); ctx.draw(); } catch (Throwable ignored) {}
            }

            Module xr = INSTANCE.moduleManager.getByName("XRay");
            if (xr != null && xr.isEnabled() && xr instanceof dev.meoray.client.feature.render.XRay xRay) {
                try { ctx.draw(); xRay.onHudRender(ctx, tickDelta); ctx.draw(); } catch (Throwable ignored) {}
            }

            Module ce = INSTANCE.moduleManager.getByName("ChestESP");
            if (ce != null && ce.isEnabled() && ce instanceof dev.meoray.client.feature.render.ChestESP chestESP) {
                try { ctx.draw(); chestESP.onHudRender(ctx, tickDelta); ctx.draw(); } catch (Throwable ignored) {}
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            Module esp = INSTANCE.moduleManager.getByName("ESP");
            if (esp != null && esp.isEnabled() && esp instanceof ESP espModule) {
                espModule.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }

            Module he = INSTANCE.moduleManager.getByName("HitEffect");
            if (he != null && he.isEnabled() && he instanceof HitEffect heMod) {
                heMod.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }

            Module ch = INSTANCE.moduleManager.getByName("ChinaHat");
            if (ch != null && ch.isEnabled() && ch instanceof dev.meoray.client.feature.render.ChinaHat chinaHat) {
                chinaHat.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }
            Module jc = INSTANCE.moduleManager.getByName("JumpCircles");
            if (jc != null && jc.isEnabled() && jc instanceof JumpCircles jumpCircles) {
                jumpCircles.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }

            Module ie = INSTANCE.moduleManager.getByName("ItemESP");
            if (ie != null && ie.isEnabled() && ie instanceof ItemESP itemESP) {
                itemESP.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }

            Module xr = INSTANCE.moduleManager.getByName("XRay");
            if (xr != null && xr.isEnabled() && xr instanceof dev.meoray.client.feature.render.XRay xRay) {
                xRay.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }

            Module aa = INSTANCE.moduleManager.getByName("AttackAura");
            if (aa != null && aa.isEnabled() && aa instanceof dev.meoray.client.feature.combat.AttackAura aura) {
                aura.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }

            Module ce = INSTANCE.moduleManager.getByName("ChestESP");
            if (ce != null && ce.isEnabled() && ce instanceof dev.meoray.client.feature.render.ChestESP chestESP) {
                chestESP.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }

            Module te = INSTANCE.moduleManager.getByName("TargetESP");
            if (te != null && te.isEnabled() && te instanceof dev.meoray.client.feature.render.TargetESP targetESP) {
                targetESP.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }

            Module pt = INSTANCE.moduleManager.getByName("ProjectileTrails");
            if (pt != null && pt.isEnabled() && pt instanceof dev.meoray.client.feature.render.ProjectileTrails projectileTrails) {
                projectileTrails.onWorldRender(context.matrixStack(), context.tickCounter().getTickDelta(false));
            }
        });

        System.out.println("MeoRay client initialized");
    }

    public FriendManager getFriendManager() {
        return friendManager;
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

    public NickNameManager getNickNameManager() {
        return nickNameManager;
    }

    public NameGen getNameGen() {
        return nameGen;
    }

    public AltManagerScreen getAltManagerScreen() {
        if (altManagerScreen == null) {
            altManagerScreen = new AltManagerScreen(null);
        }
        return altManagerScreen;
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
