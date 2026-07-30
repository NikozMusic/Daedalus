---@meta

---@alias DaedalusEventId
---| "tick"
---| "playerJoin"
---| "playerLeave"
---| "playerDeath"
---| "playerRespawn"
---| "playerChat"
---| "playerAttackEntity"
---| "playerInteractEntity"
---| "serverStart"
---| "serverStop"
---| "entityJump"
---| "entityDamage"
---| "entityDeath"
---| "entityHurt"
---| "entityHeal"
---| "entityMove"
---| "entityLoad"
---| "blockBreak"
---| "blockPlace"
---| "blockInteract"
---| "itemUse"
---| "itemPickup"
---| "itemDrop"

---@class DaedalusEvents
---@field TICK "tick"
---@field PLAYER_JOIN "playerJoin"
---@field PLAYER_LEAVE "playerLeave"
---@field PLAYER_DEATH "playerDeath"
---@field PLAYER_RESPAWN "playerRespawn"
---@field PLAYER_CHAT "playerChat"
---@field PLAYER_ATTACK_ENTITY "playerAttackEntity"
---@field PLAYER_INTERACT_ENTITY "playerInteractEntity"
---@field SERVER_START "serverStart"
---@field SERVER_STOP "serverStop"
---@field ENTITY_JUMP "entityJump"
---@field ENTITY_DAMAGE "entityDamage"
---@field ENTITY_DEATH "entityDeath"
---@field ENTITY_HURT "entityHurt"
---@field ENTITY_HEAL "entityHeal"
---@field ENTITY_MOVE "entityMove"
---@field ENTITY_LOAD "entityLoad"
---@field BLOCK_BREAK "blockBreak"
---@field BLOCK_PLACE "blockPlace"
---@field BLOCK_INTERACT "blockInteract"
---@field ITEM_USE "itemUse"
---@field ITEM_PICKUP "itemPickup"
---@field ITEM_DROP "itemDrop"

--[[
  events - the event system. Bind a Lua function to a global or per-entity
  event via events.Events.<NAME>.

  Callback signatures by event:
    tick                 -> ()
    playerJoin           -> (player: ServerPlayer)
    playerLeave          -> (player: ServerPlayer)
    playerDeath          -> (player: ServerPlayer)
    playerRespawn        -> (player: ServerPlayer)
    playerChat           -> (player: ServerPlayer, message: string)
    playerAttackEntity   -> (player: ServerPlayer, target: Entity)
    playerInteractEntity -> (player: ServerPlayer, target: Entity)
    serverStart          -> ()
    serverStop           -> ()
    entityJump           -> (e: Entity)
    entityDamage         -> (e: Entity, amount: number)          -- post-mitigation, after damage applied
    entityDeath          -> (e: Entity)
    entityHurt           -> (e: Entity, amount: number)          -- CANCELLABLE, see below
    entityHeal           -> (e: Entity, amount: number)
    entityMove           -> (e: Entity, x: number, y: number, z: number) -- players only, threshold-gated
    entityLoad           -> (e: Entity)                          -- fires on spawn AND chunk load, not spawn-exclusive
    blockBreak           -> (player: ServerPlayer, x: integer, y: integer, z: integer, blockId: string)
    blockPlace           -> (player: ServerPlayer, x: integer, y: integer, z: integer, blockId: string)
    blockInteract        -> (player: ServerPlayer, x: integer, y: integer, z: integer) -- may fire twice per click (main + off hand)
    itemUse              -> (player: ServerPlayer, stack: ItemStack)
    itemPickup           -> (player: ServerPlayer, stack: ItemStack)
    itemDrop             -> (player: ServerPlayer, stack: ItemStack)
]]

---@class events
---@field Events DaedalusEvents
events = {}

---Binds a global (server-wide) listener for the given module. See the
---signature table above for what args your function receives per event.
---For "entityHurt", return `false` from the callback to cancel the damage.
---@param eventId DaedalusEventId
---@param callback fun(...: any): boolean|nil
function events.bindGlobal(eventId, callback) end

---Binds a listener scoped to a single entity instance.
---For "entityHurt", return `false` from the callback to cancel the damage.
---@param e Entity
---@param eventId DaedalusEventId
---@param callback fun(...: any): boolean|nil
function events.bindEntity(e, eventId, callback) end

---Removes this module's global listener(s) for the given event.
---@param eventId DaedalusEventId
function events.unbindGlobal(eventId) end

---Removes this module's listener(s) for the given event on this entity.
---@param e Entity
---@param eventId DaedalusEventId
function events.unbindEntity(e, eventId) end

---Binds a global listener that automatically unbinds itself after firing once.
---@param eventId DaedalusEventId
---@param callback fun(...: any): boolean|nil
function events.once(eventId, callback) end

---@overload fun(selector:string): Entity[]
---@overload fun(anchor:Entity, selector:string): Entity[]
---@param source CommandSourceStack
---@param selector string
---@return Entity[]
function entity.getBySelector(source, selector) end