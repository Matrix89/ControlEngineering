package malte0811.controlengineering.controlpanels.components;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import malte0811.controlengineering.ControlEngineering;
import malte0811.controlengineering.bus.BusLine;
import malte0811.controlengineering.bus.BusState;
import malte0811.controlengineering.controlpanels.PanelComponentType;
import malte0811.controlengineering.controlpanels.components.config.ColorAndSignal;
import net.minecraft.world.InteractionResult;

public class TimedButton extends PanelComponentType<ColorAndSignal, Integer> {
    public static final String TRANSLATION_KEY = ControlEngineering.MODID + ".component.timed_button";
    //TODO config?
    private static final int DELAY = 20;

    public TimedButton() {
        super(ColorAndSignal.DEFAULT, 0, ColorAndSignal.CODEC, Codec.INT, Button.SIZE, Button.HEIGHT, TRANSLATION_KEY);
    }

    @Override
    public BusState getEmittedState(ColorAndSignal config, Integer remainingOn) {
        if (isActive(remainingOn)) {
            return config.signal().singleSignalState(BusLine.MAX_VALID_VALUE);
        } else {
            return BusState.EMPTY;
        }
    }

    @Override
    public Integer tick(ColorAndSignal config, Integer remainingOn) {
        return remainingOn > 0 ? remainingOn - 1 : 0;
    }

    @Override
    public Pair<InteractionResult, Integer> click(ColorAndSignal config, Integer remainingOn, boolean sneaking) {
        if (isActive(remainingOn)) {
            return Pair.of(InteractionResult.PASS, remainingOn);
        } else {
            return Pair.of(InteractionResult.SUCCESS, DELAY);
        }
    }

    @Override
    public boolean canClientDistinguish(Integer stateA, Integer stateB) {
        return isActive(stateA) != isActive(stateB);
    }

    public static boolean isActive(Integer state) {
        return state > 0;
    }
}
