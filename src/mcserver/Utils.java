package mcserver;

import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.*;

public class Utils {
  private Utils() {} // prevent instantiation

  public static Map<String, String> getServerProperties(String serverPropertiesFile){

    Map<String, String> serverProperties = new HashMap<>();
    try{
      Files.lines(Path.of(serverPropertiesFile))
        .forEach((String line) -> {
          //System.out.println(line);
          String[] parts= line.split("=");
          serverProperties.put(parts[0], parts[1]);
        });
      return serverProperties;
    }
    catch (Exception e){
      serverProperties.put("motd", "Welcome to my Minecraft Server");
      serverProperties.put("server-name", "Minecraft Server");
      serverProperties.put("port", "25565");
      serverProperties.put("spawn-message-format", "%s joined");
      serverProperties.put("despawn-message-format", "%s left the game");
      return serverProperties;
    }
  }

  public static String byteArrayToString(byte[] byteArray){
    return new String(byteArray).trim();
  }

  public static byte[] stringToByteArray(String string){
    // automatically truncate if the string is of length greater than 64
    return Arrays.copyOfRange(String.format("%-64s", string).getBytes(StandardCharsets.US_ASCII), 0, 64);
  }

  public static byte assignPlayerID(CopyOnWriteArrayList<Client> clientList){
    // this problem is so leetcode like...
    // Given a list an unsorted list of distinct integers, where each value can be a integer from the range of [0, n], 
    // we need to find the smallest integer from the range of [0, n] that's not in that list.
    // For example, a list like [5, 4, 0, 2] should return a 1.
    // a list like [0, 1, 2, 4] should return a 3, etc.
    boolean[] seen = new boolean[256]; // Java always 0 out the byte array
    // O(n) space, O(n) time. Space isn't a concern since it's just a playerID is just a byte.
    // note that n isn't the size of clientList, but 256.
    for (Client client: clientList){
      seen[Byte.toUnsignedInt(client.playerID)] = true;
    }
    for (byte i = 0; i < 256; i++){
      if (!seen[i]){
        // if we haven't seen it in the clientList, assign that ID
        return i;
      }
    }
    return (byte) 255;
  }

  public static void main(String[] args) {
    Map<String, String> serverProperties = getServerProperties("server.properties");
    System.out.println(serverProperties);
  }

}
