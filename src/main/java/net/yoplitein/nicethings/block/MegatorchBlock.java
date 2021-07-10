package net.yoplitein.nicethings.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class MegatorchBlock extends Block
{
    public static final int RADIUS = 75;
    static final VoxelShape SHAPE = Block.createCuboidShape(5, 0, 5, 11, 24, 11);
    
    public MegatorchBlock(Settings settings)
    {
        super(settings);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
    {
        return SHAPE;
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random)
    {
        for(int x = 0; x < 3; x++)
            world.addParticle(
                ParticleTypes.FLAME,
                pos.getX() + 0.5 + 0.05 * random.nextGaussian(),
                pos.getY() + 1.55 + 0.01 * random.nextGaussian(),
                pos.getZ() + 0.5 + 0.05 * random.nextGaussian(),
                0.01 * (random.nextFloat() < 0.5 ? 1.0 : -1.0),
                0.005 + 0.005 * random.nextFloat(),
                0.01 * (random.nextFloat() < 0.5 ? 1.0 : -1.0)
            );
        
        for(int x = 0; x < 6; x++)
            world.addParticle(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5 + 0.1 * random.nextGaussian(),
                pos.getY() + 1.55 + 0.01 * random.nextGaussian(),
                pos.getZ() + 0.5 + 0.1 * random.nextGaussian(),
                0.01 * (random.nextFloat() < 0.5 ? 1.0 : -1.0),
                0.005 + 0.005 * random.nextFloat(),
                0.01 * (random.nextFloat() < 0.5 ? 1.0 : -1.0)
            );
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos)
    {
        if(direction != Direction.DOWN) return state;
        if(neighborState.isSolidBlock(world, pos)) return state;
        return Blocks.AIR.getDefaultState();
    }
}
