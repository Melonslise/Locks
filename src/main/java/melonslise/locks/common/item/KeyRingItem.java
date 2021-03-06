package melonslise.locks.common.item;

import java.util.List;
import java.util.stream.Collectors;

import melonslise.locks.common.capability.CapabilityProvider;
import melonslise.locks.common.capability.KeyRingInventory;
import melonslise.locks.common.container.KeyRingContainer;
import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.init.LocksSoundEvents;
import melonslise.locks.common.util.Lockable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;

// TODO Add amount of keys to tooltip
public class KeyRingItem extends Item
{
	public final int rows;

	public KeyRingItem(Properties props, int rows)
	{
		super(props);
		this.rows = rows;
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, CompoundNBT nbt)
	{
		return new CapabilityProvider(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, new KeyRingInventory(stack, this.rows, 9));
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
	{
		if(!player.world.isRemote)
			NetworkHooks.openGui((ServerPlayerEntity) player, new KeyRingContainer.Provider(player.getHeldItem(hand)), new KeyRingContainer.Writer(hand));
		return new ActionResult<>(ActionResultType.PASS, player.getHeldItem(hand));
	}

	@Override
	public ActionResultType onItemUse(ItemUseContext ctx)
	{
		World world = ctx.getWorld();
		BlockPos pos = ctx.getPos();
		return world.getCapability(LocksCapabilities.LOCKABLES)
			.map(lockables ->
			{
				return ctx.getItem().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
					.map(inv->
					{
						List<Lockable> intersecting = lockables.get().values().stream().filter(lockable1 -> lockable1.box.intersects(pos)).collect(Collectors.toList());
						if(intersecting.isEmpty())
							return ActionResultType.PASS;
						for(int a = 0; a < inv.getSlots(); ++a)
						{
							int id = LockingItem.getOrSetId(inv.getStackInSlot(a));
							List<Lockable> matching = intersecting.stream().filter(lockable1 -> lockable1.lock.id == id).collect(Collectors.toList());
							if(matching.isEmpty())
								continue;
							for(Lockable lockable : matching)
								lockable.lock.setLocked(!lockable.lock.isLocked());
							world.playSound(ctx.getPlayer(), pos, LocksSoundEvents.LOCK_OPEN.get(), SoundCategory.BLOCKS, 1F, 1F);
							return ActionResultType.SUCCESS;
						}
						return ActionResultType.PASS;
					})
					.orElse(ActionResultType.PASS);
			})
			.orElse(ActionResultType.PASS);
	}
}