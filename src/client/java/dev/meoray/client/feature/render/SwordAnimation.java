package dev.meoray.client.feature.render;

import dev.meoray.client.core.Category;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.ModeSetting;
import dev.meoray.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

public class SwordAnimation extends Module {

    public final ModeSetting mode = add(new ModeSetting("Mode", "Swang",
        "Swang", "Stab", "Slide", "Spin", "Smooth", "Sigma"));

    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 3.0, 0.05));

    private static SwordAnimation INSTANCE;

    private float smoothSwing = 0f;
    private float smoothEquip = 0f;
    private float idleTime = 0f;
    private long lastTime = System.currentTimeMillis();

    public SwordAnimation() {
        super("SwordAnimation", "Красивые анимации меча", Category.RENDER);
        INSTANCE = this;
    }

    public static SwordAnimation get() {
        return INSTANCE;
    }

    public boolean isSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            return stack.isIn(ItemTags.SWORDS);
        } catch (Exception e) {
            return stack.getItem().toString().toLowerCase().contains("sword");
        }
    }

    public void applyTransform(MatrixStack matrices, Arm arm, float swingProgress, float equipProgress, ItemStack stack) {
        if (!isEnabled() || !isSword(stack)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastTime) / 1000f);
        lastTime = now;
        idleTime += dt;

        float smoothFactor = Math.min(1f, dt * 12f);
        smoothSwing += (swingProgress - smoothSwing) * smoothFactor;
        smoothEquip += (equipProgress - smoothEquip) * smoothFactor;

        boolean rightArm = arm == Arm.RIGHT;
        int side = rightArm ? 1 : -1;

        float sp = speed.getValue().floatValue();
        float swing = (float) Math.sin(Math.sqrt(Math.max(0f, smoothSwing)) * Math.PI);
        float swingEase = (float) (1.0 - Math.pow(1.0 - smoothSwing, 3));
        float wave = (float) Math.sin(smoothSwing * Math.PI * 2f);

        switch (mode.getValue()) {
            case "Swang" -> {
                matrices.translate(
                    -0.4f * swing * side * sp,
                    0.1f * swing * sp,
                    -0.3f * swing * sp
                );
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(swing * 35f * side * sp));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(swing * 20f * side * sp));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-swing * 40f * sp));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wave * 5f * sp));
            }

            case "Stab" -> {
                matrices.translate(
                    -0.05f * swingEase * side * sp,
                    -0.1f * swing * sp,
                    -0.55f * swingEase * sp
                );
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-swing * 55f * sp));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(swing * 90f * side * sp));
            }

            case "Slide" -> {
                matrices.translate(
                    swing * 0.5f * side * sp,
                    swing * 0.05f * sp,
                    -swing * 0.2f * sp
                );
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(swing * 45f * side * sp));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-swing * 25f * sp));
            }

            case "Spin" -> {
                matrices.translate(0f, 0.1f * swing * sp, -0.15f * swing * sp);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(smoothSwing * 360f * side * sp));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-swing * 25f * sp));
            }

            case "Smooth" -> {
                float smooth = (float) (1.0 - Math.pow(1.0 - smoothSwing, 4));
                matrices.translate(
                    -smooth * 0.2f * side * sp,
                    smooth * 0.08f * sp,
                    -smooth * 0.3f * sp
                );
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(smooth * 25f * side * sp));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-smooth * 30f * sp));
            }

            case "Sigma" -> {
                float idleS = (float) Math.sin(idleTime * 2.5) * 0.02f;
                float idleS2 = (float) Math.cos(idleTime * 1.8) * 0.015f;
                matrices.translate(
                    idleS * side - 0.15f * swingEase * side * sp,
                    idleS2 - 0.05f * swing * sp,
                    -0.4f * swingEase * sp
                );
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(swingEase * 28f * side * sp));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-swingEase * 42f * sp));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(wave * 10f * side * sp));
            }
        }
    }
}
