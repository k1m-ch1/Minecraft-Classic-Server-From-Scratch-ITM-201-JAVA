package mcserver;

import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

  public static void main(String[] args) {
    Map<String, String> serverProperties = getServerProperties("server.properties");
    System.out.println(serverProperties);
  }
}
