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
    Socket clientSocket = serverSocket.accept();

    BlockingQueue<Packet> clientToServer = new LinkedBlockingQueue<>();

    // we want this to be an array list or something
    Client clientThread = new Client(clientSocket,
        (ByteArrayTag) world.get("BlockArray"),
        (CompoundTag) world.get("Spawn"),
        (ShortTag) world.get("X"),
        (ShortTag) world.get("Y"),
        (ShortTag) world.get("Z")
    );

    clientThread.start();
    try {
      while (clientThread.isAlive()){
        clientThread.serverToClient.put(new Message((byte) 0xff, String.format("%-64s", "hello").getBytes(StandardCharsets.US_ASCII)));
      }  
    } catch (Exception e) {
      System.out.println("sth went wrong");
    }
    serverSocket.close();
  }
}
