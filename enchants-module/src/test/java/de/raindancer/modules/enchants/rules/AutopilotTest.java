package de.raindancer.modules.enchants.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AutopilotTest {

    private static final double[] EAST = {1, 0, 0};

    private static double length(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    @Test
    @DisplayName("top speed: a third of the setting's extra per level, all of it at III, never below a rocket")
    void topSpeed() {
        assertThat(Autopilot.topSpeed(1, 200)).isCloseTo(Autopilot.ROCKET_TOP * 4 / 3, within(1e-9));
        assertThat(Autopilot.topSpeed(2, 200)).isCloseTo(Autopilot.ROCKET_TOP * 5 / 3, within(1e-9));
        assertThat(Autopilot.topSpeed(3, 200)).isCloseTo(Autopilot.ROCKET_TOP * 2, within(1e-9));
        assertThat(Autopilot.topSpeed(9, 200)).as("III is the most").isEqualTo(Autopilot.topSpeed(3, 200));
        assertThat(Autopilot.topSpeed(3, 50)).as("never slower than a rocket").isEqualTo(Autopilot.ROCKET_TOP);
    }

    @Test
    @DisplayName("the cruise speed asked for stays between the slowest and the level's top")
    void cruise() {
        assertThat(Autopilot.cruise(40, 3)).isEqualTo(2.0);
        assertThat(Autopilot.cruise(500, 3)).isEqualTo(3.0);
        assertThat(Autopilot.cruise(1, 3)).isEqualTo(Autopilot.SLOWEST / 20);
    }

    @Test
    @DisplayName("the scroll wheel: up (to the slot on the left) is faster, round the hotbar's ends too")
    void notches() {
        assertThat(Autopilot.notches(4, 3)).isEqualTo(1);
        assertThat(Autopilot.notches(4, 5)).isEqualTo(-1);
        assertThat(Autopilot.notches(0, 8)).as("from the first slot up to the last").isEqualTo(1);
        assertThat(Autopilot.notches(8, 0)).isEqualTo(-1);
        assertThat(Autopilot.notches(2, 2)).isZero();
    }

    @Test
    @DisplayName("cruise mode: holds the cruise speed, past a rocket's top, and brakes rather than race past it")
    void cruiseMode() {
        double cruise = Autopilot.topSpeed(3, 200);
        double[] v = {0.5, 0, 0};
        for (int tick = 0; tick < 40; tick++) {
            Autopilot.Tick step = Autopilot.fly(v, EAST, 0, 100, 100.0, cruise, false, true);
            v = step.velocity();
        }
        assertThat(length(v)).as("faster than a rocket's 1.5 a tick").isCloseTo(cruise, within(0.03));
        Autopilot.Tick diving = Autopilot.fly(new double[]{2, -3, 0}, new double[]{0.5, -0.87, 0}, 60, 100, null,
                2, false, true);
        assertThat(length(diving.velocity())).as("braked to the cruise speed").isCloseTo(2, within(1e-9));
        assertThat(diving.thrusting()).isFalse();
    }

    @Test
    @DisplayName("economy mode: no thrust in a dive, none until the speed has sagged, gentler when it comes")
    void economy() {
        double cruise = 2;
        assertThat(Autopilot.fly(new double[]{1.9, 0, 0}, EAST, 0, 100, 100.0, cruise, true, true).thrusting())
                .as("close enough").isFalse();
        assertThat(Autopilot.fly(new double[]{1.9, 0, 0}, EAST, 0, 100, 100.0, cruise, false, true).thrusting())
                .as("cruise mode would").isTrue();
        assertThat(Autopilot.fly(new double[]{1, -0.6, 0}, new double[]{0.8, -0.6, 0}, 37, 100, null, cruise, true,
                true).thrusting()).as("diving: gravity is free").isFalse();
        double[] eco = Autopilot.fly(new double[]{1, 0, 0}, EAST, 0, 100, 100.0, cruise, true, true).velocity();
        double[] full = Autopilot.fly(new double[]{1, 0, 0}, EAST, 0, 100, 100.0, cruise, false, true).velocity();
        assertThat(eco[0]).isGreaterThan(1).isLessThan(full[0]);
        Autopilot.Tick fast = Autopilot.fly(new double[]{2, -3, 0}, new double[]{0.5, -0.87, 0}, 60, 100, null,
                cruise, true, true);
        assertThat(fast.velocity()).as("no braking either: speed already paid for").isNull();
    }

    @Test
    @DisplayName("flying level holds the altitude levelled off at; looking up or down steers away from it, freely")
    void altitude() {
        Autopilot.Tick levelled = Autopilot.fly(new double[]{1.5, -0.2, 0}, EAST, 5, 120, null, 1.5, true, false);
        assertThat(levelled.holdY()).isEqualTo(120.0);
        Autopilot.Tick sunk = Autopilot.fly(new double[]{1.5, -0.2, 0}, EAST, 5, 118, 120.0, 1.5, true, false);
        assertThat(sunk.velocity()[1]).as("back up to it").isPositive();
        assertThat(sunk.velocity()[0]).as("across untouched").isEqualTo(1.5);
        Autopilot.Tick climbing = Autopilot.fly(new double[]{1.2, 0.6, 0}, new double[]{0.8, 0.6, 0}, -37, 118,
                120.0, 1.2, true, false);
        assertThat(climbing.holdY()).as("the player steers").isNull();
        assertThat(climbing.velocity()).as("and is left alone").isNull();
        Autopilot.Tick above = Autopilot.fly(new double[]{1.5, 0, 0}, EAST, 0, 140, 140.0, 1.5, true, false);
        assertThat(above.holdY()).as("the new altitude, once level again").isEqualTo(140.0);
    }

    @Test
    @DisplayName("out of fuel it still holds the altitude, but cannot thrust")
    void noFuel() {
        Autopilot.Tick step = Autopilot.fly(new double[]{0.5, 0, 0}, EAST, 0, 100, 100.0, 2, false, false);
        assertThat(step.thrusting()).isFalse();
        assertThat(step.velocity()[0]).isEqualTo(0.5);
    }

    @Test
    @DisplayName("a rocket burns as long as its flight duration says")
    void burn() {
        assertThat(Autopilot.burnTicks(1)).isEqualTo(26);
        assertThat(Autopilot.burnTicks(3)).isEqualTo(46);
        assertThat(Autopilot.burnTicks(-4)).isEqualTo(16);
    }

    @Test
    @DisplayName("plain rockets first; the longest-burning, or in economy mode the most per gunpowder; stars last")
    void pickRocket() {
        List<Autopilot.Rocket> carried = List.of(new Autopilot.Rocket(3, 3, true), new Autopilot.Rocket(7, 1, false),
                new Autopilot.Rocket(12, 2, false));
        assertThat(Autopilot.pick(carried, false)).hasValue(12);
        assertThat(Autopilot.pick(carried, true)).hasValue(7);
        assertThat(Autopilot.pick(List.of(new Autopilot.Rocket(3, 3, true)), false)).hasValue(3);
        assertThat(Autopilot.pick(List.of(), true)).isEmpty();
    }

    @Test
    @DisplayName("blocks a second from blocks a tick")
    void perSecond() {
        assertThat(Autopilot.blocksPerSecond(2.5)).isEqualTo(50.0);
    }
}
