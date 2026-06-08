package dev.meoray.client.account.altmanager;

import dev.meoray.client.util.animations.Animation;
import dev.meoray.client.util.animations.Direction;
import dev.meoray.client.util.animations.EaseBackIn;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class NickName {
    private final Animation animation;
    private final String tag;
    private final String nickname;

    public NickName(String nickname, String tag) {
        this.nickname = nickname;
        this.tag = tag;
        this.animation = new EaseBackIn(400, 1.0, 0.1f, Direction.BACKWARDS);
    }

    public Animation getAnimation() {
        return this.animation;
    }

    public String getTag() {
        return this.tag;
    }

    public String getNickname() {
        return this.nickname;
    }
}
