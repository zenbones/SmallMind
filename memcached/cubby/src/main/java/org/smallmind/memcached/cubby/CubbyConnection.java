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
package org.smallmind.memcached.cubby;

import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.codec.LargeValueCompressingCodec;
import org.smallmind.memcached.cubby.codec.ObjectStreamCubbyCodec;
import org.smallmind.memcached.cubby.command.GetCommand;
import org.smallmind.memcached.cubby.command.SetCommand;
import org.smallmind.memcached.cubby.translator.DefaultKeyTranslator;
import org.smallmind.memcached.cubby.translator.KeyTranslator;
import org.smallmind.memcached.cubby.translator.LargeKeyHashingTranslator;

public class CubbyConnection {

  public CubbyConnection ()
    throws Exception {

    KeyTranslator keyTranslator = new LargeKeyHashingTranslator(new DefaultKeyTranslator());
    CubbyCodec codec = new LargeValueCompressingCodec(new ObjectStreamCubbyCodec());
    EventLoop eventLoop;
    Thread eventThread;

    eventThread = new Thread(eventLoop = new EventLoop(this, "localhost", 11211, 300, 300));

    eventThread.setDaemon(true);
    eventThread.start();

    System.out.println("send...");

    Response response;

    response = eventLoop.send(new SetCommand().setKey("hello").setValue("goodbye"), keyTranslator, codec, null);
    System.out.println(response);
    response = eventLoop.send(new GetCommand().setKey("hello").setCas(true), keyTranslator, codec, null);
    System.out.println(response);
    Object value = codec.deserialize(response.getValue());
    System.out.println(value);
    response = eventLoop.send(new GetCommand().setKey("hello2").setCas(true), keyTranslator, codec, null);
    System.out.println(response);
    response = eventLoop.send(new GetCommand().setKey("hello2").setCas(true), keyTranslator, codec, null);
    System.out.println(response);
    //    eventLoop.send(new NoopCommand(new ObjectStreamCodec()));

    Thread.sleep(3000);
  }

  public static void main (String... args)
    throws Exception {

    new CubbyConnection();
  }

  public void start () {

  }

  public void disconnected () {

  }
}
