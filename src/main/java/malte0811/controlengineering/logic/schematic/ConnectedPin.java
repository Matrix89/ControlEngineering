package malte0811.controlengineering.logic.schematic;

import malte0811.controlengineering.logic.cells.SignalType;
import malte0811.controlengineering.logic.schematic.symbol.PlacedSymbol;
import malte0811.controlengineering.logic.schematic.symbol.SymbolPin;
import malte0811.controlengineering.util.math.RectangleI;
import malte0811.controlengineering.util.math.Vec2i;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;

public class ConnectedPin {
    private final PlacedSymbol symbol;
    private final SymbolPin pin;

    public ConnectedPin(PlacedSymbol symbol, SymbolPin pin) {
        this.symbol = symbol;
        this.pin = pin;
    }

    public boolean isAnalog() {
        return pin.getType() == SignalType.ANALOG;
    }

    public SymbolPin getPin() {
        return pin;
    }

    public PlacedSymbol getSymbol() {
        return symbol;
    }

    public Vec2i getPosition() {
        return symbol.getPosition().add(pin.getPosition());
    }

    public void render(PoseStack stack, int wireColor) {
        pin.render(stack, symbol.getPosition().x, symbol.getPosition().y, wireColor);
    }

    public RectangleI getShape() {
        final Vec2i basePos = getPosition();
        return new RectangleI(
                basePos.x + (pin.isOutput() ? -1 : 0), basePos.y,
                basePos.x + (pin.isOutput() ? 1 : 2), basePos.y + 1
        );
    }

    @Override
    public String toString() {
        return "Pin_" + getPosition().x + "_" + getPosition().y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectedPin that = (ConnectedPin) o;
        return symbol.equals(that.symbol) && pin.equals(that.pin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, pin);
    }
}
