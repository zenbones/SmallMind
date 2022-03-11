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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.memcached.cubby.command.Command;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.util.SelfDestructive;

public class RequestCallback implements SelfDestructive {

  private final CountDownLatch resultLatch = new CountDownLatch(1);
  private final AtomicReference<Stint> timeoutStintRef = new AtomicReference<>();
  private final AtomicReference<ServerResponse> resultRef = new AtomicReference<>();
  private final AtomicReference<IOException> exceptionRef = new AtomicReference<>();
  private final Command command;

  public RequestCallback (Command command) {

    this.command = command;
  }

  @Override
  public void destroy (Stint timeoutStint) {

    timeoutStintRef.set(timeoutStint);

    resultLatch.countDown();
  }

  public ServerResponse getResult ()
    throws InterruptedException, IOException {

    ServerResponse response;

    resultLatch.await();
    if ((response = resultRef.get()) == null) {

      IOException exception;

      if ((exception = exceptionRef.get()) != null) {

        throw exception;
      } else {

        Stint timeoutStint = timeoutStintRef.get();

        throw new ResponseTimeoutException("The timeout(%s) milliseconds was exceeded while waiting for a response from command(%s)", (timeoutStint == null) ? "unknown" : String.valueOf(timeoutStint.toMilliseconds()), command);
      }
    } else {

      return response;
    }
  }

  public void setResult (ServerResponse response) {

    resultRef.set(response);
    resultLatch.countDown();
  }

  public void setException (IOException ioException) {

    exceptionRef.set(ioException);
    resultLatch.countDown();
  }
}