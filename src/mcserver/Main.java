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
    ServerSocket serverSocket = new ServerSocket(25565);
    BlockingQueue<Packet> clientToServer = new LinkedBlockingQueue<>();
    CopyOnWriteArrayList<Client> clientList = new CopyOnWriteArrayList<>();
    Accept acceptThread = new Accept(serverSocket, clientList);
    acceptThread.start();

    while (true){
      clientList.removeIf(client -> client.isDisconnected);
      // handle pinging (fast)
      for(Client client: clientList){
        try{
          client.serverToClient.put(new Ping());
        }
        catch (InterruptedException e){
          System.out.println("Something went wrong..., marking the client as disconnected");
          client.isDisconnected = true;
        }
      }

      // TODO: handle client queue (slow)
      System.out.println("Clients: " + clientList.size());
    }
  }
}
