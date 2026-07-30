---@meta

---@class Entity : userdata
---A live Minecraft entity (any kind - mob, item entity, projectile, player, etc).

---@class ServerPlayer : Entity
---A connected, online player. Also valid anywhere `Entity` is expected.

---@class ServerLevel : userdata
---A Minecraft dimension/world (overworld, the_nether, the_end).

---@class ItemStack : userdata
---A Minecraft item stack (count + item type + components/NBT-ish data).

---@class Menu : userdata
---A Daedalus-created chest GUI menu (`DaedalusChestMenu`), returned by `gui.open`.
