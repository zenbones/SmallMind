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
package org.smallmind.scribe.pen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises {@link FileAppender}'s write, rollover, cleanup, and close behavior against a temp
 * directory and a {@link RecordFixture}. The rollover timestamp uses a colon-free pattern because the
 * default ISO timestamp contains {@code :}, which is illegal in Windows file names.
 */
@Test(groups = "unit")
public class FileAppenderTest {

  private static final String NEW_LINE = System.lineSeparator();

  private Path directory;
  private Path logPath;

  @BeforeMethod
  public void createDirectory ()
    throws IOException {

    directory = Files.createTempDirectory("scribe-fileappender-test");
    logPath = directory.resolve("app.log");
  }

  @AfterMethod
  public void removeDirectory ()
    throws IOException {

    if (directory != null) {
      try (java.util.stream.Stream<Path> walk = Files.walk(directory)) {
        walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException ioException) {
            throw new RuntimeException(ioException);
          }
        });
      }
    }
  }

  private Rollover sizeRollover (long maxBytes) {

    return new Rollover(new DateFormatTimestamp(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS")), new FileSizeRolloverRule(maxBytes, FileSizeQuantifier.BYTES));
  }

  private int archiveCount ()
    throws IOException {

    int count = 0;

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "app-*.log")) {
      for (Path ignored : stream) {
        count++;
      }
    }

    return count;
  }

  public void testWritesFormattedOutputToFile ()
    throws IOException, LoggerException {

    FileAppender appender = new FileAppender(logPath, new PatternFormatter("%m"));

    appender.publish(new RecordFixture().setMessage("hello"));
    appender.close();

    Assert.assertEquals(Files.readString(logPath, StandardCharsets.UTF_8), "hello" + NEW_LINE);
  }

  public void testRolloverArchivesPreviousFileAndStartsFresh ()
    throws IOException, LoggerException {

    FileAppender appender = new FileAppender(logPath, sizeRollover(1), new PatternFormatter("%m"));

    appender.publish(new RecordFixture().setMessage("first"));
    appender.publish(new RecordFixture().setMessage("second"));
    appender.close();

    Assert.assertTrue(archiveCount() >= 1);
    Assert.assertEquals(Files.readString(logPath, StandardCharsets.UTF_8), "second" + NEW_LINE);
  }

  public void testCleanupPrunesArchivesDuringRollover ()
    throws IOException, LoggerException {

    Cleanup cleanup = new Cleanup(new FileCountCleanupRule(1));
    FileAppender appender = new FileAppender(logPath, sizeRollover(1), cleanup, new PatternFormatter("%m"), null);

    appender.publish(new RecordFixture().setMessage("one"));
    appender.publish(new RecordFixture().setMessage("two"));
    appender.publish(new RecordFixture().setMessage("three"));
    appender.close();

    // FileCountCleanupRule(1) keeps a single archive; the vacuum runs after each rollover.
    Assert.assertTrue(archiveCount() <= 1);
  }

  public void testClosedAppenderDoesNotWrite ()
    throws IOException, LoggerException {

    FileAppender appender = new FileAppender(logPath, new PatternFormatter("%m"));

    appender.publish(new RecordFixture().setMessage("kept"));
    appender.close();
    // Post-close publish is routed to the error handler (printStackTrace here), leaving the file intact.
    appender.publish(new RecordFixture().setMessage("ignored"));

    Assert.assertEquals(Files.readString(logPath, StandardCharsets.UTF_8), "kept" + NEW_LINE);
  }

  public void testStringConstructorWritesOutput ()
    throws IOException, LoggerException {

    FileAppender appender = new FileAppender(logPath.toString(), new PatternFormatter("%m"));

    appender.publish(new RecordFixture().setMessage("via-string"));
    appender.close();

    Assert.assertEquals(Files.readString(logPath, StandardCharsets.UTF_8), "via-string" + NEW_LINE);
  }

  public void testSetLogFileReopensStreamAtNewPath ()
    throws IOException, LoggerException {

    Path otherPath = directory.resolve("other.log");
    FileAppender appender = new FileAppender(logPath, new PatternFormatter("%m"));

    appender.setLogFile(otherPath.toString());
    appender.publish(new RecordFixture().setMessage("redirected"));
    appender.close();

    Assert.assertEquals(appender.getLogPath(), otherPath);
    Assert.assertEquals(Files.readString(otherPath, StandardCharsets.UTF_8), "redirected" + NEW_LINE);
  }

  @Test(expectedExceptions = IOException.class)
  public void testDirectoryPathIsRejected ()
    throws IOException {

    new FileAppender(directory);
  }

  public void testRolloverAndCleanupAccessorsRoundTrip ()
    throws IOException, LoggerException {

    FileAppender appender = new FileAppender(logPath, new PatternFormatter("%m"));
    Rollover rollover = sizeRollover(1024);
    Cleanup cleanup = new Cleanup(new FileCountCleanupRule(2));

    Assert.assertNull(appender.getRollover());
    Assert.assertNull(appender.getCleanup());

    appender.setRollover(rollover);
    appender.setCleanup(cleanup);

    Assert.assertSame(appender.getRollover(), rollover);
    Assert.assertSame(appender.getCleanup(), cleanup);
    Assert.assertEquals(appender.getLogPath(), logPath);

    appender.close();
  }

  public void testConstructorOverloadsAllOpenAWritableStream ()
    throws IOException, LoggerException {

    PatternFormatter formatter = new PatternFormatter("%m");
    Rollover rollover = sizeRollover(1024);
    Cleanup cleanup = new Cleanup(new FileCountCleanupRule(2));

    FileAppender[] appenders = {
      new FileAppender(logPath, rollover),
      new FileAppender(logPath, cleanup),
      new FileAppender(logPath, formatter),
      new FileAppender(logPath, rollover, formatter),
      new FileAppender(logPath, cleanup, formatter),
      new FileAppender(logPath, formatter, null),
      new FileAppender(logPath, rollover, formatter, null),
      new FileAppender(logPath, cleanup, formatter, null),
      new FileAppender(logPath, rollover, cleanup, formatter, null)
    };

    for (FileAppender appender : appenders) {
      Assert.assertEquals(appender.getLogPath(), logPath);
      appender.close();
    }
  }

  public void testStringPathConstructorOverloadsAllOpenAWritableStream ()
    throws IOException, LoggerException {

    String logFile = logPath.toString();
    PatternFormatter formatter = new PatternFormatter("%m");
    Rollover rollover = sizeRollover(1024);
    Cleanup cleanup = new Cleanup(new FileCountCleanupRule(2));

    FileAppender[] appenders = {
      new FileAppender(logFile),
      new FileAppender(logFile, rollover),
      new FileAppender(logFile, cleanup),
      new FileAppender(logFile, formatter),
      new FileAppender(logFile, rollover, formatter),
      new FileAppender(logFile, cleanup, formatter),
      new FileAppender(logFile, formatter, null),
      new FileAppender(logFile, rollover, formatter, null),
      new FileAppender(logFile, cleanup, formatter, null),
      new FileAppender(logFile, rollover, cleanup, formatter, null)
    };

    for (FileAppender appender : appenders) {
      Assert.assertEquals(appender.getLogPath(), logPath);
      appender.close();
    }
  }

  @Test(expectedExceptions = LoggerException.class)
  public void testHandleOutputAfterCloseThrows ()
    throws IOException, LoggerException {

    FileAppender appender = new FileAppender(logPath, new PatternFormatter("%m"));

    appender.close();
    appender.handleOutput("after close" + NEW_LINE);
  }

  public void testCloseIsIdempotent ()
    throws IOException, LoggerException {

    FileAppender appender = new FileAppender(logPath, new PatternFormatter("%m"));

    appender.close();
    appender.close();
  }
}
