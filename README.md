# References

- [Our bible (minecraft wiki)](https://minecraft.wiki/w/Minecraft_Wiki:Protocol_documentation#:~:text=Minecraft%3A%20Java%20Edition%20Classic)

- [List of servers (and a list of people to bother if I get really stuck lol)](https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Classic_Server_List)

- fCraft for minecraft world conversion

- [block types](https://minecraft.wiki/w/Java_Edition_Classic_data_values)

# What we'll use

- `.cw` for the file 

# Notes

## Parsing a minecraft world file

So since we're using a `.cw` file, here's pipeline

- So a `.cw` file is actually a gzip file of some other uncompressed file
- that uncompressed file has a scheme that we gotta learn (like NBT)

