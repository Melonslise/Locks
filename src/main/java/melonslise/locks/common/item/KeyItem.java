package melonslise.locks.common.item;

import java.util.List;
import java.util.stream.Collectors;

import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.init.LocksSoundEvents;
import melonslise.locks.common.util.Lockable;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class KeyItem extends LockingItem
{
	public KeyItem(Properties props)
	{
		super(props);
	}

	// TODO Sound pitch
	@Override
	public ActionResultType onItemUse(ItemUseContext ctx)
	{
		World world = ctx.getWorld();
		BlockPos pos = ctx.getPos();
		return world.getCapability(LocksCapabilities.LOCKABLES)
			.map(lockables ->
			{
				int id = getOrSetId(ctx.getItem());
				List<Lockable> matching = lockables.get().values().stream().filter(lockable1 -> lockable1.box.intersects(pos) && lockable1.lock.id == id).collect(Collectors.toList());
				if(matching.isEmpty())
					return ActionResultType.PASS;
				world.playSound(ctx.getPlayer(), pos, LocksSoundEvents.LOCK_OPEN.get(), SoundCategory.BLOCKS, 1F, 1F);
				if(world.isRemote)
					return ActionResultType.SUCCESS;
				for(Lockable lockable : matching)
					lockable.lock.setLocked(!lockable.lock.isLocked());
				return ActionResultType.SUCCESS;
			})
			.orElse(ActionResultType.PASS);
	}
}