package com.teslicek.txoptimizations;

import com.mojang.authlib.services.ServicesKeySet;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.RemoteChatSession;
import org.jspecify.annotations.Nullable;

public final class ChatSessionCache {

    private static final int                         MAX_ENTRIES = 4096;
    private static final Map<Key, RemoteChatSession> VALIDATED   = new HashMap<>();

    private ChatSessionCache() {
    }

    public static Key key(ServicesKeySet keys, UUID profileId, RemoteChatSession.Data data) {
        return new Key(keys, profileId, data.sessionId(), data.profilePublicKey().expiresAt(), data.profilePublicKey().key(), ByteBuffer.wrap(data.profilePublicKey().keySignature().clone()));
    }

    public static @Nullable RemoteChatSession find(Key key) {
        return VALIDATED.get(key);
    }

    public static void store(Key key, RemoteChatSession session) {
        if (VALIDATED.size() >= MAX_ENTRIES)
            VALIDATED.clear();

        VALIDATED.put(key, session);
    }

    public record Key(ServicesKeySet keys, UUID profileId, UUID sessionId, Instant expiresAt, PublicKey key, ByteBuffer signature) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Key that && this.keys == that.keys && this.profileId.equals(that.profileId) && this.sessionId.equals(that.sessionId) && this.expiresAt.equals(that.expiresAt) && this.key.equals(that.key) && this.signature.equals(that.signature);
        }

        @Override
        public int hashCode() {
            int hash = System.identityHashCode(this.keys);

            hash = 31 * hash + this.profileId.hashCode();
            hash = 31 * hash + this.sessionId.hashCode();
            hash = 31 * hash + this.expiresAt.hashCode();
            hash = 31 * hash + this.key.hashCode();

            return 31 * hash + this.signature.hashCode();
        }
    }
}
