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
  BlockingQueue<Packet> clientToServer;
  byte playerIDCounter = 0;

  Accept(
    ServerSocket serverSocket,
    CopyOnWriteArrayList<Client> clientList,
    BlockingQueue<Packet> clientToServer
  ){
    // TODO: include a counter used to initialize playerID such that there's no conflict. Also, monitor playerID counter to the console (0xff is reserved)
    this.serverSocket = serverSocket;
    this.clientList = clientList;
    this.clientToServer = clientToServer;
  }

  public void run(){
    try{
      // TODO: create a .toml or .yaml or something file to store all the configurations
      while (true){
        Socket clientSocket = serverSocket.accept();
        Client clientThread = new Client(
          clientSocket,
          this.clientToServer,
          this.playerIDCounter
        );
        this.playerIDCounter += 1;
        clientList.add(clientThread);
        clientThread.start();
      }
    } catch (IOException e) {
      System.out.println("Idk, something happened");
      e.printStackTrace();
    }
  }
}
