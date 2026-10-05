package com.ville.fastbots.mixin;

import com.mojang.authlib.GameProfile;
import com.ville.fastbots.FastBots;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mixin(targets = "carpet.patches.EntityPlayerMPFake")
public abstract class EntityPlayerMPFakeMixin {
    @Shadow
    private static CompletableFuture<GameProfile> fetchGameProfile(MinecraftServer server, UUID id) {
        throw new AssertionError();
    }

    @Redirect(method = "createFake", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/OldUsersConverter;convertMobOwnerIfNecessary(Lnet/minecraft/server/MinecraftServer;Ljava/lang/String;)Ljava/util/UUID;"))
    private static UUID fastbots$resolve(MinecraftServer server, String name) {
        return FastBots.resolve(server, name);
    }

    // Carpet falls back to the plain profile when the fetched one has an
    // empty name, so an offline bot skips the skin download entirely
    @Redirect(method = "createFake", at = @At(value = "INVOKE",
            target = "Lcarpet/patches/EntityPlayerMPFake;fetchGameProfile(Lnet/minecraft/server/MinecraftServer;Ljava/util/UUID;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<GameProfile> fastbots$fetch(MinecraftServer server, UUID id) {
        if (FastBots.isOffline(id)) {
            return CompletableFuture.completedFuture(new GameProfile(id, ""));
        }
        return fetchGameProfile(server, id);
    }
}
