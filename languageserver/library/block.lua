---@meta

---@class block
block = {}

---Gets the block id at a position (e.g. "minecraft:stone").
---@param level ServerLevel
---@param x integer
---@param y integer
---@param z integer
---@return string blockId
function block.get(level, x, y, z) end

---Sets the block at a position. Errors if `blockId` is invalid or unknown.
---@param level ServerLevel
---@param x integer
---@param y integer
---@param z integer
---@param blockId string  # e.g. "minecraft:stone"
function block.set(level, x, y, z, blockId) end

---@param level ServerLevel
---@param x integer
---@param y integer
---@param z integer
---@return boolean isAir
function block.isAir(level, x, y, z) end

---Naturally destroys the block (drops items, plays effects), as if broken by survival play.
---
---NOTE: `break` is a reserved word in Lua and cannot be used with dot-syntax.
---You must call it with bracket/index syntax: `block["break"](level, x, y, z)`.
---@param level ServerLevel
---@param x integer
---@param y integer
---@param z integer
function block["break"](level, x, y, z) end

---Returns the combined block/sky light level at a position, 0-15.
---@param level ServerLevel
---@param x integer
---@param y integer
---@param z integer
---@return integer light
function block.getLight(level, x, y, z) end

---@param level ServerLevel
---@param x integer
---@param y integer
---@param z integer
---@return boolean canSeeSky
function block.canSeeSky(level, x, y, z) end
