package dev.meoray.client.mixin;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Module;
import dev.meoray.client.core.setting.BooleanSetting;
import dev.meoray.client.util.render.SkyShaderHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    private static Module cachedBetterWorld;
    private static boolean lookedUp;
    private static final Matrix4f sProj = new Matrix4f();
    private static final Matrix4f sView = new Matrix4f();
    private static final Matrix4f sIdent = new Matrix4f();
    private static boolean activeFrame;

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void onRenderSky(float r, float g, float b, CallbackInfo ci) {
        activeFrame = shouldOverride();
        if (activeFrame) {
            ci.cancel();
            drawSky();
        }
    }

    @Inject(method = "renderSkyDark", at = @At("HEAD"), cancellable = true)
    private void onRenderSkyDark(MatrixStack matrices, CallbackInfo ci) {
        if (activeFrame) ci.cancel();
    }

    private static boolean shouldOverride() {
        Module mod = getBetterWorld();
        if (mod == null || !mod.isEnabled()) return false;
        var skyShader = findSetting(mod, "Sky Shader");
        if (skyShader == null || !skyShader.getValue()) return false;
        if (SkyShaderHolder.getProgram() == null) return false;
        var mc = MinecraftClient.getInstance();
        if (mc.world == null) return false;
        return true;
    }

    private static Module getBetterWorld() {
        if (!lookedUp) {
            lookedUp = true;
            cachedBetterWorld = MeoRayClient.INSTANCE.moduleManager.getByName("BetterWorld");
        }
        return cachedBetterWorld;
    }

    private static BooleanSetting findSetting(Module mod, String name) {
        for (var s : mod.getSettings()) {
            if (s.getName().equals(name) && s instanceof BooleanSetting bs)
                return bs;
        }
        return null;
    }

    private static void drawSky() {
        ShaderProgram prog = SkyShaderHolder.getProgram();
        if (prog == null) return;
        var mc = MinecraftClient.getInstance();
        sProj.set(RenderSystem.getProjectionMatrix());
        sView.set(RenderSystem.getModelViewMatrix());
        prog.initializeUniforms(VertexFormat.DrawMode.QUADS, sView, sProj, mc.getWindow());
        GlUniform uInvProj = prog.getUniform("InvProjMat");
        GlUniform uInvView = prog.getUniform("InvViewMat");
        if (uInvProj != null) uInvProj.set(new Matrix4f(sProj).invert());
        if (uInvView != null) uInvView.set(new Matrix4f(sView).invert());
        SkyShaderHolder.uploadConfig();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(prog);
        prog.bind();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(sIdent, ProjectionType.PERSPECTIVE);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buf.vertex(-1f, -1f, 1f);
        buf.vertex(1f, -1f, 1f);
        buf.vertex(1f, 1f, 1f);
        buf.vertex(-1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        mv.popMatrix();
        prog.unbind();
        RenderSystem.clearShader();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }
}
