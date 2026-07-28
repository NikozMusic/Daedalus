---@meta

---Loads and executes another .lua file from this same module by its
---(extension-less) relative path, e.g. `require("util/math")`.
---@param name string
---@return any
function require(name) end

---The entity that ran the command, if any (console-invoked commands have no
---sender). Only present in command-file scope.
---@type Entity|nil
sender = nil

---Parsed command arguments, 1-indexed. Each token is auto-classified into a
---player/entity userdata (for @s @p @r @a @e selectors or an online
---player's name), boolean, number, or plain string. Only present in
---command-file scope.
---@type table<integer, Entity|ServerPlayer|table|boolean|number|string>
args = nil

---Number of parsed tokens in `args`. Only present in command-file scope.
---@type integer
argCount = nil

---The raw, unparsed argument string as typed after the command name (empty
---string if none were given). Only present in command-file scope.
---@type string
rawArgs = nil
