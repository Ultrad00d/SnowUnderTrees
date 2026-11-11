package bl4ckscor3.mod.snowundertrees;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = SnowUnderTrees.MODID)
public class LevelTickHandler {
	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Pre event) {
		if (!event.getLevel().isClientSide) {
			ServerLevel level = (ServerLevel) event.getLevel();

			if (level.isRaining() && Configuration.CONFIG.enableWhenSnowing.get()) {
				if (SnowUnderTrees.isSereneSeasonsLoaded() && !SereneSeasonsHandler.generateSnowAndIce())
					return;

				int randomTickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);

				SnowUnderTrees.runForChunks(level, chunk -> addSnowUnderTrees(level, chunk, randomTickSpeed));
			}
		}
	}

	private static void addSnowUnderTrees(ServerLevel level, LevelChunk chunk, int randomTickSpeed) {
		ChunkPos chunkPos = chunk.getPos();
		int chunkX = chunkPos.getMinBlockX();
		int chunkZ = chunkPos.getMinBlockZ();

		for (int i = 0; i < randomTickSpeed; i++) {
			if (SnowUnderTrees.RANDOM.nextInt(48) == 0) {
				BlockPos randomPos = level.getBlockRandomPos(chunkX, 0, chunkZ, 15);

				if (level.getBlockState(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, randomPos).below()).is(BlockTags.LEAVES)) {
					BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, randomPos);

					if (SnowUnderTrees.isDynamicTreesLoaded())
						pos = DynamicTreesHandler.findGround(level, pos.mutable());

					if (pos == null)
						continue;

					if (!SnowUnderTrees.isBiomeDisabled(level.getBiome(pos)) && SnowUnderTrees.placeSnow(level, pos)) {
						BlockPos posBelow = pos.below();
						BlockState stateBelow = level.getBlockState(posBelow);

						if (stateBelow.hasProperty(SnowyDirtBlock.SNOWY))
							level.setBlock(posBelow, stateBelow.setValue(SnowyDirtBlock.SNOWY, true), 2);
					}
				} else {
					tryPropagate(level, randomPos.getX(), randomPos.getZ());
				}
			}
		}
	}

	private static void tryPropagate(ServerLevel level, int x, int z) {
		BlockPos groundPos = SnowUnderTrees.findGroundUnderTree(level, x, z);
		if (groundPos == null) return;
		if (SnowUnderTrees.isSnow(level, groundPos)) return;
		if (SnowUnderTrees.isBiomeDisabled(level.getBiome(groundPos))) return;
		if (!hasAnchorSnow(level, groundPos)) return;

		SnowUnderTrees.placeSnowAndUpdateGround(level, groundPos);
	}

	private static boolean hasAnchorSnow(ServerLevel level, BlockPos pos) {
		for (Direction dir : Direction.Plane.HORIZONTAL)
			if (isAnchorSnow(level, pos.relative(dir))) return true;
		return false;
	}

	private static boolean isAnchorSnow(ServerLevel level, BlockPos pos) {
		if (!SnowUnderTrees.isSnow(level, pos)) return false;
		BlockPos topPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
		if (level.getBlockState(topPos.below()).is(BlockTags.LEAVES)) return true;
		return level.canSeeSky(pos);
	}

	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Post event) {
		if (!event.getLevel().isClientSide && SnowUnderTrees.isSereneSeasonsLoaded())
			SereneSeasonsHandler.tryMeltSnowUnderTrees((ServerLevel) event.getLevel());
	}
}
