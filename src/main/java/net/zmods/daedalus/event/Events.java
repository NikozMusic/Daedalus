package net.zmods.daedalus.event;

public enum Events {
    // Global events
    TICK("tick"),
    PLAYER_JOIN("playerJoin"),
    PLAYER_LEAVE("playerLeave"),
    PLAYER_DEATH("playerDeath"),
    PLAYER_RESPAWN("playerRespawn"),
    PLAYER_CHAT("playerChat"),
    PLAYER_ATTACK_ENTITY("playerAttackEntity"),
    PLAYER_INTERACT_ENTITY("playerInteractEntity"),

    // Server lifecycle events
    SERVER_START("serverStart"),
    SERVER_STOP("serverStop"),

    // Entity events
    ENTITY_JUMP("entityJump"),
    ENTITY_DAMAGE("entityDamage"),
    ENTITY_DEATH("entityDeath"),
    ENTITY_HURT("entityHurt"), // cancellable - fires before damage is applied
    ENTITY_HEAL("entityHeal"),
    ENTITY_MOVE("entityMove"), // players only, threshold-gated
    ENTITY_LOAD("entityLoad"), // fires on spawn AND on chunk load - not spawn-exclusive

    // Block events
    BLOCK_BREAK("blockBreak"),
    BLOCK_PLACE("blockPlace"),
    BLOCK_INTERACT("blockInteract"),

    // Item events
    ITEM_USE("itemUse"),
    ITEM_PICKUP("itemPickup"),
    ITEM_DROP("itemDrop");

    public final String id;

    Events(String id) {
        this.id = id;
    }

    public static Events fromId(String id) {
        for (Events e : Events.values()) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }
}