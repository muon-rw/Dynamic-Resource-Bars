package dev.muon.dynamic_resource_bars.harness;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.SubElementType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

record HarnessRequest(String id, String command, boolean reloadResources, List<Size> sizes, List<Shot> shots) {

    static final String DEFAULT_SIZE = "854x480@2";

    record Size(int width, int height, int guiScale) {
        String label() {
            return width + "x" + height + "@" + (guiScale == 0 ? "auto" : guiScale + "x");
        }

        static Size parse(String spec) {
            String[] dimsAndScale = spec.trim().toLowerCase(Locale.ROOT).split("@");
            String[] dims = dimsAndScale[0].split("x");
            if (dims.length != 2) throw new IllegalArgumentException("Size must look like 854x480@2, got '" + spec + "'");
            int scale = dimsAndScale.length < 2 || dimsAndScale[1].startsWith("auto")
                    ? 0 : Integer.parseInt(dimsAndScale[1].replace("x", ""));
            return new Size(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]), scale);
        }
    }

    record Shot(String name, String screen, JsonObject player, JsonObject config, List<String> commands,
                List<JsonObject> actions, int settleTicks, List<Crop> crops, int zoom, int padding, float[] look,
                List<String> requiresMods, int recordTicks, List<JsonObject> timeline) {}

    /** Either a bar (optionally one of its sub-elements) or a literal GUI-space rect relative to {@code anchor}. */
    record Crop(String name, DraggableElement element, SubElementType sub, int[] rect, String anchor) {

        static Crop parse(JsonElement json) {
            if (json.isJsonPrimitive()) {
                String[] parts = json.getAsString().split(":");
                DraggableElement element = DraggableElement.valueOf(parts[0].toUpperCase(Locale.ROOT));
                SubElementType sub = parts.length > 1 ? SubElementType.valueOf(parts[1].toUpperCase(Locale.ROOT)) : null;
                return new Crop(json.getAsString().toLowerCase(Locale.ROOT).replace(':', '_'), element, sub, null, null);
            }
            JsonObject obj = json.getAsJsonObject();
            JsonArray r = obj.getAsJsonArray("rect");
            int[] rect = {r.get(0).getAsInt(), r.get(1).getAsInt(), r.get(2).getAsInt(), r.get(3).getAsInt()};
            return new Crop(obj.get("name").getAsString(), null, null, rect,
                    obj.has("anchor") ? obj.get("anchor").getAsString() : "top_left");
        }
    }

    static HarnessRequest read(Path file) throws IOException {
        String id = file.getFileName().toString().replaceFirst("\\.json$", "");
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        return parse(id, root);
    }

    static HarnessRequest parse(String id, JsonObject root) {
        String command = root.has("command") ? root.get("command").getAsString() : null;
        boolean reload = root.has("reloadResources") && root.get("reloadResources").getAsBoolean();

        List<Size> sizes = new ArrayList<>();
        if (root.has("sizes")) {
            for (JsonElement s : root.getAsJsonArray("sizes")) sizes.add(Size.parse(s.getAsString()));
        }
        if (sizes.isEmpty()) sizes.add(Size.parse(DEFAULT_SIZE));

        JsonObject defaults = root.has("defaults") ? root.getAsJsonObject("defaults") : new JsonObject();
        List<Shot> shots = new ArrayList<>();
        if (root.has("shots")) {
            for (JsonElement s : root.getAsJsonArray("shots")) shots.add(parseShot(merge(defaults, s.getAsJsonObject())));
        }
        return new HarnessRequest(id, command, reload, sizes, shots);
    }

    /** One level deep, so a shot's {@code player} or {@code config} block adds to the defaults' block instead of replacing it. */
    private static JsonObject merge(JsonObject defaults, JsonObject shot) {
        JsonObject merged = defaults.deepCopy();
        for (Map.Entry<String, JsonElement> e : shot.entrySet()) {
            JsonElement base = merged.get(e.getKey());
            if (base != null && base.isJsonObject() && e.getValue().isJsonObject()) {
                JsonObject combined = base.getAsJsonObject().deepCopy();
                e.getValue().getAsJsonObject().entrySet().forEach(inner -> combined.add(inner.getKey(), inner.getValue()));
                merged.add(e.getKey(), combined);
            } else {
                merged.add(e.getKey(), e.getValue());
            }
        }
        return merged;
    }

    private static Shot parseShot(JsonObject s) {
        if (!s.has("name")) throw new IllegalArgumentException("Every shot needs a \"name\"");
        String name = s.get("name").getAsString();
        if (!name.matches("[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Shot name '" + name + "' must be filename-safe");

        List<String> commands = new ArrayList<>();
        if (s.has("commands")) s.getAsJsonArray("commands").forEach(c -> commands.add(c.getAsString()));

        List<JsonObject> actions = new ArrayList<>();
        if (s.has("actions")) s.getAsJsonArray("actions").forEach(a -> actions.add(a.getAsJsonObject()));

        List<Crop> crops = null;
        if (s.has("crop")) {
            crops = new ArrayList<>();
            JsonElement c = s.get("crop");
            if (c.isJsonArray()) {
                for (JsonElement e : c.getAsJsonArray()) crops.add(Crop.parse(e));
            } else if (!c.getAsString().equalsIgnoreCase("none")) {
                crops.add(Crop.parse(c));
            }
        }

        float[] look = null;
        if (s.has("look") && s.get("look").isJsonArray()) {
            JsonArray l = s.getAsJsonArray("look");
            look = new float[]{l.get(0).getAsFloat(), l.get(1).getAsFloat()};
        } else if (!s.has("look") || !s.get("look").getAsString().equalsIgnoreCase("free")) {
            look = new float[]{0f, -90f};
        }

        List<String> requiresMods = new ArrayList<>();
        if (s.has("requiresMod")) requiresMods.add(s.get("requiresMod").getAsString());
        if (s.has("requiresMods")) s.getAsJsonArray("requiresMods").forEach(m -> requiresMods.add(m.getAsString()));

        List<JsonObject> timeline = new ArrayList<>();
        if (s.has("timeline")) s.getAsJsonArray("timeline").forEach(t -> timeline.add(t.getAsJsonObject()));

        return new Shot(
                name,
                s.has("screen") ? s.get("screen").getAsString() : "hud",
                s.has("player") ? s.getAsJsonObject("player") : new JsonObject(),
                s.has("config") ? s.getAsJsonObject("config") : new JsonObject(),
                commands,
                actions,
                s.has("settleTicks") ? s.get("settleTicks").getAsInt() : 30,
                crops,
                s.has("zoom") ? s.get("zoom").getAsInt() : 0,
                s.has("padding") ? s.get("padding").getAsInt() : 4,
                look,
                requiresMods,
                s.has("recordTicks") ? s.get("recordTicks").getAsInt() : 0,
                timeline);
    }
}
