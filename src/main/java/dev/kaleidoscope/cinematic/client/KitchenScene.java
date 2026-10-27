package dev.kaleidoscope.cinematic.client;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IPot;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.Scene;
import dev.kaleidoscope.cinematic.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Own lifecycle: deliberately never registers a fake menu with CI or writes CI's scene globals. */
@EventBusSubscriber(modid = KaleidoscopeCinematic.ID, value = Dist.CLIENT)
public final class KitchenScene {
    private static BlockPos pos;
    private static Object levelIdentity, entityIdentity;
    private static Vec3 origin;
    private static long began, sentAt, actionAt;
    private static int sequence, waiting = -1;
    private static boolean pot;
    private enum Motion { CUT, PLACE, COLLECT, POUR, STIR }
    private static Motion motion = Motion.CUT;
    private static ItemStack actionItem = ItemStack.EMPTY;
    private static float entryYaw, entryPitch;
    private static String message = "";
    static { KitchenNetwork.resultListener = KitchenScene::result; }
    private static Minecraft mc() { return Minecraft.getInstance(); }
    private static boolean active() { return pos != null; }
    private static double seconds(long start) { return (System.nanoTime() - start) / 1_000_000_000.0; }
    private static void end() {
        pos = null; levelIdentity = null; entityIdentity = null; origin = null;
        waiting = -1; actionItem = ItemStack.EMPTY; actionAt = 0;
        // Camera angles, hand visibility and transforms exist only inside render callbacks.
        // No camera entity, FOV option, player rotation or CI global state needs restoring.
    }
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        var mc = mc();
        if (mc.player == null || mc.level == null) { end(); return; }
        if (mc.player.isShiftKeyDown()) { end(); return; }
        if (!active()) {
            if (!event.isUseItem() || !mc.options.getCameraType().isFirstPerson()
                    || Scene.active() || !(mc.hitResult instanceof BlockHitResult hit)
                    || !KitchenTarget.supported(mc.level, hit.getBlockPos())) return;
            pos = hit.getBlockPos().immutable();
            levelIdentity = mc.level; entityIdentity = mc.level.getBlockEntity(pos);
            origin = mc.player.position(); entryYaw = mc.player.getYRot(); entryPitch = mc.player.getXRot();
            pot = entityIdentity instanceof IPot; began = System.nanoTime();
            message = "左键操作 · 滚轮换工具 · 潜行/移动/ESC退出";
        } else if (event.isAttack() && waiting < 0 && seconds(began) > 0.3) {
            waiting = ++sequence; sentAt = System.nanoTime();
            actionItem = mc.player.getMainHandItem().copy();
            motion = Motion.PLACE;
            if (entityIdentity instanceof ChoppingBoardBlockEntity board) {
                motion = board.getMaxCutCount() > 0 && board.getCurrentCutCount() >= board.getMaxCutCount()
                    ? Motion.COLLECT : board.getCurrentCutStack().isEmpty() ? Motion.PLACE : Motion.CUT;
            } else if (entityIdentity instanceof IPot p) {
                var state = mc.level.getBlockState(pos);
                if (p.getStatus() >= IPot.FINISHED) motion = Motion.COLLECT;
                else if (state.hasProperty(com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock.HAS_OIL)
                    && !state.getValue(com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock.HAS_OIL)) motion = Motion.POUR;
                else if (actionItem.is(com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod.KITCHEN_SHOVEL)) motion = Motion.STIR;
            }
            PacketDistributor.sendToServer(new KitchenNetwork.Use(mc.level.dimension().location(), pos, waiting));
        }
        event.setCanceled(true); event.setSwingHand(false);
    }
    private static void result(KitchenNetwork.Result result) {
        if (!active() || !pos.equals(result.pos()) || waiting != result.sequence()) return;
        waiting = -1;
        if (result.accepted()) { actionAt = System.nanoTime(); message = "操作已由服务器处理"; }
        else { actionItem = ItemStack.EMPTY; message = "操作未执行：检查工具、设备状态、距离或权限"; }
    }
    @SubscribeEvent public static void key(InputEvent.Key event) {
        if (active() && event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_ESCAPE) end();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!active()) return;
        var mc = mc();
        if (mc.player == null || mc.level != levelIdentity || !mc.player.isAlive()
                || mc.player.isSpectator() || mc.player.isSleeping() || mc.player.isShiftKeyDown()
                || mc.screen != null || !mc.options.getCameraType().isFirstPerson() || Scene.active()
                || mc.player.position().distanceToSqr(origin) > 0.04
                || !KitchenTarget.supported(mc.level,pos) || mc.level.getBlockEntity(pos) != entityIdentity) {
            end(); return;
        }
        if (waiting >= 0 && seconds(sentAt) > 3) { end(); }
    }
    private static Vec3 work() {
        return Vec3.atBottomCenterOf(pos).add(0, pot ? 0.65 : 0.14, 0);
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (!active() || mc().player == null) return;
        Vec3 d = work().subtract(event.getCamera().getPosition());
        float yaw = (float)(Math.toDegrees(Math.atan2(d.z,d.x)) - 90);
        float pitch = (float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z)));
        float t = (float)Math.min(1, seconds(began)/0.3);
        t = t*t*(3-2*t);
        event.setYaw(entryYaw + Mth.wrapDegrees(yaw-entryYaw)*t);
        event.setPitch(Mth.lerp(t,entryPitch,pitch));
    }
    @SubscribeEvent public static void hideHands(RenderHandEvent event) {
        if (active()) event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (!active() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || mc().player == null) return;
        var mc = mc();
        if (!(mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof PlayerRenderer renderer)) return;
        var buffers = mc.renderBuffers().bufferSource();
        var camera = event.getCamera().getPosition();
        Vec3 forward = work().subtract(camera).multiply(1,0,1).normalize();
        Vec3 right = new Vec3(-forward.z,0,forward.x);
        double phase = actionAt == 0 ? 1 : Math.min(1,seconds(actionAt)/0.3);
        double move = Math.sin(Math.PI * phase);
        Vec3 left = work().add(right.scale(-0.22)).add(0,0.04,0);
        Vec3 main = work().add(right.scale(0.18)).add(0,0.07 + 0.24*move,0);
        if (pot && motion == Motion.STIR && phase < 1) main = work().add(right.scale(0.2*Math.cos(phase*Math.PI*2)))
                .add(forward.scale(0.2*Math.sin(phase*Math.PI*2))).add(0,0.12,0);
        if (motion == Motion.COLLECT || motion == Motion.PLACE) {
            main = work().add(right.scale(0.18)).add(forward.scale(-0.22*move)).add(0,0.08+0.1*move,0);
        } else if (motion == Motion.POUR) {
            main = work().add(right.scale(0.15)).add(0,0.3,0);
        }
        int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level,pos.above());
        arm(event.getPoseStack(),renderer,buffers,left.subtract(camera),false,light,entryYaw);
        arm(event.getPoseStack(),renderer,buffers,main.subtract(camera),true,light,entryYaw);
        ItemStack item = phase < 1 ? actionItem : mc.player.getMainHandItem();
        if (!item.isEmpty()) {
            PoseStack pose = event.getPoseStack(); pose.pushPose();
            Vec3 point = main.subtract(camera); pose.translate(point.x,point.y,point.z);
            pose.mulPose(Axis.YP.rotationDegrees(-entryYaw));
            pose.mulPose(Axis.XP.rotationDegrees((float)(-65 + move*35))); pose.scale(0.45f,0.45f,0.45f);
            mc.getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED,light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);
            pose.popPose();
        }
    }
    private static void arm(PoseStack pose, PlayerRenderer renderer,
            net.minecraft.client.renderer.MultiBufferSource buffers, Vec3 point, boolean right, int light,float yaw) {
        pose.pushPose();
        pose.translate(point.x,point.y,point.z);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-85)); pose.scale(0.65f,0.65f,0.65f);
        if (right) renderer.renderRightHand(pose,buffers,light,mc().player);
        else renderer.renderLeftHand(pose,buffers,light,mc().player);
        pose.popPose();
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        if (!active() || mc().level == null) return;
        String progress = "";
        if (mc().level.getBlockEntity(pos) instanceof ChoppingBoardBlockEntity board) {
            progress = "  切割 " + board.getCurrentCutCount() + "/" + board.getMaxCutCount();
        } else if (mc().level.getBlockEntity(pos) instanceof IPot p) progress = "  锅状态 " + p.getStatus();
        event.getGuiGraphics().drawString(mc().font, Component.literal(message + progress),8,8,0xffffff);
    }
    private KitchenScene() {}
}
