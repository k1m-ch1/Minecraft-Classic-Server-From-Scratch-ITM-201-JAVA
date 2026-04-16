package mcserver;

import mcserver.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;

import net.querz.nbt.tag.*;
import net.querz.nbt.io.*;

class Accept extends Thread{
  ServerSocket serverSocket; 
  CopyOnWriteArrayList<Client> clientList;

  Accept(ServerSocket serverSocket, CopyOnWriteArrayList<Client> clientList){
    this.serverSocket = serverSocket;
    this.clientList = clientList;
  }

  public void run(){
    try{
      NamedTag namedTag = NBTUtil.read("worlds/world.cw");
      CompoundTag world = (CompoundTag) namedTag.getTag();

      while (true){
        Socket clientSocket = serverSocket.accept();
        Client clientThread = new Client(clientSocket,
          (ByteArrayTag) world.get("BlockArray"),
          (CompoundTag) world.get("Spawn"),
          (ShortTag) world.get("X"),
          (ShortTag) world.get("Y"),
          (ShortTag) world.get("Z")
        );
        clientList.add(clientThread);
        clientThread.start();
      }
    } catch (IOException e) {
      System.out.println("Idk, something happened");
    }
  }
}
