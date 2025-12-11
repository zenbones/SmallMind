/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.phalanx.wire;

import java.util.Date;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.WiredService;

public class WireTestingServiceImpl implements WireTestingService, WiredService {

  @Override
  public int getVersion () {

    return 1;
  }

  @Override
  public String getServiceName () {

    return "WireTestService";
  }

  @Override
  public void setResponseTransport (ResponseTransport responseTransport)
    throws Exception {

    responseTransport.register(WireTestingServiceImpl.class, this);
  }

  @Override
  public boolean hasContext () {

    return ContextFactory.exists(TestWireContext.class);
  }

  @Override
  public void requiresContext () {

  }

  @Override
  public void doNothing () {

  }

  @Override
  public boolean echoBoolean (boolean b) {

    return b;
  }

  @Override
  public byte echoByte (byte b) {

    return b;
  }

  @Override
  public short echoShort (short s) {

    return s;
  }

  @Override
  public int echoInt (int i) {

    return i;
  }

  @Override
  public long echoLong (long l) {

    return l;
  }

  @Override
  public float echoFloat (float f) {

    return f;
  }

  @Override
  public double echoDouble (double d) {

    return d;
  }

  @Override
  public char echoChar (char c) {

    return c;
  }

  @Override
  public String echoString (@Argument("string") String string) {

    return string;
  }

  @Override
  public Date echoDate (@Argument("date") Date date) {

    return date;
  }

  @Override
  public Color[] echoColors (@Argument("colors") Color... colors) {

    return colors;
  }

  @Override
  public Integer addNumbers (@Argument("x") int x, @Argument("y") int y) {

    return x + y;
  }

  @Override
  public void throwError ()
    throws WireTestingException {

    throw new WireTestingException();
  }
}
