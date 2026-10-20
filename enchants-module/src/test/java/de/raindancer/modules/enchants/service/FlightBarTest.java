package de.raindancer.modules.enchants.service;

import de.raindancer.core.ui.actionbar.ActionBarPriority;
import de.raindancer.core.ui.actionbar.ActionBars;
import de.raindancer.core.ui.effect.Cues;
import de.raindancer.core.ui.effect.Effects;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.enchants.model.AirState;
import de.raindancer.modules.enchants.model.FlightBudget;
import de.raindancer.modules.enchants.rules.AirRule;
import de.raindancer.modules.enchants.store.AirStore;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlightBarTest {

    private final ActionBars bars = mock(ActionBars.class);
    private final Messages messages = mock(Messages.class);
    private final Effects effects = mock(Effects.class);
    private final Player player = mock(Player.class);
    private final UUID id = UUID.randomUUID();
    private AirService air;

    @BeforeEach
    void setUp() {
        when(player.getUniqueId()).thenReturn(id);
        when(messages.get(anyString(), any(Object[].class))).thenAnswer(call -> Component.text(call.<String>getArgument(0)));
        air = new AirService(new AirRule(), new AirStore(), null, null, effects, messages, bars, null);
    }

    private static AirState withLeft(long millis) {
        AirState state = new AirState();
        state.budget = FlightBudget.of(millis);
        return state;
    }

    @Test
    @DisplayName("in flight the time left is on the action bar, as a low-priority commentary that stays until cleared")
    void showsTimeLeft() {
        air.showFlightBar(player, withLeft(42_000), true);
        verify(messages).get("enchants.flight-remaining", "time", "42s");
        verify(bars).show(eq(id), eq(AirService.FLIGHT_BAR), any(Component.class),
                eq(ActionBars.UNTIL_CLEARED), eq(ActionBarPriority.LOW));
    }

    @Test
    @DisplayName("the bar is drawn again only when the second shown changes, not on every tick")
    void redrawsOnlyOnChange() {
        AirState state = withLeft(10_000);
        air.showFlightBar(player, state, true);
        state.budget.drain(400);
        air.showFlightBar(player, state, true);
        state.budget.drain(700);
        air.showFlightBar(player, state, true);
        verify(bars, times(2)).show(eq(id), eq(AirService.FLIGHT_BAR), any(Component.class), any(Duration.class),
                any(ActionBarPriority.class));
    }

    @Test
    @DisplayName("the last seconds switch to the running-low line")
    void runningLow() {
        air.showFlightBar(player, withLeft(4_200), true);
        verify(messages).get("enchants.flight-remaining-low", "time", "5s");
    }

    @Test
    @DisplayName("landing takes the bar away once, and a player who never flew is never touched")
    void clearsOnLanding() {
        AirState state = withLeft(10_000);
        air.showFlightBar(player, state, false);
        verifyNoInteractions(bars);

        air.showFlightBar(player, state, true);
        air.showFlightBar(player, state, false);
        air.showFlightBar(player, state, false);
        verify(bars, times(1)).clear(id, AirService.FLIGHT_BAR);
    }

    @Test
    @DisplayName("taking off again after landing shows the bar again, even with the same seconds left")
    void showsAgainAfterLanding() {
        AirState state = withLeft(10_000);
        air.showFlightBar(player, state, true);
        air.showFlightBar(player, state, false);
        air.showFlightBar(player, state, true);
        verify(bars, times(2)).show(eq(id), eq(AirService.FLIGHT_BAR), any(Component.class), any(Duration.class),
                any(ActionBarPriority.class));
        verify(bars, never()).forget(id);
    }

    @Test
    @DisplayName("the last five seconds tick once each, and the seconds before them are silent")
    void ticksWhileRunningLow() {
        AirState state = withLeft(7_000);
        for (int i = 0; i < 70; i++) {
            air.showFlightBar(player, state, true);
            state.budget.drain(100);
        }
        air.showFlightBar(player, state, true);
        verify(effects, times(5)).play(id, Cues.COUNTDOWN);
    }

    @Test
    @DisplayName("redrawing the same second does not tick again")
    void oneTickPerSecond() {
        AirState state = withLeft(3_000);
        air.showFlightBar(player, state, true);
        air.showFlightBar(player, state, true);
        verify(effects, times(1)).play(id, Cues.COUNTDOWN);
    }

    @Test
    @DisplayName("running out sounds the end of the countdown and puts up the notice")
    void emptyingSoundsDone() {
        air.ranOut(player);
        verify(effects).play(id, Cues.COUNTDOWN_DONE);
        verify(bars).show(eq(id), eq("enchants-flight-notice"), any(Component.class), any(Duration.class),
                eq(ActionBarPriority.NORMAL));
    }
}
