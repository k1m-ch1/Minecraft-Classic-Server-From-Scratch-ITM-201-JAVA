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
    //TODO: make the world file into a config file
    Map<String, String> serverProperties = Utils.getServerProperties("server.properties");
    final String worldPath = serverProperties.get("world-path");
    NamedTag namedTag = NBTUtil.read(worldPath);
    CompoundTag world = (CompoundTag) namedTag.getTag();
    byte[] blockArray = ((ByteArrayTag) world.get("BlockArray")).getValue();
    // x, y and z refers to the dimensions of the world
    short x = ((ShortTag) world.get("X")).asShort();
    short y = ((ShortTag) world.get("Y")).asShort();
    short z = ((ShortTag) world.get("Z")).asShort();

    CompoundTag spawn = (CompoundTag) world.get("Spawn");
    short xSpawn = (short) (((ShortTag) spawn.get("X")).asShort() * 32);
    short ySpawn = (short) (((ShortTag) spawn.get("Y")).asShort() * 32 + 51);
    short zSpawn = (short) (((ShortTag) spawn.get("Z")).asShort() * 32);
    byte yawSpawn = ((ByteTag) spawn.get("H")).asByte();
    byte pitchSpawn = ((ByteTag) spawn.get("P")).asByte();

    ServerSocket serverSocket = new ServerSocket(Integer.parseInt(serverProperties.get("port")));
    System.out.printf("Started a socket at port %s\n", serverProperties.get("port"));

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
          System.out.printf("Got interrupted while writing to serverToClient queue. Marking the %s (ID: %d) as disconnected.\n", Utils.byteArrayToString(client.playerName), client.playerID);
          try{
            client.clientToServer.put(new Despawn(client.playerID, client.playerName));
          }
          catch (InterruptedException err){
            System.out.println("Thread got interrupted while writing to clientToServer queue again. Something really bad must've happened.");
            err.printStackTrace();
          }
        }
      }
    }, 0, 1, TimeUnit.SECONDS);

    // main loop
    while (true) {
      try {
        Packet p = clientToServer.take();
        if (p instanceof WorldRequest) {
          WorldRequest worldRequestPacket = (WorldRequest) p;
          for (Client client : clientList) {
            if (client.playerID != worldRequestPacket.playerID) {
              continue;
            }
            client.serverToClient.put(new WorldResponse(
                blockArray,
                x,
                y,
                z));
            // once we receive the world response packet from the main thread, mark it as ready (even though world is still loading) such that the main
            // thread can already write stuff into the queue
            // We do it in the main thread since we can guarantee that the client won't miss any packets (since the main thread is responsible for putting stuff into the serverToClient queue)
            client.ready = true;

            // broadcast spawn player to everyone
            for (Client clientToBroadcastSpawn : clientList) {
              if (!clientToBroadcastSpawn.ready) {
                continue;
              }
              // TODO: make this in server.properties
              byte[] serverNameAsByteArray = Utils.stringToByteArray(serverProperties.get("server-name"));
              String welcomeMessage = String.format(serverProperties.get("spawn-message-format"), Utils.byteArrayToString(client.playerName));
              byte[] welcomeMessageAsByteArray = Utils.stringToByteArray(welcomeMessage);
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
          world.putByteArray("BlockArray", blockArray);
          // I know that we shouldn't write to file in the main thread... but... it should be fine...
          NBTUtil.write(world, worldPath);
          for (Client clientToSetBlock : clientList) {
            if (!clientToSetBlock.ready) {
              continue;
            }
            clientToSetBlock.serverToClient.put(setBlockRequest);
          }
        } else if (p instanceof PositionAndOrientation) {
          PositionAndOrientation positionAndOrientationPacket = (PositionAndOrientation) p;
          for (Client clientToUpdatePosition : clientList) {
            if ((clientToUpdatePosition.playerID == positionAndOrientationPacket.playerID)
                || !clientToUpdatePosition.ready) {
              // we don't need to send an acknowledge to the player who requested
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
        } 
        else if (p instanceof Despawn){
          Despawn despawnPacket = (Despawn) p;
          clientList.removeIf(client -> client.playerID == despawnPacket.playerID);
          for (Client clientToRelay : clientList) {
            if (!clientToRelay.ready) {
              continue;
            }
            clientToRelay.serverToClient.put(despawnPacket);
          }
        }
        else {
          System.out.println("Main thread got an unimplemented class: " + p.getClass());
        }
      } catch (InterruptedException e) {
        System.out.println("The main thread got interupted. Something must have went terribly wrong.");
        e.printStackTrace();
      }
    }
  }
}
