package org.smallmind.forge.style;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;

public class Wombat {

  public static void main (final String... args)
    throws IOException {

    Path root = Paths.get("C:/Users/david/Documents/Nutshell/empyrean/aeon/pantheon/com/forio/epicenter");

    Files.walkFileTree(root, new SimpleFileVisitor<>() {

      @Override
      public FileVisitResult postVisitDirectory (Path dir, IOException exc)
        throws IOException {

        if ((!dottedPath(dir)) && Files.exists(dir.resolve("pom.xml"))) {
          System.out.println(dir);

          try (InputStream processStream = new ProcessBuilder().command("C:\\Users\\david\\Documents\\Nutshell\\empyrean\\tools\\maven\\apache-maven-3.8.6\\bin\\mvn.cmd", "dependency:analyze", "-N").directory(dir.toFile()).start().getInputStream()) {

            int singleChar;

            while ((singleChar = processStream.read()) >= 0) {
              System.out.print((char)singleChar);
            }
          }
        }

        return FileVisitResult.CONTINUE;
      }
    });
  }

  public static boolean dottedPath (Path path) {

    for (int index = 0; index < path.getNameCount(); index++) {
      if (path.getName(index).toString().charAt(0) == '.') {

        return true;
      }
    }

    return false;
  }
}
