/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.xml;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtilities {

  public static String toString (Node node) {

    return toString(node, false);
  }

  public static String toString (Node node, boolean indent) {

    return toString(node, 0, indent);
  }

  private static String toString (Node node, int level, boolean indent) {

    StringBuilder nodeBuilder = new StringBuilder();

    switch (node.getNodeType()) {
      case Node.TEXT_NODE:
        if (indent) {
          nodeBuilder.append(setIndent(level));
        }
        nodeBuilder.append(node.getNodeValue());
        if (indent) {
          nodeBuilder.append("\n");
        }
        break;
      case Node.ELEMENT_NODE:

        NamedNodeMap attributeMap;
        NodeList childList;

        if (indent) {
          nodeBuilder.append(setIndent(level));
        }
        nodeBuilder.append("<");
        nodeBuilder.append(((Element)node).getTagName());

        attributeMap = node.getAttributes();
        for (int loop = 0; loop < attributeMap.getLength(); loop++) {
          nodeBuilder.append(" ");
          nodeBuilder.append(attributeMap.item(loop).getNodeName());
          nodeBuilder.append("=\"");
          nodeBuilder.append(attributeMap.item(loop).getNodeValue());
          nodeBuilder.append("\"");
        }

        if (node.hasChildNodes()) {
          nodeBuilder.append(">");
          if (indent) {
            nodeBuilder.append("\n");
          }

          childList = node.getChildNodes();
          for (int loop = 0; loop < childList.getLength(); loop++) {
            nodeBuilder.append(toString(childList.item(loop), level + 1, indent));
          }

          if (indent) {
            nodeBuilder.append(setIndent(level));
          }
          nodeBuilder.append("</");
          nodeBuilder.append(((Element)node).getTagName());
          nodeBuilder.append(">");
          if (indent) {
            nodeBuilder.append("\n");
          }
        }
        else {
          nodeBuilder.append("/>");
          if (indent) {
            nodeBuilder.append("\n");
          }
        }
        break;
      default:
        nodeBuilder.append("<Unkown Node Type (");
        nodeBuilder.append(node.getNodeType());
        nodeBuilder.append(")/>");
        if (indent) {
          nodeBuilder.append("\n");
        }
    }

    return nodeBuilder.toString();
  }

  private static String setIndent (int level) {

    StringBuilder spaceBuilder = new StringBuilder();

    for (int count = 0; count < level; count++) {
      spaceBuilder.append("   ");
    }

    return spaceBuilder.toString();
  }

  public static String encode (String unencodedValue) {

    CharacterIterator charIterator;
    StringBuilder encodeBuilder;
    char currentChar;

    encodeBuilder = new StringBuilder();
    charIterator = new StringCharacterIterator(unencodedValue);
    while ((currentChar = charIterator.current()) != CharacterIterator.DONE) {
      switch (currentChar) {
        case '&':
          encodeBuilder.append("&amp;");
          break;
        case '<':
          encodeBuilder.append("&lt;");
          break;
        case '>':
          encodeBuilder.append("&gt;");
          break;
        default:
          encodeBuilder.append(currentChar);
      }
      charIterator.next();
    }

    return encodeBuilder.toString();
  }

}
