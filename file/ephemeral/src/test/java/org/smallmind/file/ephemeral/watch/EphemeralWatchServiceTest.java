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
package org.smallmind.file.ephemeral.watch;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.List;
import org.smallmind.file.ephemeral.EphemeralFileSystem;
import org.smallmind.file.ephemeral.EphemeralFileSystemProvider;
import org.smallmind.file.ephemeral.EphemeralPath;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the {@link EphemeralWatchService} state machine together with the heap-event wiring
 * that bridges {@link org.smallmind.file.ephemeral.EphemeralFileStore} mutations into
 * {@link java.nio.file.WatchEvent} notifications. State-machine concerns (signalling, reset,
 * cancellation, closure) are exercised by calling {@link EphemeralWatchService#fire} directly
 * with a changed path. End-to-end behaviour is exercised by performing real
 * {@link Files} operations and asserting that the registered key observes the expected events
 * with relative {@link WatchEvent#context() contexts}.
 */
@Test(groups = "unit")
public class EphemeralWatchServiceTest {

  private EphemeralFileSystem ephemeralFileSystem;

  @BeforeClass
  public void beforeClass () {

    EphemeralFileSystemProvider provider = new EphemeralFileSystemProvider("ephemeral");

    ephemeralFileSystem = (EphemeralFileSystem)provider.getFileSystem(URI.create("ephemeral:///"));
  }

  @AfterMethod
  public void afterMethod () {

    ephemeralFileSystem.clear();
  }

  private EphemeralPath createDirectory (String text)
    throws IOException {

    EphemeralPath path = (EphemeralPath)ephemeralFileSystem.getPath(text);

    Files.createDirectory(path);

    return path;
  }

  public void testPollOnEmptyServiceReturnsNull ()
    throws IOException {

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      Assert.assertNull(service.poll());
    }
  }

  public void testFireRoutesToRegisteredKey ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_MODIFY});

      service.fire(watched, StandardWatchEventKinds.ENTRY_MODIFY, watched);

      WatchKey polled = service.poll();

      Assert.assertSame(polled, key);

      List<WatchEvent<?>> events = polled.pollEvents();

      Assert.assertEquals(events.size(), 1);
      Assert.assertEquals(events.get(0).kind(), StandardWatchEventKinds.ENTRY_MODIFY);
    }
  }

  public void testFireForUnsubscribedKindIgnored ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_CREATE});

      service.fire(watched, StandardWatchEventKinds.ENTRY_MODIFY, watched);

      Assert.assertNull(service.poll());
    }
  }

  public void testRepeatedFireWhileSignalledCoalescesToOneKeyOffering ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_MODIFY});

      service.fire(watched, StandardWatchEventKinds.ENTRY_MODIFY, watched);
      service.fire(watched, StandardWatchEventKinds.ENTRY_MODIFY, watched);

      Assert.assertSame(service.poll(), key);
      Assert.assertNull(service.poll());

      Assert.assertEquals(key.pollEvents().size(), 2);
    }
  }

  public void testResetWithPendingEventsRequeuesKey ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_MODIFY});

      service.fire(watched, StandardWatchEventKinds.ENTRY_MODIFY, watched);

      Assert.assertSame(service.poll(), key);

      service.fire(watched, StandardWatchEventKinds.ENTRY_MODIFY, watched);

      Assert.assertTrue(key.reset());
      Assert.assertSame(service.poll(), key);
    }
  }

  public void testResetWithoutPendingClearsSignalledFlag ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_MODIFY});

      service.fire(watched, StandardWatchEventKinds.ENTRY_MODIFY, watched);
      service.poll();
      key.pollEvents();

      Assert.assertTrue(key.reset());

      service.fire(watched, StandardWatchEventKinds.ENTRY_MODIFY, watched);

      Assert.assertSame(service.poll(), key);
    }
  }

  public void testCancelKeyStopsFurtherDispatch ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_MODIFY});

      key.cancel();

      Assert.assertFalse(key.isValid());

      service.fire(watched, StandardWatchEventKinds.ENTRY_MODIFY, watched);

      Assert.assertNull(service.poll());
    }
  }

  public void testCloseInvalidatesKeys ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");
    EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService();
    WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_MODIFY});

    service.close();

    Assert.assertTrue(service.isClosed());
    Assert.assertFalse(key.isValid());
  }

  @Test(expectedExceptions = ClosedWatchServiceException.class)
  public void testPollOnClosedServiceRejected ()
    throws IOException {

    EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService();

    service.close();
    service.poll();
  }

  public void testCloseIsIdempotent ()
    throws IOException {

    EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService();

    service.close();
    service.close();

    Assert.assertTrue(service.isClosed());
  }

  @Test(expectedExceptions = NotDirectoryException.class)
  public void testRegisterAgainstNonDirectoryRejected ()
    throws IOException {

    EphemeralPath file = (EphemeralPath)ephemeralFileSystem.getPath("/regular.txt");

    Files.writeString(file, "hi");

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {
      file.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_MODIFY});
    }
  }

  public void testFileCreationFiresEntryCreateWithRelativeContext ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_CREATE});

      Files.writeString(ephemeralFileSystem.getPath("/watched/leaf.txt"), "hi", StandardCharsets.UTF_8);

      Assert.assertSame(service.poll(), key);

      List<WatchEvent<?>> events = key.pollEvents();

      Assert.assertFalse(events.isEmpty());
      Assert.assertEquals(events.get(0).kind(), StandardWatchEventKinds.ENTRY_CREATE);
      Assert.assertEquals(events.get(0).context().toString(), "leaf.txt");
    }
  }

  public void testFileWriteFiresEntryModify ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    Files.writeString(ephemeralFileSystem.getPath("/watched/leaf.txt"), "seed", StandardCharsets.UTF_8);

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_MODIFY});

      Files.writeString(ephemeralFileSystem.getPath("/watched/leaf.txt"), "updated", StandardCharsets.UTF_8);

      Assert.assertSame(service.poll(), key);

      boolean modifyObserved = false;

      for (WatchEvent<?> event : key.pollEvents()) {
        if (StandardWatchEventKinds.ENTRY_MODIFY.equals(event.kind()) && "leaf.txt".equals(event.context().toString())) {
          modifyObserved = true;
        }
      }

      Assert.assertTrue(modifyObserved);
    }
  }

  public void testFileDeleteFiresEntryDelete ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    Files.writeString(ephemeralFileSystem.getPath("/watched/leaf.txt"), "hi", StandardCharsets.UTF_8);

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_DELETE});

      Files.delete(ephemeralFileSystem.getPath("/watched/leaf.txt"));

      Assert.assertSame(service.poll(), key);

      List<WatchEvent<?>> events = key.pollEvents();

      Assert.assertFalse(events.isEmpty());
      Assert.assertEquals(events.get(0).kind(), StandardWatchEventKinds.ENTRY_DELETE);
      Assert.assertEquals(events.get(0).context().toString(), "leaf.txt");
    }
  }

  public void testDeepDescendantBubblesToAncestorWatcher ()
    throws IOException {

    EphemeralPath watched = createDirectory("/watched");

    Files.createDirectory(ephemeralFileSystem.getPath("/watched/inner"));

    try (EphemeralWatchService service = (EphemeralWatchService)ephemeralFileSystem.newWatchService()) {

      WatchKey key = watched.register(service, new WatchEvent.Kind<?>[] {StandardWatchEventKinds.ENTRY_CREATE});

      Files.writeString(ephemeralFileSystem.getPath("/watched/inner/leaf.txt"), "hi", StandardCharsets.UTF_8);

      Assert.assertSame(service.poll(), key);

      List<WatchEvent<?>> events = key.pollEvents();
      String observed = null;

      for (WatchEvent<?> event : events) {
        if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
          observed = event.context().toString();
        }
      }

      Assert.assertEquals(observed, "inner/leaf.txt");
    }
  }
}
