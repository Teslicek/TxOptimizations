# TxOptimizations

Client-side Fabric mod for **Minecraft 26.3** (Java 25) that removes repeated per-frame work from the render thread without changing settings or what you see.

## Optimizations

### Rain fog

Every frame, `AtmosphericFogEnvironment.updateRainFogState` looks up the biome and the sky light at the camera position to fade in rain fog. A spark profile showed the sky light lookup alone taking about 2.8% of the render thread. TxOptimizations looks both up once per client tick and reuses them for the other frames of that tick, as long as the camera stays in the same block of the same level. The values only feed a slowly fading fog multiplier, so the result is visually identical.

### Particle light

Every frame, each visible particle looks up the light level at its position again (`Particle.getLightCoords`). Particles only move once per tick, so at high frame rates the same lookup repeats many times per tick. TxOptimizations caches the result per particle for the current client tick and position. A light change reaches particles at most one tick (50 ms) later. Particle types that compute their own light without calling the base method are unaffected.

### Vertex buffer recycling

At the end of every frame, `StagedVertexBuffer$GpuBufferPool.tryRecycleBuffers` checks every pending batch of vertex buffers to see whether the GPU is done with it. Each check is a native Vulkan call (`vkWaitSemaphores` with a zero timeout), and a spark profile showed these checks taking about 2.8% of the render thread. The batches are added in submission order and the GPU finishes submissions in that order, so TxOptimizations checks them from the oldest and stops at the first one that is still in use. Several pools (staging, vertex and index buffers for the world and the GUI) run this check every frame, and the oldest batch is usually still in use, so each pool would still make one native call per frame. While this recycle pass runs, TxOptimizations also remembers the lowest submit index the GPU reported as unfinished in the current frame and answers "not yet" for that index and every later one without asking the GPU again. It never answers "done" from memory, and vanilla's own fast path for already completed submits still runs first.

When the GPU does have to be asked, any zero-timeout completion check reads the submit semaphore's counter with `vkGetSemaphoreCounterValue` instead of calling `vkWaitSemaphores` with a zero timeout. The Vulkan spec defines both as the same question (has the counter reached this submit), so the answer is identical. The counter value is also stored as the encoder's completed submit index, so every older submit is answered by vanilla's fast path without any native call. Waits with a timeout are not touched. The result is the same set of recycled buffers with far fewer native calls. If a batch finishes in the middle of a frame or out of order, it is only reused one frame later.

### Block entity renderer lookup

Every frame, every block entity (chests, signs, heads, banners) asks `BlockEntityRenderDispatcher.getRenderer` for its renderer, which is a hash map lookup by block entity type. On a server lobby with many block entities this took about 1.7% of the render thread. TxOptimizations remembers the renderer on each block entity together with the renderer map it came from. A resource reload replaces that map, so the next lookup after a reload refreshes automatically, and a block entity's type never changes. The renderer returned is always the same one vanilla would return.

### Entity renderer lookup

Every frame, every entity asks `EntityRenderDispatcher.getRenderer` for its renderer, which runs a type switch to separate players and mannequins from other entities and then looks the renderer up by entity type in a hash map. TxOptimizations remembers the renderer on each entity together with the renderer map it came from, the same way it does for block entities. A resource reload replaces that map, so the next lookup after a reload refreshes automatically. Players and mannequins are never cached here because their renderer depends on the skin model.

### Sound listener updates

Every frame, `SoundEngine.updateSource` sends the camera's position and direction to the sound thread, and every send wakes that thread from sleep. At thousands of frames per second this took about 1% of the render thread. TxOptimizations skips the update when the camera has not moved or turned since the last one that was sent, and otherwise sends at most one update every 5 ms. Each frame compares against the last update actually sent, so the final position after you stop moving is always sent within 5 ms. OpenAL applies listener changes once per mixing block, which is normally longer than 5 ms, so the limit is not audible. When the sound system restarts (`loadLibrary`), the last sent update is forgotten so the listener is set again on the next frame.

### Left-to-right text

Every piece of text drawn on screen goes through `FormattedBidiReorder.reorder`, which runs ICU's bidirectional algorithm and Arabic shaping on it, even for plain English. When the language is left-to-right and the text has no right-to-left letters, Arabic numbers or explicit direction marks, the algorithm always produces a single left-to-right run covering the whole text, and shaping changes nothing. TxOptimizations detects that case with ICU's own character direction data and builds the same single run directly. Any text with Hebrew, Arabic or direction control characters, and every right-to-left language, still goes through vanilla.

### Camera fluid

`Camera.getFluidInCamera` checks the blocks at the camera and at the four near-plane corners for water, lava and powder snow, and it is called several times per frame. TxOptimizations computes it once per frame and reuses the answer while the camera's level, position and rotation are the same.

### Spare vertex buffers

At the end of every frame, the vertex buffer pools close every spare buffer that was not used in that frame, so a buffer that is needed again a frame later has to be created again. TxOptimizations keeps a spare buffer for up to 3 idle frames before closing it. Spares that get reused never reach that limit, and buffers the GPU is still using are never touched. Gnetum's HUD flushing still skips the cleanup the same way it does in vanilla.

### Translation templates

Every time a translated text is resolved, `TranslatableContents.decomposeTemplate` runs a regular expression over the translation string to find `%s`, `%1$s` and `%%`. TxOptimizations parses each distinct translation string once, remembers the literal parts and argument positions, and replays them in the same order with the same argument lookups. Invalid translation strings are left to vanilla so they fail and fall back exactly as before. Up to 4096 strings are kept, and the cache is cleared when it fills up.

### F3 entry checks

`DebugScreenEntryList.isCurrentlyEnabled` searches the list of enabled F3 entries one by one, and it is called for many entries every frame. TxOptimizations answers from a hash set built from that list, rebuilt whenever vanilla's own version counter for the list changes.

### Particle layers

Every frame, each particle quad is added to `QuadParticleRenderState`, which looks up the storage for the particle's layer in a hash map. The layer is a record, so every lookup hashes its fields again. Particles only ever use six constant layer objects and the map never loses an entry, so TxOptimizations remembers each layer object it has seen together with its storage and returns that storage directly. A layer it has not seen yet still goes through the map.

### Buffer builder elements

Every new `BufferBuilder` looks up up to eight vertex elements (position, color, UVs, normal, line width) by name in its vertex format, and the format's element map compares the names one by one. Vertex formats never change, so TxOptimizations resolves those eight elements once per vertex format with the vanilla lookup and reuses the result for every later `BufferBuilder` of that format, including the constructor's check that the format has a position element.

### Player skin model lookup

Every frame, `EntityRenderDispatcher.getAvatarRenderer` reads each player's skin through the player list and skin manager to choose the wide or slim player model, once for culling and once for drawing. TxOptimizations keeps the skin it read for that player during the current frame and reuses it for the second call. A skin that finishes loading is picked up on the next frame.

### Translucent sorting

Translucent geometry such as entity translucency, text, see-through name tags and glints is sorted back to front every frame. Vanilla reads the center of every quad with six bounds-checked `ByteBuffer.getFloat` calls and writes the six sorted indices per quad through an `IntConsumer` lambda. TxOptimizations reads the same floats straight from native memory when the buffer is direct and in native byte order (one bounds check for the whole range up front), computes the centers with the same arithmetic, and writes the same index values straight to the buffer's memory when it is direct and in native byte order, with one bounds check for the whole range, and through a short or int view of the buffer otherwise. Sodium's sort of up to 80 quads ran fastutil's indirect quicksort with a comparator per comparison and then a pass that orders equal keys by index; it now packs each key and index into one long and sorts those, which gives exactly the same order (by key, then by index). Unstable sorts are left as they were. The bytes written and the final buffer position are identical. Sodium's closest-point entity sort option, when enabled, replaces the center decoding with its own and is unaffected.

### Glow check spectator state

Sodium asks `Minecraft.shouldEntityAppearGlowing` for every entity every frame, and for every entity that is not glowing vanilla checks whether the local player is a spectator. TxOptimizations computes that once per frame and reuses it for the other entities.

### Environment attribute values

Sky color, fog color and other environment attributes are read through `EnvironmentAttributeProbe$ValueProbe.get`, which blends the previous and current tick values for the frame's partial tick every time it is called, several times per frame. The blend only depends on those two values and the partial tick, so TxOptimizations remembers the last result and returns it while both values and the partial tick are unchanged.

### GPU crash checkpoints

When the GPU supports `VK_AMD_buffer_marker` or `VK_NV_device_diagnostic_checkpoints`, renderpearl writes a marker into the command buffer at every render pass so a driver crash report can name the last pass that ran. That took about 0.4% of the render thread. TxOptimizations makes renderpearl use its built-in no-op checkpoint storage, the same one it uses on GPUs without those extensions. Rendering is unchanged, but a GPU driver crash report no longer lists the last render passes.

### Particle rotation

Every camera-facing particle allocated a new rotation quaternion each frame. The quaternion is now reused, reset to identity first as the new one was.

### Main target clear

Every frame cleared the main color and depth target before rendering, and the world render cleared the same target again with the fog color before drawing anything. When a world is rendered, the first clear is skipped. Screens without a world still clear as before.

### Particle corners

For every particle, Sodium rotated the four corners of the quad by the particle rotation, four quaternion rotations per particle per frame. Camera-facing particles all carry the camera rotation, so the rotated corners are the same for each of them. The rotated corners are kept while the rotation stays bit-for-bit the same, and only the per-particle scale and offset are applied, in the same order as before, so every vertex is identical.

### Debug lines

Hitboxes and other debug lines are built with a pose that is always the identity, yet vanilla transformed every vertex position and normal through it, allocating a vector each time, and multiplied both ends of every line by the full view matrix only to read the view depth for near-plane clipping. With hitboxes shown this cost several percent of the render thread. The values are now written directly, only the depth row of the view matrix is evaluated with the same arithmetic, and an error is thrown if the pose is ever anything but the identity. When the line buffer duplicates vertices for line quads and has position, color, normal and line width, each line is written straight into the buffer in its final form (start, its copy, end) with the same encoding the buffer builder uses, leaving the end vertex for the builder to copy exactly as it would after its own calls.

### Item quad normals

Sodium writes item model quads by transforming and renormalizing the normal of every vertex, although the four vertices of a quad nearly always share one normal. The transformed normal is now reused while consecutive vertices carry the same normal, so most quads do this work once instead of four times.

### Indirect terrain draws

On GPUs with the multi-draw extension Sodium hands every section's draw commands to the driver through `vkCmdDrawMultiIndexedEXT`, and the driver walks all of them on the render thread each frame. Sodium's indirect path writes the same commands into a GPU-visible buffer and issues one indirect draw per region, so the GPU reads the list instead. It is now used whenever the device supports indirect multi-draw.

### Region pass lookups

Each region kept its per-pass section data and cached draw batches in hash maps keyed by render pass, looked up for every region and pass each frame. With only three passes they are now also held in arrays indexed by pass, and the lookups read the arrays.

### Repeated binds

Binding a pipeline on a render pass validated its attachments, recorded a pipeline bind, forgot every uniform and then set all of them again, even when that same pipeline was already bound on the pass. Vertex and index buffers were also bound again through a native call before every draw that used the buffer already bound. Binding the pipeline, vertex buffer slice or index buffer and type that is already bound on the pass is now skipped, unless the buffer has been closed so vanilla's error still fires. Vulkan keeps the bound pipeline, its pushed descriptors, push constants and buffer bindings, and every uniform set afterwards still goes through the normal path, so every draw sees the same state as before.

### Clears on load

Every frame cleared the world target, the depth before the held item and the depth before the GUI with separate clear commands, each followed by a barrier that drains the whole GPU, and then started a render pass on the same target that loaded the cleared contents. A clear is now held back and folded into the next render pass on that texture as a clear on load when the pass covers the whole texture, which needs neither the separate command nor the barrier. Buffer uploads and timestamps may pass a held clear because they never touch the image. Any other command, or a pass that does not cover the texture, records the held clear first exactly as before.

### Upload barriers

The Vulkan backend follows every buffer upload with a barrier that makes the whole GPU finish all earlier work before anything else may start. Several uniform uploads happen back to back each frame (global settings, projection matrices, lighting), so the GPU drained once per upload. Consecutive uploads and buffer-to-buffer copies to separate buffer ranges now share a single barrier (a copy also waits when its source or destination overlaps a range still waiting), which is recorded before the next command of any other kind, before an upload that overlaps one still waiting for its barrier, and before the command buffer ends. Timestamp queries do not trigger it.

### World text layout

Name tags, holograms and other text in the world were laid out again every frame: each character looked up its glyph, built a positioned glyph instance and updated the text bounds. Components cache their visual-order sequence, so text the server has not changed arrives as the same sequence object every frame. The laid-out text is now kept per sequence object, position, colors, shadow and outline, and drawn again from the cache. Text with obfuscated characters or with player head and atlas sprite glyphs is never cached, unused layouts are dropped after 200 frames, and everything is dropped when fonts reload or font options change. Drawing a laid-out text only reads it, so the cached copy draws exactly like a fresh one.

### Direct vertex writes

Sodium writes every glyph, particle, entity cuboid and item quad into a small stack buffer and then copies it into the frame's vertex buffer, one native memory copy per glyph or cuboid. At a busy server spawn these copies took about 10% of the render thread. When the vertex buffer uses exactly the format being written, TxOptimizations reserves the space in the buffer first and writes the same bytes straight into it, updating the vertex count and last vertex pointer the same way Sodium's push does. Buffers with another format keep Sodium's converting push.

### Attribute lookups

Every attribute read, such as an entity's scale or name tag distance each frame, looked the attribute up with `computeIfAbsent` and a freshly allocated lookup function. Attribute instances are never removed from an entity's attribute map, so the lookup now reads the map first and only falls back to vanilla's `computeIfAbsent` with the same creation function when the attribute is not there yet.

### Descriptor pushes

Every draw with a changed uniform pushed the pass's whole descriptor set to the driver, and uniforms are set again for most draws even when they hold the same buffers and textures as before. Each render pass now remembers the values and pipeline layout of its last push and skips a push whose values and layout are identical. Vulkan keeps pushed descriptors for the same pipeline layout, so every draw reads the same descriptors as before. A new render pass always pushes.

### Entity shadow blocks

Every entity with a shadow checked the blocks under it every frame: the block below, the light level, whether the block below has a full collision shape and its shape. These answers are now kept per block position for the current client tick and shared by all entities, then dropped at the next tick or level change. The shadow's strength and position are still computed every frame from the entity and the camera; a block or light change under a shadow shows up to one tick (50 ms) later.

### Map colors

When a map's contents change, every one of its 16384 pixels was converted from its packed color byte to a color by looking up the map color and scaling it by the brightness. Map colors and brightness levels are fixed, so the 256 possible results are computed once with the same vanilla function and looked up per pixel. Servers that update maps in item frames often, like leaderboards, pay this on every update.

### Hitbox cuboids

Each hitbox or debug cuboid created 24 corner vectors every frame for its 12 edges and 24 more when filled, although a box has 8 corners. The edges and faces now share the 8 corner vectors, which are immutable, so the same lines and quads are emitted with fewer allocations.

### Feature renderer list

Preparing each frame walked the list of feature renderers through a freshly built Guava filter that skips empty slots, three times per frame preparation. The filtered list only changes when a renderer is registered, so it is built once at registration and reused.

### Entity visibility reuse

With entity hitboxes shown (F3+B), the hitbox renderer runs the same visibility check for every entity that entity extraction already ran earlier in the frame. Each entity remembers its result for the frame, and a second check with the same frustum, camera position, partial tick and fade setting reuses it. Any call with different inputs computes normally.

### Block outline edges

The outline around the targeted block walks the block shape's voxel grid every frame to find its edges, which took about 2% of the render thread on complex shapes. The edges of the last 8 shapes are kept and replayed in the same order, so the outline is identical. Block shapes are immutable and shared, so a new shape object is simply recomputed.

### FPS counter

TxOptimizations draws its own FPS counter in the top left corner (`123 FPS`, white, no shadow, at 2,2), hidden while the F3 screen is open or the HUD is hidden. The number is the average frame rate over the last 0.5 seconds and updates every 0.5 seconds, the same as Sodium Extra's FPS overlay. It only counts frames, and the text is rebuilt only when the number changes, so drawing it skips the per-frame translation and text-direction work that Sodium Extra's overlay goes through.

Sodium Extra records every frame and sorts the last 5 seconds of frame times twice every 0.5 seconds for its 1% and 0.1% low values, even when its overlay is off, which took about 0.8% of the render thread at high frame rates. When Sodium Extra is installed, TxOptimizations skips that recording, so Sodium Extra's own FPS overlay (its Show FPS option, `show_fps` in `config/sodium-extra-options.json`) should be switched off.

### Baked block entities

Chests, ender chests, shulker boxes, bells, skulls, banner poles, decorated pots, copper golem statues and cushions are baked into the chunk mesh like normal blocks instead of being drawn again every frame. They switch back to per-frame rendering only while they move: a chest or shulker box while its lid is open, a bell while it swings, a decorated pot while it wobbles, an animated dragon or piglin head. Banner flags keep waving because only the pole and bar are baked. Player heads and decorated pots with sherds stay per-frame, and a named cushion only draws its name tag per frame. In a server lobby profile this cut block entity rendering from about 4.1% to 0.35% of the render thread.

Block entities with nothing to draw are skipped: signs without text, beacons without a beam, campfires and shelves without items and lecterns without a book. The answer is remembered on each block entity for the current client tick, so a sign that gains text shows it at most one tick later. Sign text facing away from the camera is not drawn.

### Atlases and batching

- Font atlas: font glyph textures are 1024x1024 instead of 256x256, so text spans far fewer textures and needs fewer texture switches and draw calls.
- Map atlas: map images are packed into shared 2048x2048 textures, so walls of maps in item frames draw in a few calls instead of one per map.
- Glyph vertex builder: consecutive glyphs of the same render type reuse the same vertex builder instead of looking it up for every glyph.
- Animated item atlas: animated item icons in the GUI share one atlas that is cleared once per frame instead of slot by slot.
- Scissor state equality: vanilla merges draws of the same prepared render type inside groups it marks as reorderable, but `RenderType.prepare` copies the scissor state every time and `ScissorState` has no value equality, so two prepared render types never compare equal and the merge almost never happens. Comparing scissor states by value lets that vanilla merge work, which cuts draw calls. Strictly ordered groups are untouched.

When a loaded resource pack overrides the `core/text` shader, the font atlas stays at 256x256 unless the pack lists `font_atlas_resizing` under `compatible_features` in an `immediatelyfast` metadata section, and the map atlas is switched off when the pack lists `map_atlas_generation` under `incompatible_features`. Server resource packs with custom text shaders therefore look the same as in vanilla.

### HUD cache

The HUD is drawn into an offscreen framebuffer at up to 60 frames per second (20 while a screen is open), and every frame only blends that cached image over the world. Each HUD frame is spread over 3 render frames: HUD layers are split into 3 groups of roughly equal measured cost, and each frame redraws one group, so no single frame pays for the whole HUD. Animations inside cached layers advance by the summed frame time of the cycle. Layers whose blending reads the color underneath them, like the crosshair, switch to being drawn every frame the first time they appear, and so does anything drawn outside a registered HUD layer, including the FPS counter. Resizing the window, opening or closing a screen or hiding the HUD redraws the HUD directly for two cached frames so nothing shows stale. The cached image is blended over the world only inside up to 8 non-overlapping rectangles covering the bounds of everything drawn into it, padded by 2 GUI pixels, instead of over the whole screen, so the GPU blends the areas around the hotbar, chat, scoreboard and other HUD parts instead of every pixel. Vanilla drops GUI elements without bounds and orders overlapping elements by them, so every drawn element reports them.

### Rain culling

Rain and snow are built column by column around the camera every frame. Columns whose vertical span lies outside the camera's view are skipped. Each column draws with its own position-seeded randomness, so the visible rain looks exactly the same.

For every column that is drawn, vanilla also asked the level for the precipitation type at that block, which looks up the biome through the biome zoom noise and the biome temperature. In rain a spark profile showed this at 20% of the render thread. The answer only depends on the chunk being loaded and its biome data, so it is cached per block position and the cache is dropped whenever a chunk is loaded, unloaded or gets new biomes, and when the level changes. The column bounds come from the values vanilla already computed instead of a second heightmap lookup.

### Entity culling

Entities and block entities hidden behind solid blocks are not extracted or drawn. Every 5 ticks the client tick records the culling box and position of every loaded entity, plus every block entity within 64 blocks of the camera whose renderer isn't drawn off screen. A background thread then traces rays from the camera to sample points on the camera-facing faces of each box, through blocks whose state is a solid render cube, and marks it culled when no ray gets through. This runs again every tick the camera moves or a new capture arrives. Ray results per block are cached for the pass, and only the touched cache entries are cleared before the next one.

Entities farther than 128 blocks, glowing entities, the player, entities whose renderer ignores culling and boxes larger than 50 blocks are never culled, and anything found visible stays visible for at least one second. Culled entities that show their name still draw the name tag and the below-name score through walls, at the same distances vanilla uses. Block entities outside the camera's view are skipped as well, without allocating a box.

Entities that are culled, or were not extracted in any frame since the previous tick, only run a basic tick on the client: vanilla's common tick (old position and rotation, invulnerability timer, position interpolation, tick count), `aiStep`, hurt time and the warden heartbeat. A tick with no frame since the previous one, below 20 FPS, never counts entities as out of camera. The camera entity, vehicles, boats, minecarts, display entities and firework rockets always tick fully.

### Persistent buffer mapping

Every write into a CPU-visible GPU buffer (dynamic transforms, uniforms, fog, clouds, vertex staging, Sodium's indirect command and uniform buffers) mapped the buffer's memory with `vmaMapMemory` and unmapped it again when the view closed, two native calls per write. A spark profile showed these calls at 2.8% of the render thread. Each buffer is now mapped the first time it is written and stays mapped until it is destroyed. Every mappable buffer is allocated host-visible and host-coherent, so the GPU sees the same bytes at the same addresses without any flush, and vanilla's check that a buffer is not closed while a view is open still works.

### Texel buffer views

Pushing descriptors that use a texel buffer (Sodium's section time buffer for every terrain layer, the cloud buffer) created a Vulkan buffer view on every push and queued it for destruction two frames later, 1.4% of the render thread. The view only depends on the buffer, offset, size and format, so it is now kept on the buffer (up to 8 per buffer) and destroyed together with the buffer, after the GPU is done with it.

### Pipeline switches

Switching pipelines inside a render pass re-applied every uniform ever set in that pass through a hash map pass, and checked the pipeline's color formats against the pass attachments again. Only the new pipeline's own uniforms are now applied, which gives the backend the same values, and the format check runs once per pipeline per render pass.

### Early present

The frame's single submit waits for the previous frame to finish on the GPU before Minecraft presents the finished frame. The finished frame is now presented right after it is submitted, before that wait, so the driver's present work overlaps the GPU finishing the previous frame. The same image is presented in the same order.

### Submit collections

The hand and screen effect passes drained their submit collections twice per frame and threw away the empty ones, so the next frame built them again with all their phases. Empty collections are now kept and reused; an empty phase draws nothing.

### GUI item atlas sweep

At the end of every frame the GUI item atlas walked all its slots looking for slots marked to be dropped after the frame. The sweep now only runs when a slot was marked since the last sweep.

### Sodium per-frame checks

Sodium read the operating system name to pick the sub-texel precision every frame and updated its chunk build time estimators every frame. The precision is computed once, and the estimators only update after new build results arrived; without new data the update leaves them unchanged.

### Viewport and scissor

Every render pass set the viewport and scissor again, even when the command buffer already had exactly those values from the previous pass. All pipelines declare both as dynamic state, which stays valid across render passes in the same command buffer, so a value that is already set is no longer recorded again. The remembered state is dropped whenever a new command buffer starts recording.

### Particle vertices

Particles were turned into vertices one at a time: a callback per particle, Sodium's per-particle hook with its own buffer lookup, and a separate vertex reservation for each quad. Each particle layer is now written in one pass straight from the stored particle arrays into one reservation, with the same corner math, colors, UVs and light, so the vertex data is identical. Buffers that cannot be written directly still go through the vanilla per-particle path.

### Model part rotation

Every rotated model part (entity limbs, heads, armor) rotated both its pose matrix and its normal matrix with JOML's `rotateZYX`, and each call computed the same three sines and cosines again. The angles are now turned into sines and cosines once and both matrices are rotated with JOML's own formulas, including its separate paths for identity, translation-only and affine matrices and the matrix property flags it sets. A test of 2,000,000 random matrices and angles against JOML 1.10.9 matched every bit.

### Item model bounds

Items on shelves and dropped items asked for their model's bounding box every frame, and because their render state is cleared or recreated every frame, vanilla rebuilt the box from every vertex of the model each time (about 1% of the render thread at a server spawn full of shelves). The box only depends on the model's extents, its item transform, the hand and its local transform, so single-layer items now keep it in a cache keyed by exactly those values. Items with Fabric mesh quads, which add their own vertices to the box, and multi-layer items still compute it every time.

### Region bookkeeping

Sodium walked its whole hash table of terrain regions every frame to let each region free GPU memory it no longer needs, and most of that time went into stepping over empty table slots (0.87% of the render thread in a singleplayer profile). Regions are now also kept in a plain list that is updated when a region is created or removed, and the per-frame pass walks that list. Every region is still visited once per frame; only the order differs, which only changes which free region id is handed out next.

### Raycast air

Every block a raycast passes through (the per-frame crosshair pick, the third-person camera collision) asked the block for its shape. Air never has a shape in any raycast mode, so air blocks now return the empty shape directly.

### Particle collision

Every particle with physics checked its movement against the blocks around it each tick by collecting every block collision shape in the swept box into a new list (up to 0.8% of the render thread at a server spawn full of dust particles). Particles have no entity colliders and no world border, so when every block in that box is air, vanilla returns the movement unchanged. The box is now first checked directly in the chunk sections, skipping all-air sections whole, and only boxes that contain any non-air block go through vanilla's collision.

### Animated sprite marks

Sodium only animates textures that are on screen, so every frame it marked every animated sprite in every visible section as active (0.85% of the render thread in a singleplayer profile). The mark is only cleared when the texture atlases advance their animations once per client tick, so marking the same sprites again in later frames of the same tick changes nothing. The marking pass now runs again only after an atlas animation step, a new set of render lists, or a change to any section's animated sprites.

### Translucent sort reuse

Translucent entity, item and text quads are sorted back to front before every upload, 3.4 to 4.7% of the render thread at busy server spawns. Each sort's result is now remembered by its position among the frame's sorts, and the next frame first checks in one pass whether the remembered order is still sorted under the new distances (ties ordered by quad index). If it is, it is exactly the order a stable sort would produce, so it is used without sorting. Otherwise the quads are sorted with the same orderings Sodium uses (signed keys up to 80 quads, unsigned above), using packed key and index values, a reused buffer, and skipping radix passes where every key lands in the same bucket. A test of 26,037 random sorts against Sodium's own sort matched every result.

### Transient command buffer

Every frame started a command buffer for copies into transient GPU memory and submitted it first, even though nothing in the game records such copies in a normal frame. That buffer is now only started when the first copy is recorded, and it is then placed first in the frame's submission, exactly where it would have been. Frames without such copies submit one command buffer fewer.

### Continued render passes

Ending a render pass recorded the end of the rendering instance and a full barrier right away, and the next pass began a new instance, even when it drew into exactly the same color and depth textures. The end of a pass is now held back. If the next pass uses the same attachments and render area, clears nothing and nothing else was recorded in between, it keeps drawing in the same rendering instance, as happens from the sky pass into the main world pass in clear weather. Draws inside one rendering instance keep their order for color, depth and blending, and a pass cannot sample its own attachments, so the image is the same. Anything else that records a command, ends the command buffer, or begins a different pass ends the held pass first, exactly as before. The GPU pass profiler disables this while it records.

### Frustum box test

Every entity, block entity and rain column visibility check asked JOML for a full box/frustum classification (outside, inside or intersecting), which tests each plane twice, and then only used whether the box was outside. The check now uses JOML's plain box test, which runs exactly the first of those two tests per plane in the same order, so the answer is identical with half the plane math.

### Dynamic transform equality

Each draw's model-view, color, offset and texture matrix are compared with the previously written ones to reuse the uniform slice. That record's generated `equals` went through method-handle dispatch on every comparison (0.2 to 0.4% of the render thread). It is now a plain comparison of the same four components with `Objects.equals`, the cheap vectors first, which returns the same answer.

### Memory

Less live memory means shorter and rarer garbage collections.

- Block state neighbor table: instead of every block state holding its own table of neighbor states, all states of a block share one bit-packed table that `setValue` indexes into. `getValue` keeps reading vanilla's per-state value array.
- Block state cache deduplication: identical collision shapes and face sturdiness arrays in the per-state cache are shared instead of stored once per state.
- Empty item component patches: items with no changed components share one empty map instead of each allocating their own.

## Build

```
gradlew build
```

Produces `build/libs/txoptimizations-26.3-1.0.0.jar` and copies it to `dist/`.

## Install

Drop `dist/txoptimizations-26.3-1.0.0.jar` into your Minecraft `mods/` folder.

Requires:

- Fabric Loader `>= 0.19.5`
- Minecraft `26.3`
- Java `25`
