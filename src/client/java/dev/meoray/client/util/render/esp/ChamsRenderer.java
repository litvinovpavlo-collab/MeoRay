package dev.meoray.client.util.render.esp;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ChamsRenderer {
    private static final List<Entry> QUEUE = new ArrayList();

    public static void enqueue(EntityModel<?> model, MatrixStack matrices, int[] colors) {
        BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        model.render(matrices, buf, 15728880, OverlayTexture.DEFAULT_UV, -1);
        BuiltBuffer captured = buf.endNullable();
        if (captured != null) {
            int size = captured.getBuffer().remaining();
            BuiltBuffer.DrawParameters params = captured.getDrawParameters();
            BufferAllocator allocator = new BufferAllocator(size);
            allocator.allocate(size);
            BufferAllocator.CloseableBuffer closeable = allocator.getAllocated();
            if (closeable == null) {
                captured.close();
                allocator.close();
            } else {
                ByteBuffer dst = closeable.getBuffer();
                dst.put(captured.getBuffer());
                dst.flip();
                captured.close();
                QUEUE.add(new Entry(allocator, closeable, params, colors));
            }
        }
    }

    public static void renderAll() {
        if (!QUEUE.isEmpty()) {
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_ENTITY_TRANSLUCENT);
            RenderSystem.setShaderTexture(0, MinecraftClient.getInstance().getTextureManager().getTexture(Identifier.ofVanilla("textures/misc/white.png")).getGlId());
            MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
            MinecraftClient.getInstance().gameRenderer.getOverlayTexture().setupOverlayColor();
            RenderSystem.enableCull();

            for (Entry e : QUEUE) {
                int chamsColor = e.colors()[0];
                int chamsThroughColor = e.colors()[1];
                RenderSystem.disableBlend();
                RenderSystem.colorMask(false, false, false, false);
                RenderSystem.depthMask(true);
                RenderSystem.depthFunc(515);
                draw(e, -1);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ONE, DstFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.depthMask(false);
                RenderSystem.depthFunc(515);
                draw(e, chamsColor);
                RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
                draw(e, chamsColor);
                RenderSystem.depthFunc(519);
                draw(e, chamsThroughColor);
            }

            for (Entry e : QUEUE) {
                e.release();
            }

            QUEUE.clear();
            MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
            MinecraftClient.getInstance().gameRenderer.getOverlayTexture().teardownOverlayColor();
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
        }
    }

    private static void draw(Entry e, int color) {
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, a);
        e.closeable().getBuffer().rewind();
        BuiltBuffer built = new BuiltBuffer(e.closeable(), e.params()) {
            public void close() {
            }
        };
        BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Environment(EnvType.CLIENT)
    private static record Entry(BufferAllocator allocator, BufferAllocator.CloseableBuffer closeable, BuiltBuffer.DrawParameters params, int[] colors) {
        void release() {
            this.closeable.close();
            this.allocator.close();
        }
    }
}
