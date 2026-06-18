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
package org.smallmind.testbench.style;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Maven-style enforcement tool that tidies dependency declarations across a project tree. For each
 * module it runs {@code mvn dependency:analyze -N} and rewrites the module's {@code pom.xml} to drop
 * unused declared dependencies, add ones used in source but not declared, and narrow dependencies
 * that are only used in tests down to {@code test} scope. Hidden directories (any path segment
 * starting with {@code '.'}) and the project root itself are skipped, and the resulting list is
 * re-sorted into canonical order.
 *
 * <p>This is a developer command-line tool rather than a runtime library type; entry is through
 * {@link #main(String...)} or, programmatically, {@link #walkProject(Path)}.
 */
public class DependencyReducer {

  /**
   * Tracks which section of {@code mvn dependency:analyze} output is currently being consumed as the
   * report is read line by line.
   */
  private enum ParseState {
    /**
     * Outside any recognized section; lines are discarded.
     */
    IGNORED,
    /**
     * The "used undeclared" section: dependencies referenced by compiled sources but missing from the pom.
     */
    USED_UNDECLARED,
    /**
     * The "unused declared" section: dependencies declared in the pom but not referenced by compiled sources.
     */
    UNUSED_DECLARED,
    /**
     * The "non-test scoped test only" section: dependencies used only by tests yet declared at a broader scope.
     */
    NON_TEST_SCOPED_TEST_ONLY
  }

  /**
   * Command-line entry point that reduces the dependencies of the project tree rooted at the first
   * argument.
   *
   * @param args the command-line arguments; {@code args[0]} must be the root path of the Maven
   * project to process
   * @throws IOException if file traversal or a Maven invocation fails
   */
  static void main (String... args)
    throws IOException {

    walkProject(Paths.get(args[0]));
  }

  /**
   * Locates Maven, then visits every module directory beneath {@code projectPath} (those containing
   * a {@code pom.xml}), runs {@code mvn dependency:analyze -N} in each, and rewrites the module's pom
   * when the analysis reports corrections. Directories with a dot-prefixed path segment and the
   * project root itself are skipped.
   *
   * @param projectPath the root of the Maven project tree to process
   * @throws IOException if directory traversal or a Maven invocation fails
   * @throws RuntimeException if the Maven executable cannot be located, or wrapping a parse,
   * transform, or write failure on an individual pom
   */
  public static void walkProject (Path projectPath)
    throws IOException {

    String mvnPath;

    if ((mvnPath = findMavenCommand(projectPath)) == null) {
      throw new RuntimeException("Unable to locate 'mvn.cmd'");
    } else {

      Files.walkFileTree(projectPath, new SimpleFileVisitor<>() {

        @Override
        public FileVisitResult postVisitDirectory (Path dir, IOException exc)
          throws IOException {

          if ((!dir.equals(projectPath)) && (!dottedPath(dir)) && Files.exists(dir.resolve("pom.xml"))) {

            LinkedList<DependencyReference> usedUndeclaredList = new LinkedList<>();
            LinkedList<DependencyReference> unusedDeclaredList = new LinkedList<>();
            LinkedList<DependencyReference> nonTestScopedTestOnlyList = new LinkedList<>();
            ByteArrayOutputStream buffer = ProcessOutputUtility.buffer(dir, mvnPath, "dependency:analyze", "-N");

            System.out.println(dir);
            try (BufferedReader lineReader = new BufferedReader(new StringReader(buffer.toString(StandardCharsets.UTF_8)))) {

              ParseState state = ParseState.IGNORED;
              String singleLine;

              while ((singleLine = lineReader.readLine()) != null) {
                if (singleLine.endsWith("Used undeclared dependencies found:")) {
                  state = ParseState.USED_UNDECLARED;
                } else if (singleLine.endsWith("Unused declared dependencies found:")) {
                  state = ParseState.UNUSED_DECLARED;
                } else if (singleLine.endsWith("Non-test scoped test only dependencies found:")) {
                  state = ParseState.NON_TEST_SCOPED_TEST_ONLY;
                } else if (singleLine.endsWith("------------------------------------------------------------------------")) {
                  break;
                } else if (ParseState.USED_UNDECLARED.equals(state)) {
                  usedUndeclaredList.add(new DependencyReference(singleLine.substring("[WARNING]    ".length())));
                } else if (ParseState.UNUSED_DECLARED.equals(state)) {
                  unusedDeclaredList.add(new DependencyReference(singleLine.substring("[WARNING]    ".length())));
                } else if (ParseState.NON_TEST_SCOPED_TEST_ONLY.equals(state)) {
                  nonTestScopedTestOnlyList.add(new DependencyReference(singleLine.substring("[WARNING]    ".length())));
                }
              }
            }

            if ((!usedUndeclaredList.isEmpty()) || (!unusedDeclaredList.isEmpty()) || (!nonTestScopedTestOnlyList.isEmpty())) {
              try {
                rewritePom(dir.resolve("pom.xml"), usedUndeclaredList, unusedDeclaredList, nonTestScopedTestOnlyList);
              } catch (SAXException | ParserConfigurationException | TransformerException exception) {
                throw new RuntimeException(exception);
              }
            }
          }

          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  /**
   * Resolves the Maven executable for the current operating system, delegating to
   * {@link MavenCommandLocator#inWindows} on Windows and {@link MavenCommandLocator#inLinux}
   * elsewhere.
   *
   * @param commandDir the working directory for the locator probe
   * @return the absolute path to the Maven executable, or {@code null} if Maven is not on the PATH
   * @throws IOException if the operating-system probe command cannot be executed
   */
  private static String findMavenCommand (Path commandDir)
    throws IOException {

    if (System.getProperty("os.name").startsWith("Windows")) {

      return MavenCommandLocator.inWindows(commandDir);
    } else {

      return MavenCommandLocator.inLinux(commandDir);
    }
  }

  /**
   * Reports whether any name component of {@code path} begins with a dot, marking it as a hidden or
   * internal directory to skip.
   *
   * @param path the path to inspect
   * @return {@code true} if any name component starts with {@code '.'}, otherwise {@code false}
   */
  private static boolean dottedPath (Path path) {

    for (int index = 0; index < path.getNameCount(); index++) {
      if (path.getName(index).toString().charAt(0) == '.') {

        return true;
      }
    }

    return false;
  }

  /**
   * Parses the pom at {@code pomPath}, applies the supplied dependency adjustments to its top-level
   * {@code <dependencies>} element, and writes the result back only if something actually changed. A
   * {@code <dependencies>} element left empty by the adjustments is removed entirely.
   *
   * @param pomPath the path to the {@code pom.xml} to update
   * @param usedUndeclaredList dependencies referenced by sources but missing from the pom, to be added
   * @param unusedDeclaredList dependencies declared in the pom but unused, to be removed
   * @param nonTestScopedTestOnlyList dependencies to narrow to {@code test} scope
   * @throws IOException if the file cannot be read or written
   * @throws SAXException if the pom is not well-formed XML
   * @throws ParserConfigurationException if a DOM parser cannot be created
   * @throws TransformerException if the updated document cannot be serialized
   */
  private static void rewritePom (Path pomPath, List<DependencyReference> usedUndeclaredList, LinkedList<DependencyReference> unusedDeclaredList, LinkedList<DependencyReference> nonTestScopedTestOnlyList)
    throws IOException, SAXException, ParserConfigurationException, TransformerException {

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(Files.newInputStream(pomPath));
    NodeList projectNodeList = doc.getElementsByTagName("project");
    boolean changed = false;

    if (projectNodeList.getLength() > 0) {

      Node projectNode = projectNodeList.item(0);
      NodeList dependenciesNodeList = ((Element)projectNode).getElementsByTagName("dependencies");

      if (dependenciesNodeList.getLength() > 0) {
        for (int dependenciesIndex = 0; dependenciesIndex < dependenciesNodeList.getLength(); dependenciesIndex++) {

          Node dependenciesNode;

          if ((dependenciesNode = dependenciesNodeList.item(dependenciesIndex)).getParentNode().equals(projectNode)) {

            Node adjustedDepenenciesNode;

            if ((adjustedDepenenciesNode = adjustDependencies(dependenciesNode, usedUndeclaredList, unusedDeclaredList, nonTestScopedTestOnlyList)) != null) {
              changed = true;

              if (adjustedDepenenciesNode.hasChildNodes()) {
                projectNode.replaceChild(adjustedDepenenciesNode, dependenciesNode);
              } else {
                projectNode.removeChild(dependenciesNode);
              }
            }

            break;
          }
        }
      }
    }

    if (changed) {

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/testbench/style/pretty-print.xslt")));
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(Files.newOutputStream(pomPath));

      transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
      transformer.transform(source, result);
    }
  }

  /**
   * Applies the three categories of correction to a {@code <dependencies>} node: unused declared
   * dependencies are removed, used-undeclared ones are added (via {@link #createDependencyElement}),
   * and over-scoped ones have their {@code <scope>} set to {@code test}. When anything changed, the
   * surviving dependencies are re-sorted into canonical order on a fresh replacement node.
   *
   * @param parentNode the {@code <dependencies>} node to adjust
   * @param usedUndeclaredList dependencies to add
   * @param unusedDeclaredList dependencies to remove
   * @param nonTestScopedTestOnlyList dependencies to narrow to {@code test} scope
   * @return a replacement node with all adjustments applied and re-sorted, or {@code null} if no
   * change was needed
   */
  private static Node adjustDependencies (Node parentNode, List<DependencyReference> usedUndeclaredList, LinkedList<DependencyReference> unusedDeclaredList, LinkedList<DependencyReference> nonTestScopedTestOnlyList) {

    Node replacementParentNode = parentNode.cloneNode(false);
    boolean changed = false;

    NodeList dependencyNodeList = ((Element)parentNode).getElementsByTagName("dependency");
    LinkedList<DependencyWrapper> dependencyWrapperList = new LinkedList<>();

    if (dependencyNodeList.getLength() > 0) {
      for (int dependencyIndex = 0; dependencyIndex < dependencyNodeList.getLength(); dependencyIndex++) {

        Node dependencyNode = dependencyNodeList.item(dependencyIndex);

        dependencyWrapperList.add(new DependencyWrapper(dependencyNode));
      }
    }

    for (DependencyReference unusedDeclaredReference : unusedDeclaredList) {

      Iterator<DependencyWrapper> definedDependencyIter = dependencyWrapperList.iterator();

      while (definedDependencyIter.hasNext()) {

        DependencyWrapper definedWrapper = definedDependencyIter.next();

        if (definedWrapper.getGroupId().equals(unusedDeclaredReference.getGroupId()) && definedWrapper.getArtifactId().equals(unusedDeclaredReference.getArtifactId())) {
          changed = true;
          definedDependencyIter.remove();
          break;
        }
      }
    }

    for (DependencyReference usedUndeclaredReference : usedUndeclaredList) {
      changed = true;
      dependencyWrapperList.add(new DependencyWrapper(createDependencyElement(parentNode.getOwnerDocument(), usedUndeclaredReference)));
    }

    for (DependencyReference nonTestScopedTestOnlyReference : nonTestScopedTestOnlyList) {
      for (DependencyWrapper definedWrapper : dependencyWrapperList) {
        if (definedWrapper.getGroupId().equals(nonTestScopedTestOnlyReference.getGroupId()) && definedWrapper.getArtifactId().equals(nonTestScopedTestOnlyReference.getArtifactId())) {

          NodeList scopeNodeList = ((Element)definedWrapper.getDependencyNode()).getElementsByTagName("scope");

          changed = true;

          if (scopeNodeList.getLength() > 0) {
            scopeNodeList.item(0).setTextContent("test");
          } else {

            Element scopeElement = parentNode.getOwnerDocument().createElement("scope");

            scopeElement.setTextContent("test");
            definedWrapper.getDependencyNode().appendChild(scopeElement);
          }

          break;
        }
      }
    }

    if (!changed) {

      return null;
    } else {

      Collections.sort(dependencyWrapperList);

      for (DependencyWrapper dependencyWrapper : dependencyWrapperList) {
        replacementParentNode.appendChild(dependencyWrapper.getDependencyNode());
      }

      return replacementParentNode;
    }
  }

  /**
   * Builds a minimal {@code <dependency>} element from a parsed reference, carrying only
   * {@code <groupId>}, {@code <artifactId>}, and {@code <scope>} children; no {@code <version>} is
   * emitted, on the assumption that the version is managed elsewhere.
   *
   * @param document the DOM document that will own the new element
   * @param dependencyReference the reference supplying groupId, artifactId, and scope
   * @return a new, unattached {@code <dependency>} element populated from the reference
   */
  private static Element createDependencyElement (Document document, DependencyReference dependencyReference) {

    Element addedDependency = document.createElement("dependency");
    Element groupIdElement = document.createElement("groupId");
    Element artifactElement = document.createElement("artifactId");
    Element scopeElement = document.createElement("scope");

    groupIdElement.setTextContent(dependencyReference.getGroupId());
    artifactElement.setTextContent(dependencyReference.getArtifactId());
    scopeElement.setTextContent(dependencyReference.getScope());

    addedDependency.appendChild(groupIdElement);
    addedDependency.appendChild(artifactElement);
    addedDependency.appendChild(scopeElement);

    return addedDependency;
  }
}
