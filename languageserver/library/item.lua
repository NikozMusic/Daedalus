---@meta

---@class item
item = {}

---Creates a new item stack. Errors if `itemId` is invalid/unknown.
---@param itemId string  # e.g. "minecraft:diamond_sword"
---@param count integer
---@return ItemStack
function item.createItem(itemId, count) end

---@param stack ItemStack
---@return string itemId
function item.getId(stack) end

---@param stack ItemStack
---@return boolean
function item.isEmpty(stack) end

---@param stack ItemStack
---@return integer
function item.getCount(stack) end

---@param stack ItemStack
---@param count integer
function item.setCount(stack, count) end

---@param stack ItemStack
---@return integer
function item.getMaxStackSize(stack) end

---Returns a new, independent copy of the stack.
---@param stack ItemStack
---@return ItemStack
function item.copy(stack) end

---@param stack ItemStack
---@return boolean
function item.isDamageable(stack) end

---@param stack ItemStack
---@return integer damage
function item.getDamage(stack) end

---@param stack ItemStack
---@param damage integer
function item.setDamage(stack, damage) end

---@param stack ItemStack
---@return integer
function item.getMaxDamage(stack) end

---Returns true if both stacks are the same item type with the same components,
---ignoring stack count.
---@param a ItemStack
---@param b ItemStack
---@return boolean
function item.matches(a, b) end
