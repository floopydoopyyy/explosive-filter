package com.explosivefilter.mixin;

import com.explosivefilter.listener.ChatListener;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ChatMixin {

    @Shadow public ServerPlayerEntity player;

    // 1.16.5 Yarn: handler method is onChatMessage(ChatMessageC2SPacket).
    // Verify name in generated sources if mixin fails to apply.
    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void onHandleChat(ChatMessageC2SPacket packet, CallbackInfo ci) {
        ChatListener.handle(packet.getChatMessage(), player);
    }
}
