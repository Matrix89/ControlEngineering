package malte0811.controlengineering.tiles.tape;

import it.unimi.dsi.fastutil.bytes.ByteList;
import malte0811.controlengineering.blocks.CEBlocks;
import malte0811.controlengineering.blocks.shapes.ListShapes;
import malte0811.controlengineering.blocks.shapes.SelectionShapeOwner;
import malte0811.controlengineering.blocks.shapes.SelectionShapes;
import malte0811.controlengineering.blocks.shapes.SingleShape;
import malte0811.controlengineering.blocks.tape.TeletypeBlock;
import malte0811.controlengineering.items.EmptyTapeItem;
import malte0811.controlengineering.items.PunchedTapeItem;
import malte0811.controlengineering.tiles.base.CETileEntity;
import malte0811.controlengineering.tiles.base.IExtraDropTile;
import malte0811.controlengineering.util.BitUtils;
import malte0811.controlengineering.util.CachedValue;
import malte0811.controlengineering.util.ItemUtil;
import malte0811.controlengineering.util.math.Matrix4;
import malte0811.controlengineering.util.serialization.Codecs;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static malte0811.controlengineering.util.ShapeUtils.createPixelRelative;

public class TeletypeTile extends CETileEntity implements SelectionShapeOwner, IExtraDropTile {
    public static final VoxelShape INPUT_SHAPE = createPixelRelative(11, 6, 2, 15, 9, 4);
    public static final VoxelShape OUTPUT_SHAPE = createPixelRelative(2, 6, 1, 6, 10, 5);

    private TeletypeState state = new TeletypeState();

    public TeletypeTile(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        this.state = Codecs.readOptional(TeletypeState.CODEC, nbt.get("state")).orElseGet(TeletypeState::new);
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT compound) {
        compound = super.write(compound);
        compound.put("state", Codecs.encode(TeletypeState.CODEC, state));
        return compound;
    }

    private final CachedValue<Direction, SelectionShapes> selectionShapes = new CachedValue<>(
            () -> getBlockState().get(TeletypeBlock.FACING), f -> createSelectionShapes(f, this)
    );

    @Override
    public SelectionShapes getShape() {
        return selectionShapes.get();
    }

    private ActionResultType removeWrittenClick(PlayerEntity player) {
        ByteList written = state.getData();
        if (!written.isEmpty() && player != null) {
            ItemUtil.giveOrDrop(player, PunchedTapeItem.withBytes(written.toByteArray()));
            written.clear();
            return ActionResultType.SUCCESS;
        } else {
            return ActionResultType.FAIL;
        }
    }

    private ActionResultType removeOrAddClearTape(PlayerEntity player, ItemStack item) {
        final int length = EmptyTapeItem.getLength(item);
        if (length > 0) {
            //TODO limit?
            state.addAvailable(length);
            item.shrink(1);
        } else if (state.getAvailable() > 0 && player != null) {
            ItemUtil.giveOrDrop(player, EmptyTapeItem.withLength(state.getAvailable()));
            state.setAvailable(0);
        }
        return ActionResultType.SUCCESS;
    }

    private static SelectionShapes createSelectionShapes(Direction d, TeletypeTile tile) {
        List<SelectionShapes> subshapes = new ArrayList<>(2);
        // Punched tape output
        subshapes.add(new SingleShape(
                OUTPUT_SHAPE, ctx -> tile.removeWrittenClick(ctx.getPlayer())
        ));
        // Add clear tape to input/take it from input
        subshapes.add(new SingleShape(
                INPUT_SHAPE,
                ctx -> tile.removeOrAddClearTape(ctx.getPlayer(), ctx.getItem())
        ));
        return new ListShapes(
                TeletypeBlock.SHAPE_PROVIDER.apply(d),
                Matrix4.inverseFacing(d),
                subshapes,
                ctx -> {
                    CEBlocks.TELETYPE.get().openContainer(
                            ctx.getPlayer(), tile.getBlockState(), ctx.getWorld(), ctx.getPos()
                    );
                    return ActionResultType.SUCCESS;
                }
        );
    }

    public TeletypeState getState() {
        return state;
    }

    @Override
    public void getExtraDrops(Consumer<ItemStack> dropper) {
        if (state.getAvailable() > 0) {
            dropper.accept(EmptyTapeItem.withLength(state.getAvailable()));
        }
        if (!state.getData().isEmpty() || state.getErased() > 0) {
            byte[] bytes = new byte[state.getData().size() + state.getErased()];
            System.arraycopy(state.getData().toByteArray(), 0, bytes, 0, state.getData().size());
            Arrays.fill(bytes, state.getData().size(), bytes.length, BitUtils.fixParity((byte) 0xff));
            dropper.accept(PunchedTapeItem.withBytes(bytes));
        }
    }
}
