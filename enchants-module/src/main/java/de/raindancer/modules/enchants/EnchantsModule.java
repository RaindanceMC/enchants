package de.raindancer.modules.enchants;

import de.raindancer.core.content.enchant.EnchantUnlocks;
import de.raindancer.core.content.enchant.CustomEnchantments;

import de.raindancer.core.data.settings.SettingsStore;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.modules.api.FlexModule;
import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.api.ModuleContext;
import de.raindancer.modules.api.ModuleInfo;
import de.raindancer.core.content.enchant.EnchantDef;
import de.raindancer.modules.enchants.listener.AirListener;
import de.raindancer.modules.enchants.listener.VeinAdvancementListener;
import de.raindancer.modules.enchants.rules.AirRule;
import de.raindancer.modules.enchants.service.AirService;
import de.raindancer.modules.enchants.service.AbilitiesSkillView;
import de.raindancer.modules.enchants.service.AbilityService;
import de.raindancer.modules.enchants.screen.AbilitiesScreen;
import de.raindancer.modules.enchants.store.AbilityStore;
import de.raindancer.core.ui.tree.SkillView;
import de.raindancer.core.ui.tree.Skills;
import de.raindancer.modules.enchants.listener.ElytraListener;
import de.raindancer.modules.enchants.listener.TabListener;
import de.raindancer.modules.enchants.listener.ToolListener;
import de.raindancer.modules.enchants.service.ChestLinkService;
import de.raindancer.modules.enchants.service.ElytraService;
import de.raindancer.modules.enchants.service.ToolService;
import de.raindancer.modules.enchants.service.WornEffectService;
import de.raindancer.modules.enchants.store.ElytraStore;
import de.raindancer.modules.enchants.store.AirStore;
import de.raindancer.modules.enchants.util.PermissionNodes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Rain's own enchantments — Veinminer (the enchantment itself; vein mining stays Veinminer's, gated by
 * veintoggle), Night Vision, Flight, Double Jump, Boost, Launch, Swiftness, Haste, Soulbound, Wisdom,
 * Excavator, Harvester, Smelting, Telekinesis and Chest Link — and the advancements for them. Double Jump,
 * Swiftness and Night Vision can also be unlocked in the abilities tree. Auto Sell is
 * registered here too and enforced by the economy, which knows the prices.
 *
 * <p>Nothing here hands the enchantments out: no enchanting table, villager or loot entry. A boss drop
 * or a shop gives a book through {@code CustomEnchantments.book}.
 */
public final class EnchantsModule implements FlexModule {

    private volatile de.raindancer.modules.enchants.service.AutopilotService autopilot;

    /** Worn effects are looked at every this-many ticks; an armour change is answered at once. */
    private static final int NIGHT_VISION_EVERY = 10;
    /** Chest Link drops are gathered this many ticks before they are sent. */
    private static final int LINK_EVERY = 20;

    private static final ModuleInfo INFO = ModuleInfo.of("enchants", "Enchants", "0.43.1")
            .describedAs("Thirty real enchantments with advancements, and the abilities tree bought with skill points")
            .by("Raindancer118");

    private AirService air;
    private WornEffectService wornEffects;
    private ChestLinkService links;

    @Override
    public ModuleInfo info() {
        return INFO;
    }

    @Override
    public List<EnchantDef> enchantments() {
        return EnchantsDefinitions.all();
    }

    private java.util.function.BooleanSupplier shardsOffered;

    @Override
    public void enable(ModuleContext context) {
        SettingsStore<EnchantsSettings> settings = context.settings(EnchantsSettings.class, EnchantsSettings.DEFAULTS);
        context.core().messages().defineFrom(EnchantsModule.class.getResourceAsStream("messages.yml"),
                context.chat().brand()::chatPrefix);
        PermissionNodes.register(context.plugin().getServer());
        // Shards are gone from the game; guilds must not offer unlocking with them.
        shardsOffered = () -> false;
        EnchantUnlocks.shardsFrom(context.plugin(), shardsOffered);

        // Before the advancements are defined: the full set counts only what is switched on.
        CustomEnchantments.switchOff(settings.current().switchedOff());
        settings.onChange(fresh -> CustomEnchantments.switchOff(fresh.switchedOff()));
        EnchantAdvancements advancements = new EnchantAdvancements(context.core().advancements());
        advancements.define();
        AbilityService abilities = new AbilityService(AbilityService.loadTree(), new AbilityStore(), settings::current,
                AbilityService.livePoints());
        // What is spent here comes off the skills' points (Core 2.108.0's pool); an older Core has no pool.
        try {
            de.raindancer.core.ui.tree.SkillPoints.spender(context.plugin(), AbilityService.SPENDER,
                    abilities::spentFor);
            context.closeWith(() -> de.raindancer.core.ui.tree.SkillPoints.removeSpender(AbilityService.SPENDER));
        } catch (LinkageError olderCore) {
            context.log().info("RainsCore is older than 2.108.0: abilities cannot be bought with skill points.");
        }
        air = new AirService(new AirRule(), new AirStore(), settings::current, advancements,
                context.core().effects(), context.core().messages(), context.core().actionBars(),
                context.core().flight(), abilities);
        context.closeWith(air.follow(context.core()));
        wornEffects = new WornEffectService(advancements, abilities);
        AbilitiesScreen abilityScreen = new AbilitiesScreen(abilities, context.core().messages(), context.chat().brand());
        // /skills lists the tree beside the other skills (Core 2.36.0's registry); /enchants abilities opens it without.
        try {
            SkillView view = new AbilitiesSkillView(abilities, abilityScreen::open);
            Skills.register(context.plugin(), view);
            context.closeWith(() -> Skills.unregister(view.id()));
        } catch (LinkageError olderCore) {
            context.log().info("RainsCore is older than 2.36.0: abilities are not listed in /skills.");
        }
        ElytraService elytra = new ElytraService(new ElytraStore(), settings::current, advancements,
                context.core().effects(), context.core().messages());
        ToolService tools = new ToolService(context.plugin(), settings::current, advancements,
                context.core().messages());
        tools.learnRecipes();
        // Data packs may add furnace recipes after the plugins are enabled.
        Scheduling.globalLater(context.plugin(), 1, tools::learnRecipes);
        links = new ChestLinkService(context.plugin(), context.core().land(), advancements, context.core().messages(),
                context.core().effects());

        context.listener(new AirListener(air, wornEffects));
        context.listener(new de.raindancer.modules.enchants.listener.AbilityListener(abilities));
        // Ranks bought on another server arrive a moment after joining, with the rest of the player's data.
        context.closeWith(context.core().onSharedDataApplied((player, namespaces) -> {
            if (namespaces.contains(de.raindancer.modules.enchants.store.AirStore.NAMESPACE)) {
                de.raindancer.core.platform.util.Scheduling.entity(context.plugin(), player,
                        () -> abilities.remember(player));
            }
        }));
        for (Player online : context.plugin().getServer().getOnlinePlayers()) {
            de.raindancer.core.platform.util.Scheduling.entity(context.plugin(), online, () -> abilities.remember(online));
        }
        context.listener(new VeinAdvancementListener(advancements));
        context.listener(new de.raindancer.modules.enchants.listener.FishingAdvancementListener(advancements));
        context.listener(new ElytraListener(elytra));
        context.listener(new de.raindancer.modules.enchants.listener.OffenceListener(
                new de.raindancer.modules.enchants.service.OffenceService(context.plugin(), context.core().combat(), advancements),
                new de.raindancer.modules.enchants.service.ArmourService(advancements)));
        context.listener(new de.raindancer.modules.enchants.listener.VolleyListener(
                new de.raindancer.modules.enchants.service.VolleyService(context.plugin(), advancements)));
        context.listener(new de.raindancer.modules.enchants.listener.HeatSeekingListener(
                new de.raindancer.modules.enchants.service.HeatSeekingService(context.plugin(), context.core().combat(),
                        context.core().messages(), context.core().effects(), advancements)));
        context.listener(new ToolListener(tools, links));
        context.listener(new TabListener(advancements));
        context.listener(new de.raindancer.modules.enchants.listener.ThorListener(
                new de.raindancer.modules.enchants.service.ThorService(context.plugin(), advancements)));
        context.listener(new de.raindancer.modules.enchants.listener.LookListener(context.plugin()));
        de.raindancer.modules.enchants.service.EnchantsPack.contribute(context.plugin(), context.core());
        context.closeWith(() -> de.raindancer.modules.enchants.service.EnchantsPack.withdraw(context.core()));
        de.raindancer.modules.enchants.service.AutopilotService autopilot = new de.raindancer.modules.enchants.service.AutopilotService(
                context.plugin(), context.core().messages(), settings::current, advancements, elytra);
        this.autopilot = autopilot;
        context.listener(new de.raindancer.modules.enchants.listener.AutopilotListener(autopilot));
        // /preferences (Core 2.103.0) lists these; /heatseeking and /autopilot economy keep working.
        try {
            for (de.raindancer.core.ui.preference.Preference each
                    : EnchantPreferences.build(autopilot, context.core().messages())) {
                de.raindancer.core.ui.preference.Preferences.offer(context.plugin(), each);
                context.closeWith(() -> de.raindancer.core.ui.preference.Preferences.withdraw(each));
            }
        } catch (LinkageError olderCore) {
            context.log().info("RainsCore is older than 2.103.0: heat-seeking aim and autopilot economy are not on /preferences.");
        }
        EnchantsCommands.ready(new EnchantsServices(context.plugin().getServer(), context.core().messages(),
                autopilot, abilityScreen));

        int[] beat = {0};
        var timer = Scheduling.globalTimer(context.plugin(), 1, 1, task -> {
            int now = beat[0]++;
            boolean checkNightVision = now % NIGHT_VISION_EVERY == 0;
            if (now % LINK_EVERY == 0) {
                links.flush();
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                Scheduling.entity(context.plugin(), player, () -> {
                    air.tick(player);
                    elytra.tick(player);
                    autopilot.tick(player);
                    if (checkNightVision) {
                        wornEffects.sync(player);
                    }
                });
            }
        });
        context.closeWith(timer::cancel);
        context.log().info("Enchants are up: {} enchantment(s) registered.", EnchantsDefinitions.all().size());
    }

    @Override
    public List<ModuleCommand> commands() {
        return EnchantsCommands.declared();
    }

    @Override
    public void disable() {
        if (shardsOffered != null) {
            EnchantUnlocks.retractShards(shardsOffered);
        }
        CustomEnchantments.switchOff(java.util.List.of());
        if (links != null) {
            links.flush();
        }
        if (air != null) {
            air.releaseAll();
        }
        if (wornEffects != null) {
            Bukkit.getOnlinePlayers().forEach(wornEffects::release);
        }
        EnchantsCommands.stopped();
        if (autopilot != null) {
            autopilot.stopAll();
            autopilot = null;
        }
    }
}
