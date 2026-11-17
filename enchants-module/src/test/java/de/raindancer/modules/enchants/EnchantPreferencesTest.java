package de.raindancer.modules.enchants;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.enchants.service.AutopilotService;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("elytra enchantments on /preferences")
class EnchantPreferencesTest {

    private final AutopilotService autopilot = mock(AutopilotService.class);
    private final Messages messages = mock(Messages.class);
    private final Player player = mock(Player.class);
    private final PersistentDataContainer data = mock(PersistentDataContainer.class);

    EnchantPreferencesTest() {
        when(player.getPersistentDataContainer()).thenReturn(data);
    }

    @Test
    @DisplayName("heat-seeking and autopilot economy, both in group Elytra")
    void built() {
        List<Preference> all = EnchantPreferences.build(autopilot, messages);
        assertThat(all).extracting(Preference::id).containsExactly("enchants:heatseeking",
                "enchants:autopilot-economy");
        assertThat(all).extracting(Preference::group).containsOnly("Elytra");
        assertThat(all.get(0).options()).containsExactly("Precise", "Wide");
        assertThat(all.get(1).options()).containsExactly("On", "Off");
    }

    @Test
    @DisplayName("heat-seeking is precise until the player picked wide, and says so when changed")
    void heatSeeking() {
        Preference heat = EnchantPreferences.build(autopilot, messages).get(0);
        when(data.has(any(org.bukkit.NamespacedKey.class))).thenReturn(false);
        assertThat(heat.state(player)).isEqualTo("Precise");
        when(data.has(any(org.bukkit.NamespacedKey.class))).thenReturn(true);
        assertThat(heat.state(player)).isEqualTo("Wide");
        heat.choose().accept(player, 1);
        verify(messages).send(player, "enchants.heat-seeking-wide");
        heat.choose().accept(player, 0);
        verify(messages).send(player, "enchants.heat-seeking-precise");
    }

    @Test
    @DisplayName("economy mode goes through the autopilot service")
    void economy() {
        Preference economy = EnchantPreferences.build(autopilot, messages).get(1);
        when(autopilot.economy(player)).thenReturn(true);
        assertThat(economy.state(player)).isEqualTo("On");
        economy.choose().accept(player, 1);
        verify(autopilot).setEconomy(player, false);
        economy.choose().accept(player, 0);
        verify(autopilot).setEconomy(player, true);
    }
}
