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
      while (true){
        Socket clientSocket = serverSocket.accept();
        byte assignedPlayerID = Utils.assignPlayerID(this.clientList);
        if (assignedPlayerID == (byte) 255){
          // we must reserve ID 255 for the server. Disconnect the client.
          DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
          out.writeByte(0x0e);
          out.write(Utils.stringToByteArray("The server is probably full. Try again later."));
          continue;
        }
        Client clientThread = new Client(
          clientSocket,
          this.clientToServer,
          assignedPlayerID
        );
        clientList.add(clientThread);
        clientThread.start();
      }
    } catch (IOException e) {
      System.out.println("An IOException in the accept thread? Something REALLY BAD must have happened...");
      e.printStackTrace();
    }
  }
}
