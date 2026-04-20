package mcserver;

import mcserver.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;

import net.querz.nbt.tag.*;
import net.querz.nbt.io.*;

public class Main {

  public static void main(String[] args) throws IOException {
    System.out.println("Starting a new server...");

    NamedTag namedTag = NBTUtil.read("worlds/world.cw");
    CompoundTag world = (CompoundTag) namedTag.getTag();
    // TODO: schedule a backup service
    byte[] blockArray = ((ByteArrayTag) world.get("BlockArray")).getValue();
    // x, y and z refers to the dimensions of the world
    short x = ((ShortTag) world.get("X")).asShort();
    short y = ((ShortTag) world.get("Y")).asShort();
    short z = ((ShortTag) world.get("Z")).asShort();

    /*
     * // spawn player
     * out.writeByte(0x07); // packet ID
     * out.writeByte(0xff); // player ID
     * out.write(username);
     * out.writeShort(((ShortTag) this.spawn.get("X")).asShort() * 32);
     * out.writeShort(((ShortTag) this.spawn.get("Y")).asShort() * 32 + 51);
     * out.writeShort(((ShortTag) this.spawn.get("Z")).asShort() * 32);
     * out.writeByte(((ByteTag) this.spawn.get("H")).asByte());
     * out.writeByte(((ByteTag) this.spawn.get("P")).asByte());
     */
    CompoundTag spawn = (CompoundTag) world.get("Spawn");
    short xSpawn = (short) (((ShortTag) spawn.get("X")).asShort() * 32);
    short ySpawn = (short) (((ShortTag) spawn.get("Y")).asShort() * 32 + 51);
    short zSpawn = (short) (((ShortTag) spawn.get("Z")).asShort() * 32);
    byte yawSpawn = ((ByteTag) spawn.get("H")).asByte();
    byte pitchSpawn = ((ByteTag) spawn.get("P")).asByte();
    ServerSocket serverSocket = new ServerSocket(25565); // TODO: make this a parameter in server.properties
    BlockingQueue<Packet> clientToServer = new LinkedBlockingQueue<>();
    CopyOnWriteArrayList<Client> clientList = new CopyOnWriteArrayList<>();
    Accept acceptThread = new Accept(serverSocket, clientList, clientToServer);
    acceptThread.start();

    // start heartbeat service
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    scheduler.scheduleAtFixedRate(() -> {
      for (Client client : clientList) {
        try {
          if (client.ready) {
            client.serverToClient.put(new Ping());
          }
        } catch (InterruptedException e) {
          System.out.println("Something went wrong..., marking the client as disconnected");
          client.isDisconnected = true;
          // TODO: broadcast despawn (perhaps I should make that a method or something)
        }
      }
    }, 0, 1, TimeUnit.SECONDS);

    // main loop
    while (true) {
      try {
        Packet p = clientToServer.take();
        clientList.removeIf(client -> client.isDisconnected);
        if (p instanceof WorldRequest) {
          WorldRequest worldRequestPacket = (WorldRequest) p;
          System.out.println("Requested for world");
          System.out.println("Player ID: " + worldRequestPacket.playerID);
          System.out.println("Player name: " + new String(worldRequestPacket.playerName).trim());
          for (Client client : clientList) {
            if (client.playerID != worldRequestPacket.playerID) {
              continue;
            }
            client.serverToClient.put(new WorldResponse(
                blockArray,
                x,
                y,
                z));
            System.out.println("Successfully sent world response to " + new String(client.playerName).trim()
                + " with ID of " + client.playerID);
            // broadcast spawn player to everyone
            for (Client clientToBroadcastSpawn : clientList) {
              if (!clientToBroadcastSpawn.ready) {
                continue;
              }
              String serverName = "Server";
              byte[] serverNameAsByteArray = String.format("%-64s", serverName).getBytes(StandardCharsets.US_ASCII);

              String welcomeMessage = "[" + new String(client.playerName).trim() + "]" + " has joined the game";
              byte[] welcomeMessageAsByteArray = String.format("%-64s", welcomeMessage)
                  .getBytes(StandardCharsets.US_ASCII);

              clientToBroadcastSpawn.serverToClient.put(new SpawnPlayer(
                  client.playerID,
                  client.playerName,
                  xSpawn,
                  ySpawn,
                  zSpawn,
                  yawSpawn,
                  pitchSpawn));

              // broadcast the message that a new player has joined
              clientToBroadcastSpawn.serverToClient.put(new Message(
                  (byte) 0xff,
                  serverNameAsByteArray,
                  welcomeMessageAsByteArray));

              // System.out.println("Successfully sent broadcasted spawn to " + new
              // String(clientToBroadcastSpawn.playerName).trim() + " with ID of " +
              // clientToBroadcastSpawn.playerID);

            }
            for (Client clientToSpawn : clientList) {
              if (clientToSpawn.playerID == client.playerID || !clientToSpawn.ready) {
                continue;
              }
              client.serverToClient.put(
                  new SpawnPlayer(
                      clientToSpawn.playerID, // although it's not atomic, playerID is pretty much constant
                      clientToSpawn.playerName,
                      (short) clientToSpawn.x.get(),
                      (short) clientToSpawn.y.get(),
                      (short) clientToSpawn.z.get(),
                      (byte) clientToSpawn.yaw.get(),
                      (byte) clientToSpawn.pitch.get()));
            }
          }
        } else if (p instanceof SetBlock) {
          SetBlock setBlockRequest = (SetBlock) p;
          // indexing according to the formula in the wiki
          blockArray[setBlockRequest.x + x * setBlockRequest.z + x * z * setBlockRequest.y] = setBlockRequest.block;
          for (Client clientToSetBlock : clientList) {
            if (!clientToSetBlock.ready) {
              continue;
            }
            clientToSetBlock.serverToClient.put(setBlockRequest);
          }
          // not comfortable with changing deltas without correction mechanisms but... TCP
          // is reliable right...?
        } else if (p instanceof PositionAndOrientation) {
          // TODO: edit this! send the update packets!
          PositionAndOrientation positionAndOrientationPacket = (PositionAndOrientation) p;
          for (Client clientToUpdatePosition : clientList) {
            if ((clientToUpdatePosition.playerID == positionAndOrientationPacket.playerID)
                || !clientToUpdatePosition.ready) {
              // we don't need to acknowledge the player who requested
              continue;
            }
            clientToUpdatePosition.serverToClient.put(positionAndOrientationPacket);
          }
        } else if (p instanceof Message) {
          Message messagePacket = (Message) p;
          for (Client clientToMessage : clientList) {
            if (!clientToMessage.ready) {
              continue;
            }
            clientToMessage.serverToClient.put(messagePacket);
          }
        } else {
          System.out.println("Main thread got an unimplemented class: " + p.getClass());
        }
      } catch (InterruptedException e) {
        System.out.println("The main thread got interupted. Something must have went terribly wrong.");
        e.printStackTrace();
      }
    }
  }
}
