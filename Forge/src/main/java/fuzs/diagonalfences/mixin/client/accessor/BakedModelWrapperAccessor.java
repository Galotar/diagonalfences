package fuzs.diagonalfences.mixin.client.accessor;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BakedModelWrapper.class)
public interface BakedModelWrapperAccessor<T extends BakedModel> {

    @Accessor(remap = false)
    T getOriginalModel();

    @Accessor(remap = false)
    @Mutable
    void setOriginalModel(T originalModel);
}
