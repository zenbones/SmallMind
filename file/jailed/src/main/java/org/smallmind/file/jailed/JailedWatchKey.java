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
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link WatchKey} that mirrors a registration made against the backing native file system,
 * presenting both the watched path and the events it carries in jail space.
 *
 * <p>Validity, reset and cancellation are delegated to the native key, so the watch service
 * continues to behave exactly as the native one does; only the paths visible to the caller are
 * translated.
 *
 * @see JailedWatchService
 */
public class JailedWatchKey implements WatchKey {

  /**
   * The file system whose jail the translated paths belong to.
   */
  private final JailedFileSystem jailedFileSystem;

  /**
   * The native key whose registration this key mirrors.
   */
  private final WatchKey nativeWatchKey;

  /**
   * The jailed path that was registered, reported as this key's watchable.
   */
  private final Path jailedPath;

  /**
   * Constructs a key mirroring a native registration.
   *
   * @param jailedFileSystem the {@link JailedFileSystem} in whose jail the paths lie
   * @param nativeWatchKey   the native {@link WatchKey} returned by the native registration
   * @param jailedPath       the jailed path that was registered
   */
  public JailedWatchKey (JailedFileSystem jailedFileSystem, WatchKey nativeWatchKey, Path jailedPath) {

    this.jailedFileSystem = jailedFileSystem;
    this.nativeWatchKey = nativeWatchKey;
    this.jailedPath = jailedPath;
  }

  /**
   * Returns the native key that this key mirrors.
   *
   * @return the native {@link WatchKey}
   */
  public WatchKey getNativeWatchKey () {

    return nativeWatchKey;
  }

  /**
   * Indicates whether this key is still valid.
   *
   * @return {@code true} if the native registration is still valid
   */
  @Override
  public boolean isValid () {

    return nativeWatchKey.isValid();
  }

  /**
   * Retrieves and removes all pending events for this key, with each event context translated
   * into jail space.
   *
   * @return the list of pending events, which is empty if there are none
   * @throws UncheckedIOException if an event context can not be translated into jail space
   * @throws SecurityException    if an event context lies outside the jail
   */
  @Override
  public List<WatchEvent<?>> pollEvents () {

    LinkedList<WatchEvent<?>> jailedWatchEventList = new LinkedList<>();

    for (WatchEvent<?> nativeWatchEvent : nativeWatchKey.pollEvents()) {
      jailedWatchEventList.add(translateWatchEvent(nativeWatchEvent));
    }

    return jailedWatchEventList;
  }

  /**
   * Translates a native watch event into an event whose context lies in jail space.
   *
   * <p>An event whose context is not a path - such as
   * {@link java.nio.file.StandardWatchEventKinds#OVERFLOW} - is passed through untouched.
   *
   * @param nativeWatchEvent the native event to translate
   * @return the event as it should be observed from inside the jail
   * @throws UncheckedIOException if the context can not be translated into jail space
   * @throws SecurityException    if the context lies outside the jail
   */
  @SuppressWarnings("unchecked")
  private WatchEvent<?> translateWatchEvent (WatchEvent<?> nativeWatchEvent) {

    if (!(nativeWatchEvent.context() instanceof Path)) {

      return nativeWatchEvent;
    } else {
      try {

        return new JailedWatchEvent<>((WatchEvent.Kind<Path>)nativeWatchEvent.kind(), nativeWatchEvent.count(), jailedFileSystem.getJailedPathTranslator().wrapPath(jailedFileSystem, (Path)nativeWatchEvent.context()));
      } catch (IOException ioException) {
        throw new UncheckedIOException(ioException);
      }
    }
  }

  /**
   * Resets this key so that it can receive further events.
   *
   * @return {@code true} if the key remains valid and has been reset
   */
  @Override
  public boolean reset () {

    return nativeWatchKey.reset();
  }

  /**
   * Cancels the registration with the watch service.
   */
  @Override
  public void cancel () {

    nativeWatchKey.cancel();
  }

  /**
   * Returns the jailed path for which this key was created.
   *
   * @return the registered jailed {@link Path}
   */
  @Override
  public Watchable watchable () {

    return jailedPath;
  }
}
