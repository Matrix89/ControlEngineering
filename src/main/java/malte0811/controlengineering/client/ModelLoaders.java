package malte0811.controlengineering.client;


import malte0811.controlengineering.ControlEngineering;
import malte0811.controlengineering.client.model.SpecialModelLoader;
import malte0811.controlengineering.client.model.logic.DynamicLogicModelLoader;
import malte0811.controlengineering.client.model.panel.PanelModel;
import malte0811.controlengineering.client.render.panel.PanelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ControlEngineering.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModelLoaders {
    public static final ResourceLocation PANEL_MODEL = new ResourceLocation(ControlEngineering.MODID, "panel");
    public static final ResourceLocation LOGIC_CABINET = new ResourceLocation(
            ControlEngineering.MODID, "dynamic_logic"
    );

    @SubscribeEvent
    public static void registerModelLoaders(ModelRegistryEvent ev) {
        ModelLoaderRegistry.registerLoader(PANEL_MODEL, new SpecialModelLoader(
                PanelModel::new, PanelRenderer.PANEL_TEXTURE_LOC
        ));
        ModelLoaderRegistry.registerLoader(LOGIC_CABINET, new DynamicLogicModelLoader());
    }
}
