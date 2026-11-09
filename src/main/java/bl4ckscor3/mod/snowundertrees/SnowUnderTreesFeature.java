package bl4ckscor3.mod.snowundertrees;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.HashSet;
import java.util.Set;

public class SnowUnderTreesFeature extends Feature<NoneFeatureConfiguration> {
	public SnowUnderTreesFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
		BlockPos origin = ctx.origin();
		WorldGenLevel level = ctx.level();
		BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
		Set<BlockPos> firstPassSnow = new HashSet<>();
		boolean[][] underCanopy = new boolean[16][16];

		// Pass 1: place snow under leaves, remember which columns had canopy on top
		for (int xi = 0; xi < 16; xi++) {
			for (int zi = 0; zi < 16; zi++) {
				int x = origin.getX() + xi, z = origin.getZ() + zi;
				mPos.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);

				if (!level.getBlockState(mPos).is(BlockTags.LEAVES)) continue;
				underCanopy[xi][zi] = true;

				mPos.setY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));

				BlockPos snowPos;
				if (SnowUnderTrees.isDynamicTreesLoaded()) {
					BlockPos dtGround = DynamicTreesHandler.findGround(level, mPos);
					if (dtGround == null) continue;
					snowPos = dtGround.immutable();
				} else {
					BlockPos logGround = SnowUnderTrees.findGroundUnderTree(level, x, z);
					snowPos = logGround != null ? logGround : mPos.immutable();
				}

				if (SnowUnderTrees.placeSnowAndUpdateGround(level, snowPos))
					firstPassSnow.add(snowPos);
			}
		}

		// Pass 2: propagate one step to columns under bare log branches
		for (int xi = 0; xi < 16; xi++) {
			for (int zi = 0; zi < 16; zi++) {
				if (underCanopy[xi][zi]) continue;

				int x = origin.getX() + xi, z = origin.getZ() + zi;
				BlockPos groundPos = SnowUnderTrees.findGroundUnderTree(level, x, z);
				if (groundPos == null || SnowUnderTrees.isSnow(level, groundPos)) continue;

				for (Direction dir : Direction.Plane.HORIZONTAL) {
					if (firstPassSnow.contains(groundPos.relative(dir))) {
						SnowUnderTrees.placeSnowAndUpdateGround(level, groundPos);
						break;
					}
				}
			}
		}

		return true;
	}
}
