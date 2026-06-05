package dev.meoray.client.feature.combat;

/**
 * Пресеты настроек AttackAura для разных серверов.
 * Подобраны эмпирически на основе их античитов.
 */
public class AuraPresets {

    public static class Preset {
        public final double distance;
        public final double additionalDistance;
        public final String mode;              // Rage / Silent
        public final String attackMode;        // 1.9+ / Legacy
        public final double accuracy;
        public final String sprintReset;       // Packet / Vanilla / None
        public final double smoothness;
        public final double humanization;
        public final double prediction;
        public final double gcd;
        public final boolean smartCriticals;
        public final boolean onlyCriticals;
        public final boolean tpsSync;
        public final boolean breakShield;
        public final boolean desyncShield;

        public Preset(double distance, double additionalDistance, String mode, String attackMode,
                      double accuracy, String sprintReset, double smoothness, double humanization,
                      double prediction, double gcd, boolean smartCriticals, boolean onlyCriticals,
                      boolean tpsSync, boolean breakShield, boolean desyncShield) {
            this.distance = distance;
            this.additionalDistance = additionalDistance;
            this.mode = mode;
            this.attackMode = attackMode;
            this.accuracy = accuracy;
            this.sprintReset = sprintReset;
            this.smoothness = smoothness;
            this.humanization = humanization;
            this.prediction = prediction;
            this.gcd = gcd;
            this.smartCriticals = smartCriticals;
            this.onlyCriticals = onlyCriticals;
            this.tpsSync = tpsSync;
            this.breakShield = breakShield;
            this.desyncShield = desyncShield;
        }
    }

    // === FUNTIME.SU ===
    // Античит: проверки на резкие повороты, snap aim, reach 4.0
    // Тактика: плавные повороты, средний рандом, прицел в body, sprint reset packet
    public static final Preset FUNTIME = new Preset(
            3.8,      // distance — на funtime reach строгий
            2.5,      // additionalDistance
            "Silent", // mode
            "1.9+",   // attackMode
            96,       // accuracy
            "Packet", // sprintReset
            85,       // smoothness — плавнее
            2.0,      // humanization — больше рандома
            1.2,      // prediction
            0.5,      // gcd — обязательно для funtime
            true,     // smartCriticals
            false,    // onlyCriticals
            true,     // tpsSync
            true,     // breakShield
            false     // desyncShield
    );

    // === SPOOKYTIME.NET ===
    // Античит: похож на funtime, чуть мягче
    public static final Preset SPOOKYTIME = new Preset(
            4.0,
            2.5,
            "Silent",
            "1.9+",
            95,
            "Packet",
            75,
            1.8,
            1.3,
            0.5,
            true,
            false,
            true,
            true,
            false
    );

    // === REALLYWORLD ===
    // Античит: средняя строгость, проверяет CPS
    public static final Preset REALLYWORLD = new Preset(
            4.0,
            2.0,
            "Silent",
            "1.9+",
            98,
            "Vanilla",
            70,
            1.5,
            1.0,
            0.5,
            true,
            false,
            true,
            true,
            false
    );

    // === HOLYWORLD ===
    // Античит: жёсткий на reach, мягкий на rotations
    public static final Preset HOLYWORLD = new Preset(
            3.7,      // строгий reach
            2.5,
            "Silent",
            "1.9+",
            97,
            "Packet",
            80,
            1.7,
            1.1,
            0.5,
            true,
            false,
            true,
            true,
            false
    );

    /**
     * Получить пресет по имени
     */
    public static Preset getByName(String name) {
        return switch (name) {
            case "Funtime"     -> FUNTIME;
            case "Spookytime"  -> SPOOKYTIME;
            case "ReallyWorld" -> REALLYWORLD;
            case "Holyworld"   -> HOLYWORLD;
            default -> null; // Custom — ничего не меняем
        };
    }
}
