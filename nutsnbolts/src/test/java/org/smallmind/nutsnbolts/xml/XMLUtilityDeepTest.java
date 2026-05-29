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
package org.smallmind.nutsnbolts.xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Test(groups = "unit")
public class XMLUtilityDeepTest {

  public void testIndentedSerializationIncludesNewlines ()
    throws Exception {

    Document document = newDocument("<root><child>text</child></root>");

    String pretty = XMLUtility.toString(document.getDocumentElement(), true);

    Assert.assertTrue(pretty.contains("\n"));
    Assert.assertTrue(pretty.contains("<child>"));
    Assert.assertTrue(pretty.contains("text"));
  }

  public void testCompactSerializationOmitsIndentation ()
    throws Exception {

    Document document = newDocument("<root><child>v</child></root>");

    String compact = XMLUtility.toString(document.getDocumentElement(), false);

    Assert.assertFalse(compact.contains("\n"));
    Assert.assertTrue(compact.contains("<child>v</child>"));
  }

  public void testEmptyElementUsesSelfClosingTag ()
    throws Exception {

    Document document = newDocument("<root/>");

    String serialized = XMLUtility.toString(document.getDocumentElement(), false);

    Assert.assertEquals(serialized, "<root/>");
  }

  public void testAttributesArePreservedAndQuoted ()
    throws Exception {

    Document document = newDocument("<root><child id=\"42\" name=\"a\"/></root>");

    String serialized = XMLUtility.toString(document.getDocumentElement(), false);

    Assert.assertTrue(serialized.contains("id=\"42\""));
    Assert.assertTrue(serialized.contains("name=\"a\""));
  }

  public void testEncodeReplacesReservedCharacters () {

    Assert.assertEquals(XMLUtility.encode("a & b < c > d"), "a &amp; b &lt; c &gt; d");
  }

  public void testEncodeOnEmptyString () {

    Assert.assertEquals(XMLUtility.encode(""), "");
  }

  public void testEncodeLeavesPlainAsciiAlone () {

    Assert.assertEquals(XMLUtility.encode("hello world"), "hello world");
  }

  private static Document newDocument (String xml)
    throws Exception {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();

    return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }
}
