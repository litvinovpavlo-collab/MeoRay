package dev.meoray.client.util.render;

import net.minecraft.client.gl.CompiledShader;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramDefinition;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkyShaderHolder {
    private static final Logger LOG = LoggerFactory.getLogger("SkyShaderHolder");
    private static final Identifier VERT = Identifier.of("meoray", "shaders/core/sky.vsh");
    private static final Identifier FRAG = Identifier.of("meoray", "shaders/core/sky.fsh");
    private static ShaderProgram program;
    private static volatile boolean dirty = true;

    public static void markDirty() { dirty = true; }

    public static void reload(ResourceManager mgr) {
        if (program != null) { program.close(); program = null; }
        try {
            String vs = read(mgr, VERT);
            String fs = read(mgr, FRAG);
            CompiledShader vert = CompiledShader.compile(VERT, CompiledShader.Type.VERTEX, vs);
            CompiledShader frag = CompiledShader.compile(FRAG, CompiledShader.Type.FRAGMENT, fs);
            program = ShaderProgram.create(vert, frag, VertexFormats.POSITION);
            vert.close(); frag.close();
            float[] id = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
            program.set(List.of(
                u("GameTime", "float", 1, List.of(0f)),
                u("FogColor", "float", 4, List.of(0f, 0f, 0f, 0f)),
                u("FogStart", "float", 1, List.of(0f)),
                u("FogEnd", "float", 1, List.of(192f)),
                u("InvProjMat", "matrix4x4", 16, fl(id)),
                u("InvViewMat", "matrix4x4", 16, fl(id)),
                u("SkyZenith", "float", 3, fl(SkyConfig.SKY_ZENITH)),
                u("SkyHorizon", "float", 3, fl(SkyConfig.SKY_HORIZON)),
                u("NebColor1", "float", 3, fl(SkyConfig.NEB_COLOR1)),
                u("NebColor2", "float", 3, fl(SkyConfig.NEB_COLOR2)),
                u("NebIntensity", "float", 1, List.of(SkyConfig.NEB_INTENSITY)),
                u("StarColor", "float", 3, fl(SkyConfig.STAR_COLOR))
            ), List.of());
            dirty = true;
            LOG.info("[SkyShader] Loaded OK");
        } catch (Exception e) {
            LOG.error("[SkyShader] Failed", e);
        }
    }

    public static void uploadConfig() {
        if (program != null && dirty) {
            dirty = false;
            GlUniform u = program.getUniform("SkyZenith");
            if (u != null) u.set(SkyConfig.SKY_ZENITH[0], SkyConfig.SKY_ZENITH[1], SkyConfig.SKY_ZENITH[2]);
            u = program.getUniform("SkyHorizon");
            if (u != null) u.set(SkyConfig.SKY_HORIZON[0], SkyConfig.SKY_HORIZON[1], SkyConfig.SKY_HORIZON[2]);
            u = program.getUniform("NebColor1");
            if (u != null) u.set(SkyConfig.NEB_COLOR1[0], SkyConfig.NEB_COLOR1[1], SkyConfig.NEB_COLOR1[2]);
            u = program.getUniform("NebColor2");
            if (u != null) u.set(SkyConfig.NEB_COLOR2[0], SkyConfig.NEB_COLOR2[1], SkyConfig.NEB_COLOR2[2]);
            u = program.getUniform("NebIntensity");
            if (u != null) u.set(SkyConfig.NEB_INTENSITY);
            u = program.getUniform("StarColor");
            if (u != null) u.set(SkyConfig.STAR_COLOR[0], SkyConfig.STAR_COLOR[1], SkyConfig.STAR_COLOR[2]);
        }
    }

    public static ShaderProgram getProgram() { return program; }

    private static ShaderProgramDefinition.Uniform u(String name, String type, int count, List<Float> values) {
        return new ShaderProgramDefinition.Uniform(name, type, count, values);
    }

    private static List<Float> fl(float[] arr) {
        List<Float> l = new ArrayList<>(arr.length);
        for (float f : arr) l.add(f);
        return l;
    }

    private static String read(ResourceManager mgr, Identifier id) throws Exception {
        Optional<Resource> opt = mgr.getResource(id);
        if (opt.isEmpty()) throw new Exception("Not found: " + id);
        try (InputStream s = opt.get().getInputStream()) {
            return new String(s.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
