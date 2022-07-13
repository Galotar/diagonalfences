package fuzs.diagonalfences.client.core;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;

public interface ClientAbstractions {

    BakedModel createWrappedBakedModel(BakedModel baseModel, Map<Direction, List<BakedQuad>> quadMap);
}
