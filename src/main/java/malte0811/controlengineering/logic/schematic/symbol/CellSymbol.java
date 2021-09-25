package malte0811.controlengineering.logic.schematic.symbol;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import malte0811.controlengineering.gui.SubTexture;
import malte0811.controlengineering.logic.cells.LeafcellType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

import static malte0811.controlengineering.logic.schematic.symbol.SchematicSymbols.SYMBOLS_SHEET;

public class CellSymbol extends SchematicSymbol<Unit> {
    private final LeafcellType<?> type;
    private final SubTexture texture;
    private final List<SymbolPin> pins;

    public CellSymbol(LeafcellType<?> type, int uMin, int vMin, int uSize, int vSize, List<SymbolPin> pins) {
        super(Unit.INSTANCE, Codec.unit(Unit.INSTANCE));
        this.type = type;
        this.pins = pins;
        this.texture = new SubTexture(SYMBOLS_SHEET, uMin, vMin, uMin + uSize, vMin + vSize, 64);
        for (SymbolPin pin : pins) {
            if (pin.isOutput()) {
                Preconditions.checkState(type.getOutputPins().containsKey(pin.getPinName()));
            } else {
                Preconditions.checkState(type.getInputPins().containsKey(pin.getPinName()));
            }
        }
    }

    @Override
    public void renderCustom(PoseStack transform, int x, int y, @Nullable Unit state) {
        texture.blit(transform, x, y);
    }

    @Override
    public int getXSize() {
        return texture.getWidth();
    }

    @Override
    public int getYSize() {
        return texture.getHeight();
    }

    @Override
    public List<SymbolPin> getPins(@Nullable Unit unit) {
        return pins;
    }

    @Override
    public void createInstanceWithUI(Consumer<? super SymbolInstance<Unit>> onDone) {
        // No config required/possible
        onDone.accept(newInstance());
    }

    public static String getTranslationKey(LeafcellType<?> type) {
        return "cell." + type.getRegistryName().getNamespace() + "." + type.getRegistryName().getPath() + ".name";
    }

    @Override
    public Component getName() {
        return new TranslatableComponent(getTranslationKey(type));
    }

    public LeafcellType<?> getCellType() {
        return type;
    }

    @Override
    public String toString() {
        return "[Cell:" + getCellType() + "]";
    }
}
