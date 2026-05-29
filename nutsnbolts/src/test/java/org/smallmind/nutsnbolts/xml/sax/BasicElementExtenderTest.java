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
package org.smallmind.nutsnbolts.xml.sax;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

@Test(groups = "unit")
public class BasicElementExtenderTest {

  private static InputSource source (String xml) {

    return new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  public void testBasicElementExtenderRecordsLocalNameAndContent ()
    throws Exception {

    List<BasicElementExtender> created = new ArrayList<>();
    BasicElementExtenderDocumentExtender documentExtender = new BasicElementExtenderDocumentExtender(created);

    ExtensibleSAXParser.parse(documentExtender, source("<root>hello world</root>"), null, false);

    Assert.assertFalse(created.isEmpty(), "an element extender should have been created");
    BasicElementExtender rootExtender = created.get(0);
    Assert.assertEquals(rootExtender.getLocalName(), "root");
    Assert.assertEquals(rootExtender.getContent(), "hello world");
  }

  public void testDoNothingElementExtenderConsumesElementWithoutFailure ()
    throws Exception {

    DoNothingDocumentExtender documentExtender = new DoNothingDocumentExtender();

    ExtensibleSAXParser.parse(documentExtender, source("<root><child>ignored</child></root>"), null, false);

    Assert.assertTrue(documentExtender.endDocumentFired, "do-nothing extender should still allow end-of-document delivery");
  }

  private static class BasicElementExtenderDocumentExtender extends AbstractDocumentExtender {

    private final List<BasicElementExtender> created;

    BasicElementExtenderDocumentExtender (List<BasicElementExtender> created) {

      this.created = created;
    }

    @Override
    public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts) {

      BasicElementExtender extender = new BasicElementExtender();

      created.add(extender);

      return extender;
    }
  }

  private static class DoNothingDocumentExtender extends AbstractDocumentExtender {

    private boolean endDocumentFired;

    @Override
    public void endDocument () {

      endDocumentFired = true;
    }

    @Override
    public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts) {

      return new DoNothingElementExtender();
    }
  }
}
