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
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CSVReaderTest {

  public void testSimpleUnquotedRecordSplitsOnCommas ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("a,b,c"))) {

      Assert.assertEquals(reader.readLine(), new String[] {"a", "b", "c"});
      Assert.assertNull(reader.readLine());
    }
  }

  public void testQuotedFieldStripsSurroundingQuotes ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("\"a\",\"b\",\"c\""))) {

      Assert.assertEquals(reader.readLine(), new String[] {"a", "b", "c"});
    }
  }

  public void testQuotedFieldKeepsEmbeddedComma ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("\"a,b\",c"))) {

      Assert.assertEquals(reader.readLine(), new String[] {"a,b", "c"});
    }
  }

  public void testDoubleQuoteInsideQuotedFieldIsEscaped ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("\"a\"\"b\",c"))) {

      Assert.assertEquals(reader.readLine(), new String[] {"a\"b", "c"});
    }
  }

  public void testQuotedFieldSpansMultipleLines ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("\"first\nsecond\",x"))) {

      String[] fields = reader.readLine();

      Assert.assertEquals(fields.length, 2);
      Assert.assertEquals(fields[0], "first" + System.lineSeparator() + "second");
      Assert.assertEquals(fields[1], "x");
    }
  }

  public void testEmptyFieldsArePreserved ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("a,,c"))) {

      Assert.assertEquals(reader.readLine(), new String[] {"a", "", "c"});
    }
  }

  public void testTrimFieldsStripsLeadingAndTrailingWhitespace ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("  a  , b ,c"))) {

      reader.setTrimFields(true);

      Assert.assertEquals(reader.readLine(), new String[] {"a", "b", "c"});
    }
  }

  public void testHeaderRowIsConsumedSeparately ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("name,age\nAlice,30\nBob,25"), true)) {

      Assert.assertEquals(reader.getHeaders(), new String[] {"name", "age"});

      String[] firstRow = reader.readLine();

      Assert.assertEquals(reader.getField("name", firstRow), "Alice");
      Assert.assertEquals(reader.getField("age", firstRow), "30");
    }
  }

  public void testGetFieldReturnsNullForUnknownHeader ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("a,b\n1,2"), true)) {

      Assert.assertNull(reader.getField("missing", reader.readLine()));
    }
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testGetFieldFailsWhenHeadersDisabled ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("a,b"))) {

      reader.getField("a", reader.readLine());
    }
  }

  public void testReadsMultipleRecords ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("a,b\nc,d\ne,f"))) {

      Assert.assertEquals(reader.readLine(), new String[] {"a", "b"});
      Assert.assertEquals(reader.readLine(), new String[] {"c", "d"});
      Assert.assertEquals(reader.readLine(), new String[] {"e", "f"});
      Assert.assertNull(reader.readLine());
    }
  }

  @Test(expectedExceptions = CSVParseException.class)
  public void testUnterminatedQuotedFieldIsRejected ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("\"unterminated"))) {

      reader.readLine();
    }
  }

  @Test(expectedExceptions = CSVParseException.class)
  public void testQuoteFollowedByNonCommaIsRejected ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("\"a\"b,c"))) {

      reader.readLine();
    }
  }

  @Test(expectedExceptions = CSVParseException.class)
  public void testQuoteAfterUnquotedContentIsRejected ()
    throws IOException, CSVParseException {

    try (CSVReader reader = new CSVReader(new StringReader("ab\"c\",d"))) {

      reader.readLine();
    }
  }
}
