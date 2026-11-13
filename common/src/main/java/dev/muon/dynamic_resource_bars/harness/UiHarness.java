package dev.muon.dynamic_resource_bars.harness;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.mixin.MouseHandlerAccessor;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.render.AbstractBarRenderer;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.TickHandler;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Dev-only screenshot harness for agent-driven UI iteration. Set {@code DRB_UI_HARNESS} to
 * {@code watch} to keep the client running and poll {@code <DRB_UI_HARNESS_DIR>/inbox} for request
 * files, or to a request file path to run it once and quit. See {@code ui-harness/README.md}.
 */
public final class UiHarness {

    private static final String MODE = System.getenv("DRB_UI_HARNESS");
    private static final String WORLD = envOr("DRB_UI_HARNESS_WORLD", "DRB UI Harness");
    private static final int WORLD_WARMUP_TICKS = 60;
    private static final int POLL_INTERVAL_TICKS = 10;
    private static final int ACTION_SPACING_TICKS = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<DraggableElement, AbstractBarRenderer> RENDERERS = new EnumMap<>(Map.of(
            DraggableElement.HEALTH_BAR, HealthBarRenderer.INSTANCE,
            DraggableElement.MANA_BAR, ManaBarRenderer.INSTANCE,
            DraggableElement.STAMINA_BAR, StaminaBarRenderer.INSTANCE,
            DraggableElement.ARMOR_BAR, ArmorBarRenderer.INSTANCE,
            DraggableElement.AIR_BAR, AirBarRenderer.INSTANCE));

    private enum Phase { BOOT, LOADING_WORLD, WARMUP, IDLE, RUNNING, STOPPED }

    private enum Stage { RELOADING, BEGIN, ACTIONS, SETTLE, RECORDING, CAPTURING }

    private record Step(HarnessRequest.Shot shot, HarnessRequest.Size size) {}

    private static final class Job {
        final HarnessRequest request;
        final Path outDir;
        final List<Step> steps = new ArrayList<>();
        final JsonObject manifest = new JsonObject();
        final JsonArray shotEntries = new JsonArray();
        Stage stage = Stage.BEGIN;
        int stepIndex;
        int actionIndex;
        int wait;
        JsonObject playerJson;
        HarnessPlayerState playerState;
        JsonObject entry;
        CompletableFuture<?> pending;
        int recordTick;
        final List<CompletableFuture<?>> recordedFrames = new ArrayList<>();

        Job(HarnessRequest request, Path outDir) {
            this.request = request;
            this.outDir = outDir;
            for (HarnessRequest.Size size : request.sizes()) {
                for (HarnessRequest.Shot shot : request.shots()) steps.add(new Step(shot, size));
            }
        }

        Step step() {
            return steps.get(stepIndex);
        }
    }

    private static Phase phase = Phase.BOOT;
    private static Path root;
    private static int phaseTicks;
    private static boolean oneShotQueued;
    private static Job job;
    private static Map<Field, Object> configBaseline;

    private UiHarness() {}

    public static boolean isEnabled() {
        return MODE != null && !MODE.isBlank() && Services.PLATFORM.isDevelopmentEnvironment();
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        try {
            tick(mc);
        } catch (Exception e) {
            Constants.LOG.error("[UiHarness] tick failed", e);
            if (job != null) failJob(mc, e);
        }
    }

    private static void tick(Minecraft mc) throws IOException {
        if (root == null) {
            root = Path.of(envOr("DRB_UI_HARNESS_DIR", mc.gameDirectory.toPath().resolve("ui-harness").toString())).toAbsolutePath();
            Files.createDirectories(root.resolve("inbox"));
            Files.createDirectories(root.resolve("out"));
            mc.options.pauseOnLostFocus = false;
            mc.options.onboardAccessibility = false;
            mc.options.cloudStatus().set(CloudStatus.OFF);
            writeStatus("booting");
            Constants.LOG.info("[UiHarness] mode '{}', directory {}", MODE, root);
        }
        phaseTicks++;
        switch (phase) {
            case BOOT -> {
                if (mc.getOverlay() == null
                        && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
                    Constants.LOG.info("[UiHarness] opening world '{}'", WORLD);
                    HarnessWorld.openOrCreate(mc, WORLD, () -> {
                        Constants.LOG.error("[UiHarness] could not open world '{}'", WORLD);
                        writeStatus("error: could not open world");
                    });
                    enter(Phase.LOADING_WORLD, "loading_world");
                }
            }
            case LOADING_WORLD -> {
                if (mc.player != null && mc.level != null && mc.screen == null) {
                    HarnessWorld.runSetupCommands(mc);
                    enter(Phase.WARMUP, "loading_world");
                }
            }
            case WARMUP -> {
                if (phaseTicks >= WORLD_WARMUP_TICKS) {
                    configBaseline = snapshotConfig();
                    enter(Phase.IDLE, "ready");
                }
            }
            case IDLE -> {
                if (mc.player == null) return;
                if (phaseTicks % POLL_INTERVAL_TICKS == 0) pollForRequest(mc);
            }
            case RUNNING -> runJob(mc);
            case STOPPED -> {}
        }
    }

    private static void enter(Phase next, String status) {
        phase = next;
        phaseTicks = 0;
        writeStatus(status);
    }

    private static void pollForRequest(Minecraft mc) throws IOException {
        if (!MODE.equalsIgnoreCase("watch")) {
            if (oneShotQueued) {
                stop(mc);
                return;
            }
            oneShotQueued = true;
            startRequest(mc, Path.of(MODE).toAbsolutePath(), false);
            return;
        }
        Path next;
        try (Stream<Path> files = Files.list(root.resolve("inbox"))) {
            next = files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .min(Comparator.comparingLong(UiHarness::lastModified))
                    .orElse(null);
        }
        if (next != null) startRequest(mc, next, true);
    }

    private static void startRequest(Minecraft mc, Path file, boolean consume) throws IOException {
        String id = file.getFileName().toString().replaceFirst("\\.json$", "");
        Path outDir = root.resolve("out").resolve(id);
        prepareOutDir(outDir);

        HarnessRequest request;
        try {
            request = HarnessRequest.read(file);
        } catch (Exception e) {
            JsonObject manifest = baseManifest(id, outDir);
            manifest.addProperty("status", "error");
            manifest.addProperty("error", "Could not parse request: " + e.getMessage());
            writeJson(outDir.resolve("manifest.json"), manifest);
            return;
        } finally {
            if (consume) Files.deleteIfExists(file);
        }

        if ("quit".equalsIgnoreCase(request.command())) {
            JsonObject manifest = baseManifest(id, outDir);
            manifest.addProperty("status", "ok");
            writeJson(outDir.resolve("manifest.json"), manifest);
            stop(mc);
            return;
        }

        Constants.LOG.info("[UiHarness] running '{}': {} shot(s) x {} size(s)", id, request.shots().size(), request.sizes().size());
        job = new Job(request, outDir);
        job.manifest.add("request", GSON.toJsonTree(Map.of("id", id, "sizes", request.sizes().stream().map(HarnessRequest.Size::label).toList())));
        if (request.reloadResources() || "reload".equalsIgnoreCase(request.command())) {
            job.stage = Stage.RELOADING;
            job.pending = mc.reloadResourcePacks();
        }
        enter(Phase.RUNNING, "busy: " + id);
    }

    private static void runJob(Minecraft mc) throws IOException {
        Job j = job;
        if (j.stage == Stage.RELOADING) {
            // Vanilla completes the reload future before its loading overlay finishes fading out.
            if (!j.pending.isDone() || mc.getOverlay() != null) return;
            j.pending = null;
            j.stage = Stage.BEGIN;
        }
        if (j.stepIndex >= j.steps.size()) {
            finishJob(mc, "ok");
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null) throw new IllegalStateException("Player left the world mid-request");

        Step step = j.step();
        if (j.stage != Stage.BEGIN && j.playerState != null) {
            j.playerState.pin(player);
            pinLook(player, step.shot().look());
        }

        switch (j.stage) {
            case BEGIN -> {
                String missing = step.shot().requiresMods().stream()
                        .filter(modId -> !Services.PLATFORM.isModLoaded(modId))
                        .findFirst()
                        .orElse(null);
                if (missing != null) {
                    JsonObject skipped = new JsonObject();
                    skipped.addProperty("name", step.shot().name());
                    skipped.addProperty("size", step.size().label());
                    skipped.addProperty("screen", step.shot().screen());
                    skipped.addProperty("skipped", true);
                    skipped.addProperty("reason", "Mod '" + missing + "' is not loaded");
                    j.shotEntries.add(skipped);
                    j.stepIndex++;
                    return;
                }
                beginStep(mc, j, step);
                j.stage = Stage.ACTIONS;
                j.wait = ACTION_SPACING_TICKS;
            }
            case ACTIONS -> {
                if (j.wait-- > 0) return;
                if (j.actionIndex < step.shot().actions().size()) {
                    j.wait = performAction(mc, j, step.shot().actions().get(j.actionIndex++));
                } else {
                    j.stage = Stage.SETTLE;
                    j.wait = step.shot().settleTicks();
                }
            }
            case SETTLE -> {
                if (mc.getOverlay() != null || j.wait-- > 0) return;
                HarnessRequest.Shot shot = step.shot();
                List<HarnessCapture.ResolvedCrop> crops = resolveCrops(mc, shot);
                j.entry.add("player", HarnessPlayerState.describe(player));
                j.entry.add("elements", describeElements(player));
                String baseName = shot.name() + "__" + step.size().label().replace('@', '_');
                if (shot.recordTicks() > 0) {
                    j.entry.add("frames", new JsonArray());
                    j.recordTick = 0;
                    j.recordedFrames.clear();
                    j.stage = Stage.RECORDING;
                    return;
                }
                j.pending = HarnessCapture.capture(mc, j.outDir, baseName, crops, shot.zoom(), shot.padding(), j.entry);
                j.stage = Stage.CAPTURING;
            }
            case RECORDING -> {
                HarnessRequest.Shot shot = step.shot();
                for (JsonObject action : shot.timeline()) {
                    if (action.get("at").getAsInt() == j.recordTick) performAction(mc, j, action);
                }
                String frameName = shot.name() + "__" + step.size().label().replace('@', '_')
                        + String.format(Locale.ROOT, "__f%04d", j.recordTick);
                JsonObject frame = new JsonObject();
                frame.addProperty("tick", j.recordTick);
                j.entry.getAsJsonArray("frames").add(frame);
                j.recordedFrames.add(HarnessCapture.capture(mc, j.outDir, frameName, List.of(), shot.zoom(), shot.padding(), frame));
                if (++j.recordTick >= shot.recordTicks()) {
                    j.pending = CompletableFuture.allOf(j.recordedFrames.toArray(CompletableFuture[]::new));
                    j.stage = Stage.CAPTURING;
                }
            }
            case CAPTURING -> {
                if (!j.pending.isDone()) return;
                try {
                    j.pending.join();
                } catch (Exception e) {
                    j.entry.addProperty("error", "Capture failed: " + rootMessage(e));
                }
                j.shotEntries.add(j.entry);
                j.pending = null;
                j.stepIndex++;
                j.stage = Stage.BEGIN;
            }
            case RELOADING -> {}
        }
    }

    private static void beginStep(Minecraft mc, Job j, Step step) {
        HarnessRequest.Shot shot = step.shot();
        HarnessRequest.Size size = step.size();
        j.actionIndex = 0;
        j.entry = new JsonObject();
        j.entry.addProperty("name", shot.name());
        j.entry.addProperty("size", size.label());
        j.entry.addProperty("screen", shot.screen());

        Window window = mc.getWindow();
        if (window.isFullscreen()) window.toggleFullScreen();
        if (window.getWidth() != size.width() || window.getHeight() != size.height()) {
            window.setWindowed(size.width(), size.height());
        }
        mc.options.guiScale().set(size.guiScale());
        mc.resizeDisplay();

        restoreConfig();
        applyConfig(shot.config());

        mc.setScreen(HarnessScreens.create(shot.screen()));
        mc.gui.getChat().clearMessages(false);
        mc.getToasts().clear();
        setMouse(mc, -1000, -1000);

        j.playerJson = shot.player().deepCopy();
        j.playerState = HarnessPlayerState.parse(j.playerJson);
        j.playerState.applyEffects(mc.player);
        j.playerState.pin(mc.player);
        pinLook(mc.player, shot.look());
        HarnessWorld.runCommands(mc, shot.commands());
        // Restarts the overlay pulse so every capture lands on the same phase of it.
        TickHandler.resetFlash();
    }

    /** Returns how many ticks to wait before the next action. */
    private static int performAction(Minecraft mc, Job j, JsonObject action) {
        Screen screen = mc.screen;
        if (action.has("wait")) return action.get("wait").getAsInt();
        if (action.has("hover")) {
            JsonArray p = action.getAsJsonArray("hover");
            setMouse(mc, p.get(0).getAsDouble(), p.get(1).getAsDouble());
        } else if (action.has("click")) {
            JsonArray p = action.getAsJsonArray("click");
            double x = p.get(0).getAsDouble(), y = p.get(1).getAsDouble();
            int button = action.has("button") ? action.get("button").getAsInt() : 0;
            setMouse(mc, x, y);
            if (requireScreen(j, screen, "click")) {
                screen.mouseClicked(x, y, button);
                screen.mouseReleased(x, y, button);
            }
        } else if (action.has("drag")) {
            JsonArray p = action.getAsJsonArray("drag");
            double x0 = p.get(0).getAsDouble(), y0 = p.get(1).getAsDouble();
            double x1 = p.get(2).getAsDouble(), y1 = p.get(3).getAsDouble();
            int button = action.has("button") ? action.get("button").getAsInt() : 0;
            if (requireScreen(j, screen, "drag")) {
                screen.mouseClicked(x0, y0, button);
                screen.mouseDragged(x1, y1, button, x1 - x0, y1 - y0);
                screen.mouseReleased(x1, y1, button);
            }
            setMouse(mc, x1, y1);
        } else if (action.has("key")) {
            ParsedKey key = parseKey(action.get("key").getAsString());
            if (requireScreen(j, screen, "key")) {
                screen.keyPressed(key.keyCode(), 0, key.modifiers());
                screen.keyReleased(key.keyCode(), 0, key.modifiers());
            }
        } else if (action.has("player")) {
            action.getAsJsonObject("player").entrySet().forEach(e -> j.playerJson.add(e.getKey(), e.getValue()));
            j.playerState = HarnessPlayerState.parse(j.playerJson);
            if (action.getAsJsonObject("player").has("effects")) j.playerState.applyEffects(mc.player);
        } else if (action.has("config")) {
            applyConfig(action.getAsJsonObject("config"));
        } else if (action.has("command")) {
            HarnessWorld.runCommands(mc, List.of(action.get("command").getAsString()));
        } else {
            HarnessCapture.warn(j.entry, "Unknown action " + action);
        }
        return ACTION_SPACING_TICKS;
    }

    private static boolean requireScreen(Job j, Screen screen, String action) {
        if (screen != null) return true;
        HarnessCapture.warn(j.entry, "'" + action + "' needs an open screen; the bare HUD has no input handler");
        return false;
    }

    private record ParsedKey(int keyCode, int modifiers) {}

    private static ParsedKey parseKey(String spec) {
        int modifiers = 0;
        String key = spec.toLowerCase(Locale.ROOT);
        while (key.contains("+")) {
            String mod = key.substring(0, key.indexOf('+'));
            key = key.substring(key.indexOf('+') + 1);
            modifiers |= switch (mod) {
                case "shift" -> GLFW.GLFW_MOD_SHIFT;
                case "ctrl", "control" -> GLFW.GLFW_MOD_CONTROL;
                case "alt" -> GLFW.GLFW_MOD_ALT;
                default -> throw new IllegalArgumentException("Unknown modifier '" + mod + "' in key '" + spec + "'");
            };
        }
        return new ParsedKey(InputConstants.getKey("key.keyboard." + key).getValue(), modifiers);
    }

    /** Coordinates are GUI pixels; vanilla reads the mouse in window pixels. */
    private static void setMouse(Minecraft mc, double guiX, double guiY) {
        Window window = mc.getWindow();
        MouseHandlerAccessor mouse = (MouseHandlerAccessor) mc.mouseHandler;
        mouse.dynamic_resource_bars$setXpos(guiX * window.getScreenWidth() / window.getGuiScaledWidth());
        mouse.dynamic_resource_bars$setYpos(guiY * window.getScreenHeight() / window.getGuiScaledHeight());
    }

    private static void pinLook(LocalPlayer player, float[] look) {
        if (look == null) return;
        player.setYRot(look[0]);
        player.setXRot(look[1]);
        player.yRotO = look[0];
        player.xRotO = look[1];
        player.setYHeadRot(look[0]);
    }

    private static List<HarnessCapture.ResolvedCrop> resolveCrops(Minecraft mc, HarnessRequest.Shot shot) {
        List<HarnessCapture.ResolvedCrop> out = new ArrayList<>();
        if (shot.crops() == null) {
            if (!HarnessScreens.showsHud(shot.screen())) return out;
            for (var e : RENDERERS.entrySet()) {
                out.add(new HarnessCapture.ResolvedCrop(e.getKey().name().toLowerCase(Locale.ROOT), e.getValue().getScreenRect(mc.player)));
            }
            return out;
        }
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        for (HarnessRequest.Crop crop : shot.crops()) {
            ScreenRect rect;
            if (crop.element() != null) {
                AbstractBarRenderer renderer = RENDERERS.get(crop.element());
                rect = crop.sub() == null ? renderer.getScreenRect(mc.player) : renderer.getSubElementRect(crop.sub(), mc.player);
            } else {
                int[] r = crop.rect();
                int ax = switch (crop.anchor().toLowerCase(Locale.ROOT)) {
                    case "top_center", "center", "bottom_center" -> w / 2;
                    case "top_right", "bottom_right" -> w;
                    default -> 0;
                };
                int ay = switch (crop.anchor().toLowerCase(Locale.ROOT)) {
                    case "center" -> h / 2;
                    case "bottom_left", "bottom_center", "bottom_right" -> h;
                    default -> 0;
                };
                rect = new ScreenRect(ax + r[0], ay + r[1], r[2], r[3]);
            }
            out.add(new HarnessCapture.ResolvedCrop(crop.name(), rect));
        }
        return out;
    }

    private static JsonObject describeElements(LocalPlayer player) {
        JsonObject elements = new JsonObject();
        for (var e : RENDERERS.entrySet()) {
            AbstractBarRenderer renderer = e.getValue();
            JsonObject el = new JsonObject();
            el.add("rect", HarnessCapture.rectJson(renderer.getScreenRect(player)));
            el.addProperty("alpha", renderer.currentAlpha());
            JsonObject subs = new JsonObject();
            for (SubElementType sub : SubElementType.values()) {
                ScreenRect r = renderer.getSubElementRect(sub, player);
                if (r.width() > 0 || r.height() > 0) subs.add(sub.name(), HarnessCapture.rectJson(r));
            }
            el.add("subElements", subs);
            elements.add(e.getKey().name(), el);
        }
        return elements;
    }

    private static void finishJob(Minecraft mc, String status) throws IOException {
        Job j = job;
        job = null;
        if (mc.screen != null) mc.setScreen(null);
        restoreConfig();
        ModConfigManager.getClient().save();
        if (mc.player != null) {
            HarnessPlayerState.parse(new JsonObject()).applyEffects(mc.player);
        }

        JsonObject manifest = baseManifest(j.request.id(), j.outDir);
        manifest.addProperty("status", status);
        j.manifest.entrySet().forEach(e -> manifest.add(e.getKey(), e.getValue()));
        manifest.add("shots", j.shotEntries);
        writeJson(j.outDir.resolve("manifest.json"), manifest);
        Constants.LOG.info("[UiHarness] '{}' finished: {}", j.request.id(), status);
        enter(Phase.IDLE, "ready");
    }

    private static void failJob(Minecraft mc, Exception e) {
        Job j = job;
        if (j.entry != null) {
            j.entry.addProperty("error", rootMessage(e));
            j.shotEntries.add(j.entry);
        }
        j.manifest.addProperty("error", rootMessage(e));
        try {
            finishJob(mc, "error");
        } catch (IOException io) {
            Constants.LOG.error("[UiHarness] could not write manifest", io);
            job = null;
            enter(Phase.IDLE, "ready");
        }
    }

    private static void stop(Minecraft mc) {
        enter(Phase.STOPPED, "stopped");
        mc.stop();
    }

    private static Map<Field, Object> snapshotConfig() {
        Map<Field, Object> snapshot = new LinkedHashMap<>();
        ClientConfig config = ModConfigManager.getClient();
        for (Field f : ClientConfig.class.getDeclaredFields()) {
            int mods = f.getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) continue;
            try {
                f.setAccessible(true);
                snapshot.put(f, f.get(config));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return snapshot;
    }

    private static void restoreConfig() {
        ClientConfig config = ModConfigManager.getClient();
        configBaseline.forEach((f, v) -> {
            try {
                f.set(config, v);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static void applyConfig(JsonObject overrides) {
        ClientConfig config = ModConfigManager.getClient();
        for (Map.Entry<String, JsonElement> e : overrides.entrySet()) {
            Field field = configBaseline.keySet().stream()
                    .filter(f -> f.getName().equals(e.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("ClientConfig has no field '" + e.getKey() + "'"));
            try {
                field.set(config, GSON.fromJson(e.getValue(), field.getGenericType()));
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static JsonObject baseManifest(String id, Path outDir) {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("id", id);
        manifest.addProperty("loader", Services.PLATFORM.getPlatformName());
        manifest.addProperty("finishedAt", Instant.now().toString());
        manifest.addProperty("outDir", outDir.toString());
        return manifest;
    }

    private static void prepareOutDir(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        try (Stream<Path> files = Files.list(outDir)) {
            for (Path p : files.toList()) {
                String name = p.getFileName().toString();
                if (Files.isRegularFile(p) && (name.endsWith(".png") || name.endsWith(".json"))) Files.delete(p);
            }
        }
    }

    private static void writeStatus(String state) {
        if (root == null) return;
        JsonObject status = new JsonObject();
        status.addProperty("state", state);
        status.addProperty("mode", MODE);
        status.addProperty("world", WORLD);
        status.addProperty("loader", Services.PLATFORM.getPlatformName());
        status.addProperty("pid", ProcessHandle.current().pid());
        status.addProperty("updatedAt", Instant.now().toString());
        try {
            writeJson(root.resolve("status.json"), status);
        } catch (IOException e) {
            Constants.LOG.error("[UiHarness] could not write status", e);
        }
    }

    /** Writes through a temp file so pollers never read a half-written manifest. */
    private static void writeJson(Path target, JsonObject json) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(json));
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static long lastModified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
