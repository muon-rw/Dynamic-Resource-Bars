package dev.muon.dynamic_resource_bars.harness;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.Util;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class HarnessCapture {

    record ResolvedCrop(String name, ScreenRect guiRect) {}

    private HarnessCapture() {}

    /**
     * Reads back the last rendered frame and writes the full frame plus each crop, scaled up with
     * nearest-neighbor so individual texture pixels stay legible. Completes with the shot's manifest entry.
     */
    static CompletableFuture<JsonObject> capture(Minecraft mc, Path dir, String baseName, List<ResolvedCrop> crops,
                                                 int zoom, int padding, JsonObject entry) {
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        Util.ioPool().execute(() -> {
            try (image) {
                int scale = Math.max(1, Math.round(image.getWidth() / (float) guiWidth));
                int cropZoom = zoom > 0 ? zoom : Math.max(1, Math.round(4f / scale));

                Path full = dir.resolve(baseName + ".png");
                image.writeToFile(full);
                entry.addProperty("file", full.toAbsolutePath().toString());
                entry.addProperty("framebuffer", image.getWidth() + "x" + image.getHeight());
                entry.addProperty("pixelsPerGuiPixel", scale);
                if (isUniform(image, 0, 0, image.getWidth(), image.getHeight())) {
                    warn(entry, "Full frame is a single color; the capture is not valid evidence");
                }

                JsonArray cropEntries = new JsonArray();
                for (ResolvedCrop crop : crops) {
                    cropEntries.add(writeCrop(image, dir, baseName, crop, scale, cropZoom, padding, entry));
                }
                entry.add("crops", cropEntries);
                result.complete(entry);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private static JsonObject writeCrop(NativeImage frame, Path dir, String baseName, ResolvedCrop crop,
                                        int scale, int zoom, int padding, JsonObject shotEntry) throws Exception {
        ScreenRect r = crop.guiRect();
        JsonObject out = new JsonObject();
        out.addProperty("name", crop.name());
        out.add("guiRect", rectJson(r));
        if (r.width() <= 0 || r.height() <= 0) {
            warn(shotEntry, "Crop '" + crop.name() + "' has an empty rect; the element did not lay out");
            return out;
        }

        int x0 = Math.max(0, (r.x() - padding) * scale);
        int y0 = Math.max(0, (r.y() - padding) * scale);
        int x1 = Math.min(frame.getWidth(), (r.x() + r.width() + padding) * scale);
        int y1 = Math.min(frame.getHeight(), (r.y() + r.height() + padding) * scale);
        if (x1 <= x0 || y1 <= y0) {
            warn(shotEntry, "Crop '" + crop.name() + "' is entirely off screen");
            return out;
        }

        int w = x1 - x0, h = y1 - y0;
        try (NativeImage zoomed = new NativeImage(w * zoom, h * zoom, false)) {
            for (int y = 0; y < h * zoom; y++) {
                for (int x = 0; x < w * zoom; x++) {
                    zoomed.setPixelRGBA(x, y, frame.getPixelRGBA(x0 + x / zoom, y0 + y / zoom));
                }
            }
            Path file = dir.resolve(baseName + "__" + crop.name() + ".png");
            zoomed.writeToFile(file);
            out.addProperty("file", file.toAbsolutePath().toString());
            out.addProperty("zoom", zoom);
        }
        boolean uniform = isUniform(frame, x0, y0, w, h);
        out.addProperty("uniform", uniform);
        if (uniform) warn(shotEntry, "Crop '" + crop.name() + "' is a single color; the element may not have drawn");
        return out;
    }

    private static boolean isUniform(NativeImage image, int x0, int y0, int w, int h) {
        int first = image.getPixelRGBA(x0, y0);
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                if (image.getPixelRGBA(x, y) != first) return false;
            }
        }
        return true;
    }

    static JsonArray rectJson(ScreenRect r) {
        JsonArray a = new JsonArray();
        a.add(r.x());
        a.add(r.y());
        a.add(r.width());
        a.add(r.height());
        return a;
    }

    static void warn(JsonObject entry, String message) {
        if (!entry.has("warnings")) entry.add("warnings", new JsonArray());
        entry.getAsJsonArray("warnings").add(message);
    }
}
