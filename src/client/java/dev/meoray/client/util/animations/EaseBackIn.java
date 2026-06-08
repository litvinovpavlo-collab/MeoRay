package dev.meoray.client.util.animations;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EaseBackIn extends Animation {
    private final float easeAmount;

    public EaseBackIn(int ms, double endPoint, float easeAmount) {
        super(ms, endPoint);
        this.easeAmount = easeAmount;
    }

    public EaseBackIn(int ms, double endPoint, float easeAmount, Direction direction) {
        super(ms, endPoint, direction);
        this.easeAmount = easeAmount;
    }

    protected boolean correctOutput() {
        return true;
    }

    protected double getEquation(double x) {
        double x1 = x / (double)this.duration;
        float shrink = this.easeAmount + 1.0F;
        return Math.max(0.0, 1.0 + shrink * Math.pow(x1 - 1.0, 3.0) + this.easeAmount * Math.pow(x1 - 1.0, 2.0));
    }
}
