package dev.meoray.client.feature.render;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.core.setting.NumberSetting;
import dev.meoray.client.core.setting.SectionSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class FreeCam extends Module {

    public static FreeCam INSTANCE;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final SectionSetting movementSection = add(new SectionSetting("Movement"));
    public final NumberSetting baseSpeed = movementSection.add(
            new NumberSetting("Speed", 8.0, 1.0, 30.0, 0.5));
    public final NumberSetting boostMultiplier = movementSection.add(
            new NumberSetting("Boost Multiplier", 3.0, 1.5, 8.0, 0.1));
    public final NumberSetting smoothness = movementSection.add(
            new NumberSetting("Smoothness", 0.15, 0.05, 1.0, 0.05));

    public final SectionSetting mouseSection = add(new SectionSetting("Mouse"));
    public final NumberSetting mouseSensitivity = mouseSection.add(
            new NumberSetting("Mouse Sensitivity", 1.0, 0.1, 3.0, 0.05));
    public final BooleanSetting scrollSpeed = mouseSection.add(
            new BooleanSetting("Scroll = Change Speed", true));
    public final BooleanSetting rightClickBoost = mouseSection.add(
            new BooleanSetting("Right Click = Boost", false));

    public final SectionSetting safetySection = add(new SectionSetting("Safety"));
    public final BooleanSetting blockInteractions = safetySection.add(
            new BooleanSetting("Block All Interactions", true));
    public final NumberSetting maxDistance = safetySection.add(
            new NumberSetting("Max Distance", 50.0, 10.0, 200.0, 5.0));
    public final BooleanSetting returnOnDisable = safetySection.add(
            new BooleanSetting("Return Camera On Disable", true));

    private Vec3d camPos = Vec3d.ZERO;
    public float camYaw = 0f;
    public float camPitch = 0f;

    private Vec3d camVelocity = Vec3d.ZERO;
    private Vec3d targetVelocity = Vec3d.ZERO;
    private double speedMultiplier = 1.0;
    private long lastFrameTime = 0;

    private boolean keyW, keyA, keyS, keyD, keySpace, keyShift, keyCtrl, mouseRight;

    public FreeCam() {
        super("FreeCam", "Render-only camera without cheating the server", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            setEnabled(false);
            return;
        }

        camPos = mc.player.getCameraPosVec(1.0f);
        camYaw = mc.player.getYaw();
        camPitch = mc.player.getPitch();
        camVelocity = Vec3d.ZERO;
        targetVelocity = Vec3d.ZERO;
        speedMultiplier = 1.0;
        lastFrameTime = System.nanoTime();

        super.onEnable();
    }

    @Override
    public void onDisable() {
        camVelocity = Vec3d.ZERO;
        targetVelocity = Vec3d.ZERO;

        super.onDisable();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        if (mc.player == null) return;

        Vec3d playerPos = mc.player.getCameraPosVec(1.0f);
        double dist = camPos.distanceTo(playerPos);

        if (dist > maxDistance.getValue()) {
            Vec3d toPlayer = playerPos.subtract(camPos).normalize();
            camPos = playerPos.subtract(toPlayer.multiply(maxDistance.getValue() * 0.9));
        }
    }

    public void onFrameUpdate() {
        if (!isEnabled()) return;
        if (mc.player == null) return;

        long now = System.nanoTime();
        float deltaTime = (now - lastFrameTime) / 1_000_000_000f;
        lastFrameTime = now;

        if (deltaTime > 0.1f) deltaTime = 0.1f;
        if (deltaTime < 0.0001f) deltaTime = 0.0001f;

        pollInput();
        updateTargetVelocity();
        smoothVelocity(deltaTime);

        camPos = camPos.add(
                camVelocity.x * deltaTime,
                camVelocity.y * deltaTime,
                camVelocity.z * deltaTime
        );
    }

    private void pollInput() {
        long handle = mc.getWindow().getHandle();

        if (mc.currentScreen != null) {
            keyW = keyA = keyS = keyD = false;
            keySpace = keyShift = keyCtrl = false;
            mouseRight = false;
            return;
        }

        keyW     = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        keyA     = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        keyS     = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        keyD     = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        keySpace = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        keyShift = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
        keyCtrl  = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
        mouseRight = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private void updateTargetVelocity() {
        if (mc.currentScreen != null) {
            targetVelocity = Vec3d.ZERO;
            return;
        }

        float forward = 0, strafe = 0, vertical = 0;
        if (keyW) forward += 1;
        if (keyS) forward -= 1;
        if (keyA) strafe += 1;
        if (keyD) strafe -= 1;
        if (keySpace) vertical += 1;
        if (keyShift) vertical -= 1;

        float yawRad = (float) Math.toRadians(camYaw);
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);
        double strafeX = Math.cos(yawRad);
        double strafeZ = Math.sin(yawRad);

        double moveX = dirX * forward + strafeX * strafe;
        double moveZ = dirZ * forward + strafeZ * strafe;

        double horizLen = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (horizLen > 1.0) {
            moveX /= horizLen;
            moveZ /= horizLen;
        }

        double currentSpeed = baseSpeed.getValue() * speedMultiplier;
        boolean boost = keyCtrl || (rightClickBoost.getValue() && mouseRight);
        if (boost) currentSpeed *= boostMultiplier.getValue();

        targetVelocity = new Vec3d(
                moveX * currentSpeed,
                vertical * currentSpeed,
                moveZ * currentSpeed
        );
    }

    private void smoothVelocity(float deltaTime) {
        double smooth = smoothness.getValue();
        double t = 1.0 - Math.exp(-deltaTime / smooth);

        camVelocity = new Vec3d(
                camVelocity.x + (targetVelocity.x - camVelocity.x) * t,
                camVelocity.y + (targetVelocity.y - camVelocity.y) * t,
                camVelocity.z + (targetVelocity.z - camVelocity.z) * t
        );
    }

    public void onMouseDelta(double cursorDeltaX, double cursorDeltaY) {
        if (!isEnabled()) return;

        double sens = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        sens = sens * sens * sens * 8.0 * mouseSensitivity.getValue();

        boolean invert = mc.options.getInvertYMouse().getValue();
        double yDelta = invert ? -cursorDeltaY : cursorDeltaY;

        camYaw += cursorDeltaX * sens * 0.15;
        camPitch += yDelta * sens * 0.15;

        camPitch = MathHelper.clamp(camPitch, -89.9f, 89.9f);
        camYaw = camYaw % 360f;
    }

    public void onMouseScroll(double scrollDelta) {
        if (!scrollSpeed.getValue()) return;

        if (scrollDelta > 0) {
            speedMultiplier = Math.min(5.0, speedMultiplier + 0.15);
        } else if (scrollDelta < 0) {
            speedMultiplier = Math.max(0.1, speedMultiplier - 0.15);
        }
    }

    public Vec3d getCamPos() { return camPos; }
    public float getCamYaw() { return camYaw; }
    public float getCamPitch() { return camPitch; }

    public boolean shouldBlockInteractions() {
        return isEnabled() && blockInteractions.getValue();
    }
}
