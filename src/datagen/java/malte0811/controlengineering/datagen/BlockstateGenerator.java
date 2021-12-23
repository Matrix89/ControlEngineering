package malte0811.controlengineering.datagen;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.data.blockstates.ConnectorBlockBuilder;
import blusunrize.immersiveengineering.data.models.SplitModelBuilder;
import com.google.common.collect.ImmutableMap;
import malte0811.controlengineering.ControlEngineering;
import malte0811.controlengineering.blocks.CEBlocks;
import malte0811.controlengineering.blocks.bus.BusInterfaceBlock;
import malte0811.controlengineering.blocks.bus.BusRelayBlock;
import malte0811.controlengineering.blocks.bus.LineAccessBlock;
import malte0811.controlengineering.blocks.logic.LogicCabinetBlock;
import malte0811.controlengineering.blocks.logic.LogicWorkbenchBlock;
import malte0811.controlengineering.blocks.panels.PanelBlock;
import malte0811.controlengineering.blocks.panels.PanelCNCBlock;
import malte0811.controlengineering.blocks.panels.PanelDesignerBlock;
import malte0811.controlengineering.blocks.tape.KeypunchBlock;
import malte0811.controlengineering.client.ModelLoaders;
import malte0811.controlengineering.datagen.modelbuilder.DynamicModelBuilder;
import malte0811.controlengineering.datagen.modelbuilder.LogicCabinetBuilder;
import malte0811.controlengineering.util.DirectionUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder.PartialBlockstate;
import net.minecraftforge.client.model.generators.loaders.CompositeModelBuilder;
import net.minecraftforge.client.model.generators.loaders.OBJLoaderBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;

public class BlockstateGenerator extends BlockStateProvider {
    private static final ConfiguredModel EMPTY_MODEL = new ConfiguredModel(
            new ModelFile.UncheckedModelFile(new ResourceLocation(Lib.MODID, "block/ie_empty"))
    );

    public BlockstateGenerator(DataGenerator gen, ExistingFileHelper exFileHelper) {
        super(gen, ControlEngineering.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerConnector(
                obj("bus_relay.obj"), CEBlocks.BUS_RELAY, 90, RenderType.solid(), BusRelayBlock.FACING
        );
        registerConnector(
                obj("line_access.obj"), CEBlocks.LINE_ACCESS, 0, RenderType.cutout(), LineAccessBlock.FACING
        );
        registerConnector(
                obj("bus_interface.obj"), CEBlocks.BUS_INTERFACE, 90, RenderType.solid(), BusInterfaceBlock.FACING
        );

        panelModel();
        column2(obj("panel_cnc.obj"), CEBlocks.PANEL_CNC, PanelCNCBlock.FACING);
        column2(obj("keypunch.obj"), CEBlocks.KEYPUNCH, KeypunchBlock.FACING);
        logicCabinetModel();
        rotatedWithOffset(
                CEBlocks.LOGIC_WORKBENCH,
                obj("logic_cabinet/workbench.obj", modLoc("transform/block_half_size")),
                LogicWorkbenchBlock.Offset.ORIGIN, LogicWorkbenchBlock.OFFSET,
                LogicWorkbenchBlock.FACING
        );
        rotatedWithOffset(
                CEBlocks.PANEL_DESIGNER,
                obj("panel_designer.obj", modLoc("transform/block_half_size")),
                PanelDesignerBlock.Offset.ORIGIN, PanelDesignerBlock.OFFSET,
                PanelDesignerBlock.FACING
        );
    }

    private void panelModel() {
        BlockModelBuilder baseModel = models().cubeAll("panel/base", modLoc("block/control_panel"));
        BlockModelBuilder topModel = models().getBuilder("panel/top")
                .customLoader(DynamicModelBuilder.customLoader(ModelLoaders.PANEL_MODEL))
                .end();
        getVariantBuilder(CEBlocks.CONTROL_PANEL.get())
                .partialState()
                .with(PanelBlock.IS_BASE, true)
                .setModels(new ConfiguredModel(baseModel))
                .partialState()
                .with(PanelBlock.IS_BASE, false)
                .setModels(new ConfiguredModel(topModel));
    }

    private void logicCabinetModel() {
        BlockModelBuilder chassis = obj("logic_cabinet/chassis.obj", modLoc("transform/block_half_size"));
        BlockModelBuilder logicModel = models().getBuilder("combined_logic_cabinet")
                .customLoader(CompositeModelBuilder::begin)
                .submodel("static", chassis)
                .submodel("dynamic", models().getBuilder("dynamic_logic_cabinet")
                        .customLoader(LogicCabinetBuilder::begin)
                        .board(obj("logic_cabinet/board.obj"))
                        .tube(obj("logic_cabinet/tube.obj"))
                        .end())
                .end();
        horizontalRotated(
                CEBlocks.LOGIC_CABINET,
                LogicCabinetBlock.FACING,
                logicModel,
                ImmutableMap.of(LogicCabinetBlock.HEIGHT, 0)
        );
        emptyModel(CEBlocks.LOGIC_CABINET, ImmutableMap.of(LogicCabinetBlock.HEIGHT, 1));
        itemModels().getBuilder(ItemModels.name(CEBlocks.LOGIC_CABINET))
                .parent(chassis);
    }

    private void column2(ModelFile mainModel, RegistryObject<? extends Block> block, Property<Direction> facing) {
        itemModels().getBuilder(ItemModels.name(block)).parent(mainModel);
        var splitModel = models().getBuilder(mainModel.getLocation().getPath() + "_split")
                .customLoader(SplitModelBuilder::begin)
                .innerModel(mainModel)
                .parts(List.of(Vec3i.ZERO, new Vec3i(0, 1, 0)))
                .end();
        horizontalRotated(block, facing, splitModel);
    }

    private <T extends Comparable<T>> void rotatedWithOffset(
            RegistryObject<? extends Block> b,
            ModelFile mainModel,
            T baseOffset, Property<T> offsetProp,
            Property<Direction> facing
    ) {
        for (T offset : offsetProp.getPossibleValues()) {
            ModelFile model;
            if (offset == baseOffset) {
                model = mainModel;
            } else {
                model = EMPTY_MODEL.model;
            }
            horizontalRotated(b, facing, model, ImmutableMap.of(offsetProp, offset));
        }
        itemModels().getBuilder(ItemModels.name(b))
                .parent(mainModel);
    }

    private BlockModelBuilder obj(String objFile) {
        return obj(objFile, mcLoc("block"));
    }

    private BlockModelBuilder obj(String objFile, ResourceLocation parent) {
        return models()
                .withExistingParent(objFile.replace('.', '_'), parent)
                .customLoader(OBJLoaderBuilder::begin)
                .modelLocation(addModelsPrefix(modLoc(objFile)))
                .flipV(true)
                .detectCullableFaces(false)
                .end();
    }

    private void registerConnector(
            ModelFile mainModel,
            RegistryObject<? extends Block> block,
            int xForHorizontal,
            RenderType layer,
            Property<Direction> facing
    ) {
        ConnectorBlockBuilder.builder(models(), getVariantBuilder(block.get()), ($1, $2) -> {})
                .rotationData(facing, xForHorizontal)
                .fixedModel(mainModel)
                .layers(layer)
                .build();
        itemModels().getBuilder(ItemModels.name(block)).parent(mainModel);
    }

    private void horizontalRotated(RegistryObject<? extends Block> b, Property<Direction> facing, ModelFile model) {
        horizontalRotated(b, facing, model, ImmutableMap.of());
    }

    private void horizontalRotated(
            RegistryObject<? extends Block> b,
            Property<Direction> facing,
            ModelFile model,
            Map<Property<?>, Comparable<?>> additional
    ) {
        for (Direction d : DirectionUtils.BY_HORIZONTAL_INDEX) {
            PartialBlockstate partial = getVariantBuilder(b.get())
                    .partialState()
                    .with(facing, d);
            for (var fixedProperty : additional.entrySet()) {
                partial = withUnchecked(partial, fixedProperty.getKey(), fixedProperty.getValue());
            }
            partial.modelForState()
                    .rotationY((int) d.toYRot())
                    .modelFile(model)
                    .addModel();
        }
        itemModels().getBuilder(ItemModels.name(b)).parent(model);
    }

    private void emptyModel(RegistryObject<? extends Block> b, Map<Property<?>, Comparable<?>> additional) {
        var partialState = getVariantBuilder(b.get()).partialState();
        for (var fixedProperty : additional.entrySet()) {
            partialState = withUnchecked(partialState, fixedProperty.getKey(), fixedProperty.getValue());
        }
        partialState.modelForState()
                .modelFile(EMPTY_MODEL.model)
                .addModel();
    }

    private <T extends Comparable<T>>
    PartialBlockstate withUnchecked(PartialBlockstate original, Property<T> prop, Comparable<?> value) {
        return original.with(prop, (T) value);
    }

    private ResourceLocation addModelsPrefix(ResourceLocation in) {
        return new ResourceLocation(in.getNamespace(), "models/" + in.getPath());
    }

}