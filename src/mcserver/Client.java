package mcserver;

import mcserver.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import net.querz.nbt.tag.*;
import net.querz.nbt.io.*;

class ClientWriter extends Thread {
  // the goal of this thread is to take stuff out of the serverToClient queue and
  // write it to the client appropriately

  Client client;

  ClientWriter(
      Client client) {
    this.client = client;
  }

  public void run() {
    try {
      while (true) {
        Packet p = client.serverToClient.take();
        if (p instanceof Ping) {
          this.client.out.writeByte(0x01);
          /*
           * System.out.println("Client got pinged!");
           * System.out.println("Current position: " + this.client.x.get() + ", " +
           * this.client.y.get() + ", " + this.client.z.get());
           */
        } else if (p instanceof SpawnPlayer) {
          SpawnPlayer spawnPlayerPacket = (SpawnPlayer) p;
          /*
           * System.out.println("Got a spawn player response");
           * System.out.println("Player name: " + new
           * String(spawnPlayerPacket.playerName).trim());
           * System.out.println("Player ID: " + spawnPlayerPacket.playerID);
           * System.out.println("x: " + spawnPlayerPacket.x);
           * System.out.println("y: " + spawnPlayerPacket.y);
           * System.out.println("z: " + spawnPlayerPacket.z);
           * System.out.println("yaw: " + spawnPlayerPacket.yaw);
           * System.out.println("pitch: " + spawnPlayerPacket.pitch);
           */
          if (spawnPlayerPacket.playerID == client.playerID) {
            System.out.println(client.playerID);
            // spawn player
            this.client.x = new AtomicInteger(spawnPlayerPacket.x);
            this.client.y = new AtomicInteger(spawnPlayerPacket.y);
            this.client.z = new AtomicInteger(spawnPlayerPacket.z);
            this.client.yaw = new AtomicInteger(spawnPlayerPacket.yaw);
            this.client.pitch = new AtomicInteger(spawnPlayerPacket.pitch);
            this.client.out.writeByte(0x07); // packet ID
            this.client.out.writeByte(0xff); // player ID (0xff means spawn ourself)
          } else {
            this.client.out.writeByte(0x07); // packet ID
            this.client.out.writeByte(spawnPlayerPacket.playerID);
          }
          this.client.out.write(spawnPlayerPacket.playerName);
          this.client.out.writeShort(spawnPlayerPacket.x);
          this.client.out.writeShort(spawnPlayerPacket.y);
          this.client.out.writeShort(spawnPlayerPacket.z);
          this.client.out.writeByte(spawnPlayerPacket.yaw);
          this.client.out.writeByte(spawnPlayerPacket.pitch);
        } else if (p instanceof SetBlock) {
          SetBlock setBlockPacket = (SetBlock) p;
          this.client.out.writeByte(0x06); // packet ID
          this.client.out.writeShort(setBlockPacket.x);
          this.client.out.writeShort(setBlockPacket.y);
          this.client.out.writeShort(setBlockPacket.z);
          this.client.out.writeByte(setBlockPacket.block);
        } else if (p instanceof PositionAndOrientation) {
          // TODO: this is actually all incorrect, we dont' just send 0x08, we send
          // changes in X, Y, Z, yaw, pitch using the 0x09 packet, and we do it
          // occasionally, perhaps implement an event listener or something to check when
          // the client changes position
          PositionAndOrientation positionAndOrientationPacket = (PositionAndOrientation) p;
          byte playerID = positionAndOrientationPacket.playerID;
          if (playerID == this.client.playerID){
            // if we're refering to ourselves, send a playerID of 0xff by convention in the protocol
            playerID = (byte) 0xff;
          }
          this.client.out.writeByte(0x08); // position and orientation
          this.client.out.writeByte(playerID);
          this.client.out.writeShort(positionAndOrientationPacket.x);
          this.client.out.writeShort(positionAndOrientationPacket.y);
          this.client.out.writeShort(positionAndOrientationPacket.z);
          this.client.out.writeByte(positionAndOrientationPacket.yaw);
          this.client.out.writeByte(positionAndOrientationPacket.pitch);
        } else if (p instanceof Message) {
          Message messagePacket = (Message) p;
          // just relay it (but i also need a prompt)
          String prompt = "[" + new String(messagePacket.playerName).trim() + "]" + " : ";
          byte[] promptAsByteArray = String.format("%-64s", prompt).getBytes(StandardCharsets.US_ASCII);
          this.client.out.writeByte(0x0d); // packet ID
          this.client.out.writeByte(messagePacket.playerID);
          this.client.out.write(promptAsByteArray);

          // now for the actual message
          this.client.out.writeByte(0x0d);
          this.client.out.writeByte(messagePacket.playerID);
          this.client.out.write(messagePacket.text);
        } 
        else if (p instanceof Despawn){
          Despawn despawnPacket = (Despawn) p;
          this.client.out.writeByte(0x0c); // despawn packet ID is 0x0c
          this.client.out.writeByte(despawnPacket.playerID);

          // send a despawn message TODO: make the despawn message configurable in the server.properties file?
          String despawnMessageFormat = "[%s] just disconnected";
          String despawnMessage = String.format(despawnMessageFormat, new String(despawnPacket.playerName).trim());
          byte[] despawnMessageAsByteArray = String.format("%-64s", despawnMessage).getBytes(StandardCharsets.US_ASCII);
          this.client.serverToClient.put(new Message(
            despawnPacket.playerID,
            despawnPacket.playerName,
            despawnMessageAsByteArray
          ));
        }
        else {
          // NOTE: remember that if the playerID is the same as ours, make sure to send
          // the playerID as 0xff
          System.out.println("Client writer got an unimplemented packet of class: " + p.getClass());
        }
      }
    } catch (IOException | InterruptedException e) {
      System.out.println("Exception in ClientWriter thread, client probably disconnected");
      try{
        this.client.clientToServer.put(new Despawn(this.client.playerID, this.client.playerName));
      }
      catch (InterruptedException err){
        System.out.println("Thread got interrupted while writing to clientToServer array. Something really bad must've happened.");
        err.printStackTrace();
      }
      e.printStackTrace();
    }
  }
}

class ClientReader extends Thread {
  // the goal of this thread is to read stuff from the client and ensure that it's
  // in the right format to send to the clientToServer thread
  Client client;

  ClientReader(
      Client client) {
    this.client = client;
  }

  public void run() {
    try {
      while (true) {
        byte packetID = this.client.in.readByte();
        if (packetID == 0x05) {
          short x = this.client.in.readShort();
          short y = this.client.in.readShort();
          short z = this.client.in.readShort();
          byte mode = this.client.in.readByte();
          byte block = this.client.in.readByte();
          if (mode == 0x00) {
            // 0x00 means client wants to destroy the block
            block = 0x00;
            // we set it to air
          }
          this.client.clientToServer.put(new SetBlock(
              x,
              y,
              z,
              block));
        } else if (packetID == 0x08) {
          // 0x08 is the position and orientation packet
          byte playerID = this.client.in.readByte();
          short x = this.client.in.readShort();
          short y = this.client.in.readShort();
          short z = this.client.in.readShort();
          byte yaw = this.client.in.readByte();
          byte pitch = this.client.in.readByte();

          if (this.client.x.get() != x ||
          this.client.y.get() != y ||
          this.client.z.get() != z ||
          this.client.yaw.get() != yaw ||
          this.client.pitch.get() != pitch){
            this.client.x = new AtomicInteger(x);
            this.client.y = new AtomicInteger(y);
            this.client.z = new AtomicInteger(z);
            this.client.yaw = new AtomicInteger(yaw);
            this.client.pitch = new AtomicInteger(pitch);
            this.client.clientToServer.put(new PositionAndOrientation(
              this.client.playerID,
              x,
              y,
              z,
              yaw,
              pitch
            ));
          }
        }
        else if(packetID == 0x0d) {
          // 0x0d is the message packet
          byte message[] = new byte[64];
          byte messageColor = this.client.in.readByte();
          this.client.in.readFully(message);
          this.client.clientToServer.put(new Message(
              this.client.playerID,
              this.client.playerName,
              message));
        }
      }
    } catch (IOException | InterruptedException e) {
      System.out.println("Exception in ClientReader thread, client probably disconnected");

      try{
        this.client.clientToServer.put(new Despawn(this.client.playerID, this.client.playerName));
      }
      catch (InterruptedException err){
        System.out.println("Thread got interrupted while writing to clientToServer array. Something really bad must've happened.");
        err.printStackTrace();
      }

      e.printStackTrace();
    }
  }
}

class Client extends Thread {
  BlockingQueue<Packet> serverToClient = new LinkedBlockingQueue<>();
  BlockingQueue<Packet> clientToServer;
  Socket clientSocket;
  DataInputStream in;
  DataOutputStream out;
  AtomicInteger x;
  AtomicInteger y;
  AtomicInteger z;
  AtomicInteger yaw;
  AtomicInteger pitch;

  byte playerID;
  byte[] playerName;
  boolean ready = false;
  // if ready is false, we don't to flood the queue with ping requests

  public Client(
      Socket clientSocket,
      BlockingQueue<Packet> clientToServer,
      byte playerID) throws IOException {
    this.clientSocket = clientSocket;
    this.clientToServer = clientToServer;
    this.in = new DataInputStream(clientSocket.getInputStream());
    this.out = new DataOutputStream(clientSocket.getOutputStream());
    this.playerID = playerID;
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
      String serverName = "minecraft classic server"; // TODO: make these guys a server.properties parameter
      String serverMessageOfTheDay = "just a boring server";
      byte[] serverNameAsBytes = String.format("%-64s", serverName).getBytes(StandardCharsets.US_ASCII);
      byte[] serverMessageOfTheDayAsBytes = String.format("%-64s", serverMessageOfTheDay)
          .getBytes(StandardCharsets.US_ASCII);
      out.write(serverNameAsBytes); // server name
      out.write(serverMessageOfTheDayAsBytes); // server message of the day
      out.writeByte(0x00); // user type. 0x00 is normal user or 0x64 is op
      out.flush();

      // level initalize
      out.writeByte(0x02);

      // mark it as ready (even though world is still loading) such that the main
      // tread can write stuff into the queue
      this.clientToServer.put(new WorldRequest(
          this.playerID,
          this.playerName));
      WorldResponse worldResponsePacket = (WorldResponse) this.serverToClient.take();
      this.ready = true;
      /*
       * System.out.println("Client got the world response packet");
       * System.out.println("World size: " + worldResponsePacket.blockArray.length);
       * System.out.println("World x: " + worldResponsePacket.x);
       * System.out.println("World y: " + worldResponsePacket.y);
       * System.out.println("World z: " + worldResponsePacket.z);
       */
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gzip = new GZIPOutputStream(baos);
      gzip.write(ByteBuffer
          .allocate(4)
          .putInt(worldResponsePacket.blockArray.length)
          .array());
      gzip.write(worldResponsePacket.blockArray);
      gzip.close();
      byte[] compressedByteArray = baos.toByteArray();
      for (int i = 0; i < compressedByteArray.length; i += 1024) {
        // level data chunk
        out.writeByte(0x03);
        byte[] chunk = new byte[1024];
        short length = (short) Math.min(1024, compressedByteArray.length - i);
        out.writeShort(length); // sending one byte at a time
        System.arraycopy(compressedByteArray, i, chunk, 0, length);
        out.write(chunk); // sending the chunk
        int percentageCompleted = ((i + length) * 100) / compressedByteArray.length;
        out.writeByte(percentageCompleted); // percent complete;
        System.out.println("Percentage completed: " + percentageCompleted);
      }

      // level finalize
      out.writeByte(0x04);
      out.writeShort(worldResponsePacket.x);
      out.writeShort(worldResponsePacket.y);
      out.writeShort(worldResponsePacket.z);

      ClientWriter clientWriter = new ClientWriter(this);
      ClientReader clientReader = new ClientReader(this);
      clientWriter.start();
      clientReader.start();
      clientWriter.join();
      clientReader.join();
    } catch (IOException | InterruptedException e) {
      System.out.println("Something happened, client disconnected");
      // TODO: need to send a DespawnPacket to the server
      e.printStackTrace();
    }
  }
}
