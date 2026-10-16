package de.raindancer.modules.enchants.rules;

/** What Flight and Double Jump permit. Decides, and does nothing else. */
public final class AirRule {

    /** Blocks-per-tick of upward push at 100 %, a little over two blocks of height. */
    private static final double UP_AT_FULL_POWER = 0.75;
    private static final long RUNNING_LOW_SECONDS = 5;

    public long capacityMillis(int flightLevel, int secondsPerLevel) {
        return Math.max(0, flightLevel) * Math.max(0, secondsPerLevel) * 1000L;
    }

    public int jumpsLeft(int jumpLevel, int used) {
        return Math.max(0, jumpLevel - Math.max(0, used));
    }

    public boolean mayFly(int flightLevel, long remainingMillis) {
        return flightLevel > 0 && remainingMillis > 0;
    }

    public boolean allowFlight(int flightLevel, long remainingMillis, int jumpLevel, int jumpsUsed) {
        return mayFly(flightLevel, remainingMillis) || jumpsLeft(jumpLevel, jumpsUsed) > 0;
    }

    /** Whole seconds, rounded up: the display reads 0 only once the tank really is empty. */
    public long secondsShown(long remainingMillis) {
        return remainingMillis <= 0 ? 0 : (remainingMillis + 999) / 1000;
    }

    public boolean runningLow(long secondsShown) {
        return secondsShown <= RUNNING_LOW_SECONDS;
    }

    public double jumpUp(int powerPercent) {
        return UP_AT_FULL_POWER * powerPercent / 100.0;
    }
}
