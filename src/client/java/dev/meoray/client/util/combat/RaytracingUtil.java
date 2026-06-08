package dev.meoray.client.util.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import dev.meoray.client.rotation.Rotation;

import java.util.function.Predicate;

public final class RaytracingUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private RaytracingUtil() {}

    public static BlockHitResult raycast(double range, Rotation angle, boolean includeFluids) {
        if (mc.player == null) return null;
        return raycast(mc.player.getCameraPosVec(1.0F), range, angle, includeFluids);
    }

    public static BlockHitResult raycast(Vec3d vec, double range, Rotation angle, boolean includeFluids) {
        Entity entity = mc.cameraEntity;
        if (entity == null || mc.world == null) return null;
        Vec3d rotationVec = angle.toVector();
        Vec3d end = vec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
        FluidHandling fluidHandling = includeFluids ? FluidHandling.ANY : FluidHandling.NONE;
        RaycastContext context = new RaycastContext(vec, end, ShapeType.OUTLINE, fluidHandling, entity);
        return mc.world.raycast(context);
    }

    public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType) {
        return raycast(start, end, shapeType, mc.player);
    }

    public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, Entity entity) {
        if (mc.world == null) return null;
        return mc.world.raycast(new RaycastContext(start, end, shapeType, FluidHandling.NONE, entity));
    }

    public static EntityHitResult raytraceEntity(double range, Rotation angle, Predicate<Entity> filter) {
        Entity entity = mc.cameraEntity;
        if (entity == null) return null;
        Vec3d cameraVec = entity.getCameraPosVec(1.0F);
        Vec3d rotationVec = angle.toVector();
        Vec3d end = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
        Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0D, 1.0D, 1.0D);
        return ProjectileUtil.raycast(entity, cameraVec, end, box,
                e -> !e.isSpectator() && filter.test(e), range * range);
    }

    public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
        if (mc.player == null) return false;
        Vec3d cameraVec = mc.player.getEyePos();
        return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
    }
}
