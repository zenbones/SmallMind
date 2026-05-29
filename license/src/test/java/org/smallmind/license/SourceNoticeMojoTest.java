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
package org.smallmind.license;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.smallmind.license.stencil.JavaDocStencil;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SourceNoticeMojoTest {

  private Path tempRoot;
  private Path sourceDirectory;
  private Path includedFile;
  private Path excludedFile;

  @BeforeMethod
  public void createWorkspace ()
    throws IOException {

    tempRoot = Files.createTempDirectory("source-notice-mojo-test");
    sourceDirectory = Files.createDirectories(tempRoot.resolve("src/main/java"));
    includedFile = Files.write(sourceDirectory.resolve("Included.java"), "package x;\nclass Included {}\n".getBytes(StandardCharsets.UTF_8));
    excludedFile = Files.write(sourceDirectory.resolve("Excluded.java"), "package x;\nclass Excluded {}\n".getBytes(StandardCharsets.UTF_8));
    Files.write(tempRoot.resolve("NOTICE"), "Copyright Notice\n".getBytes(StandardCharsets.UTF_8));
  }

  @AfterMethod
  public void deleteWorkspace ()
    throws IOException {

    if (tempRoot != null) {
      try (Stream<Path> walk = Files.walk(tempRoot)) {
        walk.sorted(Comparator.reverseOrder()).forEach((path) -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException ioException) {
            throw new RuntimeException(ioException);
          }
        });
      }
    }
  }

  public void testExcludedFilesAreNotRewrittenWhenExcludesMatch ()
    throws Exception {

    SourceNoticeMojo mojo = newMojo(buildRule(new String[] {"Excluded.java"}));

    mojo.execute();

    Assert.assertTrue(readFile(includedFile).startsWith("/*"), "Included.java should have received a notice header");
    Assert.assertFalse(readFile(excludedFile).startsWith("/*"), "Excluded.java should have been skipped by the exclude filter");
  }

  public void testAllMatchingFilesAreRewrittenWhenExcludesIsNull ()
    throws Exception {

    SourceNoticeMojo mojo = newMojo(buildRule(null));

    mojo.execute();

    Assert.assertTrue(readFile(includedFile).startsWith("/*"), "Included.java should have received a notice header");
    Assert.assertTrue(readFile(excludedFile).startsWith("/*"), "Excluded.java should have received a notice header when no excludes are configured");
  }

  public void testAllMatchingFilesAreRewrittenWhenExcludesIsEmpty ()
    throws Exception {

    SourceNoticeMojo mojo = newMojo(buildRule(new String[0]));

    mojo.execute();

    Assert.assertTrue(readFile(includedFile).startsWith("/*"), "Included.java should have received a notice header");
    Assert.assertTrue(readFile(excludedFile).startsWith("/*"), "Excluded.java should have received a notice header when excludes is empty");
  }

  private Rule buildRule (String[] excludes) {

    Rule rule = new Rule();

    rule.setId("java-headers");
    rule.setFileTypes(new String[] {"*.java"});
    rule.setExcludes(excludes);
    rule.setStencilId(JavaDocStencil.class.getName());
    rule.setNotice("NOTICE");

    return rule;
  }

  private SourceNoticeMojo newMojo (Rule rule)
    throws Exception {

    Build build = new Build();
    build.setSourceDirectory(sourceDirectory.toString());
    build.setScriptSourceDirectory(tempRoot.resolve("src/main/scripts").toString());
    build.setTestSourceDirectory(tempRoot.resolve("src/test/java").toString());

    Model model = new Model();
    model.setBuild(build);

    MavenProject project = new MavenProject(model);
    project.setFile(tempRoot.resolve("pom.xml").toFile());

    SourceNoticeMojo mojo = new SourceNoticeMojo();
    setField(mojo, "project", project);
    setField(mojo, "rules", new Rule[] {rule});
    setField(mojo, "lineEndings", LineEndings.UNIX);
    setField(mojo, "includeResources", false);
    setField(mojo, "includeTests", false);
    setField(mojo, "allowNoticeRemoval", false);
    setField(mojo, "verbose", false);

    return mojo;
  }

  private void setField (Object target, String name, Object value)
    throws Exception {

    Field field = target.getClass().getDeclaredField(name);

    field.setAccessible(true);
    field.set(target, value);
  }

  private String readFile (Path path)
    throws IOException {

    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}
