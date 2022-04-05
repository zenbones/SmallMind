/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.file.ephemeral;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;

public class Wombat {

  public static void main (String... args)
    throws Exception {

    System.out.println(".........................................");
    System.out.println(FileSystems.getDefault());
    Path ps = Paths.get("C:\\Users\\david\\Documents\\response.txt");
    System.out.println(Files.isRegularFile(ps));
    System.out.println(new String(Files.readAllBytes(ps)));
    System.out.println(".........................................");
    Path pe = Paths.get("/opt/epicenter/twimble/farkle");
    Files.createDirectories(pe);
    try (SeekableByteChannel bc = Files.newByteChannel(pe.resolve("sparkle.txt"), Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE))) {
      ByteBuffer bb = ByteBuffer.allocate(1024);
      bb.put("Hello out there!".getBytes());
      bb.flip();
      bc.write(bb);
    }
    System.out.println(new String(Files.readAllBytes(pe)));
  }
}
