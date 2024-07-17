/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DependencyOrganizer {

  public static void main (String... args)
    throws Exception {

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(Files.newInputStream(Paths.get("C:/Users/david/Desktop/pom.xml")));

    NodeList projectNodeList = doc.getElementsByTagName("project");

    if (projectNodeList.getLength() > 0) {

      Node projectNode = projectNodeList.item(0);
      NodeList dependencyManagementNodelist = ((Element)projectNode).getElementsByTagName("dependencyManagement");

      if (dependencyManagementNodelist.getLength() > 0) {

        Node dependencyManagementNode = dependencyManagementNodelist.item(0);
        NodeList dependenciesNodeList = ((Element)dependencyManagementNode).getElementsByTagName("dependencies");

        if (dependenciesNodeList.getLength() > 0) {

          Node dependenciesNode = dependenciesNodeList.item(0);
        }
      }
    }

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(new File("C:/Users/david/Desktop/pom2.xml"));
    transformer.transform(source, result);
  }

  private Node sortDependencies (Node parentNode) {

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
