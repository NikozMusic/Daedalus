---@meta

---@alias DaedalusClickType
---| "left"
---| "right"
---| "shift_left"
---| "shift_right"
---| "hotbar_swap"
---| "middle"
---| "drop"
---| "drop_all"
---| "drag"
---| "double_click"
---| "other"

---@class gui
gui = {}

---Opens a Daedalus-controlled chest GUI for a player. Vanilla item
---transfer/pickup/drag is suppressed entirely in these menus - use
---`gui.onClick` to implement your own click behavior.
---@param player ServerPlayer
---@param title string
---@param rows integer
---@return Menu
function gui.open(player, title, rows) end

---Returns the menu the player currently has open, or nil if it's just
---their own inventory (or nothing).
---@param player ServerPlayer/nil
---@return Menu|nil
function gui.getOpen(player) end

---@param player ServerPlayer
---@return string|nil
function gui.getTitle(player) end

---Closes whatever container the player currently has open.
---@param player ServerPlayer
function gui.close(player) end

---@param menu Menu
---@param slot integer  # 0-indexed
---@param stack ItemStack
function gui.setItem(menu, slot, stack) end

---@param menu Menu
---@param slot integer  # 0-indexed
---@return ItemStack|nil
function gui.getItem(menu, slot) end

---@param menu Menu
---@param slot integer  # 0-indexed
function gui.clearItem(menu, slot) end

---@param menu Menu
---@return integer
function gui.getSlotCount(menu) end

---Registers a click handler for a Daedalus menu created via `gui.open`.
---@param menu Menu
---@param callback fun(player: ServerPlayer, slot: integer, clickType: DaedalusClickType)
function gui.onClick(menu, callback) end
