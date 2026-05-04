package dev.muon.dynamic_resource_bars.compat;

import tictim.paraglider.api.plugin.ParagliderPlugin;
import tictim.paraglider.api.stamina.StaminaPlugin;

/**
 * Paragliders plugin that suppresses the in-world stamina wheel so DRB can drive the stamina UI
 * via {@link ParaglidersStaminaProvider} without double-rendering.
 *
 * <p>Discovered by Paragliders' {@code NeoForgeParagliderPluginLoader} via the runtime-retained
 * {@link ParagliderPlugin} annotation — it scans every mod's bytecode for the annotation, then
 * reflectively instantiates the class via its public no-arg constructor. We never reference this
 * class from our own code, so when Paragliders is absent the JVM never resolves
 * {@link StaminaPlugin} or any other Paragliders type — the dependency stays optional.
 *
 * <p>{@link StaminaPlugin#removeStaminaWheel()} is read once at startup and cached as a final
 * boolean inside {@code ParagliderClientMod}, so this is a global, install-time decision: when
 * DRB and Paragliders are both installed, the wheel is hidden regardless of which stamina source
 * the user picks at runtime. That's the expected behaviour — users who install both mods want
 * DRB to own the stamina UI, and the wheel duplicating values that DRB also renders adds noise
 * rather than information. Users who want the wheel back can uninstall DRB.
 *
 * <p>This was originally attempted via {@code event.wrapLayer(paraglider:stamina_wheel, ...)},
 * but Paragliders registers that layer with {@code registerAboveAll} during its own
 * {@code RegisterGuiLayersEvent} listener — which fires after ours at default priority — so the
 * wrap call hit a missing-target {@code IllegalArgumentException} and was silently dropped. The
 * plugin API has none of those ordering quirks.
 */
@ParagliderPlugin
public final class DynamicResourceBarsParagliderPlugin implements StaminaPlugin {

    @Override
    public boolean removeStaminaWheel() {
        return true;
    }
}
