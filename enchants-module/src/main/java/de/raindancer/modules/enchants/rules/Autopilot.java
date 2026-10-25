package de.raindancer.modules.enchants.rules;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Autopilot's flight assist, in blocks a tick. The player steers all the time; Autopilot holds the cruise speed they
 * picked — burning rockets and Boost charges for it, past a rocket's own top speed — and, while they fly level, the
 * altitude they levelled off at. In economy mode it spends as little fuel as it can and gets to the cruise speed
 * second: no thrust in a dive, gentler thrust, a little slower before it lights up again, no braking.
 */
public final class Autopilot {

    /** A firework rocket's top speed on an elytra. */
    public static final double ROCKET_TOP = 1.5;
    /** The slowest cruise speed one can pick, in blocks a second. */
    public static final double SLOWEST = 10;
    /** One notch of the scroll wheel, in blocks a second. */
    public static final double NOTCH = 2;
    /** Looking no further up or down than this counts as flying level: the altitude is held. */
    public static final double LEVEL_PITCH = 12;
    /** In economy mode the next thrust waits until the speed has sagged to this share of the cruise speed. */
    static final double ECONOMY_SAG = 0.9;
    /** Sinking faster than this is a dive: in economy mode, free speed. */
    static final double DIVING = -0.1;
    private static final double CLIMB_TO_HOLD = 0.4;

    /** A rocket somewhere in the inventory: its slot, its flight duration, whether it carries stars. */
    public record Rocket(int slot, int power, boolean explosive) {
    }

    /**
     * One tick's outcome.
     *
     * @param velocity what to set, or null to leave the player's own glide alone
     * @param thrusting whether it burnt fuel this tick
     * @param holdY the altitude held from now on, or null while the player climbs or dives
     */
    public record Tick(double[] velocity, boolean thrusting, Double holdY) {
    }

    private Autopilot() {
    }

    /** The top speed at {@code level}: a rocket's, plus a third of the setting's extra per level. */
    public static double topSpeed(int level, int percentAtThree) {
        double extra = Math.max(0, percentAtThree - 100) * Math.clamp(level, 1, 3) / 3.0;
        return ROCKET_TOP * (100 + extra) / 100.0;
    }

    /** The cruise speed one asked for, in blocks a second, as blocks a tick between {@link #SLOWEST} and the top. */
    public static double cruise(double perSecond, double top) {
        return Math.clamp(perSecond / 20.0, Math.min(SLOWEST / 20.0, top), top);
    }

    /** Scrolled from one hotbar slot to another: notches up (faster) are positive. Scrolling up selects the slot left. */
    public static int notches(int from, int to) {
        int moved = Math.floorMod(to - from + 4, 9) - 4;
        return -moved;
    }

    /** One tick of thrust along {@code direction} (a unit vector): {@code gain} of the way to {@code speed}, never past. */
    public static double[] thrust(double[] velocity, double[] direction, double speed, double gain) {
        double[] next = new double[3];
        for (int i = 0; i < 3; i++) {
            next[i] = velocity[i] + (direction[i] * speed - velocity[i]) * gain;
        }
        return next;
    }

    /**
     * One tick of the assist.
     *
     * @param velocity how the player moved last tick
     * @param look where they look (a unit vector), and {@code pitch} the game's pitch, positive down
     * @param holdY the altitude held so far, or null
     * @param fuel whether a rocket or a charge burns, or one could be lit
     */
    public static Tick fly(double[] velocity, double[] look, double pitch, double y, Double holdY, double cruise,
                           boolean economy, boolean fuel) {
        boolean level = Math.abs(pitch) <= LEVEL_PITCH;
        Double hold = level ? (holdY == null ? Double.valueOf(y) : holdY) : null;
        double[] direction = look.clone();
        if (level) {
            double across = Math.hypot(look[0], look[2]);
            direction = across < 1e-9 ? new double[]{0, 0, 0} : new double[]{look[0] / across, 0, look[2] / across};
        }
        double speed = Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]);
        boolean wants = economy
                ? speed < cruise * ECONOMY_SAG && (level || velocity[1] > DIVING)
                : speed < cruise - 0.025;
        boolean thrusting = wants && fuel;
        double[] next = null;
        if (thrusting) {
            next = thrust(velocity, direction, cruise, economy ? 0.25 : 0.5);
        } else if (!economy && speed > cruise * 1.05) {
            // Cruise speed comes first: air brakes rather than racing past it in a dive.
            double scale = cruise / speed;
            next = new double[]{velocity[0] * scale, velocity[1] * scale, velocity[2] * scale};
        }
        if (hold != null) {
            if (next == null) {
                next = velocity.clone();
            }
            next[1] = Math.clamp((hold - y) * 0.25, -CLIMB_TO_HOLD, CLIMB_TO_HOLD);
        }
        return new Tick(next, thrusting, hold);
    }

    /** How long a rocket of this flight duration burns: a launched one's average lifetime. */
    public static int burnTicks(int power) {
        return 10 * (Math.max(0, power) + 1) + 6;
    }

    /**
     * The rocket to burn next: a plain one first, one with stars only when nothing else is left. In economy mode the
     * one that burns longest for its gunpowder (the shortest), otherwise the longest-burning.
     */
    public static OptionalInt pick(List<Rocket> carried, boolean economy) {
        Comparator<Rocket> power = Comparator.comparing(Rocket::power);
        return carried.stream()
                .min(Comparator.comparing(Rocket::explosive).thenComparing(economy ? power : power.reversed()))
                .map(rocket -> OptionalInt.of(rocket.slot())).orElse(OptionalInt.empty());
    }

    public static double blocksPerSecond(double blocksPerTick) {
        return blocksPerTick * 20;
    }
}
