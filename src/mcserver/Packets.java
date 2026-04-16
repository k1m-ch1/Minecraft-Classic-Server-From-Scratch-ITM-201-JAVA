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
  byte[] text;

  Message(byte playerID, byte[] text){
    this.playerID = playerID;
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
