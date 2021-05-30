package com.fuzs.diagonalfences.mixin;

import com.fuzs.diagonalfences.block.IGenerationStateBlock;
import com.fuzs.diagonalfences.mixin.accessor.IBlockStateAccessor;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.Property;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings({"unused", "deprecation"})
@Mixin(BlockState.class)
public abstract class BlockStateMixin extends AbstractBlock.AbstractBlockState {

    @Shadow
    @Final
    private static Codec<BlockState> CODEC;

    protected BlockStateMixin(Block block, ImmutableMap<Property<?>, Comparable<?>> propertyValueMap, MapCodec<BlockState> stateCodec) {

        super(block, propertyValueMap, stateCodec);
    }

    static {

        System.out.println(CODEC.hashCode());
        IBlockStateAccessor.setCodec(func_235897_a_(Registry.BLOCK, block -> ((IGenerationStateBlock) block).getGenerationState()).stable());
        System.out.println(CODEC.hashCode());
    }

}
