package dev.meoray.client.util.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import dev.meoray.client.rotation.Rotation;

public final class MultipointUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static Vec3d rotationPoint = Vec3d.ZERO;
    private static Vec3d rotationMotion = Vec3d.ZERO;

    private MultipointUtils() {}

    public static Vec3d getNearestPoint(Entity target, double distance) {
        if (mc.player == null || mc.world == null) return target.getBoundingBox().getCenter();
        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
        double maxDistSq = distance * distance;
        Box box = target.getBoundingBox();
        Vec3d boxCenter = box.getCenter();
        double stepXZ = 0.1D;
        double stepY = 0.1D;
        Vec3d bestPoint = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (double x = box.minX; x <= box.maxX; x += stepXZ) {
            for (double y = box.minY; y <= box.maxY; y += stepY) {
                for (double z = box.minZ; z <= box.maxZ; z += stepXZ) {
                    Vec3d point = new Vec3d(x, y, z);
                    if (eyePos.squaredDistanceTo(point) > maxDistSq) continue;
                    RaycastContext context = new RaycastContext(eyePos, point, ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
                    HitResult result = mc.world.raycast(context);
                    boolean visible = result.getType() == Type.MISS || result.getType() != Type.BLOCK;
                    if (visible) {
                        double score = boxCenter.squaredDistanceTo(point);
                        if (score < bestScore) {
                            bestScore = score;
                            bestPoint = point;
                        }
                    }
                }
            }
        }

        return bestPoint == null ? target.getBoundingBox().getCenter() : bestPoint;
    }

    public static Vec3d getMultipoint(Entity target, double distance) {
        if (mc.player == null) return target.getPos();
        float minMotionXZ = 0.01F;
        float maxMotionXZ = 0.03F;
        float minMotionY = 0.01F;
        float maxMotionY = 0.03F;
        double lengthX = target.getBoundingBox().getLengthX();
        double lengthY = target.getBoundingBox().getLengthY();
        double lengthZ = target.getBoundingBox().getLengthZ();

        if (rotationMotion.equals(Vec3d.ZERO)) {
            rotationMotion = new Vec3d(
                    MathUtil.random(-0.05D, 0.05D),
                    MathUtil.random(-0.05D, 0.05D),
                    MathUtil.random(-0.05D, 0.05D));
        }

        rotationPoint = rotationPoint.add(rotationMotion);

        if (rotationPoint.x >= (lengthX - 0.05D) / 2.0D) {
            rotationMotion = new Vec3d(-MathUtil.random(minMotionXZ, maxMotionXZ), rotationMotion.y, rotationMotion.z);
        }
        if (rotationPoint.y >= lengthY / 2.0D) {
            rotationMotion = new Vec3d(rotationMotion.x, -MathUtil.random(minMotionY, maxMotionY), rotationMotion.z);
        }
        if (rotationPoint.z >= (lengthZ - 0.05D) / 2.0D) {
            rotationMotion = new Vec3d(rotationMotion.x, rotationMotion.y, -MathUtil.random(minMotionXZ, maxMotionXZ));
        }
        if (rotationPoint.x <= -(lengthX - 0.05D) / 2.0D) {
            rotationMotion = new Vec3d(MathUtil.random(minMotionXZ, 0.03D), rotationMotion.y, rotationMotion.z);
        }
        if (rotationPoint.y <= 0.1D) {
            rotationMotion = new Vec3d(rotationMotion.x, MathUtil.random(minMotionY, maxMotionY), rotationMotion.z);
        }
        if (rotationPoint.z <= -(lengthZ - 0.05D) / 2.0D) {
            rotationMotion = new Vec3d(rotationMotion.x, rotationMotion.y, MathUtil.random(minMotionXZ, maxMotionXZ));
        }

        rotationPoint = rotationPoint.add(
                MathUtil.random(-0.03D, 0.03D),
                0.0D,
                MathUtil.random(-0.03D, 0.03D));

        if (!RaytracingUtil.rayTrace(mc.player.getRotationVector(), distance, target.getBoundingBox())) {
            float halfBox = (float) (lengthX / 2.0D);
            for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.05F) {
                for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.05F) {
                    for (float y1 = 0.05F; y1 <= target.getBoundingBox().getLengthY(); y1 += 0.15F) {
                        Vec3d v1 = new Vec3d(target.getX() + x1, target.getY() + y1, target.getZ() + z1);
                        Rotation rotation = Rotation.fromVec3d(v1.subtract(mc.player.getEyePos()));
                        if (RaytracingUtil.rayTrace(rotation.toVector(), distance, target.getBoundingBox())) {
                            rotationPoint = new Vec3d(x1, y1, z1);
                            break;
                        }
                    }
                }
            }
        }

        return target.getPos().add(rotationPoint);
    }

    public static void reset() {
        rotationPoint = Vec3d.ZERO;
        rotationMotion = Vec3d.ZERO;
    }
}
