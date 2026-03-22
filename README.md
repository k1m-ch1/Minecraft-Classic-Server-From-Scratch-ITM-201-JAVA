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

We'll use `Querz/NBT` with docs from [here](https://github.com/Querz/NBT)

So from the `.cw` specification, there are a lot of fields, but one of the most important one for us is the `BlockArray` compound tag.


```
00000000  00 07 6b 31 6d 63 68 31  20 20 20 20 20 20 20 20  |..k1mch1        |
00000010  20 20 20 20 20 20 20 20  20 20 20 20 20 20 20 20  |                |
*
00000040  20 20 28 6e 6f 6e 65 29  20 20 20 20 20 20 20 20  |  (none)        |
00000050  20 20 20 20 20 20 20 20  20 20 20 20 20 20 20 20  |                |
*
00000080  20 20 42                                          |  B|
00000083
```

So according to [this](https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Classic_Protocol#Server_.E2.86.92_Client_packets) docs, a packet is of the following format:

- 1 byte of packet ID
- some stuff

I don't know why this was so hard to find but [this](https://omniarchive.uk/archive/java/server/classic/) is the actual minecraft classic server (that still works btw).

to run it, do: 

```
java -cp ./c1.10.jar com.mojang.minecraft.server.MinecraftServer
```

Minecraft classic block type id is linked [here](https://minecraft.fandom.com/wiki/Java_Edition_data_values/Classic)

## the 0x03 packet id from server to client

So for this packet id is special since it contains all of the data for the world (no chunk loading logic yet).

So we get the following:

- 1 byte for packet ID
- 2 bytes for chunk length as a short
- 1024 bytes for chunk data
- 1 byte as a "percent complete"

Now after the `0x02` packet, we'll actually get a series of `0x03` packets.

We stitch all the chunk data from the `0x03` packets to get one big gzipped block array.

You can quickly verify that it's gzipped (meant to be gzipped) by checking the first 2 bytes to see that it's `0x1f8b`

## Block array

Ok, if you `gunzip` the gzipped block array, you'll get something that's almost a block array.

Basically, you need to strip the first 4 bytes which represents an integer of how big the block array is.

```
$ stat -c %s ./block_data
4194308
```

```
>>> 256 * 64 * 256
4194304
```

So if we strip the first 4 bytes of the concatenated block data, you'll get the same result.

In fact, the first 4 bytes interpreted as an `int` also gives `4194304`.

The size can be verified by doing `x*y*z` of the `0x04` packet and seeing that they're indeed the same.

So for a flat block array, we get the block type by doing:

```
index = x + (z * width) + (y * width * depth)
```

where y is actually the altitude (z axis in normal math).

so it's XZY and it's right handed

```
        Y (height)
        ↑
        |
        |
        +------→ X (width)
       /
      /
     Z (depth)
```

So bascially, if we have `(x,y,z)`, we're writing the blocks in this order:

```
(0, 0, 0), (1, 0, 0), (2, 0, 0),... ,(width - 1, 0, 0),
(0, 0, 1), (1, 0, 1), (2, 0, 1), ... ,(width - 1, 0, 1),
(0, 0, 2), (1, 0, 2), (2, 0, 2), ... ,(width - 1, 0, 2),
...
(0, 0, depth - 1), (1, 0, depth - 1), (2, 0, depth - 1), ... ,(width - 1, 0, depth - 1),
(0, 1, 0), (0, 1, 0), (0, 1, 0), ... ,(0, 1, 0),
...
```

It's like using a different base for each digit.

It's like:

```
index = x + width*z + width*depth*y
```

So we just do that until we reach the end of the byte array. Each byte of the array represents a block type, according to this table [linked here](https://minecraft.fandom.com/wiki/Java_Edition_data_values/Classic).
