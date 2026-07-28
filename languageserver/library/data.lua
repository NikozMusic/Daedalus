---@meta

--[[
  data - persistent, per-module-namespaced key/value storage attached to
  entities (Fabric Data Attachment API) and items (DataComponents.CUSTOM_DATA).

  Paths are dot-separated (e.g. "stats.strength") and are automatically
  namespaced under the calling module's id, so two modules can use the same
  path without colliding.
]]

---@class data
data = {}

-- -- Entity data --

---@param e Entity
---@param path string
---@param value any
function data.entitySet(e, path, value) end

---@param e Entity
---@param path string
---@return any
function data.entityGet(e, path) end

---@param e Entity
---@param path string
---@return boolean
function data.entityHas(e, path) end

---@param e Entity
---@param path string
function data.entityRemove(e, path) end

---Lists the top-level keys stored for this module's namespace on the entity.
---@param e Entity
---@return table<integer, string>
function data.entityList(e) end

---Clears all data this module has stored on the entity.
---@param e Entity
function data.entityClear(e) end

-- -- Item data --

---@param stack ItemStack
---@param path string
---@param value any
function data.itemSet(stack, path, value) end

---@param stack ItemStack
---@param path string
---@return any
function data.itemGet(stack, path) end

---@param stack ItemStack
---@param path string
---@return boolean
function data.itemHas(stack, path) end

---@param stack ItemStack
---@param path string
function data.itemRemove(stack, path) end

---@param stack ItemStack
---@return table<integer, string>
function data.itemList(stack) end

---@param stack ItemStack
function data.itemClear(stack) end
