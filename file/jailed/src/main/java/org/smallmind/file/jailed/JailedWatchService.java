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
package org.smallmind.file.jailed;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A {@link WatchService} that wraps the watch service of the backing native file system so that
 * jailed paths can be watched.
 *
 * <p>A jailed path can not be handed to a native watch service directly, so registration goes
 * through {@link #register(JailedPath, WatchEvent.Kind[], WatchEvent.Modifier...)}, which
 * translates the path exactly as every other jailed operation does - and therefore refuses a
 * path that escapes the jail. Each native key is paired with a {@link JailedWatchKey} for the
 * life of this service, so the keys handed back by {@link #take()} and {@link #poll()} report
 * their watched path, and the context of their events, in jail space.
 *
 * <p>Instances are obtained from {@link JailedFileSystem#newWatchService()}.
 *
 * @see JailedWatchKey
 */
public class JailedWatchService implements WatchService {

  /**
   * The jailed key mirroring each native key registered through this service.
   */
  private final ConcurrentHashMap<WatchKey, JailedWatchKey> jailedWatchKeyMap = new ConcurrentHashMap<>();

  /**
   * The file system whose jail the watched paths belong to.
   */
  private final JailedFileSystem jailedFileSystem;

  /**
   * The native watch service that performs the actual watching.
   */
  private final WatchService nativeWatchService;

  /**
   * Constructs a watch service over the watch service of the backing native file system.
   *
   * @param jailedFileSystem   the {@link JailedFileSystem} whose paths will be watched
   * @param nativeWatchService the native {@link WatchService} to delegate to
   */
  public JailedWatchService (JailedFileSystem jailedFileSystem, WatchService nativeWatchService) {

    this.jailedFileSystem = jailedFileSystem;
    this.nativeWatchService = nativeWatchService;
  }

  /**
   * Registers a jailed path with this service.
   *
   * <p>Called by {@link JailedPath#register(WatchService, WatchEvent.Kind[],
   * WatchEvent.Modifier...)}. Registering a path that is already registered returns the key
   * created by the original registration, as the native services do.
   *
   * @param jailedPath the jailed path to watch
   * @param events     the events to watch for
   * @param modifiers  optional modifiers qualifying how the path is registered
   * @return the {@link JailedWatchKey} representing the registration
   * @throws IOException               if an I/O error occurs
   * @throws SecurityException         if the path can not be confined to the jail
   * @throws ProviderMismatchException if the path belongs to a different jail than this service
   */
  public WatchKey register (JailedPath jailedPath, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
    throws IOException {

    if (!jailedFileSystem.equals(jailedPath.getFileSystem())) {
      throw new ProviderMismatchException();
    } else {

      Path nativePath = jailedFileSystem.getJailedPathTranslator().unwrapPath(jailedPath);
      WatchKey nativeWatchKey = nativePath.register(nativeWatchService, events, modifiers);

      return jailedWatchKeyMap.computeIfAbsent(nativeWatchKey, key -> new JailedWatchKey(jailedFileSystem, key, jailedPath));
    }
  }

  /**
   * Closes this watch service, which closes the native watch service and invalidates every key
   * registered through it.
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void close ()
    throws IOException {

    try {
      nativeWatchService.close();
    } finally {
      jailedWatchKeyMap.clear();
    }
  }

  /**
   * Retrieves and removes the next signalled key, or returns {@code null} if none is available.
   *
   * @return the next signalled {@link JailedWatchKey}, or {@code null}
   */
  @Override
  public WatchKey poll () {

    return translateWatchKey(nativeWatchService.poll());
  }

  /**
   * Retrieves and removes the next signalled key, waiting up to the given time if none is
   * available yet.
   *
   * @param timeout how long to wait before giving up
   * @param unit    the unit of {@code timeout}
   * @return the next signalled {@link JailedWatchKey}, or {@code null} if the wait elapsed
   * @throws InterruptedException if interrupted while waiting
   */
  @Override
  public WatchKey poll (long timeout, TimeUnit unit)
    throws InterruptedException {

    return translateWatchKey(nativeWatchService.poll(timeout, unit));
  }

  /**
   * Retrieves and removes the next signalled key, waiting if none is available yet.
   *
   * @return the next signalled {@link JailedWatchKey}
   * @throws InterruptedException if interrupted while waiting
   */
  @Override
  public WatchKey take ()
    throws InterruptedException {

    return translateWatchKey(nativeWatchService.take());
  }

  /**
   * Returns the jailed key that mirrors a native key signalled by the native service.
   *
   * @param nativeWatchKey the signalled native key, which may be {@code null}
   * @return the corresponding {@link JailedWatchKey}, or {@code null} if {@code nativeWatchKey}
   * was {@code null}
   * @throws IllegalStateException if the native key was not registered through this service
   */
  private WatchKey translateWatchKey (WatchKey nativeWatchKey) {

    JailedWatchKey jailedWatchKey;

    if (nativeWatchKey == null) {

      return null;
    } else if ((jailedWatchKey = jailedWatchKeyMap.get(nativeWatchKey)) == null) {
      throw new IllegalStateException("The signalled key was not registered with this watch service");
    } else {

      return jailedWatchKey;
    }
  }
}
