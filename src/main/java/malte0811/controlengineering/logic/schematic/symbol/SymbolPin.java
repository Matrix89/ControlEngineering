package malte0811.controlengineering.logic.schematic.symbol;

import com.mojang.blaze3d.matrix.MatrixStack;
import malte0811.controlengineering.logic.cells.PinDirection;
import malte0811.controlengineering.logic.cells.SignalType;
import malte0811.controlengineering.logic.schematic.WireSegment;
import malte0811.controlengineering.util.GuiUtil;
import malte0811.controlengineering.util.math.Vec2i;

import java.util.Objects;

public class SymbolPin {
    private final Vec2i position;
    private final SignalType type;
    private final PinDirection direction;
    private final String pinName;

    public SymbolPin(int x, int y, SignalType type, PinDirection direction, String pinName) {
        this(new Vec2i(x, y), type, direction, pinName);
    }

    public SymbolPin(Vec2i position, SignalType type, PinDirection direction, String pinName) {
        this.position = position;
        this.type = type;
        this.direction = direction;
        this.pinName = pinName;
    }

    public static SymbolPin digitalOut(int x, int y, String name) {
        return new SymbolPin(x, y, SignalType.DIGITAL, PinDirection.OUTPUT, name);
    }

    public static SymbolPin analogOut(int x, int y, String name) {
        return new SymbolPin(x, y, SignalType.ANALOG, PinDirection.OUTPUT, name);
    }

    public static SymbolPin analogIn(int x, int y, String name) {
        return new SymbolPin(x, y, SignalType.ANALOG, PinDirection.INPUT, name);
    }

    public static SymbolPin digitalIn(int x, int y, String name) {
        return new SymbolPin(x, y, SignalType.DIGITAL, PinDirection.INPUT, name);
    }

    public Vec2i getPosition() {
        return position;
    }

    public SignalType getType() {
        return type;
    }

    public String getPinName() {
        return pinName;
    }

    //TODO check usages and replace with isCombOut as necessary
    public boolean isOutput() {
        return direction.isOutput();
    }

    public boolean isCombinatorialOutput() {
        return direction.isCombinatorialOutput();
    }

    public void render(MatrixStack stack, int x, int y, int wireColor) {
        final Vec2i pinPos = getPosition();
        final int wirePixels = 1;
        final float wireXMin = pinPos.x + x + (isOutput() ? -wirePixels : WireSegment.WIRE_SPACE);
        final float wireXMax = pinPos.x + x + (isOutput() ? 1 - WireSegment.WIRE_SPACE : (1 + wirePixels));
        final float yMin = y + pinPos.y + WireSegment.WIRE_SPACE;
        final float yMax = y + pinPos.y + 1 - WireSegment.WIRE_SPACE;
        GuiUtil.fill(stack, wireXMin, yMin, wireXMax, yMax, wireColor);
        final int color = isOutput() ? 0xffff0000 : 0xff00ff00;
        GuiUtil.fill(
                stack,
                x + pinPos.x + WireSegment.WIRE_SPACE, yMin,
                x + pinPos.x + 1 - WireSegment.WIRE_SPACE, yMax,
                color
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolPin symbolPin = (SymbolPin) o;
        return position.equals(symbolPin.position) && type == symbolPin.type && direction == symbolPin.direction && pinName.equals(
                symbolPin.pinName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, type, direction, pinName);
    }
}
