package com.github.k1mch1.testing;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class ReadCW {

  public static void main(String[] args) throws IOException {
    File worldFile = new File("worlds/world.cw");
    FileInputStream classicWorldInputStream = new FileInputStream(worldFile);

    GZIPInputStream worldInputStream = new GZIPInputStream(classicWorldInputStream);

    byte[] slice = worldInputStream.readNBytes(10);

    // for b in slice
    for (byte b : slice) {
      System.out.print(String.format("%02X ", b));
    }

    System.out.println("");

    classicWorldInputStream.close();
    worldInputStream.close();
  }
}
