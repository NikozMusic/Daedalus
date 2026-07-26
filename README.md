# Daedalus Runtime

Daedalus Runtime is a Lua-based replacement for Minecraft's datapack scripting system based on Luaj.

Instead of writing large chains of commands and `.mcfunction` files, Daedalus allows you to create 'Modules' that are bundles of Lua files that fill the same role but much faster and with way cleaner syntax.

The best part is just like datapacks Daedalus is 100% serverside meaning players don't need any mods to connect.

### Notable Features

- Event-based programming that lets you bind lua functions to specific in-game events, such as the gametick or a player jumping or anything else you can think of.
- Faster execution than command-based logic
- Simple APIs for interacting with Minecraft
- Built in multithreading
- Modular by design

## Documentation
Full API documentation can be found here:

[Daedalus Docs](https://docs.comming.soon.were.still.in.beta.sorry)

## Example

```lua
events.on("tick", function()
    print("Hello from Daedalus!")
end)