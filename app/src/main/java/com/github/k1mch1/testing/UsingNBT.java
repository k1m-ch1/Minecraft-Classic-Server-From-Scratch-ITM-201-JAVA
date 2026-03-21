package com.github.k1mch1.testing;

import net.querz.nbt.tag.*;

import java.io.IOException;

import net.querz.nbt.io.*;

public class UsingNBT {
  public static void main(String[] args) throws IOException {
    NamedTag namedTag = NBTUtil.read("worlds/world.cw");
    CompoundTag world = (CompoundTag) namedTag.getTag();
    System.out.println(world.get("FormatVersion"));
  }
}
