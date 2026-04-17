package mcserver;

import mcserver.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.*;

import net.querz.nbt.tag.*;
import net.querz.nbt.io.*;



class ClientWriter extends Thread{
  // the goal of this thread is to take stuff out of the clientToServer queue and write it to the client appropriately

}

class ClientReader extends Thread{
  // the goal of this thread is to read stuff from the client and ensure that it's in the right format to send to the serverToClient thread
}
class Client extends Thread {
  boolean isDisconnected = false;
  BlockingQueue<Packet> serverToClient = new LinkedBlockingQueue<>();
  BlockingQueue<Packet> clientToServer;
  Socket clientSocket;
  DataInputStream in;
  DataOutputStream out;
  byte playerID;
  byte[] playerName;
  boolean ready = false;
  // if ready is false, we don't to flood the queue with ping requests

  public Client(
    Socket clientSocket,
    BlockingQueue<Packet> clientToServer,
    byte playerID
  ) throws IOException {
    // TODO: get rid of all these parameters (only need the socket and queue)
    this.clientSocket = clientSocket;
    this.clientToServer = clientToServer;
    this.in = new DataInputStream(clientSocket.getInputStream());
    this.out = new DataOutputStream(clientSocket.getOutputStream());
  }

  public void run() {
    try {
      byte packetID;
      byte protocolVersion;
      byte username[] = new byte[64];
      byte verificationKey[] = new byte[64];

      packetID = in.readByte();
      // let's just assume that packetID is 00
      System.out.println("Packet ID:" + packetID);

      protocolVersion = in.readByte();
      System.out.println("Protocol Version:" + protocolVersion);

      // username
      in.readFully(username);
      System.out.println("Username: " + new String(username).trim());
      this.playerName = username;
      // verificationKey
      in.readFully(verificationKey);
      System.out.println("Verification key: " + new String(verificationKey).trim());

      // read unused byte
      in.readByte();


      // TODO: turn these configurations into a .toml file
      // send out server identification
      out.writeByte(0x00); // packet ID
      out.writeByte(0x07); // protocol version
      String serverName = "minecraft classic server";
      String serverMessageOfTheDay = "just a boring server";
      byte[] serverNameAsBytes = String.format("%-64s", serverName).getBytes(StandardCharsets.US_ASCII);
      byte[] serverMessageOfTheDayAsBytes = String.format("%-64s", serverMessageOfTheDay).getBytes(StandardCharsets.US_ASCII);
      out.write(serverNameAsBytes); // server name
      out.write(serverMessageOfTheDayAsBytes); // server message of the day
      out.writeByte(0x00); // user type. 0x00 is normal user or 0x64 is op
      out.flush();
      
      // level initalize
      out.writeByte(0x02);

      // mark it as ready (even though world is still loading) such that the main tread can write stuff into the queue
      // TODO: make main thread give info for all this stuff
      this.ready = true;
      this.clientToServer.put(new WorldRequest(
        this.playerID,
        this.playerName
      ));
      WorldResponse worldResponsePacket = (WorldResponse) this.serverToClient.take();
      System.out.println("Client got the world response packet");
      System.out.println("World size: " + worldResponsePacket.blockArray.length);
      System.out.println("World x: " + worldResponsePacket.x);
      System.out.println("World y: " + worldResponsePacket.y);
      System.out.println("World z: " + worldResponsePacket.z);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gzip = new GZIPOutputStream(baos);
      gzip.write(ByteBuffer
        .allocate(4)
        .putInt(worldResponsePacket.blockArray.length)
        .array()
      );
      gzip.write(worldResponsePacket.blockArray);
      gzip.close();
      byte[] compressedByteArray = baos.toByteArray();
      for (int i = 0; i < compressedByteArray.length; i += 1024){
        // level data chunk
        out.writeByte(0x03);
        byte[] chunk = new byte[1024];
        short length = (short) Math.min(1024, compressedByteArray.length - i);
        out.writeShort(length); // sending one byte at a time
        System.arraycopy(compressedByteArray, i, chunk, 0, length);
        out.write(chunk); // sending the chunk
        int percentageCompleted = ((i + length)*100)/compressedByteArray.length;
        out.writeByte(percentageCompleted); // percent complete;
        System.out.println("Percentage completed: " + percentageCompleted);
      }
      
      // level finalize
      out.writeByte(0x04);
      out.writeShort(worldResponsePacket.x);
      out.writeShort(worldResponsePacket.y);
      out.writeShort(worldResponsePacket.z);

      SpawnPlayer spawnPlayerPacket = (SpawnPlayer) this.serverToClient.take();
      System.out.println("Got a spawn player response");
      System.out.println("Player name: " + new String(spawnPlayerPacket.playerName).trim());
      System.out.println("Player ID: " + spawnPlayerPacket.playerID);
      System.out.println("x: " + spawnPlayerPacket.x);
      System.out.println("y: " + spawnPlayerPacket.y);
      System.out.println("z: " + spawnPlayerPacket.z);
      System.out.println("yaw: " + spawnPlayerPacket.yaw);
      System.out.println("pitch: " + spawnPlayerPacket.pitch);

      // spawn player
      out.writeByte(0x07); // packet ID
      out.writeByte(0xff); // player ID
      out.write(username);
      out.writeShort(spawnPlayerPacket.x);
      out.writeShort(spawnPlayerPacket.y);
      out.writeShort(spawnPlayerPacket.z);
      out.writeByte(spawnPlayerPacket.yaw);
      out.writeByte(spawnPlayerPacket.pitch);

      // the client has spawned
      while (true){
        Packet p = serverToClient.take();
        if (p instanceof Ping){
          out.writeByte(0x01);
          System.out.println("Client got pinged!");
          //this.clientToServer.put(new Ping());
        }
      }
    } 
    catch (IOException e) {
      System.out.println("Something happened, client disconnected");
      this.isDisconnected = true;
      // TODO: need to send a DespawnPacket to the server
      e.printStackTrace();
    }
    catch (InterruptedException e){
      System.out.println("InterruptedException, client probably disconnected");
      this.isDisconnected = true;
      e.printStackTrace();
    }
  }
}
