---@meta

--[[
  text - MiniMessage-based rich text. See https://docs.advntr.dev/minimessage/
  for tag syntax (e.g. "<red>Hello</red>", "<gradient:red:blue>Hi!</gradient>").
]]

---@class text
text = {}

---Deserializes a MiniMessage string and returns its plain text with all
---formatting tags removed.
---@param input string
---@return string plain
function text.strip(input) end

---Deserializes a MiniMessage string and sends it to the player as a
---formatted system message.
---@param player ServerPlayer
---@param input string
function text.send(player, input) end


---Parses text into minimessage compatible text
---@param text string
function text.parse(text) end
