package malte0811.controlengineering.gui.logic;

import blusunrize.lib.manual.ManualUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import malte0811.controlengineering.ControlEngineering;
import malte0811.controlengineering.gui.StackedScreen;
import malte0811.controlengineering.items.IEItemRefs;
import malte0811.controlengineering.logic.schematic.ConnectedPin;
import malte0811.controlengineering.logic.schematic.Schematic;
import malte0811.controlengineering.logic.schematic.SchematicCircuitConverter;
import malte0811.controlengineering.logic.schematic.WireSegment;
import malte0811.controlengineering.logic.schematic.symbol.PlacedSymbol;
import malte0811.controlengineering.logic.schematic.symbol.SymbolInstance;
import malte0811.controlengineering.logic.schematic.symbol.SymbolPin;
import malte0811.controlengineering.network.logic.*;
import malte0811.controlengineering.tiles.logic.LogicCabinetTile;
import malte0811.controlengineering.tiles.logic.LogicWorkbenchTile.AvailableIngredients;
import malte0811.controlengineering.util.GuiUtil;
import malte0811.controlengineering.util.TextUtil;
import malte0811.controlengineering.util.math.Vec2d;
import malte0811.controlengineering.util.math.Vec2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static net.minecraft.util.Mth.ceil;
import static net.minecraft.util.Mth.floor;

public class LogicDesignScreen extends StackedScreen implements MenuAccess<LogicDesignContainer> {
    public static final String COMPONENTS_KEY = ControlEngineering.MODID + ".gui.components";
    public static final String ENABLE_DRC_KEY = ControlEngineering.MODID + ".gui.drcOn";
    public static final String DISABLE_DRC_KEY = ControlEngineering.MODID + ".gui.drcOff";
    public static final String PIN_KEY = ControlEngineering.MODID + ".gui.pin";

    private static final int TRANSLUCENT_BORDER_SIZE = 20;
    private static final int WHITE_BORDER_SIZE = 1;
    private static final int TOTAL_BORDER = TRANSLUCENT_BORDER_SIZE + WHITE_BORDER_SIZE;
    public static final int BASE_SCALE = 3;

    private final LogicDesignContainer container;
    private Schematic schematic;
    @Nullable
    private Vec2i currentWireStart = null;
    @Nullable
    private SymbolInstance<?> placingSymbol = null;
    private boolean resetAfterPlacingSymbol = false;
    private List<ConnectedPin> errors = ImmutableList.of();
    private boolean errorsShown = false;
    private float minScale = 0.5F;
    private float currentScale = BASE_SCALE;
    // In schematic coordinates
    private double centerX = 0;
    private double centerY = 0;

    public LogicDesignScreen(LogicDesignContainer container, Component title) {
        super(title);
        this.schematic = new Schematic();
        this.container = container;
    }

    @Override
    protected void init() {
        super.init();
        if (!container.readOnly) {
            addButton(new Button(
                    TOTAL_BORDER, TOTAL_BORDER, 20, 20, new TextComponent("C"),
                    btn -> minecraft.setScreen(new CellSelectionScreen(s -> {
                        placingSymbol = s;
                        resetAfterPlacingSymbol = false;
                    })),
                    makeTooltip(() -> COMPONENTS_KEY)
            ));
            addButton(new Button(
                    TOTAL_BORDER, TOTAL_BORDER + 20, 20, 20, new TextComponent("E"),
                    btn -> {
                        errorsShown = !errorsShown;
                        updateErrors();
                    },
                    makeTooltip(() -> errorsShown ? DISABLE_DRC_KEY : ENABLE_DRC_KEY)
            ));
        }
        minScale = Math.max(
                getScaleForShownSize(width, Schematic.BOUNDARY.getWidth()),
                getScaleForShownSize(height, Schematic.BOUNDARY.getHeight())
        );
    }

    private Button.OnTooltip makeTooltip(Supplier<String> key) {
        return ($, transform, x, y) -> this.renderTooltip(transform, new TranslatableComponent(key.get()), x, y);
    }

    @Override
    protected void renderForeground(@Nonnull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        matrixStack.pushPose();
        matrixStack.translate(width / 2., height / 2., 0);
        matrixStack.scale(currentScale, currentScale, 1);
        matrixStack.translate(-centerX, -centerY, 0);

        final double scale = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int) (TOTAL_BORDER * scale), (int) (TOTAL_BORDER * scale),
                (int) ((width - 2 * TOTAL_BORDER) * scale), (int) ((height - 2 * TOTAL_BORDER) * scale)
        );
        drawErrors(matrixStack);
        drawBoundary(matrixStack);
        Vec2d mousePos = getMousePosition(mouseX, mouseY);
        schematic.render(matrixStack, mousePos);

        Optional<Component> currentError = Optional.empty();
        PlacedSymbol placed = getPlacingSymbol(mousePos);
        if (placed != null) {
            currentError = schematic.getChecker().getErrorForAdding(placed);
            placed.render(matrixStack);
        } else {
            WireSegment placedWire = getPlacingSegment(mousePos);
            if (placedWire != null) {
                currentError = schematic.getChecker().getErrorForAdding(placedWire);
                final int color = currentError.isPresent() ? 0xffff5515 : 0xff785515;
                placedWire.renderWithoutBlobs(matrixStack, color);
            }
        }
        RenderSystem.disableScissor();

        matrixStack.popPose();
        renderIngredients(matrixStack);
        renderTooltip(matrixStack, mouseX, mouseY, mousePos, currentError.orElse(null));
    }

    private void renderTooltip(
            PoseStack transform, int mouseX, int mouseY, Vec2d schematicMouse, @Nullable Component currentError
    ) {
        if (currentError != null) {
            if (currentError instanceof MutableComponent) {
                ((MutableComponent) currentError).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            }
            renderTooltip(transform, currentError, mouseX, mouseY);
        } else {
            PlacedSymbol hovered = schematic.getSymbolAt(schematicMouse);
            if (hovered != null) {
                Component toShow = hovered.getSymbol().getName();
                List<FormattedCharSequence> tooltip = new ArrayList<>();
                tooltip.add(toShow.getVisualOrderText());
                for (SymbolPin pin : getHoveredPins(hovered, schematicMouse)) {
                    tooltip.add(
                            new TranslatableComponent(PIN_KEY, pin.getPinName())
                                    .withStyle(ChatFormatting.GRAY)
                                    .getVisualOrderText()
                    );
                }
                List<MutableComponent> extra = hovered.getSymbol().getExtraDesc();
                for (MutableComponent extraLine : extra) {
                    TextUtil.addTooltipLineReordering(tooltip, extraLine);
                }
                renderTooltip(transform, tooltip, mouseX, mouseY);
            }
        }
    }

    private void renderIngredients(PoseStack transform) {
        Optional<AvailableIngredients> stored = container.getAvailableIngredients();
        transform.pushPose();
        transform.translate(width - TOTAL_BORDER - 17, height - TOTAL_BORDER - 17, 0);
        final int numTubes = schematic.getNumTubes();
        final int numWires = schematic.getWireLength();
        final int numBoards = LogicCabinetTile.getNumBoardsFor(numTubes);
        renderIngredient(transform, null, numBoards, IEItemRefs.CIRCUIT_BOARD);
        transform.translate(0, -16, 0);
        renderIngredient(
                transform, stored.map(AvailableIngredients::getAvailableTubes).orElse(null), numTubes, IEItemRefs.TUBE
        );
        transform.translate(0, -16, 0);
        //TODO fix default
        renderIngredient(
                transform, stored.map(AvailableIngredients::getAvailableWires).orElse(null), numWires, IEItemRefs.WIRE
        );
        transform.popPose();
    }

    private void renderIngredient(
            PoseStack transform, @Nullable ItemStack available, int required, Supplier<Item> defaultItem
    ) {
        MutableComponent info;
        if (available != null) {
            info = new TextComponent(Math.min(available.getCount(), required) + " / " + required);
            if (available.getCount() < required) {
                info.withStyle(ChatFormatting.RED);
            }
        } else {
            info = new TextComponent(Integer.toString(required));
        }
        info.append(" x ");
        final Font font = Minecraft.getInstance().font;
        final int width = font.width(info);
        font.draw(transform, info, -width, (16 - font.lineHeight) / 2f, -1);
        if (available == null || available.isEmpty()) {
            available = defaultItem.get().getDefaultInstance();
        }
        ManualUtils.renderItemStack(transform, available, 0, 0, false);
    }

    private List<SymbolPin> getHoveredPins(PlacedSymbol hovered, Vec2d schematicMouse) {
        List<SymbolPin> result = new ArrayList<>();
        for (SymbolPin pin : hovered.getSymbol().getPins()) {
            if (new ConnectedPin(hovered, pin).getShape().containsClosed(schematicMouse)) {
                result.add(pin);
            }
        }
        return result;
    }

    private void drawErrors(PoseStack transform) {
        for (ConnectedPin pin : errors) {
            final Vec2i pos = pin.getPosition();
            fill(transform, pos.x - 1, pos.y - 1, pos.x + 2, pos.y + 2, 0xffff0000);
        }
    }

    private void drawBoundary(PoseStack transform) {
        final int color = 0xff_ff_dd_dd;
        final float offset = 2 / currentScale;
        GuiUtil.fill(
                transform,
                Schematic.GLOBAL_MIN - offset, Schematic.GLOBAL_MIN - offset,
                Schematic.GLOBAL_MAX + offset, Schematic.GLOBAL_MIN,
                color
        );
        GuiUtil.fill(
                transform,
                Schematic.GLOBAL_MIN - offset, Schematic.GLOBAL_MIN - offset,
                Schematic.GLOBAL_MIN, Schematic.GLOBAL_MAX + offset,
                color
        );
        GuiUtil.fill(
                transform,
                Schematic.GLOBAL_MAX, Schematic.GLOBAL_MIN - offset,
                Schematic.GLOBAL_MAX + offset, Schematic.GLOBAL_MAX + offset,
                color
        );
        GuiUtil.fill(
                transform,
                Schematic.GLOBAL_MIN - offset, Schematic.GLOBAL_MAX,
                Schematic.GLOBAL_MAX + offset, Schematic.GLOBAL_MAX + offset,
                color
        );
    }

    private double mouseXDown;
    private double mouseYDown;
    // Start at true: We don't want to consider a release if there wasn't a click before it
    private boolean clickWasConsumed = true;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        mouseXDown = mouseX;
        mouseYDown = mouseY;
        clickWasConsumed = false;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button) || container.readOnly) {
            return true;
        }
        if (clickWasConsumed) {
            return false;
        }
        clickWasConsumed = true;
        final Vec2d mousePos = getMousePosition(mouseX, mouseY);
        PlacedSymbol placed = getPlacingSymbol(mousePos);
        if (placed != null) {
            if (schematic.getChecker().canAdd(placed)) {
                schematic.addSymbol(placed);
                sendToServer(new AddSymbol(placed));
                if (resetAfterPlacingSymbol) {
                    placingSymbol = null;
                }
            }
        } else {
            WireSegment placedWire = getPlacingSegment(mousePos);
            if (placedWire != null) {
                if (schematic.getChecker().canAdd(placedWire)) {
                    schematic.addWire(placedWire);
                    sendToServer(new AddWire(placedWire));
                    if (placedWire.getEnd().equals(currentWireStart)) {
                        currentWireStart = placedWire.getStart();
                    } else {
                        currentWireStart = placedWire.getEnd();
                    }
                }
            } else {
                final PlacedSymbol hovered = schematic.getSymbolAt(mousePos);
                if (hovered != null &&
                        getHoveredPins(hovered, mousePos).isEmpty() &&
                        schematic.removeOneContaining(mousePos)) {
                    sendToServer(new Delete(mousePos));
                    placingSymbol = hovered.getSymbol();
                    resetAfterPlacingSymbol = true;
                } else {
                    currentWireStart = mousePos.floor();
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (!clickWasConsumed && !(Math.abs(mouseX - mouseXDown) > 1) && !(Math.abs(mouseY - mouseYDown) > 1)) {
            return false;
        }
        centerX -= dragX / currentScale;
        centerY -= dragY / currentScale;
        clampView();
        clickWasConsumed = true;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!super.mouseScrolled(mouseX, mouseY, delta)) {
            final float zoomScale = 1.1f;
            if (delta > 0) {
                currentScale *= zoomScale;
            } else {
                currentScale /= zoomScale;
            }
            currentScale = Mth.clamp(currentScale, minScale, 10);
            clampView();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!container.readOnly) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (currentWireStart != null || placingSymbol != null) {
                    currentWireStart = null;
                    placingSymbol = null;
                    return true;
                }
            } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
                final Vec2d mousePos = getMousePosition(GuiUtil.getMousePosition());
                if (schematic.removeOneContaining(mousePos)) {
                    sendToServer(new Delete(mousePos));
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderCustomBackground(
            @Nonnull PoseStack matrixStack,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        super.renderCustomBackground(matrixStack, mouseX, mouseY, partialTicks);
        fill(
                matrixStack,
                TRANSLUCENT_BORDER_SIZE,
                TRANSLUCENT_BORDER_SIZE,
                this.width - TRANSLUCENT_BORDER_SIZE,
                this.height - TRANSLUCENT_BORDER_SIZE,
                -1
        );
        fill(
                matrixStack,
                TOTAL_BORDER,
                TOTAL_BORDER,
                this.width - TOTAL_BORDER,
                this.height - TOTAL_BORDER,
                0xff3362ac
        );
    }

    private Vec2d getMousePosition(Vec2d screenPos) {
        return getMousePosition(screenPos.x, screenPos.y);
    }

    private Vec2d getMousePosition(double mouseX, double mouseY) {
        return new Vec2d(
                (mouseX - width / 2.) / currentScale + centerX,
                (mouseY - height / 2.) / currentScale + centerY
        );
    }

    @Nullable
    private PlacedSymbol getPlacingSymbol(Vec2d pos) {
        if (placingSymbol != null) {
            return new PlacedSymbol(pos.floor(), placingSymbol);
        } else {
            return null;
        }
    }

    @Nullable
    private WireSegment getPlacingSegment(Vec2d pos) {
        if (currentWireStart != null) {
            final double sizeX = Math.abs(pos.x - currentWireStart.x - .5);
            final double sizeY = Math.abs(pos.y - currentWireStart.y - .5);
            if (sizeX > sizeY) {
                return new WireSegment(
                        new Vec2i(getWireStart(currentWireStart.x, pos.x), currentWireStart.y),
                        getWireLength(currentWireStart.x, pos.x),
                        WireSegment.WireAxis.X
                );
            } else {
                return new WireSegment(
                        new Vec2i(currentWireStart.x, getWireStart(currentWireStart.y, pos.y)),
                        getWireLength(currentWireStart.y, pos.y),
                        WireSegment.WireAxis.Y
                );
            }
        }
        return null;
    }

    private int getWireStart(int fixed, double mouse) {
        return mouse < fixed ? floor(mouse) : fixed;
    }

    private int getWireLength(int fixed, double mouse) {
        return mouse < fixed ? ceil(fixed - mouse) : floor(mouse - fixed);
    }

    public void setSchematic(Schematic schematic) {
        this.schematic = schematic;
    }

    public Schematic getSchematic() {
        return schematic;
    }

    private void sendToServer(LogicSubPacket data) {
        ControlEngineering.NETWORK.sendToServer(new LogicPacket(data));
        updateErrors();
    }

    @Nonnull
    @Override
    public LogicDesignContainer getMenu() {
        return container;
    }

    public void updateErrors() {
        if (errorsShown) {
            errors = SchematicCircuitConverter.getFloatingInputs(schematic);
        } else {
            errors = ImmutableList.of();
        }
    }

    private void clampView() {
        final double halfScreenWidth = getShownSizeForScale(width, currentScale) / 2;
        final double halfScreenHeight = getShownSizeForScale(height, currentScale) / 2;
        centerX = Mth.clamp(
                centerX, Schematic.GLOBAL_MIN + halfScreenWidth, Schematic.GLOBAL_MAX - halfScreenWidth
        );
        centerY = Mth.clamp(
                centerY, Schematic.GLOBAL_MIN + halfScreenHeight, Schematic.GLOBAL_MAX - halfScreenHeight
        );
    }

    private static float getShownSizeForScale(float dimensionSize, float scale) {
        return (dimensionSize - 2 * TOTAL_BORDER - 5) / scale;
    }

    private static float getScaleForShownSize(float size, float shownSize) {
        return (size - 2 * TOTAL_BORDER - 5) / shownSize;
    }
}
