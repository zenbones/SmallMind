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
package org.smallmind.forge.style;

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
 * Runs {@code mvn dependency:analyze} on each module in a Maven project tree and rewrites the
 * affected {@code pom.xml} files to remove unused declared dependencies, add used-but-undeclared
 * dependencies, and narrow over-scoped dependencies to {@code test} scope. Hidden directories
 * (any path segment starting with {@code '.'}) and the project root itself are skipped.
 */
public class DependencyReducer {

  /**
   * Parse states used while consuming sections of {@code mvn dependency:analyze} output.
   */
  private enum ParseState {
    /**
     * Lines in this state are outside any recognized section and are discarded.
     */
    IGNORED,
    /**
     * Lines in this state list dependencies used in compiled sources but absent from the pom.
     */
    USED_UNDECLARED,
    /**
     * Lines in this state list dependencies declared in the pom but absent from compiled sources.
     */
    UNUSED_DECLARED,
    /**
     * Lines in this state list dependencies declared with a broader scope than test-only usage requires.
     */
    NON_TEST_SCOPED_TEST_ONLY
  }

  /**
   * Command-line entry point.
   *
   * @param args command-line arguments; {@code args[0]} must be the root path of the Maven project to process
   * @throws IOException if file traversal or Maven execution fails
   */
  static void main (String... args)
    throws IOException {

    walkProject(Paths.get(args[0]));
  }

  /**
   * Recursively visit every Maven module under {@code projectPath}, run {@code mvn dependency:analyze},
   * and rewrite any {@code pom.xml} files that require corrections.
   *
   * <p>Directories whose path contains a dot-prefixed segment and the project root itself are skipped.
   *
   * @param projectPath root of the Maven project tree to process
   * @throws IOException if directory traversal, Maven process execution, or pom rewriting fails
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
   * Locate the Maven executable for the current operating system.
   *
   * <p>Delegates to {@link MavenCommandLocator#inWindows} on Windows or
   * {@link MavenCommandLocator#inLinux} on all other platforms.
   *
   * @param commandDir working directory passed to the locator command
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
   * Test whether any segment of {@code path} begins with a dot, indicating a hidden or internal directory.
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
   * Parse the pom at {@code pomPath}, apply the supplied dependency adjustments, and write the
   * result back only if changes were made.
   *
   * @param pomPath                   path to the {@code pom.xml} to update
   * @param usedUndeclaredList        dependencies found in compiled sources but missing from the pom
   * @param unusedDeclaredList        dependencies declared in the pom but absent from compiled sources
   * @param nonTestScopedTestOnlyList dependencies that should be narrowed to {@code test} scope
   * @throws IOException                  if the file cannot be read or written
   * @throws SAXException                 if the XML cannot be parsed
   * @throws ParserConfigurationException if a DOM builder cannot be created
   * @throws TransformerException         if the updated document cannot be serialized
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
      Transformer transformer = transformerFactory.newTransformer(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/forge/style/pretty-print.xslt")));
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(Files.newOutputStream(pomPath));

      transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
      transformer.transform(source, result);
    }
  }

  /**
   * Apply the three categories of dependency corrections to a {@code <dependencies>} DOM node.
   *
   * <p>Unused declared dependencies are removed, used undeclared ones are added, and
   * over-scoped dependencies have their scope changed to {@code test}. The resulting list is sorted.
   *
   * @param parentNode                the {@code <dependencies>} node to adjust
   * @param usedUndeclaredList        dependencies to add
   * @param unusedDeclaredList        dependencies to remove
   * @param nonTestScopedTestOnlyList dependencies whose scope should be narrowed to {@code test}
   * @return a replacement node with all adjustments applied, or {@code null} if no changes were needed
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
   * Build a minimal {@code <dependency>} DOM element from a parsed reference.
   *
   * <p>The generated element contains only {@code <groupId>}, {@code <artifactId>}, and
   * {@code <scope>} children; no {@code <version>} is emitted.
   *
   * @param document            the DOM document that will own the new element
   * @param dependencyReference the parsed reference providing groupId, artifactId, and scope
   * @return a new {@code <dependency>} element populated with the reference values
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
