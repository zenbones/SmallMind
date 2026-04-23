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
 * Primary entry point for interacting with a memcached cluster using the Cubby NIO implementation.
 *
 * <p>{@code CubbyMemcachedClient} implements {@link ProxyMemcachedClient} and delegates all
 * network I/O to an internal {@link ConnectionMultiplexer} that distributes load across one or
 * more {@link ConnectionCoordinator} instances. Values are serialized and deserialized using the
 * {@link org.smallmind.memcached.cubby.codec.CubbyCodec} configured in the supplied
 * {@link CubbyConfiguration}.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * CubbyMemcachedClient client = new CubbyMemcachedClient(
 *     CubbyConfiguration.OPTIMAL,
 *     new MemcachedHost("primary", "cache1.example.com", 11211));
 * client.start();
 * client.set("key", 300, myObject);
 * MyObject value = client.get("key");
 * client.stop();
 * }</pre>
 */
public class CubbyMemcachedClient implements ProxyMemcachedClient {

  private final ConnectionMultiplexer connectionMultiplexer;
  private final CubbyConfiguration configuration;

  /**
   * Creates a new client for the specified hosts using the given configuration.
   *
   * @param configuration  runtime configuration covering codec, routing, timeouts, and auth
   * @param memcachedHosts one or more hosts that form the memcached cluster
   */
  public CubbyMemcachedClient (CubbyConfiguration configuration, MemcachedHost... memcachedHosts) {

    this.configuration = configuration;

    connectionMultiplexer = new ConnectionMultiplexer(configuration, memcachedHosts);
  }

  /**
   * Opens NIO connections to each configured host and starts the background health-monitor thread.
   * Must be called before any cache operations.
   *
   * @throws InterruptedException    if the calling thread is interrupted while waiting for connections
   * @throws IOException             if a socket cannot be opened to one of the hosts
   * @throws CubbyOperationException if the multiplexer fails to initialize
   */
  public synchronized void start ()
    throws InterruptedException, IOException, CubbyOperationException {

    connectionMultiplexer.start();
  }

  /**
   * Closes all open connections and stops the background health-monitor thread.
   * After this call the client must not be used for further cache operations.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for shutdown
   * @throws IOException          if closing a socket fails
   */
  public synchronized void stop ()
    throws InterruptedException, IOException {

    connectionMultiplexer.stop();
  }

  /**
   * Returns the default per-request timeout as configured in {@link CubbyConfiguration}.
   *
   * @return the default request timeout in milliseconds; {@code 0} means no timeout
   */
  @Override
  public long getDefaultTimeout () {

    return configuration.getDefaultRequestTimeoutMilliseconds();
  }

  /**
   * Not supported. Memcached servers manage their own flush lifecycle; there is no
   * client-side clear operation in this implementation.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public void clear () {

    throw new UnsupportedOperationException();
  }

  /**
   * Shuts down the client by delegating to {@link #stop()}. Implements the
   * {@link ProxyMemcachedClient#shutdown()} contract.
   *
   * @throws InterruptedException if interrupted while awaiting shutdown
   * @throws IOException          if closing a connection fails
   */
  @Override
  public void shutdown ()
    throws InterruptedException, IOException {

    stop();
  }

  /**
   * Wraps an already-decoded value and its CAS token in a {@link CASValue} for use as a
   * {@link ProxyCASResponse}.
   *
   * @param cas   the compare-and-swap token returned by the server
   * @param value the decoded cache value
   * @param <T>   the type of the cached value
   * @return a new {@link CASValue} carrying both the token and value
   */
  @Override
  public <T> ProxyCASResponse<T> createCASResponse (long cas, T value) {

    return new CASValue<>(cas, value);
  }

  /**
   * Retrieves multiple keys from the cache by issuing individual {@link #get(String)} requests
   * for each key in the supplied collection.
   *
   * @param keys the cache keys to retrieve
   * @param <T>  the expected value type shared by all keys
   * @return a map from key to decoded value; keys not found in the cache map to {@code null}
   * @throws IOException             if network I/O fails for any key
   * @throws InterruptedException    if interrupted while waiting for a response
   * @throws CubbyOperationException if the server rejects a request
   * @throws ClassNotFoundException  if the class of a deserialized value cannot be found
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
   * Sends a raw {@link Command} to the cluster using the optionally specified timeout.
   * This lower-level method is available for callers that construct commands directly.
   *
   * @param command        the command to transmit to the server
   * @param timeoutSeconds optional timeout override in seconds; {@code null} defers to the
   *                       configured default
   * @return the parsed {@link Response} from the server
   * @throws InterruptedException    if interrupted while awaiting completion
   * @throws IOException             if network communication fails
   * @throws CubbyOperationException if routing fails or the server responds with an error
   */
  public Response send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException {

    return connectionMultiplexer.send(command, timeoutSeconds);
  }

  /**
   * Retrieves the value stored under the given key.
   *
   * @param key the cache key to look up
   * @param <T> the expected type of the stored value
   * @return the deserialized value, or {@code null} if the key does not exist in the cache
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for the server response
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if the class of the deserialized value cannot be found
   */
  public <T> T get (String key)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key), null);
    Result result = command.process(response);

    return result.isSuccessful() ? (T)configuration.getCodec().deserialize(result.getValue()) : null;
  }

  /**
   * Retrieves the value stored under the given key together with its CAS token.
   *
   * @param key the cache key to look up
   * @param <T> the expected type of the stored value
   * @return a {@link CASValue} holding the decoded value and CAS token, or {@code null} if the
   * key does not exist in the cache
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for the server response
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if the class of the deserialized value cannot be found
   */
  public <T> CASValue<T> casGet (String key)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setCas(true), null);
    Result result = command.process(response);

    return result.isSuccessful() ? new CASValue<>(result, configuration.getCodec()) : null;
  }

  /**
   * Stores a value in the cache under the given key, unconditionally overwriting any existing entry.
   *
   * @param key        the cache key under which the value is stored
   * @param expiration the time-to-live in seconds; {@code 0} means no expiration
   * @param value      the value to serialize and store
   * @param <T>        the type of the value being stored
   * @return {@code true} if the server acknowledged the store; {@code false} otherwise
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for the server response
   * @throws CubbyOperationException if the server rejects the request
   */
  public <T> boolean set (String key, int expiration, T value)
    throws IOException, InterruptedException, CubbyOperationException {

    SetCommand command;
    Response response = connectionMultiplexer.send(command = new SetCommand().setKey(key).setExpiration(expiration).setValue(configuration.getCodec().serialize(value)), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Stores a value in the cache only if the supplied CAS token matches the server's current token
   * for that key, implementing an optimistic-locking update.
   *
   * @param key        the cache key to update
   * @param expiration the time-to-live in seconds; {@code 0} means no expiration
   * @param value      the value to serialize and store
   * @param cas        the compare-and-swap token obtained from a prior {@link #casGet} call
   * @param <T>        the type of the value being stored
   * @return {@code true} if the token matched and the value was stored; {@code false} if the token
   * was stale (another writer modified the entry)
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for the server response
   * @throws CubbyOperationException if the server rejects the request
   */
  public <T> boolean casSet (String key, int expiration, T value, long cas)
    throws IOException, InterruptedException, CubbyOperationException {

    SetCommand command;
    Response response = connectionMultiplexer.send(command = new SetCommand().setKey(key).setExpiration(expiration).setCas(cas).setValue(configuration.getCodec().serialize(value)), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Removes the entry associated with the given key from the cache.
   *
   * @param key the cache key to delete
   * @return {@code true} if the server confirmed the deletion; {@code false} if the key was not found
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for the server response
   * @throws CubbyOperationException if the server rejects the request
   */
  public boolean delete (String key)
    throws IOException, InterruptedException, CubbyOperationException {

    DeleteCommand command;
    Response response = connectionMultiplexer.send(command = new DeleteCommand().setKey(key), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Removes the entry associated with the given key only if the supplied CAS token matches,
   * providing an optimistic-locking delete.
   *
   * @param key the cache key to delete
   * @param cas the compare-and-swap token that must match the server's current token
   * @return {@code true} if the token matched and the entry was deleted; {@code false} otherwise
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for the server response
   * @throws CubbyOperationException if the server rejects the request
   */
  public boolean casDelete (String key, long cas)
    throws IOException, InterruptedException, CubbyOperationException {

    DeleteCommand command;
    Response response = connectionMultiplexer.send(command = new DeleteCommand().setKey(key).setCas(cas), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Refreshes the expiration of the given key without returning its value. This is equivalent to
   * a memcached {@code touch} command.
   *
   * @param key        the cache key whose expiration is to be updated
   * @param expiration the new time-to-live in seconds; {@code 0} means no expiration
   * @return {@code true} if the key existed and its expiration was updated; {@code false} if not found
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for the server response
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if a response payload cannot be deserialized
   */
  public boolean touch (String key, int expiration)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setExpiration(expiration).setValue(false), null);

    return command.process(response).isSuccessful();
  }

  /**
   * Retrieves the value associated with the given key and simultaneously updates its expiration,
   * equivalent to a memcached {@code gat} (get-and-touch) command.
   *
   * @param key        the cache key to fetch
   * @param expiration the new time-to-live in seconds; {@code 0} means no expiration
   * @param <T>        the expected type of the stored value
   * @return the deserialized value, or {@code null} if the key does not exist in the cache
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for the server response
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if the class of the deserialized value cannot be found
   */
  public <T> T getAndTouch (String key, int expiration)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setExpiration(expiration), null);
    Result result = command.process(response);

    return result.isSuccessful() ? (T)configuration.getCodec().deserialize(result.getValue()) : null;
  }

  /**
   * Retrieves the value and CAS token for the given key while simultaneously updating its
   * expiration, equivalent to a memcached {@code gats} command.
   *
   * @param key        the cache key to fetch
   * @param expiration the new time-to-live in seconds; {@code 0} means no expiration
   * @param <T>        the expected type of the stored value
   * @return a {@link CASValue} holding the decoded value and CAS token, or {@code null} if the
   * key does not exist in the cache
   * @throws IOException             if network I/O fails
   * @throws InterruptedException    if interrupted while waiting for the server response
   * @throws CubbyOperationException if the server rejects the request
   * @throws ClassNotFoundException  if the class of the deserialized value cannot be found
   */
  public <T> CASValue<T> casGetAndTouch (String key, int expiration)
    throws IOException, InterruptedException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = connectionMultiplexer.send(command = new GetCommand().setKey(key).setExpiration(expiration).setCas(true), null);
    Result result = command.process(response);

    return result.isSuccessful() ? new CASValue<>(result, configuration.getCodec()) : null;
  }
}
