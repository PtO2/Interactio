package ky.someone.mods.interactio.recipe;

import com.google.gson.JsonObject;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.BlockOrItemOutput;
import ky.someone.mods.interactio.recipe.util.ExplosionInfo;
import ky.someone.mods.interactio.recipe.util.InWorldRecipe;
import ky.someone.mods.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;

public final class BlockExplosionRecipe implements InWorldRecipe<BlockPos, BlockState, ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final BlockOrItemOutput output;
    private final BlockIngredient input;

    public BlockExplosionRecipe(ResourceLocation id, BlockOrItemOutput output, BlockIngredient input) {
        this.id = id;
        this.output = output;
        this.input = input;
    }

    @Override
    public boolean canCraft(BlockPos pos, BlockState state) {
        return input.test(state.getBlock());
    }

    @Override
    public void craft(BlockPos pos, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        Level world = info.getWorld();
        Random rand = world.random;

        // destroying the block saves me from spawning particles myself AND it doesn't produce drops, woot!!
        world.destroyBlock(pos, false);

        // set it to the default state of our resulting block
        if (output.isBlock()) {
            Block block = output.getBlock();
            if (block != null) world.setBlockAndUpdate(pos, block.defaultBlockState());
        } else if (output.isItem()) {
            Collection<ItemStack> stacks = output.getItems();
            if (stacks != null) {
                double x = pos.getX() + Mth.nextDouble(rand, 0.25, 0.75);
                double y = pos.getY() + Mth.nextDouble(rand, 0.5, 1);
                double z = pos.getZ() + Mth.nextDouble(rand, 0.25, 0.75);

                double vel = Mth.nextDouble(rand, 0.1, 0.25);

                stacks.forEach(stack -> {
                    ItemEntity newItem = new ItemEntity(world, x, y, z, stack.copy());
                    newItem.setDeltaMovement(0, vel, 0);
                    newItem.setPickUpDelay(20);
                    world.addFreshEntity(newItem);
                });
            }
        }

        // don't let the explosion blow up the block we JUST placed
        explosion.getToBlow().remove(pos);

    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.BLOCK_EXPLODE;
    }

    public BlockOrItemOutput getOutput() {
        return this.output;
    }

    public BlockIngredient getInput() {
        return this.input;
    }

    private static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<BlockExplosionRecipe> {
        @Override
        public BlockExplosionRecipe fromJson(ResourceLocation id, JsonObject json) {
            BlockOrItemOutput output = BlockOrItemOutput.create(GsonHelper.getAsJsonObject(json, "output"));
            BlockIngredient input = BlockIngredient.deserialize(GsonHelper.getAsJsonObject(json, "input"));

            return new BlockExplosionRecipe(id, output, input);
        }

        @Nullable
        @Override
        public BlockExplosionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            BlockOrItemOutput output = BlockOrItemOutput.read(buffer);
            BlockIngredient input = BlockIngredient.read(buffer);

            return new BlockExplosionRecipe(id, output, input);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BlockExplosionRecipe recipe) {
            recipe.output.write(buffer);
            recipe.input.write(buffer);
        }
    }

}