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

import org.smallmind.nutsnbolts.email.Authentication;
import org.smallmind.nutsnbolts.email.Mail;
import org.smallmind.nutsnbolts.email.Postman;

/**
 * Appender that sends formatted log output via SMTP email.
 */
public class EmailAppender extends AbstractFormattedAppender {

  private final Postman postman;
  private String from;
  private String to;
  private String subject;

  /**
   * Creates a non-authenticated, non-secure email appender.
   *
   * @param smtpServer SMTP host
   * @param smtpPort   SMTP port
   */
  public EmailAppender (String smtpServer, int smtpPort) {

    this(smtpServer, smtpPort, Authentication.NONE, false);
  }

  /**
   * Creates an email appender with authentication.
   *
   * @param smtpServer     SMTP host
   * @param smtpPort       SMTP port
   * @param authentication auth mode
   */
  public EmailAppender (String smtpServer, int smtpPort, Authentication authentication) {

    this(smtpServer, smtpPort, authentication, false);
  }

  /**
   * Creates a non-authenticated email appender with TLS/SSL toggle.
   *
   * @param smtpServer SMTP host
   * @param smtpPort   SMTP port
   * @param secure     true to enable security
   */
  public EmailAppender (String smtpServer, int smtpPort, boolean secure) {

    this(smtpServer, smtpPort, Authentication.NONE, secure);
  }

  /**
   * Creates an email appender with full transport settings.
   *
   * @param smtpServer     SMTP host
   * @param smtpPort       SMTP port
   * @param authentication auth mode
   * @param secure         true to enable security
   */
  public EmailAppender (String smtpServer, int smtpPort, Authentication authentication, boolean secure) {

    postman = new Postman(smtpServer, smtpPort, authentication, secure);
  }

  /**
   * Creates an email appender with envelope and subject preconfigured.
   *
   * @param smtpServer SMTP host
   * @param smtpPort   SMTP port
   * @param from       from address
   * @param to         destination address
   * @param subject    subject line
   */
  public EmailAppender (String smtpServer, int smtpPort, String from, String to, String subject) {

    this(smtpServer, smtpPort);

    this.from = from;
    this.to = to;
    this.subject = subject;
  }

  /**
   * Sets the from address.
   */
  public void setFrom (String from) {

    this.from = from;
  }

  /**
   * Sets the destination address.
   */
  public void setTo (String to) {

    this.to = to;
  }

  /**
   * Sets the subject line.
   */
  public void setSubject (String subject) {

    this.subject = subject;
  }

  /**
   * Sends the formatted output as an email body.
   */
  public void handleOutput (String output)
    throws Exception {

    postman.send(new Mail(from, to, subject, output));
  }
}
