package de.raindancer.modules.enchants.service;

import de.raindancer.core.content.enchant.CustomEnchantments;
import de.raindancer.core.content.enchant.RainsEnchants;
import de.raindancer.core.ui.actionbar.ActionBars;
import de.raindancer.core.ui.effect.Effects;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.world.movement.Flight;
import de.raindancer.modules.enchants.EnchantAdvancements;
import de.raindancer.modules.enchants.EnchantsSettings;
import de.raindancer.modules.enchants.model.AirState;
import de.raindancer.modules.enchants.model.FlightBudget;
import de.raindancer.modules.enchants.rules.AirRule;
import de.raindancer.modules.enchants.store.AirStore;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** The Flight enchant's flight is a lease beside a claim's and {@code /fly}'s, never in place of them. */
class AirFlightLeaseTest {

    private final UUID id = UUID.randomUUID();
    private final Flight flight = new Flight(mock(Plugin.class), () -> 1_000L);
    private final Set<Object> saved = new HashSet<>();
    private Player player;
    private boolean allowFlight;
    private boolean flying;
    private int flightLevel = 1;
    private MockedStatic<CustomEnchantments> enchantments;
    private AirService air;
    private AirState state;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        World world = mock(World.class);
        when(world.hasCollisionsIn(any())).thenReturn(true);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(player.getBoundingBox()).thenReturn(new BoundingBox(0, 64, 0, 0.6, 65.8, 0.6));
        when(player.isOnGround()).thenReturn(true);
        when(player.getAllowFlight()).thenAnswer(call -> allowFlight);
        when(player.isFlying()).thenAnswer(call -> flying);
        doAnswer(call -> {
            allowFlight = call.getArgument(0);
            flying &= allowFlight;
            return null;
        }).when(player).setAllowFlight(anyBoolean());
        doAnswer(call -> flying = call.getArgument(0)).when(player).setFlying(anyBoolean());
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(player.getPersistentDataContainer()).thenReturn(data);
        doAnswer(call -> saved.add(call.getArgument(0))).when(data).set(any(NamespacedKey.class), any(), any());
        doAnswer(call -> saved.remove(call.getArgument(0))).when(data).remove(any(NamespacedKey.class));

        enchantments = mockStatic(CustomEnchantments.class);
        enchantments.when(() -> CustomEnchantments.levelOn(any(), eq(RainsEnchants.FLIGHT))).thenAnswer(call -> flightLevel);
        enchantments.when(() -> CustomEnchantments.levelOn(any(), eq(RainsEnchants.DOUBLE_JUMP))).thenReturn(0);

        EnchantsSettings settings = mock(EnchantsSettings.class);
        when(settings.flightSecondsPerLevel()).thenReturn(60);
        when(settings.flightRechargeSeconds()).thenReturn(60);
        air = new AirService(new AirRule(), new AirStore(), () -> settings, mock(EnchantAdvancements.class),
                mock(Effects.class), mock(Messages.class), mock(ActionBars.class), flight);
        state = new AirState();
        state.budget = FlightBudget.of(60_000);
        air.adopt(player, state);
    }

    @AfterEach
    void tearDown() {
        enchantments.close();
    }

    private void chestplateOff() {
        flightLevel = 0;
        air.tick(player);
    }

    @Test
    @DisplayName("taking the chestplate off inside a flying claim leaves the claim's flight on")
    void claimFlightOutlivesTheEnchant() {
        flight.grant(player, "claims");
        air.tick(player);
        assertThat(flight.holds(id, AirService.FLIGHT_SOURCE)).isTrue();

        chestplateOff();

        assertThat(allowFlight).isTrue();
        assertThat(flight.holds(id, "claims")).isTrue();
        assertThat(flight.holds(id, AirService.FLIGHT_SOURCE)).isFalse();
    }

    @Test
    @DisplayName("nor does it end /fly")
    void flyOutlivesTheEnchant() {
        flight.grant(player, Flight.FLY);
        air.tick(player);

        chestplateOff();

        assertThat(allowFlight).isTrue();
        assertThat(flight.hasOwnFlight(player)).isTrue();
    }

    @Test
    @DisplayName("flight only the enchant lent goes with the chestplate")
    void ownFlightEndsWithTheEnchant() {
        air.tick(player);
        assertThat(allowFlight).isTrue();
        assertThat(flight.hasOwnFlight(player)).isFalse();

        chestplateOff();

        assertThat(allowFlight).isFalse();
        assertThat(flight.isGranted(id)).isFalse();
    }

    @Test
    @DisplayName("the enchant's flight does not survive a claim lease ending after it")
    void ownRevokeAfterTheClaimLeft() {
        air.tick(player);
        flight.grant(player, "claims");
        flight.revoke(player, "claims");
        assertThat(allowFlight).as("the enchant still lends it").isTrue();

        chestplateOff();

        assertThat(allowFlight).isFalse();
    }

    @Test
    @DisplayName("flying on a claim's flight costs no charge")
    void lentFlightIsFree() {
        flight.grant(player, "claims");
        air.tick(player);
        flying = true;
        state.lastNanos = System.nanoTime() - 1_000_000_000L;

        air.tick(player);

        assertThat(state.budget.remainingMillis()).isEqualTo(60_000);
        assertThat(air.onToggleFlight(player, true)).as("the double tap is not a jump").isFalse();
    }
}
