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
package org.smallmind.memcached.cubby;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.smallmind.memcached.cubby.command.Command;
import org.smallmind.memcached.cubby.command.DeleteCommand;
import org.smallmind.memcached.cubby.command.GetCommand;
import org.smallmind.memcached.cubby.command.Result;
import org.smallmind.memcached.cubby.command.SetCommand;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.utility.ProxyCASResponse;
import org.smallmind.memcached.utility.ProxyMemcachedClient;

/**
 * Memcached client backed by the Cubby NIO implementation. This class coordinates routing to hosts,
 * serialization via the configured codec and exposes a ProxyMemcachedClient-compatible API.
 */
public class CubbyMemcachedClient implements ProxyMemcachedClient {

  private final ConnectionMultiplexer connectionMultiplexer;
  private final CubbyConfiguration configuration;

  /**
   * Creates a new client bound to the supplied configuration and hosts.
   *
   * @param configuration  runtime configuration including codec, timeouts and routing strategy
   * @param memcachedHosts the hosts that make up the memcached cluster
   */
  public CubbyMemcachedClient (CubbyConfiguration configuration, MemcachedHost... memcachedHosts) {

    this.configuration = configuration;

    connectionMultiplexer = new ConnectionMultiplexer(configuration, memcachedHosts);
  }

  /**
   * Opens the NIO connections to each configured host.
   *
   * @throws InterruptedException    if interrupted while connecting
   * @throws IOException             if a socket cannot be opened
   * @throws CubbyOperationException if the client fails to initialize
   */
  public synchronized void start ()
    throws InterruptedException, IOException, CubbyOperationException {

    connectionMultiplexer.start();
  }

  /**
   * Shuts down all open connections.
   *
   * @throws InterruptedException if interrupted while closing connections
   * @throws IOException          if a socket closure fails
   */
  public synchronized void stop ()
    throws InterruptedException, IOException {

    connectionMultiplexer.stop();
  }

  /**
   * Returns the configured default timeout.
   *
   * @return timeout in milliseconds
   */
  @Override
  public long getDefaultTimeout () {

    return configuration.getDefaultRequestTimeoutMilliseconds();
  }

  /**
   * Unsupported clear operation; memcached servers handle flush themselves.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public void clear () {

    throw new UnsupportedOperationException();
  }

  /**
   * Stops the client and releases resources.
   *
   * @throws InterruptedException if interrupted while closing
   * @throws IOException          if closing the connection fails
   */
  @Override
  public void shutdown ()
    throws InterruptedException, IOException {

    stop();
  }

  /**
   * Wraps a value and CAS token in a ProxyCASResponse implementation.
   *
   * @param cas   the compare-and-swap token returned by memcached
   * @param value the decoded value
   * @param <T>   the value type
   * @return CAS wrapper carrying both the token and value
   */
  @Override
  public <T> ProxyCASResponse<T> createCASResponse (long cas, T value) {

    return new CASValue<>(cas, value);
  }

  /**
   * Fetches multiple keys, issuing sequential get requests.
   *
   * @param keys keys to retrieve
   * @param <T>  expected value type
   * @return map of key to decoded value (missing keys map to {@code null})
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for responses
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if deserialization cannot load a class
   */
  @Override
  public <T> Map<String, T> get (Collection<String> keys)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    HashMap<String, T> resultMap = new HashMap<>();

    for (String key : keys) {
      resultMap.put(key, get(key));
    }

    return resultMap;
  }

  /**
   * Sends a raw command to the cluster honoring the supplied timeout.
   *
   * @param command        command to transmit
   * @param timeoutSeconds optional timeout in seconds; {@code null} uses configuration defaults
   * @return parsed response from the server
   * @throws InterruptedException    if interrupted while waiting
   * @throws IOException             if network communication fails
   * @throws CubbyOperationException if the server replies with an error or routing fails
   */
  public Response send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException {

    return connectionMultiplexer.send(command, timeoutSeconds);
  }

  /**
   * Retrieves a value from memcached.
   *
   * @param key cache key to fetch
   * @param <T> expected value type
   * @return decoded value or {@code null} if not found
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if deserialization cannot load a class
   */
  public <T> T get (String key)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key), null);
    Result result = command.process(response);

    return result.isSuccessful() ? (T)configuration.getCodec().deserialize(result.getValue()) : null;
  }

  /**
   * Retrieves a value and its CAS token.
   *
   * @param key cache key to fetch
   * @param <T> expected value type
   * @return CAS wrapper or {@code null} if not found
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if deserialization cannot load a class
   */
  public <T> CASValue<T> casGet (String key)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setCas(true), null);
    Result result = command.process(response);

    return result.isSuccessful() ? new CASValue<>(result, configuration.getCodec()) : null;
  }

  /**
   * Stores a value under the provided key.
   *
   * @param key        cache key to set
   * @param expiration expiration in seconds
   * @param value      value to encode and store
   * @param <T>        value type
   * @return {@code true} if the operation succeeded
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting
   * @throws CubbyOperationException if the server rejects the request
   */
  public <T> boolean set (String key, int expiration, T value)
    throws IOException, InterruptedException, CubbyOperationException {

    SetCommand command;
    Response response = connectionMultiplexer.send(command = new SetCommand().setKey(key).setExpiration(expiration).setValue(configuration.getCodec().serialize(value)), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Performs a CAS set, storing the value only if the supplied token matches.
   *
   * @param key        cache key to set
   * @param expiration expiration in seconds
   * @param value      value to encode and store
   * @param cas        compare-and-swap token to validate
   * @param <T>        value type
   * @return {@code true} if the operation succeeded
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting
   * @throws CubbyOperationException if the server rejects the request
   */
  public <T> boolean casSet (String key, int expiration, T value, long cas)
    throws IOException, InterruptedException, CubbyOperationException {

    SetCommand command;
    Response response = connectionMultiplexer.send(command = new SetCommand().setKey(key).setExpiration(expiration).setCas(cas).setValue(configuration.getCodec().serialize(value)), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Deletes the value associated with the key.
   *
   * @param key cache key to remove
   * @return {@code true} if deletion succeeded
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting
   * @throws CubbyOperationException if the server rejects the request
   */
  public boolean delete (String key)
    throws IOException, InterruptedException, CubbyOperationException {

    DeleteCommand command;
    Response response = connectionMultiplexer.send(command = new DeleteCommand().setKey(key), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Performs a CAS delete, removing the value only if the token matches.
   *
   * @param key cache key to remove
   * @param cas compare-and-swap token to validate
   * @return {@code true} if deletion succeeded
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting
   * @throws CubbyOperationException if the server rejects the request
   */
  public boolean casDelete (String key, long cas)
    throws IOException, InterruptedException, CubbyOperationException {

    DeleteCommand command;
    Response response = connectionMultiplexer.send(command = new DeleteCommand().setKey(key).setCas(cas), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Updates a key's expiration without fetching the value.
   *
   * @param key        cache key to touch
   * @param expiration new expiration in seconds
   * @return {@code true} if the touch succeeded
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if a response body cannot be deserialized
   */
  public boolean touch (String key, int expiration)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setExpiration(expiration).setValue(false), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Retrieves a value and updates its expiration.
   *
   * @param key        cache key to fetch
   * @param expiration new expiration in seconds
   * @param <T>        expected value type
   * @return decoded value or {@code null} if not found
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if deserialization cannot load a class
   */
  public <T> T getAndTouch (String key, int expiration)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setExpiration(expiration), null);
    Result result = command.process(response);

    return result.isSuccessful() ? (T)configuration.getCodec().deserialize(result.getValue()) : null;
  }

  /**
   * Retrieves a value with CAS token and updates its expiration.
   *
   * @param key        cache key to fetch
   * @param expiration new expiration in seconds
   * @param <T>        expected value type
   * @return CAS wrapper or {@code null} if not found
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if deserialization cannot load a class
   */
  public <T> CASValue<T> casGetAndTouch (String key, int expiration)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setExpiration(expiration).setCas(true), null);
    Result result = command.process(response);

    return result.isSuccessful() ? new CASValue<>(result, configuration.getCodec()) : null;
  }
}
