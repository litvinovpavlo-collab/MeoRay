package dev.meoray.client.feature.visual.wings;

import dev.meoray.client.MeoRayClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class WingsLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public WingsLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
        System.out.println("[WingsLayer] Конструктор вызван — создаём кастомную геометрию крыльев");

        // Текстура 32x32: левое крыло в UV(0,0) размер 10x16, правое в UV(10,0) (зеркально)
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // Левое крыло: крепится у левого плеча, идёт влево
        root.addChild("left_wing",
            ModelPartBuilder.create()
                .uv(0, 0)
                .cuboid(-10.0f, -8.0f, -0.5f, 10.0f, 16.0f, 1.0f),
            ModelTransform.pivot(-5.0f, 4.0f, -3.0f));

        // Правое крыло: крепится у правого плеча, идёт вправо
        root.addChild("right_wing",
            ModelPartBuilder.create()
                .uv(10, 0)
                .cuboid(0.0f, -8.0f, -0.5f, 10.0f, 16.0f, 1.0f),
            ModelTransform.pivot(5.0f, 4.0f, -3.0f));

        ModelPart model = TexturedModelData.of(modelData, 32, 32).createModel();
        this.leftWing = model.getChild("left_wing");
        this.rightWing = model.getChild("right_wing");

        System.out.println("[WingsLayer] Модель создана: leftWing=" + leftWing
            + ", rightWing=" + rightWing);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        System.out.println("[WingsLayer] render() вызван");

        if (MeoRayClient.INSTANCE == null) {
            System.out.println("[WingsLayer] INSTANCE == null — пропускаем");
            return;
        }
        Wings wingsModule = (Wings) MeoRayClient.INSTANCE.moduleManager.getByName("Wings");
        if (wingsModule == null) {
            System.out.println("[WingsLayer] Модуль Wings не найден — пропускаем");
            return;
        }
        if (!wingsModule.isEnabled()) {
            System.out.println("[WingsLayer] Модуль Wings выключен — пропускаем");
            return;
        }
        if (state.invisible) {
            System.out.println("[WingsLayer] Игрок невидим — пропускаем");
            return;
        }

        System.out.println("[WingsLayer] Рендерим крылья, режим: " + wingsModule.currentMode);

        matrices.push();

        // Привязываемся к телу игрока
        this.getContextModel().body.rotate(matrices);

        // Учёт приседания
        if (state.sneaking) {
            matrices.translate(0.0f, 0.2f, 0.08f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(25.0f));
        }

        // Масштабирование
        matrices.scale(1.8f, 1.8f, 1.8f);

        // Анимация махания
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean flying = state.isGliding || (mc.player != null && !mc.player.isOnGround());
        animateWings(state.age, flying);

        // Рендер текстур
        Identifier texture = Identifier.of("meoray", "textures/entity/wings/" + wingsModule.getTextureName());
        System.out.println("[WingsLayer] Текстура: " + texture);
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));

        leftWing.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
        System.out.println("[WingsLayer] Левое крыло отрендерено");

        rightWing.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
        System.out.println("[WingsLayer] Правое крыло отрендерено");

        matrices.pop();
        System.out.println("[WingsLayer] render() завершён");
    }

    private void animateWings(float time, boolean flying) {
        if (flying) {
            float flap = MathHelper.sin(time * 0.45f) * 0.8f;
            leftWing.yaw = 0.2f + flap;
            rightWing.yaw = -0.2f - flap;
        } else {
            float breath = MathHelper.sin(time * 0.08f) * 0.1f;
            leftWing.yaw = 0.35f + breath;
            rightWing.yaw = -0.35f - breath;
        }
        leftWing.pitch = 0.1f;
        rightWing.pitch = 0.1f;
    }
}
