package dev.kaleidoscope.cinematic;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopeCinematic.ID)
@PrefixGameTestTemplate(false)
public final class BoardGameTests {
    private static final BlockPos BOARD = new BlockPos(3,2,3);
    private static ServerPlayer player(GameTestHelper h) {
        var p = FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"KaleTest"));
        p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(BOARD);
        p.setPos(at.getX()+0.5,at.getY(),at.getZ()+1.5);
        return p;
    }
    private static ChoppingBoardBlockEntity board(GameTestHelper h) {
        h.setBlock(BOARD.below(),Blocks.STONE);
        h.setBlock(BOARD,ModBlocks.CHOPPING_BOARD.get());
        return (ChoppingBoardBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(BOARD));
    }
    private static void require(boolean condition,String description) {
        if (!condition) throw new net.minecraft.gametest.framework.GameTestAssertException(description);
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void cod_becomes_exactly_three_sashimi(GameTestHelper h) {
        var board = board(h);
        var p = player(h);
        var pos = h.absolutePos(BOARD);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD,2));
        require(KitchenNetwork.execute(p,pos),"Put cod rejected");
        require(p.getMainHandItem().getCount()==1,"Must consume exactly one cod");
        require(board.getMaxCutCount()==4 && board.getCurrentCutCount()==0,"Original cod recipe not loaded");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.IRON_KITCHEN_KNIFE.get()));
        for (int cut=1;cut<=4;cut++) {
            final int expected=cut;
            h.runAtTickTime(cut*8,() -> {
                require(KitchenNetwork.execute(p,pos),"Cut rejected: "+expected);
                require(board.getCurrentCutCount()==expected,"Each cut must advance by one");
            });
        }
        h.runAtTickTime(40,() -> {
            require(KitchenNetwork.execute(p,pos),"Collection rejected");
            require(board.getCurrentCutStack().isEmpty(),"Board not cleared");
            int total=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1.5)).stream()
                .filter(e->e.getItem().is(ModItems.SASHIMI.get())).mapToInt(e->e.getItem().getCount()).sum();
            require(total==3,"Expected exactly 3 sashimi; got "+total);
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void rejects_distance_wall_wrong_tool_and_spam(GameTestHelper h) {
        var board=board(h);var p=player(h);var pos=h.absolutePos(BOARD);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD,2));
        double x=p.getX(), y=p.getY(), z=p.getZ();
        p.setPos(x+20,y,z);
        require(!KitchenNetwork.execute(p,pos),"Remote operation accepted");
        require(p.getMainHandItem().getCount()==2,"Remote operation consumed input");
        p.setPos(x,y,z);
        h.runAtTickTime(8,() -> {
            require(KitchenNetwork.execute(p,pos),"Valid insertion rejected");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.IRON_KITCHEN_KNIFE.get()));
            require(!KitchenNetwork.execute(p,pos),"Same-tick spam accepted");
            require(board.getCurrentCutCount()==0,"Spam cut the board");
        });
        h.runAtTickTime(16,() -> {
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
            require(!KitchenNetwork.execute(p,pos),"Wrong tool accepted");
            require(board.getCurrentCutCount()==0,"Wrong tool changed progress");
        });
        h.runAtTickTime(24,() -> {
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.IRON_KITCHEN_KNIFE.get()));
            p.setPos(x,y,z+1);
            h.setBlock(BOARD.south().above(),Blocks.STONE);
            require(!KitchenNetwork.execute(p,pos),"Obstructed board accepted");
            require(board.getCurrentCutCount()==0,"Wall test changed progress");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void two_players_share_progress_without_duplicate_outputs(GameTestHelper h) {
        var board=board(h); var a=player(h); var b=player(h); var pos=h.absolutePos(BOARD);
        a.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD));
        b.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD));
        require(KitchenNetwork.execute(a,pos),"First insertion failed");
        require(!KitchenNetwork.execute(b,pos),"Occupied board accepted second cod");
        require(b.getMainHandItem().getCount()==1,"Second player's cod consumed");
        a.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.IRON_KITCHEN_KNIFE.get()));
        b.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.IRON_KITCHEN_KNIFE.get()));
        for(int tick : new int[]{8,16}) h.runAtTickTime(tick,() -> {
            require(KitchenNetwork.execute(a,pos),"Player A cut failed");
            require(KitchenNetwork.execute(b,pos),"Player B cut failed");
        });
        h.runAtTickTime(24,() -> {
            require(board.getCurrentCutCount()==4,"Shared progress incorrect");
            require(KitchenNetwork.execute(a,pos),"First collection failed");
            KitchenNetwork.execute(b,pos); // Original empty-board knife feedback is allowed, extra output is not.
            int total=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1.5)).stream()
                .filter(e->e.getItem().is(ModItems.SASHIMI.get())).mapToInt(e->e.getItem().getCount()).sum();
            require(total==3,"Concurrent collection duplicated results: "+total);
            h.succeed();
        });
    }
}
