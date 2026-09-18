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
package org.smallmind.file.ephemeral;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Differential conformance suite: every scenario is executed twice, once against the platform's
 * default file system in a throwaway temporary directory and once against an
 * {@link EphemeralFileSystem}, and the two observations must agree.
 *
 * <p>This exists because unit tests written directly against the ephemeral file system can only
 * ever assert the behaviour someone believed it had. Running the same code against a real file
 * system and comparing makes the specification itself the oracle, which is what catches a
 * divergence nobody thought to look for.
 *
 * <p>Observations are compared after normalisation, because two correct file systems still describe
 * themselves differently:
 * <ul>
 *   <li>path separators are rewritten to {@code '/'}, since the platform provider uses
 *       {@code '\'} on Windows</li>
 *   <li>a thrown exception is reduced to its class name, since the messages embed paths</li>
 * </ul>
 * Anything that legitimately cannot agree across platforms is asserted as an invariant within each
 * file system instead — see {@link #testUriRoundTripsWithinEachFileSystem()}. Symbolic link
 * creation is one such case: it requires elevation on Windows, so links are covered by
 * {@link EphemeralFileOperationsTest} rather than differentially.
 */
@Test(groups = "unit")
public class EphemeralConformanceTest {

  private EphemeralFileSystem ephemeralFileSystem;

  /**
   * Renders a path relative to a scenario's root, so that the absolute prefix of a temporary
   * directory does not enter the comparison.
   *
   * @param root the scenario root
   * @param path the path to render
   * @return the relative rendering
   */
  private static String relative (Path root, Path path) {

    return root.relativize(path).toString();
  }

  /**
   * A single unit of behaviour, expressed once and run against both file systems.
   */
  private interface Scenario {

    Object run (FileSystem fileSystem, Path root)
      throws Exception;
  }

  @BeforeClass
  public void beforeClass () {

    EphemeralFileSystemProvider provider = new EphemeralFileSystemProvider("ephemeral");

    ephemeralFileSystem = (EphemeralFileSystem)provider.getFileSystem(URI.create("ephemeral:///"));
  }

  @AfterClass
  public void afterClass () {

    ephemeralFileSystem.clear();
  }

  /**
   * Reduces an observation to a form two correct file systems will agree on.
   *
   * @param fileSystem the file system that produced the observation
   * @param observed   the returned value, or the thrown throwable
   * @return the normalised rendering of the observation
   */
  private String normalize (FileSystem fileSystem, Object observed) {

    if (observed instanceof Throwable) {

      return observed.getClass().getSimpleName();
    } else {

      return String.valueOf(observed).replace(fileSystem.getSeparator(), "/");
    }
  }

  /**
   * Removes a directory and everything beneath it.
   *
   * @param directory the directory to remove
   * @throws IOException if the tree cannot be removed
   */
  private void removeTree (Path directory)
    throws IOException {

    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile (Path file, BasicFileAttributes attributes)
        throws IOException {

        Files.delete(file);

        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory (Path visited, IOException ioException)
        throws IOException {

        Files.delete(visited);

        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Runs every scenario against both file systems and fails with the complete list of
   * disagreements, rather than stopping at the first, so that one run shows the whole picture.
   *
   * @param scenarioMap the scenarios to run, keyed by label
   * @throws IOException if a scenario's scratch directory cannot be prepared or removed
   */
  private void assertAllAgree (Map<String, Scenario> scenarioMap)
    throws IOException {

    LinkedList<String> disagreementList = new LinkedList<>();

    for (Map.Entry<String, Scenario> scenarioEntry : scenarioMap.entrySet()) {

      FileSystem nativeFileSystem = FileSystems.getDefault();
      Path nativeRoot = Files.createTempDirectory("conformance");
      String nativeObservation;
      String ephemeralObservation;

      try {
        try {
          nativeObservation = normalize(nativeFileSystem, scenarioEntry.getValue().run(nativeFileSystem, nativeRoot));
        } catch (Throwable throwable) {
          nativeObservation = normalize(nativeFileSystem, throwable);
        }
      } finally {
        if (Files.exists(nativeRoot)) {
          removeTree(nativeRoot);
        }
      }

      ephemeralFileSystem.clear();

      Path ephemeralRoot = ephemeralFileSystem.getPath("/conformance");

      Files.createDirectory(ephemeralRoot);
      try {
        ephemeralObservation = normalize(ephemeralFileSystem, scenarioEntry.getValue().run(ephemeralFileSystem, ephemeralRoot));
      } catch (Throwable throwable) {
        ephemeralObservation = normalize(ephemeralFileSystem, throwable);
      }

      if (!nativeObservation.equals(ephemeralObservation)) {
        disagreementList.add(scenarioEntry.getKey() + ": native[" + nativeObservation + "] ephemeral[" + ephemeralObservation + "]");
      }
    }

    ephemeralFileSystem.clear();

    if (!disagreementList.isEmpty()) {
      Assert.fail("The ephemeral file system diverged from the platform file system in " + disagreementList.size() + " scenario(s):" + System.lineSeparator() + String.join(System.lineSeparator(), disagreementList));
    }
  }

  public void testPathAlgebraAgrees ()
    throws IOException {

    LinkedHashMap<String, Scenario> scenarioMap = new LinkedHashMap<>();

    scenarioMap.put("root of an absolute path", (fileSystem, root) -> fileSystem.getPath("/a/b").getRoot().toString());
    scenarioMap.put("root renders as a separator", (fileSystem, root) -> fileSystem.getPath("/").toString());
    scenarioMap.put("root has no name elements", (fileSystem, root) -> fileSystem.getPath("/").getNameCount());
    scenarioMap.put("empty path renders as empty", (fileSystem, root) -> fileSystem.getPath("").toString());
    scenarioMap.put("empty path has one name element", (fileSystem, root) -> fileSystem.getPath("").getNameCount());
    scenarioMap.put("empty path has no parent", (fileSystem, root) -> String.valueOf(fileSystem.getPath("").getParent()));
    scenarioMap.put("repeated separators collapse", (fileSystem, root) -> fileSystem.getPath("/a//b").toString());
    scenarioMap.put("trailing separator is dropped", (fileSystem, root) -> fileSystem.getPath("/a/b/").toString());
    scenarioMap.put("relative path renders without a leading separator", (fileSystem, root) -> fileSystem.getPath("a/b").toString());
    scenarioMap.put("absolute path is not a prefix of a relative one", (fileSystem, root) -> fileSystem.getPath("/a/b").startsWith(fileSystem.getPath("a")));
    scenarioMap.put("the root is a prefix of any absolute path", (fileSystem, root) -> fileSystem.getPath("/a/b").startsWith(fileSystem.getPath("/")));
    scenarioMap.put("a partial name is not a prefix", (fileSystem, root) -> fileSystem.getPath("/foo/bar").startsWith(fileSystem.getPath("/fo")));
    scenarioMap.put("endsWith a trailing name", (fileSystem, root) -> fileSystem.getPath("/a/b/c").endsWith(fileSystem.getPath("c")));
    scenarioMap.put("endsWith an absolute path of equal length", (fileSystem, root) -> fileSystem.getPath("/a/b/c").endsWith(fileSystem.getPath("/a/b/c")));
    scenarioMap.put("relativize onto a shorter path", (fileSystem, root) -> fileSystem.getPath("/a/b/c").relativize(fileSystem.getPath("/a")).toString());
    scenarioMap.put("relativize onto a sibling", (fileSystem, root) -> fileSystem.getPath("/a/b").relativize(fileSystem.getPath("/a/c")).toString());
    scenarioMap.put("relativize onto a descendant", (fileSystem, root) -> fileSystem.getPath("/a/b").relativize(fileSystem.getPath("/a/b/c/d")).toString());
    scenarioMap.put("relativize onto an equal path", (fileSystem, root) -> fileSystem.getPath("/a/b").relativize(fileSystem.getPath("/a/b")).toString());
    scenarioMap.put("normalize discards a single dot", (fileSystem, root) -> fileSystem.getPath("/a/./b").normalize().toString());
    scenarioMap.put("normalize applies a parent reference", (fileSystem, root) -> fileSystem.getPath("/a/b/../c").normalize().toString());
    scenarioMap.put("normalize cannot ascend past the root", (fileSystem, root) -> fileSystem.getPath("/a/../..").normalize().toString());
    scenarioMap.put("normalize retains a leading parent reference", (fileSystem, root) -> fileSystem.getPath("../a").normalize().toString());
    scenarioMap.put("normalize of a cancelling relative path", (fileSystem, root) -> fileSystem.getPath("a/..").normalize().toString());
    scenarioMap.put("resolve an absolute path", (fileSystem, root) -> fileSystem.getPath("/a/b").resolve(fileSystem.getPath("/x")).toString());
    scenarioMap.put("resolve a relative path", (fileSystem, root) -> fileSystem.getPath("/a/b").resolve(fileSystem.getPath("c/d")).toString());
    scenarioMap.put("resolve the empty path", (fileSystem, root) -> fileSystem.getPath("/a").resolve(fileSystem.getPath("")).toString());
    scenarioMap.put("compareTo orders by name, not by length", (fileSystem, root) -> (int)Math.signum(fileSystem.getPath("/a/b").compareTo(fileSystem.getPath("/b"))));
    scenarioMap.put("compareTo places a prefix first", (fileSystem, root) -> (int)Math.signum(fileSystem.getPath("/a").compareTo(fileSystem.getPath("/a/b"))));
    scenarioMap.put("subpath is relative", (fileSystem, root) -> fileSystem.getPath("/a/b/c").subpath(1, 3).toString());
    scenarioMap.put("file name of a multi element path", (fileSystem, root) -> fileSystem.getPath("/a/b/c").getFileName().toString());
    scenarioMap.put("the root has no file name", (fileSystem, root) -> String.valueOf(fileSystem.getPath("/").getFileName()));
    scenarioMap.put("at least one root directory is reported", (fileSystem, root) -> {

      int count = 0;

      for (Path rootDirectory : fileSystem.getRootDirectories()) {
        count++;
      }

      return count > 0;
    });

    assertAllAgree(scenarioMap);
  }

  public void testGlobAgrees ()
    throws IOException {

    LinkedHashMap<String, Scenario> scenarioMap = new LinkedHashMap<>();

    scenarioMap.put("a star matches within one name", (fileSystem, root) -> fileSystem.getPathMatcher("glob:*.txt").matches(fileSystem.getPath("a.txt")));
    scenarioMap.put("a star does not cross a separator", (fileSystem, root) -> fileSystem.getPathMatcher("glob:*.txt").matches(fileSystem.getPath("a/b.txt")));
    scenarioMap.put("a double star crosses separators", (fileSystem, root) -> fileSystem.getPathMatcher("glob:**.txt").matches(fileSystem.getPath("a/b.txt")));
    scenarioMap.put("a period in a glob is literal", (fileSystem, root) -> fileSystem.getPathMatcher("glob:a.b").matches(fileSystem.getPath("axb")));
    scenarioMap.put("a question mark matches one character", (fileSystem, root) -> fileSystem.getPathMatcher("glob:a?c").matches(fileSystem.getPath("abc")));
    scenarioMap.put("a question mark does not match a separator", (fileSystem, root) -> fileSystem.getPathMatcher("glob:a?c").matches(fileSystem.getPath("a/c")));
    scenarioMap.put("a character class matches a member", (fileSystem, root) -> fileSystem.getPathMatcher("glob:[abc]").matches(fileSystem.getPath("b")));
    scenarioMap.put("a negated character class excludes a member", (fileSystem, root) -> fileSystem.getPathMatcher("glob:[!abc]").matches(fileSystem.getPath("a")));
    scenarioMap.put("a character range matches", (fileSystem, root) -> fileSystem.getPathMatcher("glob:[a-c]").matches(fileSystem.getPath("b")));
    scenarioMap.put("an alternation group matches a branch", (fileSystem, root) -> fileSystem.getPathMatcher("glob:{cat,dog}").matches(fileSystem.getPath("dog")));
    scenarioMap.put("a regex metacharacter is literal", (fileSystem, root) -> fileSystem.getPathMatcher("glob:foo(bar)").matches(fileSystem.getPath("foo(bar)")));
    scenarioMap.put("a regex metacharacter is not a group", (fileSystem, root) -> fileSystem.getPathMatcher("glob:foo(bar)").matches(fileSystem.getPath("foobar")));
    scenarioMap.put("an escaped brace is literal", (fileSystem, root) -> fileSystem.getPathMatcher("glob:a\\{b").matches(fileSystem.getPath("a{b")));
    scenarioMap.put("a regex syntax matcher applies", (fileSystem, root) -> fileSystem.getPathMatcher("regex:a.c").matches(fileSystem.getPath("abc")));

    assertAllAgree(scenarioMap);
  }

  public void testChannelBehaviourAgrees ()
    throws IOException {

    LinkedHashMap<String, Scenario> scenarioMap = new LinkedHashMap<>();

    scenarioMap.put("write then read back", (fileSystem, root) -> {

      Path file = root.resolve("x.txt");

      Files.writeString(file, "content");

      return Files.readString(file);
    });
    scenarioMap.put("a second write truncates", (fileSystem, root) -> {

      Path file = root.resolve("ov.txt");

      Files.writeString(file, "0123456789");
      Files.writeString(file, "ab");

      return Files.readString(file);
    });
    scenarioMap.put("append adds to the end", (fileSystem, root) -> {

      Path file = root.resolve("ap.txt");

      Files.writeString(file, "aa");
      Files.writeString(file, "bb", StandardOpenOption.APPEND);

      return Files.readString(file);
    });
    scenarioMap.put("a channel opened for read and write does both", (fileSystem, root) -> {

      Path file = root.resolve("rw.bin");

      Files.writeString(file, "hello");

      try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE))) {
        channel.position(0);

        ByteBuffer buffer = ByteBuffer.allocate(5);

        channel.read(buffer);

        return new String(buffer.array(), StandardCharsets.UTF_8);
      }
    });
    scenarioMap.put("a file channel reports the size", (fileSystem, root) -> {

      Path file = root.resolve("fc.bin");

      Files.writeString(file, "abc");

      try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {

        return channel.size();
      }
    });
    scenarioMap.put("a file channel reads at an absolute position", (fileSystem, root) -> {

      Path file = root.resolve("fcp.bin");

      Files.writeString(file, "abcdef");

      try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {

        ByteBuffer buffer = ByteBuffer.allocate(3);

        channel.read(buffer, 3);

        return new String(buffer.array(), StandardCharsets.UTF_8);
      }
    });
    scenarioMap.put("a write past the end zero fills", (fileSystem, root) -> {

      Path file = root.resolve("sp.bin");

      try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
        channel.position(4);
        channel.write(ByteBuffer.wrap(new byte[] {1}));
      }

      return Files.size(file) + ":" + java.util.Arrays.toString(Files.readAllBytes(file));
    });
    scenarioMap.put("truncate shortens the content", (fileSystem, root) -> {

      Path file = root.resolve("tr.txt");

      Files.writeString(file, "0123456789");

      try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE))) {
        channel.truncate(4);
      }

      return Files.readString(file);
    });
    scenarioMap.put("truncate beyond the size does nothing", (fileSystem, root) -> {

      Path file = root.resolve("trn.txt");

      Files.writeString(file, "abc");

      try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.WRITE))) {
        channel.truncate(99);
      }

      return Files.readString(file);
    });
    scenarioMap.put("reading at the end reports minus one", (fileSystem, root) -> {

      Path file = root.resolve("e.txt");

      Files.writeString(file, "");

      try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.READ))) {

        return channel.read(ByteBuffer.allocate(4));
      }
    });
    scenarioMap.put("writing to a read only channel is rejected", (fileSystem, root) -> {

      Path file = root.resolve("ro.txt");

      Files.writeString(file, "x");

      try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.READ))) {
        channel.write(ByteBuffer.wrap(new byte[] {1}));

        return "wrote";
      }
    });
    scenarioMap.put("two channels hold independent positions", (fileSystem, root) -> {

      Path file = root.resolve("two.txt");

      Files.writeString(file, "abcdef");

      try (SeekableByteChannel first = Files.newByteChannel(file, Set.of(StandardOpenOption.READ));
           SeekableByteChannel second = Files.newByteChannel(file, Set.of(StandardOpenOption.READ))) {

        ByteBuffer firstBuffer = ByteBuffer.allocate(3);
        ByteBuffer secondBuffer = ByteBuffer.allocate(3);

        first.read(firstBuffer);
        second.read(secondBuffer);

        return new String(firstBuffer.array(), StandardCharsets.UTF_8) + "|" + new String(secondBuffer.array(), StandardCharsets.UTF_8);
      }
    });
    scenarioMap.put("an input stream reads the whole file", (fileSystem, root) -> {

      Path file = root.resolve("is.txt");

      Files.writeString(file, "stream");

      try (InputStream inputStream = Files.newInputStream(file)) {

        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      }
    });
    scenarioMap.put("a buffered writer writes the whole file", (fileSystem, root) -> {

      Path file = root.resolve("bw.txt");

      try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file)) {
        bufferedWriter.write("buffered");
      }

      return Files.readString(file);
    });
    scenarioMap.put("append and read together are rejected", (fileSystem, root) -> {

      Path file = root.resolve("ar.txt");

      Files.writeString(file, "x");

      return Files.newByteChannel(file, Set.of(StandardOpenOption.APPEND, StandardOpenOption.READ));
    });
    scenarioMap.put("append and truncate together are rejected", (fileSystem, root) -> {

      Path file = root.resolve("at.txt");

      Files.writeString(file, "x");

      return Files.newByteChannel(file, Set.of(StandardOpenOption.APPEND, StandardOpenOption.TRUNCATE_EXISTING));
    });
    scenarioMap.put("opening a missing file for read is rejected", (fileSystem, root) -> Files.newByteChannel(root.resolve("missing.txt"), Set.of(StandardOpenOption.READ)));
    scenarioMap.put("create new over an existing file is rejected", (fileSystem, root) -> {

      Path file = root.resolve("cn.txt");

      Files.writeString(file, "x");

      return Files.newByteChannel(file, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE));
    });
    scenarioMap.put("delete on close removes the file", (fileSystem, root) -> {

      Path file = root.resolve("doc.txt");

      try (SeekableByteChannel channel = Files.newByteChannel(file, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE))) {
        channel.write(ByteBuffer.wrap("x".getBytes(StandardCharsets.UTF_8)));
      }

      return Files.exists(file);
    });

    assertAllAgree(scenarioMap);
  }

  public void testTreeOperationsAgree ()
    throws IOException {

    LinkedHashMap<String, Scenario> scenarioMap = new LinkedHashMap<>();

    scenarioMap.put("copying a directory produces an empty directory", (fileSystem, root) -> {

      Path source = root.resolve("a");

      Files.createDirectory(source);
      Files.writeString(source.resolve("f.txt"), "q");
      Files.copy(source, root.resolve("bee"));

      return Files.isDirectory(root.resolve("bee")) + "/" + Files.exists(root.resolve("bee/f.txt"));
    });
    scenarioMap.put("moving a directory keeps its children", (fileSystem, root) -> {

      Path source = root.resolve("a");

      Files.createDirectory(source);
      Files.createDirectory(source.resolve("inner"));
      Files.writeString(source.resolve("inner/f.txt"), "q");
      Files.move(source, root.resolve("b"));

      return Files.readString(root.resolve("b/inner/f.txt")) + "/" + Files.exists(source);
    });
    scenarioMap.put("moving a file renames it", (fileSystem, root) -> {

      Path file = root.resolve("m.txt");

      Files.writeString(file, "z");
      Files.move(file, root.resolve("n.txt"));

      return Files.readString(root.resolve("n.txt")) + "/" + Files.exists(file);
    });
    scenarioMap.put("copying onto an existing file is rejected", (fileSystem, root) -> {

      Path source = root.resolve("s.txt");
      Path target = root.resolve("t.txt");

      Files.writeString(source, "s");
      Files.writeString(target, "t");
      Files.copy(source, target);

      return "copied";
    });
    scenarioMap.put("copying onto an existing file may replace it", (fileSystem, root) -> {

      Path source = root.resolve("s.txt");
      Path target = root.resolve("t.txt");

      Files.writeString(source, "s");
      Files.writeString(target, "t");
      Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      return Files.readString(target);
    });
    scenarioMap.put("replacing a non empty directory is rejected", (fileSystem, root) -> {

      Path source = root.resolve("s.txt");
      Path target = root.resolve("t");

      Files.writeString(source, "s");
      Files.createDirectory(target);
      Files.writeString(target.resolve("child"), "c");
      Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      return "moved";
    });
    scenarioMap.put("copying into a missing directory is rejected", (fileSystem, root) -> {

      Path source = root.resolve("s.txt");

      Files.writeString(source, "s");
      Files.copy(source, root.resolve("missing/t.txt"));

      return "copied";
    });
    scenarioMap.put("nested directories can be created at once", (fileSystem, root) -> {

      Files.createDirectories(root.resolve("x/y/z"));

      return Files.isDirectory(root.resolve("x/y/z"));
    });
    scenarioMap.put("creating an existing directory is rejected", (fileSystem, root) -> {

      Files.createDirectory(root.resolve("d"));
      Files.createDirectory(root.resolve("d"));

      return "created";
    });
    scenarioMap.put("deleting a non empty directory is rejected", (fileSystem, root) -> {

      Files.createDirectory(root.resolve("d"));
      Files.writeString(root.resolve("d/child"), "c");
      Files.delete(root.resolve("d"));

      return "deleted";
    });
    scenarioMap.put("walking a tree visits every entry", (fileSystem, root) -> {

      Files.createDirectories(root.resolve("w/1"));
      Files.writeString(root.resolve("w/1/a.txt"), "a");

      try (Stream<Path> stream = Files.walk(root.resolve("w"))) {

        return stream.count();
      }
    });
    scenarioMap.put("listing a directory reports its entries", (fileSystem, root) -> {

      Files.createDirectory(root.resolve("l"));
      Files.writeString(root.resolve("l/a"), "");
      Files.writeString(root.resolve("l/b"), "");

      try (Stream<Path> stream = Files.list(root.resolve("l"))) {

        return stream.count();
      }
    });
    scenarioMap.put("a directory stream honours a glob", (fileSystem, root) -> {

      Files.createDirectory(root.resolve("g"));
      Files.writeString(root.resolve("g/a.txt"), "");
      Files.writeString(root.resolve("g/b.dat"), "");

      int count = 0;

      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(root.resolve("g"), "*.txt")) {
        for (Path entry : directoryStream) {
          count++;
        }
      }

      return count;
    });
    scenarioMap.put("a directory stream reports entries relative to its directory", (fileSystem, root) -> {

      Files.createDirectory(root.resolve("r"));
      Files.writeString(root.resolve("r/only.txt"), "");

      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(root.resolve("r"))) {

        return relative(root, directoryStream.iterator().next());
      }
    });
    scenarioMap.put("a file tree can be deleted by visiting it", (fileSystem, root) -> {

      Files.createDirectories(root.resolve("d/e"));
      Files.writeString(root.resolve("d/e/f"), "x");
      removeTree(root.resolve("d"));

      return Files.exists(root.resolve("d"));
    });
    scenarioMap.put("a stream can be copied into a file", (fileSystem, root) -> {

      Files.copy(new ByteArrayInputStream("in".getBytes(StandardCharsets.UTF_8)), root.resolve("ci.txt"));

      return Files.readString(root.resolve("ci.txt"));
    });
    scenarioMap.put("a file can be copied into a stream", (fileSystem, root) -> {

      Path file = root.resolve("co.txt");

      Files.writeString(file, "out");

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      Files.copy(file, outputStream);

      return outputStream.toString(StandardCharsets.UTF_8);
    });

    assertAllAgree(scenarioMap);
  }

  public void testMetadataAgrees ()
    throws IOException {

    LinkedHashMap<String, Scenario> scenarioMap = new LinkedHashMap<>();

    scenarioMap.put("size reflects the content", (fileSystem, root) -> {

      Path file = root.resolve("s");

      Files.writeString(file, "12345");

      return Files.size(file);
    });
    scenarioMap.put("a directory does not report the size of its contents", (fileSystem, root) -> {

      Files.createDirectory(root.resolve("ds"));
      Files.writeString(root.resolve("ds/big"), "0123456789");

      return Files.size(root.resolve("ds")) == 10;
    });
    scenarioMap.put("a missing file is not a regular file", (fileSystem, root) -> Files.isRegularFile(root.resolve("nope")));
    scenarioMap.put("a missing file does not exist", (fileSystem, root) -> Files.exists(root.resolve("nope")));
    scenarioMap.put("a wildcard attribute request includes the size", (fileSystem, root) -> {

      Path file = root.resolve("ra");

      Files.writeString(file, "x");

      return Files.readAttributes(file, "basic:*").containsKey("size");
    });
    scenarioMap.put("reading attributes of a missing file is rejected", (fileSystem, root) -> Files.readAttributes(root.resolve("nf"), BasicFileAttributes.class).size());
    scenarioMap.put("an unknown attribute is rejected", (fileSystem, root) -> {

      Path file = root.resolve("ua");

      Files.writeString(file, "x");

      return Files.readAttributes(file, "basic:bogus");
    });
    scenarioMap.put("the modified time can be set", (fileSystem, root) -> {

      Path file = root.resolve("lm");

      Files.writeString(file, "x");
      Files.setLastModifiedTime(file, FileTime.fromMillis(1000));

      return Files.getLastModifiedTime(file).toMillis();
    });
    scenarioMap.put("the real path of a missing file is rejected", (fileSystem, root) -> root.resolve("ghost").toRealPath().toString());
    scenarioMap.put("the real path normalizes", (fileSystem, root) -> {

      Files.createDirectory(root.resolve("a"));
      Files.writeString(root.resolve("a/c"), "x");

      return relative(root, root.resolve("a/./b/../c").toRealPath());
    });
    scenarioMap.put("deleting a missing file is rejected", (fileSystem, root) -> {

      Files.delete(root.resolve("ghost"));

      return "deleted";
    });
    scenarioMap.put("conditionally deleting a missing file reports false", (fileSystem, root) -> Files.deleteIfExists(root.resolve("ghost")));
    scenarioMap.put("creating an existing file is rejected", (fileSystem, root) -> {

      Path file = root.resolve("cf");

      Files.createFile(file);
      Files.createFile(file);

      return "created";
    });
    scenarioMap.put("usable space never exceeds total space", (fileSystem, root) -> {

      FileStore fileStore = Files.getFileStore(root);

      return fileStore.getUsableSpace() <= fileStore.getTotalSpace();
    });
    scenarioMap.put("a temporary file can be created in a directory", (fileSystem, root) -> {

      Path temporaryFile = Files.createTempFile(root, "prefix", ".suffix");

      return Files.exists(temporaryFile) + "/" + relative(root, temporaryFile.getParent());
    });
    scenarioMap.put("a temporary directory can be created in a directory", (fileSystem, root) -> {

      Path temporaryDirectory = Files.createTempDirectory(root, "prefix");

      return Files.isDirectory(temporaryDirectory) + "/" + relative(root, temporaryDirectory.getParent());
    });
    scenarioMap.put("a file is the same file as itself", (fileSystem, root) -> {

      Path file = root.resolve("sf");

      Files.writeString(file, "x");

      return Files.isSameFile(file, file);
    });
    scenarioMap.put("distinct files are not the same file", (fileSystem, root) -> {

      Path first = root.resolve("one");
      Path second = root.resolve("two");

      Files.writeString(first, "x");
      Files.writeString(second, "x");

      return Files.isSameFile(first, second);
    });
    scenarioMap.put("a directory is writable", (fileSystem, root) -> Files.isWritable(root));
    scenarioMap.put("a directory is readable", (fileSystem, root) -> Files.isReadable(root));
    scenarioMap.put("a written file is readable", (fileSystem, root) -> {

      Path file = root.resolve("rd");

      Files.writeString(file, "x");

      return Files.isReadable(file);
    });
    scenarioMap.put("lines can be read back", (fileSystem, root) -> {

      Path file = root.resolve("lines.txt");

      Files.write(file, List.of("one", "two", "three"));

      return Files.readAllLines(file).toString();
    });

    assertAllAgree(scenarioMap);
  }

  /**
   * Verifies the URI round trip as an invariant of each file system separately.
   *
   * <p>The two cannot be compared directly: a platform path renders as {@code C:/a/b} on Windows
   * while an ephemeral path renders as {@code /a/b}. What must hold either way is that converting a
   * path to a URI and back yields the path it started from.
   */
  public void testUriRoundTripsWithinEachFileSystem () {

    for (FileSystem fileSystem : new FileSystem[] {FileSystems.getDefault(), ephemeralFileSystem}) {

      Path path = fileSystem.getPath("/a/b").toAbsolutePath();

      Assert.assertEquals(fileSystem.provider().getPath(path.toUri()), path, "A URI round trip changed the path in " + fileSystem.getClass().getSimpleName());
    }
  }

  /**
   * Verifies that the provider can be discovered through the service loader, which is what makes
   * {@code FileSystems.getFileSystem(URI.create("ephemeral:///"))} work without the caller having
   * to construct a provider itself.
   */
  public void testProviderIsInstalled () {

    FileSystem installedFileSystem = FileSystems.getFileSystem(URI.create("ephemeral:///"));

    Assert.assertTrue(installedFileSystem instanceof EphemeralFileSystem);
    Assert.assertEquals(installedFileSystem.provider().getScheme(), "ephemeral");
  }
}
