package com.fuzs.diagonalfences.mixin.accessor;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockState.class)
public interface IBlockStateAccessor {

    @SuppressWarnings("unused")
    @Accessor
    static void setCodec(Codec<BlockState> codec) {

        throw new IllegalStateException();
    }

}
