package dev.muon.dynamic_resource_bars.foundation.data;

#if FABRIC
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import dev.muon.dynamic_resource_bars.DynamicResourceBars;

public class DynamicResourceBarsDatagen  implements DataGeneratorEntrypoint {

    @Override
    public String getEffectiveModId() {
        return DynamicResourceBars.ID;
    }

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();
        pack.addProvider(ConfigLangDatagen::new);
    }
}
#endif