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
    /*
     * System.out.println(world.get("Spawn"));
     * 
     * System.out.println(((ByteArrayTag) world.get("BlockArray")).length());
     * System.out.println(world.get("X"));
     * System.out.println(world.get("Y"));
     * System.out.println(world.get("Z"));
     */
    
    ServerSocket serverSocket = new ServerSocket(25565);
    BlockingQueue<Packet> clientToServer = new LinkedBlockingQueue<>();
    CopyOnWriteArrayList<Client> clientList = new CopyOnWriteArrayList<>();
    Accept acceptThread = new Accept(serverSocket, clientList);
    acceptThread.start();

    while (true){
      System.out.println(clientList.size());
    }

    //serverSocket.close();
  }
}
