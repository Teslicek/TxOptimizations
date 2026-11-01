package com.teslicek.txoptimizations;

import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public final class WorkerTextureLoads {

    private static final ConcurrentLinkedQueue<Request> REQUESTS = new ConcurrentLinkedQueue<>();

    private WorkerTextureLoads() {
    }

    public static AbstractTexture await(TextureManager manager, Identifier id) {
        Request request = new Request(manager, id);

        REQUESTS.add(request);

        while (request.texture == null && request.failure == null)
            Thread.onSpinWait();

        if (request.failure != null)
            throw new IllegalStateException("Loading texture " + id + " for a feature worker failed", request.failure);

        return request.texture;
    }

    public static void serve() {
        for (Request request = REQUESTS.poll(); request != null; request = REQUESTS.poll()) {
            try {
                request.texture = request.manager.getTexture(request.id);
            } catch (Throwable thrown) {
                request.failure = thrown;
            }
        }
    }

    private static final class Request {

        private final TextureManager     manager;
        private final Identifier         id;
        private volatile AbstractTexture texture;
        private volatile Throwable       failure;

        private Request(TextureManager manager, Identifier id) {
            this.manager = manager;
            this.id      = id;
        }
    }
}
