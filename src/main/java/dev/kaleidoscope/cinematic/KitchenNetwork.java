package dev.kaleidoscope.cinematic;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class KitchenNetwork {
    // Weak player keys do not retain disconnected players or server instances.
    private static final Map<ServerPlayer, Long> LAST = new WeakHashMap<>();
    public record Use(ResourceLocation dimension, BlockPos pos, int sequence) implements CustomPacketPayload {
        public static final Type<Use> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KaleidoscopeCinematic.ID,"use"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Use> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeResourceLocation(p.dimension); buf.writeBlockPos(p.pos); buf.writeVarInt(p.sequence); },
            buf -> new Use(buf.readResourceLocation(), buf.readBlockPos(), buf.readVarInt()));
        @Override public Type<Use> type() { return TYPE; }
    }
    public record Result(BlockPos pos, int sequence, boolean accepted) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KaleidoscopeCinematic.ID,"result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = StreamCodec.of(
            (buf,p) -> { buf.writeBlockPos(p.pos); buf.writeVarInt(p.sequence); buf.writeBoolean(p.accepted); },
            buf -> new Result(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean()));
        @Override public Type<Result> type() { return TYPE; }
    }
    // Client installs this callback only on the physical client; no client class in common code.
    public static java.util.function.Consumer<Result> resultListener = result -> {};
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToServer(Use.TYPE, Use.CODEC, KitchenNetwork::use);
        r.playToClient(Result.TYPE, Result.CODEC, (p,c) -> c.enqueueWork(() -> resultListener.accept(p)));
    }
    private static void use(Use request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean accepted = player.level().dimension().location().equals(request.dimension())
                && execute(player, request.pos());
            player.containerMenu.broadcastChanges();
            PacketDistributor.sendToPlayer(player, new Result(request.pos(),request.sequence(),accepted));
        });
    }
    static boolean execute(ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel();
        long now = level.getGameTime();
        if (now - LAST.getOrDefault(player, now - 6) < 6) return false;
        LAST.put(player, now);
        if (!player.isAlive() || player.isSpectator() || player.isSleeping()
                || !player.mayBuild() || player.isShiftKeyDown()
                || !KitchenTarget.supported(level,pos)
                || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player,pos)) return false;
        var shape = level.getBlockState(pos).getShape(level,pos);
        if (shape.isEmpty()) return false;
        Vec3 target = Vec3.atBottomCenterOf(pos).add(0,shape.max(net.minecraft.core.Direction.Axis.Y) * 0.5,0);
        // Both a conservative cap and the actual player block reach attribute apply.
        if (player.getEyePosition().distanceToSqr(target) > 16
                || !player.canInteractWithBlock(pos, 0)) return false;
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(), target,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) return false;
        // Keep NeoForge RightClickBlock protection hooks, item restrictions, KC's interface dispatch,
        // durability, advancements, recipes and drops in their original authoritative pipeline.
        return player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit).consumesAction();
    }
    private KitchenNetwork() {}
}
