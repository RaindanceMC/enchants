package de.raindancer.modules.enchants.command;

import de.raindancer.modules.enchants.EnchantsServices;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@code /autopilot} switches it on or off; {@code /autopilot economy} switches economy mode;
 * {@code /autopilot speed <blocks a second>} sets the cruise speed, as the scroll wheel does in flight.
 */
public final class AutopilotCommand implements BasicCommand {

    private final Supplier<EnchantsServices> services;

    public AutopilotCommand(Supplier<EnchantsServices> services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        EnchantsServices live = services.get();
        if (!(source.getSender() instanceof Player player)) {
            live.messages().send(source.getSender(), "enchants.players-only");
            return;
        }
        if (args.length == 0) {
            live.autopilot().toggle(player);
            return;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "economy", "eco" -> live.autopilot().toggleEconomy(player);
            case "speed" -> {
                try {
                    live.autopilot().setCruise(player, Double.parseDouble(args[1]));
                } catch (RuntimeException notANumber) {
                    live.messages().send(player, "enchants.autopilot-usage");
                }
            }
            default -> live.messages().send(player, "enchants.autopilot-usage");
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String typed = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ROOT);
            return List.of("economy", "speed").stream().filter(option -> option.startsWith(typed)).toList();
        }
        return args.length == 2 && args[0].equalsIgnoreCase("speed") ? List.of("20", "30", "40", "50", "60") : List.of();
    }
}
