---@meta

---@alias ServerLevel userdata # opaque handle to a Minecraft ServerLevel
---@alias DifficultyName
---| "peaceful"
---| "easy"
---| "normal"
---| "hard"
---@alias SoundCategory
---| "master"
---| "music"
---| "record"
---| "weather"
---| "block"
---| "hostile"
---| "neutral"
---| "player"
---| "ambient"
---| "voice"

---@class world
world = {}

--- Creates an explosion in the world.
---@param level ServerLevel
---@param x number
---@param y number
---@param z number
---@param power number
---@param fire? boolean # defaults to false
---@param breakBlocks? boolean # defaults to true
function world.explode(level, x, y, z, power, fire, breakBlocks) end

--- Plays a sound at a position. Broadcasts to every player in range.
---@param level ServerLevel
---@param x number
---@param y number
---@param z number
---@param soundId string # e.g. "minecraft:entity.generic.explode"
---@param volume? number
---@param pitch? number
---@param category? SoundCategory
function world.playSound(level, x, y, z, soundId, volume, pitch, category) end

--- Spawns a particle at the given position in the given level
---@param level ServerLevel
---@param particleId string
---@param x number
---@param y number
---@param z number
---@param count integer
---@param dx? number
---@param dy? number
---@param dz? number
---@param speed? number
function world.spawnParticles(level, particleId, x, y, z, count, dx, dy, dz, speed) end

--- Returns the level's current game time, in ticks.
---@param level ServerLevel
---@return integer time
function world.getTime(level) end

--- Returns whether it is currently raining in the level.
---@param level ServerLevel
---@return boolean raining
function world.isRaining(level) end

--- Returns whether it is currently thundering in the level.
---@param level ServerLevel
---@return boolean thundering
function world.isThundering(level) end

--- Sets the level's difficulty.
---@param level ServerLevel
---@param difficulty DifficultyName
function world.setDifficulty(level, difficulty) end

--- Returns the level's current difficulty.
---@param level ServerLevel
---@return DifficultyName difficulty
function world.getDifficulty(level) end

return world