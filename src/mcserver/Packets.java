package mcserver;

interface Packet {}

class Ping implements Packet{
}

class SetBlock implements Packet{
  short x, y, z;
  byte block;

  SetBlock(short x, short y, short z, byte block){
    this.x = x;
    this.y = y;
    this.z = z;
    this.block = block;
  }
}

class Message implements Packet{
  byte playerID;
  byte[] playerName; // add an extra parameter to make formatting chat a bit easier
  byte[] text;

  Message(byte playerID, byte[] playerName, byte[] text){
    this.playerID = playerID;
    this.playerName = playerName; // add an extra parameter to make formatting chat a bit easier
    this.text = text;
  }
}

class SpawnPlayer implements Packet{
  byte playerID;
  byte[] playerName;
  short x;
  short y;
  short z;
  byte yaw; // also known as heading
  byte pitch;

  SpawnPlayer(
    byte playerID,
    byte[] playerName,
    short x,
    short y,
    short z,
    byte yaw,
    byte pitch
  ){
    this.playerID = playerID;
    this.playerName = playerName;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
  }
}

class PositionAndOrientation implements Packet{
  byte playerID;
  short x;
  short y;
  short z;
  byte yaw;
  byte pitch;

  PositionAndOrientation(
    byte playerID,
    short x,
    short y,
    short z,
    byte yaw,
    byte pitch
  ){
    this.playerID = playerID;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
  }
}

class DespawnPlayer implements Packet{
  byte playerID;
  DespawnPlayer(byte playerID){
    this.playerID = playerID;
  }
}

// from here, there's just going to be some miscellaneous packets to communicate between the world and the client

class WorldRequest implements Packet{
  byte playerID;
  byte[] playerName;
  WorldRequest(byte playerID, byte[] playerName){
    this.playerID = playerID;
    this.playerName = playerName;
  }
}

class WorldResponse implements Packet{
  // this contains info for both 0x03 (level data chunks) and 0x04 (level initialize) packets 
  byte[] blockArray;
  short x;
  short y;
  short z;
  WorldResponse(byte[] blockArray, short x, short y, short z){
    this.blockArray = blockArray;
    // x, y, and z refers to the world size
    this.x = x;
    this.y = y;
    this.z = z;
  }
}


// NOTE: the world thread sends WorldResponse and then broadcast a SpawnPlayer packet, and then marks the client as ready (even though the client thread is probably still loading the 0x03 packets). This ensures that after one .take, we're left with a spawn packet to be delegated to the ClientWriter thread.
