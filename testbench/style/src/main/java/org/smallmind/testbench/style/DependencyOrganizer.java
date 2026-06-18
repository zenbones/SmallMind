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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedList;
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
 * Walks a Maven project tree and rewrites each {@code pom.xml} to sort {@code <dependency>}
 * declarations alphabetically by {@code groupId} and then by {@code artifactId}, establishing a
 * stable canonical order. The XML output is pretty-printed using a bundled XSLT stylesheet.
 * Both the top-level {@code <dependencies>} element and any {@code <dependencyManagement>}
 * block are sorted.
 */
public class DependencyOrganizer {

  /**
   * Command-line entry point.
   *
   * @param args command-line arguments; {@code args[0]} must be the root path of the Maven project to process
   * @throws IOException if the project tree cannot be traversed
   */
  public static void main (String... args)
    throws IOException {

    walkProject(Paths.get(args[0]));
  }

  /**
   * Recursively visit every {@code pom.xml} under {@code projectPath} and sort its dependency declarations.
   *
   * @param projectPath root of the Maven project tree to process
   * @throws IOException if directory traversal or a pom rewrite fails
   */
  public static void walkProject (Path projectPath)
    throws IOException {

    Files.walkFileTree(projectPath, new SimpleFileVisitor<>() {

      @Override
      public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) {

        if ("pom.xml".equals(file.getFileName().toString())) {
          try {
            rewritePom(file);
          } catch (IOException | SAXException | ParserConfigurationException | TransformerException exception) {
            throw new RuntimeException(exception);
          }
        }

        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Parse the pom at {@code pomPath}, sort its dependency declarations, and write the result back.
   *
   * <p>Both the top-level {@code <dependencies>} element and the {@code <dependencies>} child
   * of any {@code <dependencyManagement>} block are sorted if present.
   *
   * @param pomPath path to the {@code pom.xml} to rewrite
   * @throws IOException                  if the file cannot be read or written
   * @throws SAXException                 if the XML cannot be parsed
   * @throws ParserConfigurationException if a DOM builder cannot be created
   * @throws TransformerException         if the serialized XML cannot be written
   */
  private static void rewritePom (Path pomPath)
    throws IOException, SAXException, ParserConfigurationException, TransformerException {

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(Files.newInputStream(pomPath));
    NodeList projectNodeList = doc.getElementsByTagName("project");

    if (projectNodeList.getLength() > 0) {

      Node projectNode = projectNodeList.item(0);
      NodeList dependencyManagementNodelist = ((Element)projectNode).getElementsByTagName("dependencyManagement");
      NodeList dependenciesNodeList = ((Element)projectNode).getElementsByTagName("dependencies");

      if (dependencyManagementNodelist.getLength() > 0) {

        Node dependencyManagementNode = dependencyManagementNodelist.item(0);
        NodeList managedDependenciesNodeList = ((Element)dependencyManagementNode).getElementsByTagName("dependencies");

        if (managedDependenciesNodeList.getLength() > 0) {

          Node dependenciesNode = managedDependenciesNodeList.item(0);

          dependencyManagementNode.replaceChild(sortDependencies(dependenciesNode), dependenciesNode);
        }
      }

      if (dependenciesNodeList.getLength() > 0) {
        for (int dependenciesIndex = 0; dependenciesIndex < dependenciesNodeList.getLength(); dependenciesIndex++) {

          Node dependenciesNode;

          if ((dependenciesNode = dependenciesNodeList.item(dependenciesIndex)).getParentNode().equals(projectNode)) {
            projectNode.replaceChild(sortDependencies(dependenciesNode), dependenciesNode);
            break;
          }
        }
      }
    }

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/forge/style/pretty-print.xslt")));
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(Files.newOutputStream(pomPath));

    transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
    transformer.transform(source, result);
  }

  /**
   * Produce a copy of {@code parentNode} with its {@code <dependency>} children sorted by
   * {@code groupId} and then by {@code artifactId}.
   *
   * @param parentNode the {@code <dependencies>} DOM node whose children are to be sorted
   * @return a new node with the same attributes as {@code parentNode} but children in sorted order
   */
  private static Node sortDependencies (Node parentNode) {

    Node replacementParentNode = parentNode.cloneNode(false);

    NodeList dependencyNodeList = ((Element)parentNode).getElementsByTagName("dependency");
    LinkedList<DependencyWrapper> dependencyWrapperList = new LinkedList<>();

    if (dependencyNodeList.getLength() > 0) {
      for (int dependencyIndex = 0; dependencyIndex < dependencyNodeList.getLength(); dependencyIndex++) {

        Node dependencyNode = dependencyNodeList.item(dependencyIndex);

        dependencyWrapperList.add(new DependencyWrapper(dependencyNode));
      }

      Collections.sort(dependencyWrapperList);

      for (DependencyWrapper dependencyWrapper : dependencyWrapperList) {
        replacementParentNode.appendChild(dependencyWrapper.getDependencyNode());
      }
    }

    return replacementParentNode;
  }
}
