---@meta

---@alias DaedalusEventId
---| "tick"
---| "playerJoin"
---| "playerLeave"
---| "playerDeath"
---| "playerRespawn"
---| "playerChat"
---| "entityJump"
---| "entityDamage"
---| "entityDeath"
---| "entityHurt"
---| "entityHeal"
---| "entityMove"
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
---@field ENTITY_JUMP "entityJump"
---@field ENTITY_DAMAGE "entityDamage"
---@field ENTITY_DEATH "entityDeath"
---@field ENTITY_HURT "entityHurt"
---@field ENTITY_HEAL "entityHeal"
---@field ENTITY_MOVE "entityMove"
---@field BLOCK_BREAK "blockBreak"
---@field BLOCK_PLACE "blockPlace"
---@field BLOCK_INTERACT "blockInteract"
---@field ITEM_USE "itemUse"
---@field ITEM_PICKUP "itemPickup"
---@field ITEM_DROP "itemDrop"

---@class event
---@field Events DaedalusEvents
event = {}

---Binds a global (server-wide) listener for the given module. See the
---signature table above for what args your function receives per event.
---@param eventId DaedalusEventId
---@param callback fun(...: any)
function events.bindGlobal(eventId, callback) end

---Binds a listener scoped to a single entity instance.
---@param e Entity
---@param eventId DaedalusEventId
---@param callback fun(...: any)
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
---@param callback fun(...: any)
function events.once(eventId, callback) end
