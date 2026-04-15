package mcserver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

import net.querz.nbt.tag.*;
import net.querz.nbt.io.*;

class Client extends Thread {

  Socket clientSocket;
  DataInputStream in;
  DataOutputStream out;
  ByteArrayTag blockArray;
  CompoundTag spawn;

  public Client(Socket clientSocket, ByteArrayTag blockArray, CompoundTag spawn) throws IOException {
    this.clientSocket = clientSocket;
    this.blockArray = blockArray;
    this.spawn = spawn;
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

      // verificationKey
      in.readFully(verificationKey);
      System.out.println("Verification key: " + new String(verificationKey).trim());

      // read unused byte
      in.readByte();

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

      // level data chunk
      out.writeByte(0x03);

      System.out.println("Block array length: " + this.blockArray.length());
      System.out.println(this.blockArray.getValue());

    } catch (IOException e) {
      System.out.println("Idk, something happened");
    }
  }
}
