package malte0811.controlengineering.logic.cells.impl;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import malte0811.controlengineering.logic.cells.LeafcellType;
import malte0811.controlengineering.logic.cells.Pin;

import java.util.Map;

public abstract class StatelessCell extends LeafcellType<Unit> {
    protected StatelessCell(Map<String, Pin> inputPins, Map<String, Pin> outputPins, int numTubes) {
        super(inputPins, outputPins, Unit.INSTANCE, Codec.unit(Unit.INSTANCE), numTubes);
    }

    @Override
    public final Unit nextState(Object2DoubleMap<String> inputSignals, Unit currentState) {
        return currentState;
    }

    @Override
    public final Object2DoubleMap<String> getOutputSignals(Object2DoubleMap<String> inputSignals, Unit oldState) {
        return getOutputSignals(inputSignals);
    }

    protected abstract Object2DoubleMap<String> getOutputSignals(Object2DoubleMap<String> inputSignals);
}
