package com.fuzs.diagonalfences.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.blockstateprovider.BlockStateProvider;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SimpleBlockStateProvider.class)
public abstract class SimpleBlockStateProviderMixin extends BlockStateProvider {

    static {

        System.out.println(BlockState.CODEC.hashCode());
    }

}
