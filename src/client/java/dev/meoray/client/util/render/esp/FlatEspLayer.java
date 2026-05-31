package dev.meoray.client.util.render.esp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;

@Environment(EnvType.CLIENT)
public class FlatEspLayer {
    private static final ShaderProgramKey ESP_SHADER;
    public static final RenderLayer FLAT_ESP_DEPTH;
    public static final RenderLayer FLAT_ESP;

    static {
        ESP_SHADER = ShaderProgramKeys.RENDERTYPE_ENTITY_TRANSLUCENT;
        FLAT_ESP_DEPTH = RenderLayer.of("meoray_flat_esp_depth", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 256, false, false, MultiPhaseParameters.builder()
            .program(new RenderPhase.ShaderProgram(ESP_SHADER))
            .texture(new RenderPhase.Texture(Identifier.ofVanilla("textures/misc/white.png"), TriState.DEFAULT, false))
            .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
            .cull(RenderPhase.ENABLE_CULLING)
            .writeMaskState(RenderPhase.DEPTH_MASK)
            .lightmap(RenderPhase.ENABLE_LIGHTMAP)
            .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
            .build(false));
        FLAT_ESP = RenderLayer.of("meoray_flat_esp", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 256, false, true, MultiPhaseParameters.builder()
            .program(new RenderPhase.ShaderProgram(ESP_SHADER))
            .texture(new RenderPhase.Texture(Identifier.ofVanilla("textures/misc/white.png"), TriState.DEFAULT, false))
            .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
            .cull(RenderPhase.DISABLE_CULLING)
            .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
            .writeMaskState(RenderPhase.COLOR_MASK)
            .lightmap(RenderPhase.ENABLE_LIGHTMAP)
            .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
            .build(false));
    }
}
