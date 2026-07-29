---@meta

---@class minecraft
minecraft = {}

---Logs a message to the server console with a `[Lua]` prefix.
---@param msg string
function minecraft.log(msg) end

---Returns the currently installed Daedalus mod version (e.g. "0.15.0"), or "Unknown".
---@return string version
function minecraft.getDaedalusVersion() end

---Returns the running Minecraft version, or "Unknown".
---@return string version
function minecraft.getMinecraftVersion() end

---Logs a warning-level message via the Daedalus logger.
---@param msg any
function minecraft.warn(msg) end

---Logs an error-level message via the Daedalus logger.
---@param msg any
function minecraft.error(msg) end

---@param time integer
function minecraft.sleep(time) end
