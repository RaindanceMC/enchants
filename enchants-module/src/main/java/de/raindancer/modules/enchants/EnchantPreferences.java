package de.raindancer.modules.enchants;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.enchants.service.AutopilotService;
import de.raindancer.modules.enchants.service.HeatSeekingService;

import java.util.List;

/**
 * The elytra and bow switches for Core's {@code /preferences}; {@code /heatseeking} and {@code /autopilot economy}
 * stay. {@code /autopilot} itself starts a flight, so it is no preference.
 */
final class EnchantPreferences {

    private static final String GROUP = "Elytra";

    private EnchantPreferences() {
    }

    static List<Preference> build(AutopilotService autopilot, Messages messages) {
        Preference heat = Preference.choice("enchants:heatseeking", GROUP, "Heat-seeking aim",
                "Precise locks only onto what your crosshair is on. Wide takes whatever is near the shot.",
                List.of("Precise", "Wide"), player -> HeatSeekingService.precise(player) ? 0 : 1, (player, index) -> {
                    HeatSeekingService.set(player, index == 0);
                    messages.send(player, index == 0 ? "enchants.heat-seeking-precise" : "enchants.heat-seeking-wide");
                });
        Preference economy = Preference.toggle("enchants:autopilot-economy", GROUP, "Elytra autopilot economy mode",
                "Lets the Autopilot save rockets by gliding more and boosting less.", autopilot::economy,
                autopilot::setEconomy);
        return List.of(heat, economy);
    }
}
