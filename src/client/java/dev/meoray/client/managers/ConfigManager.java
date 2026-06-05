package dev.meoray.client.managers;

import com.google.gson.*;
import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.ModuleManager;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.core.setting.Setting;
import dev.meoray.client.gui.screen.MeoRayClickGUI;
import dev.meoray.client.gui.theme.Theme;
import dev.meoray.client.gui.theme.ThemeManager;
import dev.meoray.client.hud.draggable.Draggable;
import dev.meoray.client.hud.draggable.DraggableManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir;
    private final ModuleManager moduleManager;
    private final DraggableManager draggableManager;
    private final ThemeManager themeManager;

    public ConfigManager(ModuleManager moduleManager, DraggableManager draggableManager, ThemeManager themeManager) {
        this.moduleManager = moduleManager;
        this.draggableManager = draggableManager;
        this.themeManager = themeManager;
        this.configDir = resolveDir("configs");
    }

    private Path resolveDir(String sub) {
        Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        Path dir = gameDir.resolve("MeoRay").resolve(sub);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dir;
    }

    public void loadAll() {
        loadModules();
        loadDraggables();
        loadTheme();
        loadGui();
    }

    public void saveAll() {
        saveModules();
        saveDraggables();
        saveTheme();
        saveGui();
    }

    public void loadModules() {
        for (Module m : moduleManager.getModules()) {
            loadModule(m);
        }
    }

    public void loadModule(Module module) {
        String name = sanitize(module.getName());
        Path file = configDir.resolve(name + ".json");
        if (!Files.exists(file)) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("bind")) module.setKey(obj.get("bind").getAsInt());
            if (obj.has("settings")) {
                JsonObject settingsObj = obj.getAsJsonObject("settings");
                for (Setting<?> s : module.getSettings()) {
                    if (settingsObj.has(s.getName())) {
                        applySetting(s, settingsObj.get(s.getName()));
                    }
                }
            }
            if (obj.has("enabled") && obj.get("enabled").getAsBoolean()) {
                module.setEnabled(true);
            }
        } catch (Exception e) {
            System.err.println("[ConfigManager] Failed to load " + name + ": " + e.getMessage());
        }
    }

    public void saveModules() {
        for (Module m : moduleManager.getModules()) {
            saveModule(m);
        }
    }

    public void saveModule(Module module) {
        String name = sanitize(module.getName());
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", module.isEnabled());
        obj.addProperty("bind", module.getKey());
        JsonObject settingsObj = new JsonObject();
        for (Setting<?> s : module.getSettings()) {
            JsonElement el = serializeSetting(s);
            if (el != null) settingsObj.add(s.getName(), el);
        }
        obj.add("settings", settingsObj);
        writeJson(configDir.resolve(name + ".json"), obj);
    }

    private JsonElement serializeSetting(Setting<?> s) {
        if (s instanceof BooleanSetting bs) return new JsonPrimitive(bs.getValue());
        if (s instanceof NumberSetting ns) return new JsonPrimitive(ns.getValue());
        if (s instanceof ModeSetting ms) return new JsonPrimitive(ms.getValue());
        return null;
    }

    private void applySetting(Setting<?> s, JsonElement el) {
        try {
            if (s instanceof BooleanSetting bs) bs.setValue(el.getAsBoolean());
            else if (s instanceof NumberSetting ns) ns.setValue(el.getAsDouble());
            else if (s instanceof ModeSetting ms) ms.setValue(el.getAsString());
        } catch (Exception e) {
            System.err.println("[ConfigManager] Error deserializing setting '" + s.getName() + "': " + e.getMessage());
        }
    }

    public void loadDraggables() {
        if (draggableManager == null) return;
        Path file = configDir.resolve("draggables.json");
        if (!Files.exists(file)) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Draggable d : draggableManager.getDraggables()) {
                if (root.has(d.getName())) {
                    JsonObject obj = root.getAsJsonObject(d.getName());
                    if (obj.has("x")) d.setX(obj.get("x").getAsFloat());
                    if (obj.has("y")) d.setY(obj.get("y").getAsFloat());
                }
            }
        } catch (Exception e) {
            System.err.println("[ConfigManager] Failed to load draggables: " + e.getMessage());
        }
    }

    public void saveDraggables() {
        if (draggableManager == null) return;
        JsonObject root = new JsonObject();
        for (Draggable d : draggableManager.getDraggables()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", d.getX());
            obj.addProperty("y", d.getY());
            root.add(d.getName(), obj);
        }
        writeJson(configDir.resolve("draggables.json"), root);
    }

    public void loadTheme() {
        if (themeManager == null) return;
        Path file = configDir.resolve("theme.json");
        if (!Files.exists(file)) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("index")) {
                themeManager.select(obj.get("index").getAsInt());
            }
        } catch (Exception e) {
            System.err.println("[ConfigManager] Failed to load theme: " + e.getMessage());
        }
    }

    public void saveTheme() {
        if (themeManager == null) return;
        JsonObject obj = new JsonObject();
        obj.addProperty("index", themeManager.getSelectedIndex());
        writeJson(configDir.resolve("theme.json"), obj);
    }

    public void loadGui() {
        Path file = configDir.resolve("gui.json");
        if (!Files.exists(file)) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            MeoRayClickGUI gui = MeoRayClient.INSTANCE.getClickGUI();
            if (gui == null) return;
            if (obj.has("mainScale")) {
                float ms = obj.get("mainScale").getAsFloat();
                gui.setMainScaleSetting(ms);
                MeoRayClient.mainScale = ms;
            }
            if (obj.has("snowOn")) MeoRayClient.snowOn = obj.get("snowOn").getAsBoolean();
            if (obj.has("starsOn")) MeoRayClient.starsOn = obj.get("starsOn").getAsBoolean();
            if (obj.has("bubblesOn")) MeoRayClient.bubblesOn = obj.get("bubblesOn").getAsBoolean();
            if (obj.has("guiOffsetX")) gui.setGuiOffsetX(obj.get("guiOffsetX").getAsFloat());
            if (obj.has("guiOffsetY")) gui.setGuiOffsetY(obj.get("guiOffsetY").getAsFloat());
        } catch (Exception e) {
            System.err.println("[ConfigManager] Failed to load gui: " + e.getMessage());
        }
    }

    public void saveGui() {
        JsonObject obj = new JsonObject();
        MeoRayClickGUI gui = MeoRayClient.INSTANCE.getClickGUI();
        if (gui != null) {
            obj.addProperty("mainScale", gui.getMainScaleSetting());
            obj.addProperty("guiOffsetX", gui.getGuiOffsetX());
            obj.addProperty("guiOffsetY", gui.getGuiOffsetY());
        }
        obj.addProperty("snowOn", MeoRayClient.snowOn);
        obj.addProperty("starsOn", MeoRayClient.starsOn);
        obj.addProperty("bubblesOn", MeoRayClient.bubblesOn);
        writeJson(configDir.resolve("gui.json"), obj);
    }

    private void writeJson(Path path, JsonObject data) {
        try (Writer writer = new OutputStreamWriter(
                Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[ConfigManager] Write error: " + e.getMessage());
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
