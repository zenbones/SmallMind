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
package org.smallmind.nutsnbolts.email;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import jakarta.activation.DataSource;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MailTest {

  private static DataSource stubDataSource (final String name, final byte[] payload) {

    return new DataSource() {

      @Override
      public InputStream getInputStream () {

        return new ByteArrayInputStream(payload);
      }

      @Override
      public OutputStream getOutputStream () {

        throw new UnsupportedOperationException();
      }

      @Override
      public String getContentType () {

        return "application/octet-stream";
      }

      @Override
      public String getName () {

        return name;
      }
    };
  }

  private static String readAll (Reader reader)
    throws IOException {

    try (BufferedReader buffered = new BufferedReader(reader)) {

      StringBuilder builder = new StringBuilder();
      int ch;

      while ((ch = buffered.read()) != -1) {
        builder.append((char)ch);
      }

      return builder.toString();
    }
  }

  public void testReaderConstructorLeavesSubjectAndOptionalAddressingUnset () {

    Mail mail = new Mail("a@x", "b@y", new StringReader(""));

    Assert.assertEquals(mail.getFrom(), "a@x");
    Assert.assertEquals(mail.getTo(), "b@y");
    Assert.assertNull(mail.getSubject());
    Assert.assertNotNull(mail.getBodyReader());
    Assert.assertNull(mail.getReplyTo());
    Assert.assertNull(mail.getCc());
    Assert.assertNull(mail.getBcc());
    Assert.assertFalse(mail.isHtml());
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testFromToAttachmentsConstructorFailsBecauseItForwardsNullBodyToStringReader () {

    new Mail("a@x", "b@y");
  }

  public void testFromToBodyConstructorWrapsBodyInReader ()
    throws IOException {

    Mail mail = new Mail("a@x", "b@y", "hello");

    Assert.assertEquals(readAll(mail.getBodyReader()), "hello");
  }

  public void testFromToReaderConstructorUsesGivenReader ()
    throws IOException {

    Mail mail = new Mail("a@x", "b@y", new StringReader("streamed"));

    Assert.assertEquals(readAll(mail.getBodyReader()), "streamed");
  }

  public void testFromToSubjectBodyConstructorSetsSubject () {

    Mail mail = new Mail("a@x", "b@y", "Greetings", "body text");

    Assert.assertEquals(mail.getSubject(), "Greetings");
  }

  public void testReplyToCcConstructorSetsAddressing ()
    throws IOException {

    Mail mail = new Mail("a@x", "b@y", "r@z", "c@w", "subj", "body");

    Assert.assertEquals(mail.getReplyTo(), "r@z");
    Assert.assertEquals(mail.getCc(), "c@w");
    Assert.assertNull(mail.getBcc());
    Assert.assertEquals(readAll(mail.getBodyReader()), "body");
  }

  public void testFullAddressingConstructorSetsBcc () {

    Mail mail = new Mail("a@x", "b@y", "r@z", "c@w", "bcc@v", "subj", "body");

    Assert.assertEquals(mail.getBcc(), "bcc@v");
  }

  public void testReaderConstructorWithReplyToAndCcSetsAddressing ()
    throws IOException {

    Mail mail = new Mail("a@x", "b@y", "r@z", "c@w", "subj", new StringReader("from-reader"));

    Assert.assertEquals(mail.getReplyTo(), "r@z");
    Assert.assertEquals(mail.getCc(), "c@w");
    Assert.assertEquals(readAll(mail.getBodyReader()), "from-reader");
  }

  public void testFullReaderConstructorSetsBcc ()
    throws IOException {

    Mail mail = new Mail("a@x", "b@y", "r@z", "c@w", "bcc@v", "subj", new StringReader("payload"));

    Assert.assertEquals(mail.getBcc(), "bcc@v");
    Assert.assertEquals(readAll(mail.getBodyReader()), "payload");
  }

  public void testSetBodyReplacesBodyReader ()
    throws IOException {

    Mail mail = new Mail("a@x", "b@y", new StringReader(""));

    mail.setBody("replacement");
    Assert.assertEquals(readAll(mail.getBodyReader()), "replacement");
  }

  public void testAccessorsRoundTripAllScalarFields () {

    Mail mail = new Mail("a@x", "b@y", new StringReader(""));

    mail.setFrom("new-from@x");
    mail.setTo("new-to@y");
    mail.setReplyTo("rt@z");
    mail.setCc("c@w");
    mail.setBcc("bcc@v");
    mail.setSubject("S");

    Assert.assertEquals(mail.getFrom(), "new-from@x");
    Assert.assertEquals(mail.getTo(), "new-to@y");
    Assert.assertEquals(mail.getReplyTo(), "rt@z");
    Assert.assertEquals(mail.getCc(), "c@w");
    Assert.assertEquals(mail.getBcc(), "bcc@v");
    Assert.assertEquals(mail.getSubject(), "S");
  }

  public void testSetHtmlChainsAndReportsValue () {

    Mail mail = new Mail("a@x", "b@y", new StringReader(""));

    Mail returned = mail.setHtml(true);

    Assert.assertSame(returned, mail);
    Assert.assertTrue(mail.isHtml());
    mail.setHtml(false);
    Assert.assertFalse(mail.isHtml());
  }

  public void testSetHtmlNoArgMarksHtml () {

    Mail mail = new Mail("a@x", "b@y", new StringReader(""));

    Mail returned = mail.setHtml();

    Assert.assertSame(returned, mail);
    Assert.assertTrue(mail.isHtml());
  }

  public void testAddAttachmentToEmptyInitializesArray ()
    throws IOException {

    DataSource ds = stubDataSource("file.bin", new byte[] {1, 2, 3});
    Mail mail = new Mail("a@x", "b@y", new StringReader(""));

    mail.addAttachment(ds);

    Assert.assertEquals(mail.getAttachments().length, 1);
    Assert.assertSame(mail.getAttachments()[0], ds);
  }

  public void testAddAttachmentGrowsExistingArray ()
    throws IOException {

    DataSource first = stubDataSource("a.bin", new byte[] {1});
    DataSource second = stubDataSource("b.bin", new byte[] {2});
    Mail mail = new Mail("a@x", "b@y", new StringReader(""), first);

    mail.addAttachment(second);

    Assert.assertEquals(mail.getAttachments().length, 2);
    Assert.assertSame(mail.getAttachments()[0], first);
    Assert.assertSame(mail.getAttachments()[1], second);
  }

  public void testSetAttachmentsReplacesArray ()
    throws IOException {

    DataSource original = stubDataSource("a.bin", new byte[] {1});
    DataSource replacement = stubDataSource("z.bin", new byte[] {9});
    Mail mail = new Mail("a@x", "b@y", new StringReader(""), original);

    mail.setAttachments(new DataSource[] {replacement});

    Assert.assertEquals(mail.getAttachments().length, 1);
    Assert.assertSame(mail.getAttachments()[0], replacement);
  }

  public void testSetBodyReaderReplacesReader ()
    throws IOException {

    Mail mail = new Mail("a@x", "b@y", "original");

    mail.setBodyReader(new StringReader("replaced"));

    Assert.assertEquals(readAll(mail.getBodyReader()), "replaced");
  }
}
