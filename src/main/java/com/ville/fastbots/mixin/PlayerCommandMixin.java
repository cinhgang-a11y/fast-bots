package com.ville.fastbots.mixin;

import com.ville.fastbots.FastBots;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

/** /player spawn resolves the name a second time for its ban/whitelist check. */
@Mixin(targets = "carpet.commands.PlayerCommand")
public abstract class PlayerCommandMixin {
    @Redirect(method = "cantSpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/OldUsersConverter;convertMobOwnerIfNecessary(Lnet/minecraft/server/MinecraftServer;Ljava/lang/String;)Ljava/util/UUID;"))
    private static UUID fastbots$resolve(MinecraftServer server, String name) {
        return FastBots.resolve(server, name);
    }
}
