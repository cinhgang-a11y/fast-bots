package com.ville.fastbots.mixin;

import net.minecraft.server.players.CachedUserNameToIdResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Read the user cache without triggering its network lookup on a miss. Keys are lowercase names. */
@Mixin(CachedUserNameToIdResolver.class)
public interface NameCacheAccessor {
    @Accessor("profilesByName")
    Map<String, ?> fastbots$profilesByName();
}
