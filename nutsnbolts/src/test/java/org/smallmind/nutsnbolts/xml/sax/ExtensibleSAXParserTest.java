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
import org.smallmind.nutsnbolts.xml.ExtensibleSAXParserException;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Test(groups = "unit")
public class ExtensibleSAXParserTest {

  public void testParsingDispatchesStartAndEndEventsToExtenders ()
    throws Exception {

    String xml = "<root><child>hello</child><other>world</other></root>";
    RecordingDocumentExtender extender = new RecordingDocumentExtender();

    ExtensibleSAXParser.parse(extender, new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))), null, false);

    Assert.assertEquals(extender.events, List.of(
      "startDocument",
      "start:root",
      "start:child",
      "end:child:hello",
      "childCompletedInside:root",
      "start:other",
      "end:other:world",
      "childCompletedInside:root",
      "end:root:",
      "completed:root",
      "endDocument"));
  }

  public void testParsingHandlesNestedElements ()
    throws Exception {

    String xml = "<outer><inner>v</inner></outer>";
    RecordingDocumentExtender extender = new RecordingDocumentExtender();

    ExtensibleSAXParser.parse(extender, new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))), null, false);

    Assert.assertTrue(extender.events.contains("start:outer"));
    Assert.assertTrue(extender.events.contains("start:inner"));
    Assert.assertTrue(extender.events.contains("end:inner:v"));
  }

  public void testParsingRaisesWhenExtenderFactoryReturnsNull () {

    String xml = "<root/>";
    NullExtenderFactoryDocumentExtender extender = new NullExtenderFactoryDocumentExtender();

    try {
      ExtensibleSAXParser.parse(extender, new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))), null, false);
      Assert.fail("Expected SAXException wrapping ExtensibleSAXParserException");
    } catch (SAXException saxException) {
      Throwable cause = saxException.getCause();
      Assert.assertTrue(cause instanceof ExtensibleSAXParserException, "cause was " + cause);
    } catch (Exception exception) {
      Assert.fail("Unexpected exception type " + exception);
    }
  }

  @Test(expectedExceptions = SAXException.class)
  public void testParsingPropagatesNonSaxFactoryFailureWrappedInSax ()
    throws Exception {

    String xml = "<root/>";
    ExplodingDocumentExtender extender = new ExplodingDocumentExtender();

    ExtensibleSAXParser.parse(extender, new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))), null, false);
  }

  private static class RecordingDocumentExtender extends AbstractDocumentExtender {

    private final List<String> events = new ArrayList<>();

    @Override
    public void startDocument () {

      events.add("startDocument");
    }

    @Override
    public void endDocument () {

      events.add("endDocument");
    }

    @Override
    public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts) {

      return new RecordingElementExtender(events);
    }

    @Override
    public void completedChildElement (ElementExtender elementExtender) {

      events.add("completed:" + ((RecordingElementExtender)elementExtender).recordedName);
    }
  }

  private static class RecordingElementExtender extends AbstractElementExtender {

    private final List<String> events;
    private String recordedName;

    RecordingElementExtender (List<String> events) {

      this.events = events;
    }

    @Override
    public void startElement (String namespaceURI, String localName, String qName, Attributes atts) {

      recordedName = localName;
      events.add("start:" + localName);
    }

    @Override
    public void endElement (String namespaceURI, String localName, String qName, StringBuilder contentBuilder) {

      events.add("end:" + localName + ":" + contentBuilder.toString().trim());
    }

    @Override
    public void completedChildElement (ElementExtender elementExtender) {

      events.add("childCompletedInside:" + recordedName);
    }
  }

  private static class NullExtenderFactoryDocumentExtender extends AbstractDocumentExtender {

    @Override
    public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts) {

      return null;
    }
  }

  private static class ExplodingDocumentExtender extends AbstractDocumentExtender {

    @Override
    public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts) {

      throw new IllegalStateException("forced failure");
    }
  }
}
