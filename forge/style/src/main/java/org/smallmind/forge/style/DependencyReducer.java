package org.smallmind.forge.style;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import com.sun.jdi.connect.TransportTimeoutException;

public class DependencyReducer {

  private enum ParseState {IGNORED, USED_UNDECLARED, UNUSED_DECLARED}

  public static void main (final String... args)
    throws IOException {

    Path root = Paths.get("C:/Users/david/Documents/Nutshell/empyrean/aeon/pantheon/com/forio/epicenter");

    Files.walkFileTree(root, new SimpleFileVisitor<>() {

      @Override
      public FileVisitResult postVisitDirectory (Path dir, IOException exc)
        throws IOException {

        if ((!dottedPath(dir)) && Files.exists(dir.resolve("pom.xml"))) {

          LinkedList<Dependency> usedUndeclaredList = new LinkedList<>();
          LinkedList<Dependency> unusedDeclaredList = new LinkedList<>();
          ByteArrayOutputStream buffer = new ByteArrayOutputStream();
          Process process;

          System.out.println(dir);

          try (InputStream processStream = (process = new ProcessBuilder().command("C:\\Users\\david\\Documents\\Nutshell\\empyrean\\tools\\maven\\apache-maven-3.9.8\\bin\\mvn.cmd", "dependency:analyze", "-N").directory(dir.toFile()).start()).getInputStream()) {

            int singleChar;

            while ((singleChar = processStream.read()) >= 0) {
              buffer.write(singleChar);
            }
          }

          try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
              throw new TransportTimeoutException();
            }
          } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
          }

          buffer.close();

          try (BufferedReader lineReader = new BufferedReader(new StringReader(buffer.toString(StandardCharsets.UTF_8)))) {

            ParseState state = ParseState.IGNORED;
            String singleLine;

            while ((singleLine = lineReader.readLine()) != null) {
              if (singleLine.endsWith("Used undeclared dependencies found:")) {
                state = ParseState.USED_UNDECLARED;
              } else if (singleLine.endsWith("Unused declared dependencies found:")) {
                state = ParseState.UNUSED_DECLARED;
              } else if (singleLine.endsWith("------------------------------------------------------------------------")) {
                break;
              } else if (ParseState.USED_UNDECLARED.equals(state)) {
                usedUndeclaredList.add(new Dependency(singleLine.substring("[WARNING]    ".length())));
              } else if (ParseState.UNUSED_DECLARED.equals(state)) {
                unusedDeclaredList.add(new Dependency(singleLine.substring("[WARNING]    ".length())));
              }
            }
          }
        }

        return FileVisitResult.CONTINUE;
      }
    });
  }

  private static boolean dottedPath (Path path) {

    for (int index = 0; index < path.getNameCount(); index++) {
      if (path.getName(index).toString().charAt(0) == '.') {

        return true;
      }
    }

    return false;
  }

  private static class Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String type;

    public Dependency (String reference) {

      String[] split = reference.split(":");

      groupId = split[0];
      artifactId = split[1];
      classifier = split[2];
      version = split[3];
      type = split[4];
    }

    public String getGroupId () {

      return groupId;
    }

    public String getArtifactId () {

      return artifactId;
    }

    public String getVersion () {

      return version;
    }

    public String getClassifier () {

      return classifier;
    }

    public String getType () {

      return type;
    }
  }
}
