---@meta

---@class players
players = {}

---@param name string
---@return string|nil uuid
function players.getUUIDByName(name) end

---Looks up an online player by username. Returns nil if not online.
---@param name string
---@return ServerPlayer|nil
function players.getByName(name) end

---@param name string
---@return boolean
function players.isOnline(name) end

---@param player ServerPlayer
---@return string username
function players.getName(player) end

---Disconnects a player from the server.
---@param player ServerPlayer
---@param reason? string  # defaults to "Kicked by an operator."
function players.kick(player, reason) end

---Sends a plain-text system message to the player's chat.
---For rich/MiniMessage text use `text.send` instead.
---@param player ServerPlayer
---@param message string
function players.sendMessage(player, message) end

---@param player ServerPlayer
---@return integer ms
function players.getPing(player) end

---@param player ServerPlayer
---@return "survival"|"creative"|"adventure"|"spectator"
function players.getGameMode(player) end

---@param player ServerPlayer
---@param mode "survival"|"creative"|"adventure"|"spectator"
function players.setGameMode(player, mode) end

-- NOTE: players.isOp is currently disabled/commented out in the Java source
-- and is not available at runtime.
