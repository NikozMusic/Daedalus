---@meta

---@alias DaedalusLevelName "overworld"|"nether"|"end"|"all"

---@class command
command = {}

---Runs a command as an anonymous console-like source anchored to the
---overworld. Output that the command would normally print is captured and
---returned instead of being broadcast.
---@param cmd string  # leading "/" is optional
---@return integer result  # Brigadier result code (0 = failure)
---@return string output
function command.execute(cmd) end

---Runs a command as if run by the given entity (position + dimension are
---taken from the entity).
---@param e Entity
---@param cmd string
---@return integer result
---@return string output
function command.executeAs(e, cmd) end

---Runs a command anchored to a specific dimension (or all three, in which
---case the result is the max of all three runs and output is concatenated).
---@param levelName DaedalusLevelName
---@param cmd string
---@return integer result
---@return string output
function command.executeIn(levelName, cmd) end

---Runs a command at a specific position in the overworld.
---@param x number
---@param y number
---@param z number
---@param cmd string
---@return integer result
---@return string output
---@overload fun(levelName: DaedalusLevelName, x: number, y: number, z: number, cmd: string): integer, string
function command.executeAt(x, y, z, cmd) end
