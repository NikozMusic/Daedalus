---@meta

--[[
  raycast - block, entity, and combined raycasting.

  Every function returns a single result table shaped like:
    {
      hit = boolean,
      type = "block"|"entity"|"none",
      x = number, y = number, z = number,  -- world-space hit position
      distance = number,                   -- distance from the ray's start

      blockId = string,  -- only present when type == "block"
      face = string,     -- only present when type == "block" (e.g. "up", "north")

      entity = Entity,   -- only present when type == "entity"
    }
]]

---@class RaycastResult
---@field hit boolean
---@field type "block"|"entity"|"none"
---@field x number
---@field y number
---@field z number
---@field distance number
---@field blockId string|nil
---@field face string|nil
---@field entity Entity|nil

---@class raycast
raycast = {}

---Casts a ray and checks for block collisions only.
---@param level ServerLevel
---@param x1 number
---@param y1 number
---@param z1 number
---@param x2 number
---@param y2 number
---@param z2 number
---@param includeFluids? boolean  # default false - treat fluids as solid for the cast
---@return RaycastResult
function raycast.blockRay(level, x1, y1, z1, x2, y2, z2, includeFluids) end

---Casts a ray and checks for entity collisions only (closest hit wins).
---@param level ServerLevel
---@param x1 number
---@param y1 number
---@param z1 number
---@param x2 number
---@param y2 number
---@param z2 number
---@param exclude? Entity  # an entity to exclude from consideration (e.g. the shooter)
---@return RaycastResult
function raycast.entityRay(level, x1, y1, z1, x2, y2, z2, exclude) end

---Casts a ray checking both blocks and entities, returning whichever is hit first.
---@param level ServerLevel
---@param x1 number
---@param y1 number
---@param z1 number
---@param x2 number
---@param y2 number
---@param z2 number
---@param exclude? Entity
---@param includeFluids? boolean  # default false
---@param includeEntities? boolean  # default true
---@return RaycastResult
function raycast.ray(level, x1, y1, z1, x2, y2, z2, exclude, includeFluids, includeEntities) end

---Convenience cast from an entity's eyes along its current look direction.
---Automatically excludes the source entity from entity hits.
---@param e Entity
---@param distance number
---@param includeEntities? boolean  # default true
---@param includeFluids? boolean  # default false
---@return RaycastResult
function raycast.rayFromEntity(e, distance, includeEntities, includeFluids) end