package de.raindancer.modules.enchants.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.raindancer.core.content.enchant.EnchantDef;
import de.raindancer.modules.enchants.EnchantsDefinitions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnchantsPackTest {

    private static JsonObject json(String file) throws IOException {
        try (InputStream in = EnchantsPack.class.getResourceAsStream(EnchantsPack.ROOT + file)) {
            assertThat(in).as(file).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static Map<String, String> cases() throws IOException {
        JsonObject select = json(EnchantsPack.SHARD_ITEM).getAsJsonObject("model");
        Map<String, String> cases = new HashMap<>();
        for (JsonElement each : select.getAsJsonArray("cases")) {
            cases.put(each.getAsJsonObject().get("when").getAsString(),
                    each.getAsJsonObject().getAsJsonObject("model").get("model").getAsString());
        }
        return cases;
    }

    @Test
    @DisplayName("every file the pack ships is in the jar")
    void everyFileShips() throws IOException {
        for (String file : EnchantsPack.files()) {
            try (InputStream in = EnchantsPack.class.getResourceAsStream(EnchantsPack.ROOT + file)) {
                assertThat(in).as(file).isNotNull();
            }
        }
    }

    @Test
    @DisplayName("the echo shard picks its model by the shard's key, and a plain one stays an echo shard")
    void selectsByKey() throws IOException {
        JsonObject select = json(EnchantsPack.SHARD_ITEM).getAsJsonObject("model");
        assertThat(select.get("type").getAsString()).isEqualTo("minecraft:select");
        assertThat(select.get("property").getAsString()).isEqualTo("minecraft:custom_model_data");
        assertThat(select.get("index").getAsInt()).isZero();
        assertThat(select.getAsJsonObject("fallback").get("model").getAsString()).isEqualTo("minecraft:item/echo_shard");
    }

    @Test
    @DisplayName("every enchantment has its own shard model, its textures shipped")
    void everyEnchantmentHasAModel() throws IOException {
        Map<String, String> cases = cases();
        for (EnchantDef def : EnchantsDefinitions.all()) {
            String model = cases.get(def.key());
            assertThat(model).as(def.key()).isEqualTo("rainsenchants:item/shard/" + def.path());
            String file = "assets/rainsenchants/models/" + model.split(":")[1] + ".json";
            assertThat(EnchantsPack.files()).as(def.key()).contains(file);
            JsonObject json = json(file);
            assertThat(json.getAsJsonArray("elements")).as(def.key()).isNotEmpty();
            assertThat(json.getAsJsonObject("display").has("gui")).as(def.key()).isTrue();
            for (var texture : json.getAsJsonObject("textures").entrySet()) {
                String path = "assets/rainsenchants/textures/" + texture.getValue().getAsString().split(":")[1] + ".png";
                assertThat(EnchantsPack.files()).as(def.key() + " " + texture.getKey()).contains(path);
            }
        }
        assertThat(cases).hasSize(EnchantsDefinitions.all().size());
    }

    @Test
    @DisplayName("the pack declares a format range that takes in 26.2 and 26.3 clients")
    void format() throws IOException {
        JsonObject pack = json("pack.mcmeta").getAsJsonObject("pack");
        assertThat(pack.get("min_format").getAsInt()).isLessThanOrEqualTo(88);
        assertThat(pack.get("max_format").getAsInt()).isGreaterThanOrEqualTo(97);
    }

    @Test
    @DisplayName("Mjölnir: its item definition, 3D model (with a held, icon and ground look) and texture all ship")
    void mjolnir() throws IOException {
        String item = "assets/rainsenchants/items/mjolnir.json";
        assertThat(EnchantsPack.files()).contains(item);
        for (String model : List.of("mjolnir", "mjolnir_charge_1", "mjolnir_charge_2", "mjolnir_charge_3",
                            "mjolnir_charge_4")) {
            assertThat(json(item).toString()).as(model).contains("\"rainsenchants:item/" + model + "\"");
            checkModel("assets/rainsenchants/models/item/" + model + ".json");
        }
        for (String texture : List.of("mjolnir_cage", "mjolnir_cage_outer", "mjolnir_lightning_1", "mjolnir_lightning_2",
                "mjolnir_lightning_3", "mjolnir_lightning_4")) {
            assertThat(EnchantsPack.files()).as(texture).contains("assets/rainsenchants/textures/item/" + texture + ".png.mcmeta");
            assertThat(json("assets/rainsenchants/textures/item/" + texture + ".png.mcmeta").has("animation")).isTrue();
        }
    }

    @Test
    @DisplayName("Mjölnir in flight crackles liveliest; on cooldown it recharges stage by stage; ready it wears its cage")
    void mjolnirCharges() throws IOException {
        JsonObject flying = json("assets/rainsenchants/items/mjolnir.json").getAsJsonObject("model");
        assertThat(flying.get("property").getAsString()).isEqualTo("minecraft:custom_model_data");
        assertThat(flying.get("index").getAsInt()).isZero();
        assertThat(flying.getAsJsonObject("on_true").get("model").getAsString()).isEqualTo("rainsenchants:item/mjolnir_charge_4");
        JsonObject recharging = flying.getAsJsonObject("on_false");
        assertThat(recharging.get("type").getAsString()).isEqualTo("minecraft:range_dispatch");
        assertThat(recharging.get("property").getAsString()).isEqualTo("minecraft:cooldown");
        assertThat(recharging.getAsJsonObject("fallback").get("model").getAsString()).isEqualTo("rainsenchants:item/mjolnir");
        assertThat(json("assets/rainsenchants/models/item/mjolnir.json").getAsJsonObject("textures").toString())
                .as("ready, the cage").contains("rainsenchants:item/mjolnir_cage");
        var entries = recharging.getAsJsonArray("entries");
        // The cooldown counts down from 1: the most left shows the first stage, nearly ready the last.
        Map<Double, String> stages = new java.util.TreeMap<>();
        for (var entry : entries) {
            stages.put(entry.getAsJsonObject().get("threshold").getAsDouble(),
                    entry.getAsJsonObject().getAsJsonObject("model").get("model").getAsString());
        }
        assertThat(stages.values()).containsExactly("rainsenchants:item/mjolnir_charge_4",
                "rainsenchants:item/mjolnir_charge_3", "rainsenchants:item/mjolnir_charge_2",
                "rainsenchants:item/mjolnir_charge_1");
        assertThat(stages.keySet().iterator().next()).as("ready (0 left) is the caged look").isGreaterThan(0);
    }

    private static void checkModel(String file) throws IOException {
        assertThat(EnchantsPack.files()).contains(file);
        JsonObject json = json(file);
        assertThat(json.getAsJsonArray("elements")).isNotEmpty();
        for (String slot : List.of("thirdperson_righthand", "firstperson_righthand", "gui", "ground", "fixed")) {
            assertThat(json.getAsJsonObject("display").has(slot)).as(slot).isTrue();
        }
        for (var element : json.getAsJsonArray("elements")) {
            for (String corner : List.of("from", "to")) {
                for (var coordinate : element.getAsJsonObject().getAsJsonArray(corner)) {
                    assertThat(coordinate.getAsDouble()).as("Minecraft refuses elements outside -16..32")
                            .isBetween(-16.0, 32.0);
                }
            }
        }
        for (var texture : json.getAsJsonObject("textures").entrySet()) {
            assertThat(EnchantsPack.files()).as(texture.getKey()).contains(
                    "assets/rainsenchants/textures/" + texture.getValue().getAsString().split(":")[1] + ".png");
        }
    }
}
