package de.raindancer.modules.enchants;

import de.raindancer.modules.api.ModuleCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnchantsCommandsTest {

    @Test
    @DisplayName("every command describes itself in one sentence Core's directory takes: 120 characters at most, or the plugin does not start")
    void descriptionsFit() {
        for (ModuleCommand command : EnchantsCommands.declared()) {
            assertThat(command.description()).as(command.name()).hasSizeLessThanOrEqualTo(120);
        }
    }
}
