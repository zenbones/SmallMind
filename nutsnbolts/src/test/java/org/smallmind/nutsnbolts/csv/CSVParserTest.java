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
package org.smallmind.nutsnbolts.csv;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CSVParserTest {

  public void testHandlerLifecycleEventsAreInOrder ()
    throws IOException, CSVParseException {

    RecordingHandler handler = new RecordingHandler();
    CSVParser parser = new CSVParser();

    parser.setLineHandler(handler);
    parser.parse(new StringReader("a,b\nc,d"));

    Assert.assertEquals(handler.events, List.of("start", "row", "row", "end"));
  }

  public void testEachRecordIsDeliveredToHandler ()
    throws IOException, CSVParseException {

    RecordingHandler handler = new RecordingHandler();
    CSVParser parser = new CSVParser();

    parser.setLineHandler(handler);
    parser.parse(new StringReader("a,b\nc,d\ne,f"));

    Assert.assertEquals(handler.rows.size(), 3);
    Assert.assertEquals(handler.rows.get(0), new String[] {"a", "b"});
    Assert.assertEquals(handler.rows.get(2), new String[] {"e", "f"});
  }

  public void testSkipHeaderDropsFirstLine ()
    throws IOException, CSVParseException {

    RecordingHandler handler = new RecordingHandler();
    CSVParser parser = new CSVParser();

    parser.setLineHandler(handler);
    parser.setSkipHeader(true);
    parser.parse(new StringReader("id,name\n1,Alice\n2,Bob"));

    Assert.assertEquals(handler.rows.size(), 2);
    Assert.assertEquals(handler.rows.get(0), new String[] {"1", "Alice"});
  }

  public void testTrimFieldsPropagatesToReader ()
    throws IOException, CSVParseException {

    RecordingHandler handler = new RecordingHandler();
    CSVParser parser = new CSVParser();

    parser.setLineHandler(handler);
    parser.setTrimFields(true);
    parser.parse(new StringReader("  a  , b "));

    Assert.assertEquals(handler.rows.get(0), new String[] {"a", "b"});
  }

  public void testAccessorsReturnConfiguredFlags () {

    CSVParser parser = new CSVParser();

    Assert.assertFalse(parser.isSkipHeader());
    Assert.assertFalse(parser.isTrimFields());
    Assert.assertNull(parser.getLineHandler());

    DefaultCSVLineHandler handler = new DefaultCSVLineHandler();

    parser.setLineHandler(handler);
    parser.setSkipHeader(true);
    parser.setTrimFields(true);

    Assert.assertSame(parser.getLineHandler(), handler);
    Assert.assertTrue(parser.isSkipHeader());
    Assert.assertTrue(parser.isTrimFields());
  }

  private static class RecordingHandler implements CSVLineHandler {

    private final List<String> events = new ArrayList<>();
    private final List<String[]> rows = new ArrayList<>();

    @Override
    public void startDocument () {

      events.add("start");
    }

    @Override
    public void handleFields (String[] fields) {

      events.add("row");
      rows.add(fields);
    }

    @Override
    public void endDocument () {

      events.add("end");
    }
  }
}
