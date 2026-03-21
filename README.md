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

So the specification for the `.cw` file can be found [here](https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/ClassicWorld_file_format).

So this is big-endian NBT. I think big endian just means the binary representation of a regular mathematical binary number.

Yeah so first we must learn how to parse the Named Binary Tag data structure, although, we could just use a library to do it. Maybe we'll do that later, but we'll try to parse it manually first.

So an NBT file isn't too complicated to parse, it's almost like a JSON, but not human readable, meaning that maybe it's easier to parse manually.

In principle, all we have to do is:

- get the first byte, which is the tag ID
- the tag ID correspond to some data type according to [this table](https://minecraft.fandom.com/wiki/NBT_format#:~:text=Binary%20format,-An)
- if it's a primitive type, like kinda basic (bytes, ints, short, long, float...), simply just read the next few bytes according to the size of the data type.
- if it's like a byte array, read the next 4 bytes (which represents the size of the byte array), then read however that amount of bytes
- if it's a string, read the next next 2 bytes (interpret it as a short, which representation the size of the string), and then read the next few bytes and interpret it as like, a UTF-8 string
- if it's a list, then the next 1 byte is a tag denoting the type, the next 4 byte will denote the size of the list, then just read `size_of_list * size_of_datatype` amount of bytes, and interpret it as so
- if it's a compound tag, the next thing that will come is a string (its name or whatever, so just interpret it as before), and then just a bunch of tag id + payload.
- if it's an integer array, just get the next 4 bytes as a signed integer which represents its size, then just go from there (an integer is 4 bytes long).
- if it's an long array, similarly, the size is a signed integer of 4 bytes, then just the data.

But honestly, since we're kinda short of time, we'll use an external library to parse it.
