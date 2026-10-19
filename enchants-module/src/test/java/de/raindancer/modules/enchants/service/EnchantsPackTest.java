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
}
