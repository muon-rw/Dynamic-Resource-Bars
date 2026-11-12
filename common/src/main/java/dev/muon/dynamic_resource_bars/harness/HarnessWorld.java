package dev.muon.dynamic_resource_bars.harness;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.List;

final class HarnessWorld {

    /**
     * Peaceful with regeneration and damage off, so nothing on the server side moves the
     * player's stats away from what we pin on the client.
     */
    private static final List<String> SETUP_COMMANDS = List.of(
            "difficulty peaceful",
            "gamemode survival",
            "gamerule advance_time false",
            "gamerule advance_weather false",
            "gamerule natural_health_regeneration false",
            "gamerule spawn_mobs false",
            "gamerule drowning_damage false",
            "gamerule fall_damage false",
            "gamerule fire_damage false",
            "gamerule freeze_damage false",
            "gamerule show_advancement_messages false",
            "time set noon",
            "weather clear");

    private HarnessWorld() {}

    static void openOrCreate(Minecraft mc, String levelId, Runnable onFailure) {
        if (mc.getLevelSource().levelExists(levelId)) {
            mc.createWorldOpenFlows().openWorld(levelId, onFailure);
            return;
        }
        LevelSettings settings = new LevelSettings(levelId, GameType.SURVIVAL,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(levelId, settings, new WorldOptions(0L, false, false),
                WorldPresets::createFlatWorldDimensions, null);
    }

    static void runSetupCommands(Minecraft mc) {
        runCommands(mc, SETUP_COMMANDS);
    }

    static void runCommands(Minecraft mc, List<String> commands) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null || commands.isEmpty()) return;
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) return;
            for (String command : commands) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
            }
        });
    }
}
