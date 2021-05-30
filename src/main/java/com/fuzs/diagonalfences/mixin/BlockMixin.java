package com.fuzs.diagonalfences.mixin;

import com.fuzs.diagonalfences.block.IGenerationStateBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("unused")
@Mixin(Block.class)
public abstract class BlockMixin extends AbstractBlock implements IGenerationStateBlock {

    public BlockMixin(Properties properties) {

        super(properties);
    }

    @Shadow
    public final BlockState getDefaultState() {

        throw new IllegalStateException();
    }

    @Override
    public BlockState getGenerationState() {

        return this.getDefaultState();
    }

}
