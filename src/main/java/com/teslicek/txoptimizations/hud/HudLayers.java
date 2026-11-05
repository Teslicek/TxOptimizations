package com.teslicek.txoptimizations.hud;

import java.util.List;
import net.minecraft.resources.Identifier;

public final class HudLayers {

    public static final Identifier CAMERA_OVERLAYS     = Identifier.withDefaultNamespace("camera_overlays");
    public static final Identifier CROSSHAIR           = Identifier.withDefaultNamespace("crosshair");
    public static final Identifier HOTBAR              = Identifier.withDefaultNamespace("hotbar");
    public static final Identifier PLAYER_HEALTH       = Identifier.withDefaultNamespace("player_health");
    public static final Identifier VEHICLE_HEALTH      = Identifier.withDefaultNamespace("vehicle_health");
    public static final Identifier CONTEXTUAL_BACKDROP = Identifier.withDefaultNamespace("contextual_bar_background");
    public static final Identifier EXPERIENCE_LEVEL    = Identifier.withDefaultNamespace("experience_level");
    public static final Identifier CONTEXTUAL_BAR      = Identifier.withDefaultNamespace("contextual_bar");
    public static final Identifier SELECTED_ITEM       = Identifier.withDefaultNamespace("selected_item_name");
    public static final Identifier EFFECTS             = Identifier.withDefaultNamespace("effects");
    public static final Identifier BOSS_BAR            = Identifier.withDefaultNamespace("boss_bar");
    public static final Identifier SLEEP               = Identifier.withDefaultNamespace("sleep");
    public static final Identifier DEMO                = Identifier.withDefaultNamespace("demo");
    public static final Identifier SCOREBOARD          = Identifier.withDefaultNamespace("scoreboard");
    public static final Identifier OVERLAY_MESSAGE     = Identifier.withDefaultNamespace("overlay_message");
    public static final Identifier TITLE               = Identifier.withDefaultNamespace("title");
    public static final Identifier CHAT                = Identifier.withDefaultNamespace("chat");
    public static final Identifier TAB_LIST            = Identifier.withDefaultNamespace("tab_list");
    public static final Identifier SUBTITLES           = Identifier.withDefaultNamespace("subtitles");

    public static final List<Identifier> ORDER = List.of(CAMERA_OVERLAYS, CROSSHAIR, HOTBAR, PLAYER_HEALTH, VEHICLE_HEALTH, CONTEXTUAL_BACKDROP, EXPERIENCE_LEVEL, CONTEXTUAL_BAR, SELECTED_ITEM, EFFECTS, BOSS_BAR, SLEEP, DEMO, SCOREBOARD, OVERLAY_MESSAGE, TITLE, CHAT, TAB_LIST, SUBTITLES);

    private HudLayers() {
    }
}
