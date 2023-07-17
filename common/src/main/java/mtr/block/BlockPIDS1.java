package mtr.block;

import mtr.BlockEntityTypes;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockPIDS1 extends BlockPIDSBaseHorizontal {

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
		VoxelShape shape1 = IBlock.getVoxelShapeByDirection(6, 0, 0, 10, 11, 16, IBlock.getStatePropertySafe(state, FACING));
		VoxelShape shape2 = IBlock.getVoxelShapeByDirection(7.5, 11, 12.5, 8.5, 16, 13.5, IBlock.getStatePropertySafe(state, FACING));
		return Shapes.or(shape1, shape2);
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityBlockPIDS1(pos, state);
	}

	public static class TileEntityBlockPIDS1 extends TileEntityBlockPIDSBaseHorizontal {

		public static final int MAX_ARRIVALS = 1;
		public static final int LINES_PER_ARRIVAL = 1;

		public TileEntityBlockPIDS1(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.PIDS_1_TILE_ENTITY.get(), pos, state);
		}

		@Override
		public int getMaxArrivals() {
			return MAX_ARRIVALS;
		}
	}
}