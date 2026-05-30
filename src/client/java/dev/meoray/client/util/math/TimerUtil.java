package dev.meoray.client.util.math;

public class TimerUtil {
    public long lastMS = System.currentTimeMillis();

    public void reset() { this.lastMS = System.currentTimeMillis(); }

    public boolean isReached(long time) { return System.currentTimeMillis() - this.lastMS > time; }

    public void setTime(long time) { this.lastMS = time; }

    public long getTime() { return System.currentTimeMillis() - this.lastMS; }
}
