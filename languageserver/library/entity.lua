---@meta

---@class entity
entity = {}

---Looks up an entity by its UUID in a given level. Returns nil if not found.
---@param level ServerLevel
---@param uuid string
---@return Entity|nil
function entity.getByUUID(level, uuid) end

---@param e Entity
---@return ServerLevel
function entity.getWorld(e) end

---@param e Entity
---@return number x
---@return number y
---@return number z
function entity.getVelocity(e) end

---@param e Entity
---@param x number
---@param y number
---@param z number
function entity.setVelocity(e, x, y, z) end

---Adds to the entity's current velocity (does not replace it).
---@param e Entity
---@param x number
---@param y number
---@param z number
function entity.addVelocity(e, x, y, z) end

---@param e Entity
---@return number x
---@return number y
---@return number z
function entity.getPosition(e) end

---Teleports the entity to an absolute position.
---@param e Entity
---@param x number
---@param y number
---@param z number
function entity.setPosition(e, x, y, z) end

---@param e Entity
---@return string uuid
function entity.getUUID(e) end

---Kills the entity outright.
---@param e Entity
function entity.kill(e) end

---Returns the item in the entity's main hand. Only meaningful for LivingEntity
---(mobs/players); returns nil for non-living entities (item entities, projectiles, etc).
---@param e Entity
---@return ItemStack|nil
function entity.getHeldItem(e) end

