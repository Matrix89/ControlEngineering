package malte0811.controlengineering.datagen.recipes;

import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class NoAdvancementShapedBuilder extends ShapedRecipeBuilder {
    @Nullable
    private final CompoundTag nbt;

    public NoAdvancementShapedBuilder(ItemLike pResult, int pCount, @Nullable CompoundTag nbt) {
        super(pResult, pCount);
        this.nbt = nbt;
    }

    public static ShapedRecipeBuilder shaped(ItemStack result) {
        return new NoAdvancementShapedBuilder(result.getItem(), result.getCount(), result.getTag());
    }

    public static ShapedRecipeBuilder shaped(ItemLike result) {
        return shaped(result, 1);
    }

    public static ShapedRecipeBuilder shaped(RegistryObject<? extends ItemLike> regObject) {
        return shaped(regObject.get());
    }

    public static ShapedRecipeBuilder shaped(ItemLike result, int pCount) {
        return new NoAdvancementShapedBuilder(result, pCount, null);
    }

    @Nonnull
    @Override
    public ShapedRecipeBuilder unlockedBy(
            @Nonnull String pCriterionName, @Nonnull CriterionTriggerInstance pCriterionTrigger
    ) {
        throw new UnsupportedOperationException();
    }

    public void save(@Nonnull Consumer<FinishedRecipe> out, @Nonnull ResourceLocation recipeId) {
        super.unlockedBy("dummy", new ImpossibleTrigger.TriggerInstance());
        super.save(
                recipe -> out.accept(new NoAdvancementFinishedRecipe(recipe, nbt)),
                recipeId
        );
    }
}