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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Confirms that a jailed path can be watched. A native watch service can not accept a jailed
 * path at all, so registration goes through the jail, and the key and events that come back must
 * describe the change in jail space rather than in native terms.
 */
@Test(groups = "unit")
public class JailedWatchServiceTest {

  private static final long POLL_TIMEOUT_SECONDS = 30;

  private Path nativeRoot;
  private FileSystem jailedFileSystem;

  @BeforeMethod
  public void beforeMethod ()
    throws IOException {

    nativeRoot = Files.createTempDirectory("jailed-watch-test-");
    jailedFileSystem = new JailedFileSystemProvider("jailed", new RootedPathTranslator(nativeRoot)).getFileSystem(URI.create("jailed:///"));
  }

  @AfterMethod
  public void afterMethod ()
    throws IOException {

    if ((nativeRoot != null) && Files.exists(nativeRoot)) {
      try (Stream<Path> walk = Files.walk(nativeRoot)) {
        walk.sorted(Comparator.reverseOrder()).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException ignored) {
            // best-effort cleanup
          }
        });
      }
    }
  }

  public void testRegisteredKeyAndEventsAreExpressedInJailSpace ()
    throws IOException, InterruptedException {

    Path watchedDirectory = jailedFileSystem.getPath("/watched");

    Files.createDirectory(watchedDirectory);

    try (WatchService watchService = jailedFileSystem.newWatchService()) {

      WatchKey registeredKey = watchedDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

      Assert.assertTrue(registeredKey instanceof JailedWatchKey);
      Assert.assertSame(registeredKey.watchable(), watchedDirectory);

      Files.writeString(jailedFileSystem.getPath("/watched/appeared.txt"), "hello", StandardCharsets.UTF_8);

      WatchKey signalledKey = watchService.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      Assert.assertNotNull(signalledKey, "no watch event arrived within " + POLL_TIMEOUT_SECONDS + " seconds");
      Assert.assertSame(signalledKey, registeredKey);
      Assert.assertSame(signalledKey.watchable(), watchedDirectory);

      boolean sawCreation = false;

      for (WatchEvent<?> watchEvent : signalledKey.pollEvents()) {
        if (StandardWatchEventKinds.ENTRY_CREATE.equals(watchEvent.kind())) {

          Path context = (Path)watchEvent.context();

          Assert.assertTrue(context instanceof JailedPath, "expected JailedPath, got " + context.getClass());
          Assert.assertSame(context.getFileSystem(), jailedFileSystem);
          Assert.assertEquals(context.toString(), "appeared.txt");

          sawCreation = true;
        }
      }

      Assert.assertTrue(sawCreation, "no creation event was observed");
      Assert.assertTrue(signalledKey.reset());

      signalledKey.cancel();

      Assert.assertFalse(signalledKey.isValid());
    }
  }

  public void testRegistrationIsConfinedToTheJail ()
    throws IOException {

    Files.createDirectory(jailedFileSystem.getPath("/watched"));

    try (WatchService watchService = jailedFileSystem.newWatchService()) {

      WatchKey key = jailedFileSystem.getPath("/../../watched").register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

      Assert.assertEquals(((JailedWatchKey)key).getNativeWatchKey().watchable(), nativeRoot.resolve("watched"));
    }
  }
}
