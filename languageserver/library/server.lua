---@meta

---@class server
server = {}

---@return table<integer, ServerPlayer>
function server.getOnlinePlayers() end

---Stops the Minecraft server (`server.halt(false)`).
---
---NOTE: as of the current source this is NOT gated behind a config flag
---(unlike the planned `allowDeepSearch`-style gating mentioned for future
---dangerous ServerApi additions) - calling it will always attempt to stop
---the server.
function server.stop() end
