package com.ville.fastbots;

import com.ville.fastbots.mixin.NameCacheAccessor;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.server.players.UserNameToIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Carpet's /player spawn resolves every name against Mojang on the server
 * thread and then waits for a skin download. For bot armies with made-up
 * names both calls fail, each costs a round trip, and past ~150 of them the
 * rate limit stalls one tick for a minute and the watchdog kills the server.
 *
 * Names already in the user cache (real accounts spawned before) keep the
 * normal path. Anything else spawns at once with an offline UUID, and the
 * name is looked up in the background so a real account gets its skin from
 * the next spawn on.
 */
public final class FastBots {
    public static final Logger LOG = LoggerFactory.getLogger("fastbots");

    // one thread, a short queue: an army of random names must not turn into
    // a thousand queued requests; the dropped ones are gibberish anyway
    private static final ExecutorService LOOKUPS = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(16), r -> {
                Thread t = new Thread(r, "fastbots-lookup");
                t.setDaemon(true);
                return t;
            }, new ThreadPoolExecutor.DiscardPolicy());
    private static final Set<String> TRIED = ConcurrentHashMap.newKeySet();

    private FastBots() {}

    public static UUID resolve(MinecraftServer server, String name) {
        UserNameToIdResolver cache = server.services().nameToIdCache();
        if (!(cache instanceof NameCacheAccessor peek)) {
            return OldUsersConverter.convertMobOwnerIfNecessary(server, name);
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (peek.fastbots$profilesByName().containsKey(key)) {
            return OldUsersConverter.convertMobOwnerIfNecessary(server, name);   // cached: no network
        }
        if (TRIED.add(key)) {
            LOOKUPS.execute(() -> {
                try {
                    cache.get(name).ifPresent(n -> LOG.info("{} is a real account; its skin shows from the next spawn", n.name()));
                } catch (Exception e) {
                    LOG.debug("lookup of {} failed", name, e);
                }
            });
        }
        return UUIDUtil.createOfflinePlayerUUID(name);
    }

    /** Offline UUIDs are version 3 (name-based); real accounts are version 4. */
    public static boolean isOffline(UUID id) {
        return id.version() == 3;
    }
}
