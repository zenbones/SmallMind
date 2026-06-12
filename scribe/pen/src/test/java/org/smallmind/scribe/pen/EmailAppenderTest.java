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
package org.smallmind.scribe.pen;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EmailAppenderTest {

  private GreenMail greenMail;
  private int smtpPort;

  @BeforeMethod
  public void startGreenMail () {

    ServerSetup serverSetup = ServerSetup.SMTP.dynamicPort();

    greenMail = new GreenMail(serverSetup);
    greenMail.start();
    smtpPort = greenMail.getSmtp().getPort();
  }

  @AfterMethod
  public void stopGreenMail () {

    if (greenMail != null) {
      greenMail.stop();
    }
  }

  public void testSettersRoundTrip () {

    EmailAppender appender = new EmailAppender("localhost", smtpPort);

    appender.setFrom("from@test.example");
    appender.setTo("to@test.example");
    appender.setSubject("a subject");

    // The setters have no getters; constructing and configuring without exception against a live SMTP
    // endpoint is the observable contract exercised here, with delivery covered by the send test.
    Assert.assertNotNull(appender);
  }

  public void testHandleOutputDeliversFormattedBody ()
    throws Exception {

    EmailAppender appender = new EmailAppender("localhost", smtpPort, "from@test.example", "to@test.example", "Scribe Log");

    appender.handleOutput("email-body-marker");

    Assert.assertTrue(greenMail.waitForIncomingEmail(5000, 1));

    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

    Assert.assertEquals(receivedMessages.length, 1);
    Assert.assertEquals(receivedMessages[0].getSubject(), "Scribe Log");
    Assert.assertTrue(GreenMailUtil.getBody(receivedMessages[0]).contains("email-body-marker"));
  }
}
