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
import org.smallmind.memcached.cubby.command.Command;
import org.smallmind.memcached.cubby.command.DeleteCommand;
import org.smallmind.memcached.cubby.command.GetCommand;
import org.smallmind.memcached.cubby.command.Result;
import org.smallmind.memcached.cubby.command.SetCommand;
import org.smallmind.memcached.cubby.response.Response;

public class CubbyMemcachedClient {

  private final ConnectionMultiplexer connectionMultiplexer;
  private final CubbyConfiguration configuration;

  public CubbyMemcachedClient (CubbyConfiguration configuration, MemcachedHost... memcachedHosts) {

    this.configuration = configuration;

    connectionMultiplexer = new ConnectionMultiplexer(configuration, memcachedHosts);
  }

  public synchronized void start ()
    throws InterruptedException, IOException, CubbyOperationException {

    connectionMultiplexer.start();
  }

  public synchronized void stop ()
    throws InterruptedException, IOException {

    connectionMultiplexer.stop();
  }

  public Response send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException {

    return connectionMultiplexer.send(command, timeoutSeconds);
  }

  public <T> T get (String key)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key), null);
    Result<T> result = command.process(configuration.getCodec(), response);

    return result.isSuccessful() ? result.getValue() : null;
  }

  public <T> CASValue<T> casGet (String key)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setCas(true), null);
    Result<T> result = command.process(configuration.getCodec(), response);

    return result.isSuccessful() ? new CASValue<>(result) : null;
  }

  public <T> boolean set (String key, int expiration, T value)
    throws IOException, InterruptedException, CubbyOperationException {

    SetCommand command;
    Response response = connectionMultiplexer.send(command = new SetCommand().setKey(key).setExpiration(expiration).setValue(value), null);

    return command.process(configuration.getCodec(), response).isSuccessful();
  }

  public <T> boolean casSet (String key, int expiration, T value, long cas)
    throws IOException, InterruptedException, CubbyOperationException {

    SetCommand command;
    Response response = connectionMultiplexer.send(command = new SetCommand().setKey(key).setExpiration(expiration).setCas(cas).setValue(value), null);

    return command.process(configuration.getCodec(), response).isSuccessful();
  }

  public boolean delete (String key)
    throws IOException, InterruptedException, CubbyOperationException {

    DeleteCommand command;
    Response response = connectionMultiplexer.send(command = new DeleteCommand().setKey(key), null);

    return command.process(configuration.getCodec(), response).isSuccessful();
  }

  public boolean casDelete (String key, long cas)
    throws IOException, InterruptedException, CubbyOperationException {

    DeleteCommand command;
    Response response = connectionMultiplexer.send(command = new DeleteCommand().setKey(key).setCas(cas), null);

    return command.process(configuration.getCodec(), response).isSuccessful();
  }

  public boolean touch (String key, int expiration)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setExpiration(expiration).setValue(false), null);

    return command.process(configuration.getCodec(), response).isSuccessful();
  }

  public <T> T getAndTouch (String key, int expiration)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setExpiration(expiration), null);
    Result<T> result = command.process(configuration.getCodec(), response);

    return result.isSuccessful() ? result.getValue() : null;
  }

  public <T> CASValue<T> casGetAndTouch (String key, int expiration)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setExpiration(expiration).setCas(true), null);
    Result<T> result = command.process(configuration.getCodec(), response);

    return result.isSuccessful() ? new CASValue<>(result) : null;
  }
}
