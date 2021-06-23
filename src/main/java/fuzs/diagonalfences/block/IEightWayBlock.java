package fuzs.diagonalfences.block;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import fuzs.diagonalfences.api.IDiagonalBlock;
import fuzs.diagonalfences.mixin.accessor.IStateContainerAccessor;
import fuzs.diagonalfences.state.ExposedStateContainerBuilder;
import fuzs.diagonalfences.util.EightWayDirection;
import fuzs.diagonalfences.util.math.shapes.NoneVoxelShape;
import fuzs.diagonalfences.util.math.shapes.VoxelCollection;
import fuzs.diagonalfences.util.math.shapes.VoxelUtils;
import fuzs.puzzleslib.util.PuzzlesUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FourWayBlock;
import net.minecraft.block.SixWayBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public interface IEightWayBlock extends IDiagonalBlock {

    Map<List<Float>, VoxelShape[]> DIMENSIONS_TO_SHAPE_MAP = Maps.newHashMap();
    Map<EightWayDirection, BooleanProperty> DIRECTION_TO_PROPERTY_MAP = PuzzlesUtil.make(Maps.newEnumMap(EightWayDirection.class), (directions) -> {

        directions.put(EightWayDirection.NORTH, SixWayBlock.NORTH);
        directions.put(EightWayDirection.EAST, SixWayBlock.EAST);
        directions.put(EightWayDirection.SOUTH, SixWayBlock.SOUTH);
        directions.put(EightWayDirection.WEST, SixWayBlock.WEST);
        directions.put(EightWayDirection.NORTH_EAST, IDiagonalBlock.NORTH_EAST);
        directions.put(EightWayDirection.SOUTH_EAST, IDiagonalBlock.SOUTH_EAST);
        directions.put(EightWayDirection.SOUTH_WEST, IDiagonalBlock.SOUTH_WEST);
        directions.put(EightWayDirection.NORTH_WEST, IDiagonalBlock.NORTH_WEST);
    });

    boolean canConnect(IBlockReader iblockreader, BlockPos position, BlockState state, Direction direction);

    default BlockState getDefaultStates(BlockState defaultState) {

        return defaultState.with(IDiagonalBlock.NORTH_EAST, Boolean.FALSE).with(IDiagonalBlock.SOUTH_EAST, Boolean.FALSE).with(IDiagonalBlock.SOUTH_WEST, Boolean.FALSE).with(IDiagonalBlock.NORTH_WEST, Boolean.FALSE);
    }

    default MapCodec<BlockState> makeLenientMapCodec(Supplier<BlockState> defaultState, ExposedStateContainerBuilder<Block, BlockState> builder, ExposedStateContainerBuilder<Block, BlockState> additionalBuilder) {

        MapCodec<BlockState> mapcodec = MapCodec.of(Encoder.empty(), Decoder.unit(defaultState));
        for (Map.Entry<String, Property<?>> entry : ImmutableSortedMap.copyOf(builder.properties).entrySet()) {

            // ignore states added by us, world gen structures will otherwise fail to generate when our states are missing
            if (!additionalBuilder.properties.containsKey(entry.getKey())) {

                mapcodec = IStateContainerAccessor.callSetPropertyCodec(mapcodec, defaultState, entry.getKey(), entry.getValue());
            }
        }

        return mapcodec;
    }

    default void fillStateContainer2(StateContainer.Builder<Block, BlockState> builder) {

        builder.add(IDiagonalBlock.NORTH_EAST, IDiagonalBlock.SOUTH_EAST, IDiagonalBlock.SOUTH_WEST, IDiagonalBlock.NORTH_WEST);
    }

    default int makeIndex(BlockState stateIn) {

        int index = 0;
        for (Map.Entry<EightWayDirection, BooleanProperty> entry : DIRECTION_TO_PROPERTY_MAP.entrySet()) {

            if (stateIn.get(entry.getValue())) {

                index |= entry.getKey().getHorizontalIndex();
            }
        }

        return index;
    }

    default BlockState makeStateForPlacement(BlockState placementState, IBlockReader iblockreader, BlockPos basePos, FluidState fluidState) {

        placementState.with(FourWayBlock.WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        placementState = this.withDirections(EightWayDirection.getAllCardinals(), basePos, placementState, (mutablePos, newPlacementState, direction) ->
                this.canConnect(iblockreader, mutablePos, iblockreader.getBlockState(mutablePos), direction.convertTo().getOpposite()));
        placementState = this.withDirections(EightWayDirection.getAllIntercardinals(), basePos, placementState, (mutablePos, newPlacementState, direction) ->
                this.canConnectDiagonally(iblockreader.getBlockState(mutablePos)) && Stream.of(direction.getCardinalNeighbors()).map(DIRECTION_TO_PROPERTY_MAP::get).noneMatch(newPlacementState::get));

        return placementState;
    }

    default BlockState withDirections(EightWayDirection[] directions, BlockPos basePos, BlockState placementState, DirectionStatePredicate predicate) {

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        for (EightWayDirection direction : directions) {

            Vector3i directionVec = direction.directionVec;
            mutablePos.setAndOffset(basePos, directionVec.getX(), directionVec.getY(), directionVec.getZ());
            placementState = placementState.with(DIRECTION_TO_PROPERTY_MAP.get(direction), predicate.test(mutablePos, placementState, direction));
        }

        return placementState;
    }

    default BlockState updatePostPlacement2(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos, BlockState newState) {

        if (facing.getAxis().getPlane() == Direction.Plane.HORIZONTAL) {

            BlockPos.Mutable diagonalPos = new BlockPos.Mutable();
            for (EightWayDirection direction : EightWayDirection.convertTo(facing).getIntercardinalNeighbors()) {

                Vector3i directionVec = direction.directionVec;
                diagonalPos.setAndOffset(currentPos, directionVec.getX(), directionVec.getY(), directionVec.getZ());
                BlockState diagonalState = worldIn.getBlockState(diagonalPos);
                // checks if there are vertical connections where a diagonal connection should be formed
                boolean isBlocked = false;
                for (EightWayDirection cardinal : direction.getCardinalNeighbors()) {

                    isBlocked = isBlocked || newState.get(DIRECTION_TO_PROPERTY_MAP.get(cardinal));
                }

                newState = newState.with(DIRECTION_TO_PROPERTY_MAP.get(direction), !isBlocked && this.canConnectDiagonally(diagonalState));
            }

            return newState;
        }
        
        return null;
    }

    /**
     * similar to {@link net.minecraft.block.AbstractBlock.AbstractBlockState#updateNeighbours}
     */
    default void updateDiagonalNeighbors2(BlockState state, IWorld world, BlockPos pos, int flags, int recursionLeft) {

        BlockPos.Mutable diagonalPos = new BlockPos.Mutable();
        for (EightWayDirection direction : EightWayDirection.getAllIntercardinals()) {

            Vector3i directionVec = direction.directionVec;
            diagonalPos.setAndOffset(pos, directionVec.getX(), directionVec.getY(), directionVec.getZ());
            BlockState diagonalState = world.getBlockState(diagonalPos);
            if (diagonalState.getBlock() instanceof IEightWayBlock && ((IEightWayBlock) diagonalState.getBlock()).canConnectDiagonally()) {

                // checks if there are vertical connections where a diagonal connection should be formed
                boolean isBlocked = false;
                for (EightWayDirection cardinal : direction.getOpposite().getCardinalNeighbors()) {

                    isBlocked = isBlocked || diagonalState.get(DIRECTION_TO_PROPERTY_MAP.get(cardinal));
                }

                BlockState newState = diagonalState.with(DIRECTION_TO_PROPERTY_MAP.get(direction.getOpposite()), !isBlocked && ((IEightWayBlock) diagonalState.getBlock()).canConnectDiagonally(world.getBlockState(pos)));
                Block.replaceBlockState(diagonalState, newState, world, diagonalPos, flags, recursionLeft);
            }
        }
    }

    default VoxelShape[] getShapes(float nodeWidth, float extensionWidth, float nodeHeight, float extensionBottom, float extensionHeight) {

        ArrayList<Float> dimensions = Lists.newArrayList(nodeWidth, extensionWidth, nodeHeight, extensionBottom, extensionHeight);
        return DIMENSIONS_TO_SHAPE_MAP.computeIfAbsent(dimensions, dimension -> this.makeDiagonalShapes(nodeWidth, extensionWidth, nodeHeight, extensionBottom, extensionHeight));
    }

    default VoxelCollection[] makeDiagonalShapes(float nodeWidth, float extensionWidth, float nodeHeight, float extensionBottom, float extensionHeight) {

        float nodeStart = 8.0F - nodeWidth;
        float nodeEnd = 8.0F + nodeWidth;
        float extensionStart = 8.0F - extensionWidth;
        float extensionEnd = 8.0F + extensionWidth;

        VoxelShape nodeShape = Block.makeCuboidShape(nodeStart, 0.0, nodeStart, nodeEnd, nodeHeight, nodeEnd);
        Vector3d[] sideShape = new Vector3d[]{new Vector3d(extensionStart, extensionBottom, 0.0), new Vector3d(extensionEnd, extensionHeight, extensionStart)};
        VoxelShape[] verticalShapes = Stream.of(EightWayDirection.getAllCardinals()).map(direction -> direction.transform(sideShape)).map(VoxelUtils::makeCuboidShape).toArray(VoxelShape[]::new);
        VoxelShape[] diagonalShapes = Stream.of(EightWayDirection.getAllIntercardinals()).map(direction -> this.getDiagonalShape(extensionWidth, extensionBottom, extensionHeight, direction)).toArray(VoxelShape[]::new);
        VoxelShape[] sideShapes = new VoxelShape[]{verticalShapes[2], verticalShapes[3], verticalShapes[0], verticalShapes[1], diagonalShapes[2], diagonalShapes[3], diagonalShapes[0], diagonalShapes[1]};

        return this.constructStateShapes(nodeShape, sideShapes);
    }

    default VoxelCollection[] constructStateShapes(VoxelShape nodeShape, VoxelShape[] directionalShapes) {

        VoxelCollection[] stateShapes = new VoxelCollection[(int) Math.pow(2, directionalShapes.length)];
        for (int i = 0; i < stateShapes.length; i++) {

            stateShapes[i] = new VoxelCollection(nodeShape);
            for (int j = 0; j < directionalShapes.length; j++) {

                if ((i & (1 << j)) != 0) {

                    stateShapes[i].addVoxelShape(directionalShapes[j]);
                }
            }
        }

        return stateShapes;
    }

    default VoxelShape getDiagonalShape(float extensionWidth, float extensionBottom, float extensionHeight, EightWayDirection direction) {

        VoxelShape collisionShape = this.getDiagonalCollisionShape(extensionWidth, extensionBottom, extensionHeight, direction);
        // adept width for diagonal rotation
        extensionWidth = (float) Math.sqrt(extensionWidth * extensionWidth * 2);
        // cos(-pi/4)
        final float diagonalSide = 0.7071067812F * extensionWidth;
        Vector3d[] corners = VoxelUtils.createVectorArray(-diagonalSide, extensionHeight, diagonalSide, -diagonalSide + 8.0F, extensionHeight, diagonalSide + 8.0F, -diagonalSide, extensionBottom, diagonalSide, -diagonalSide + 8.0F, extensionBottom, diagonalSide + 8.0F, diagonalSide, extensionHeight, -diagonalSide, diagonalSide + 8.0F, extensionHeight, -diagonalSide + 8.0F, diagonalSide, extensionBottom, -diagonalSide, diagonalSide + 8.0F, extensionBottom, -diagonalSide + 8.0F);
        Vector3d[] edges = VoxelUtils.create12Edges(corners);
        if (direction.directionVec.getX() != 1) {

            edges = VoxelUtils.flipX(edges);
        }

        if (direction.directionVec.getZ() != 1) {

            edges = VoxelUtils.flipZ(edges);
        }

        return new NoneVoxelShape(collisionShape, VoxelUtils.scaleDown(edges));
    }

    default VoxelShape getDiagonalCollisionShape(float extensionWidth, float extensionBottom, float extensionHeight, EightWayDirection direction) {

        VoxelShape collisionShape = VoxelShapes.empty();
        for (int i = 0; i < 8; i++) {

            Vector3i directionVec = direction.directionVec;
            int posX = directionVec.getX() > 0 ? i : 16 - i;
            int posZ = directionVec.getZ() > 0 ? i : 16 - i;
            VoxelShape cuboidShape = Block.makeCuboidShape(posX - extensionWidth, extensionBottom, posZ - extensionWidth, posX + extensionWidth, extensionHeight, posZ + extensionWidth);
            collisionShape = VoxelShapes.or(collisionShape, cuboidShape);
        }

        return collisionShape;
    }

    @FunctionalInterface
    interface DirectionStatePredicate {

        boolean test(BlockPos pos, BlockState placementState, EightWayDirection direction);

    }

}