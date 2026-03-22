
def read_bytes(fd, n):
    return fd.read(n*2)

def server_identification(fd):
    print("protocol version: ", read_bytes(fd, 1))
    print("server name(string): ", bytes.fromhex(read_bytes(fd, 64)))
    print("server message of the day (string): ", bytes.fromhex(read_bytes(fd, 64)))

def get_chunk(fd):
    # remember, if chunk_length < 1024, then it is padded with 0
    chunk_length = int(read_bytes(fd, 2), 16)
    print("chunk length: ", chunk_length)
    chunk_data = read_bytes(fd, 1024)
    print("percent complete: ", int(read_bytes(fd, 1), 16))
    return chunk_data, chunk_length


if __name__ == "__main__":
    with open("./server_response_filtered", 'r') as file:

        # server identification
        print("packet id: ", read_bytes(file, 1))
        server_identification(file)
        
        # 00 is normal user, 64 is admin
        print("user type: ", read_bytes(file, 1))
        
        # next packet id
        print("packet id: ", read_bytes(file, 1)) # this is level initialize
        
        # next packet id of 3
        # and a series of packet 3 i assume
        
        chunk_data = ""

        prev_id = ""
        while (prev_id:=read_bytes(file, 1)) == "03":
            chunk_data += get_chunk(file)[0]

        # we'll need to learn how to intepret this chunk data too
        print(len(chunk_data)/2)


        # we should expect a packet id of 4 now...         
        print("packet id: ", prev_id)

        # so we need to get the x, y and z coordinates
        print("x: ", int(read_bytes(file, 2), 16))
        print("y: ", int(read_bytes(file, 2), 16))
        print("z: ", int(read_bytes(file, 2), 16))

        # spawn player
        print("packet id: ", read_bytes(file, 1))

        # we get a signed byte
        print("player id: ", read_bytes(file, 1))

        # we get the player name
        print("player name: ", bytes.fromhex(read_bytes(file, 64)))

        # we get some coords (as a FShort) which is a fixed point short
        print("x: ", read_bytes(file, 2))
        print("y: ", read_bytes(file, 2))
        print("z: ", read_bytes(file, 2))

        # we get yaw and pitch
        print("yaw: ", int(read_bytes(file, 1), 16))
        print("pitch: ", int(read_bytes(file, 1), 16))

        # now we should get some sort of message
        print("packet id: ", read_bytes(file, 1))
        print("packet id: ", read_bytes(file, 1))

        # idk why 2 pings

        # now we should get some sort of message
        print("packet id: ", read_bytes(file, 1))
        print("player id: ", read_bytes(file, 1))
        print("message: ", bytes.fromhex(read_bytes(file, 64)))

        print("packet id: ", read_bytes(file, 1))
        print("packet id: ", read_bytes(file, 1))
        print("packet id: ", read_bytes(file, 1) == '')

        # now let's try to parse the chunk data
        
        # so it is gzipped
        print(chunk_data[:4])

        # i'll save it as a file for now
        chunk_data_bytes = bytes.fromhex(chunk_data)

    with open("./chunk_data.gz", 'wb') as chunk_data_file:
        chunk_data_file.write(chunk_data_bytes)
