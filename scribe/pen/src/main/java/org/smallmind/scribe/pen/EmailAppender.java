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
 * Appender that delivers formatted log output as the body of an SMTP email message via a
 * {@link Postman} transport, with configurable sender, recipient, and subject.
 */
public class EmailAppender extends AbstractFormattedAppender {

  private final Postman postman;
  private String from;
  private String to;
  private String subject;

  /**
   * Constructs an email appender that connects to the given SMTP server without authentication
   * and without transport-layer security.
   *
   * @param smtpServer hostname or IP address of the SMTP server
   * @param smtpPort   TCP port on which the SMTP server is listening
   */
  public EmailAppender (String smtpServer, int smtpPort) {

    this(smtpServer, smtpPort, Authentication.NONE, false);
  }

  /**
   * Constructs an email appender that connects to the given SMTP server with the specified
   * authentication mode and without transport-layer security.
   *
   * @param smtpServer     hostname or IP address of the SMTP server
   * @param smtpPort       TCP port on which the SMTP server is listening
   * @param authentication the authentication mode to use when connecting to the server
   */
  public EmailAppender (String smtpServer, int smtpPort, Authentication authentication) {

    this(smtpServer, smtpPort, authentication, false);
  }

  /**
   * Constructs an email appender that connects to the given SMTP server without authentication
   * and with an optional transport-layer security (TLS/SSL) upgrade.
   *
   * @param smtpServer hostname or IP address of the SMTP server
   * @param smtpPort   TCP port on which the SMTP server is listening
   * @param secure     {@code true} to enable TLS/SSL; {@code false} for a plain-text connection
   */
  public EmailAppender (String smtpServer, int smtpPort, boolean secure) {

    this(smtpServer, smtpPort, Authentication.NONE, secure);
  }

  /**
   * Constructs an email appender with full control over the SMTP transport settings.
   *
   * @param smtpServer     hostname or IP address of the SMTP server
   * @param smtpPort       TCP port on which the SMTP server is listening
   * @param authentication the authentication mode to use when connecting to the server
   * @param secure         {@code true} to enable TLS/SSL; {@code false} for a plain-text connection
   */
  public EmailAppender (String smtpServer, int smtpPort, Authentication authentication, boolean secure) {

    postman = new Postman(smtpServer, smtpPort, authentication, secure);
  }

  /**
   * Constructs an email appender with the transport settings and message envelope fully pre-configured,
   * using no authentication and no transport-layer security.
   *
   * @param smtpServer hostname or IP address of the SMTP server
   * @param smtpPort   TCP port on which the SMTP server is listening
   * @param from       RFC 5321 email address used as the message sender (e.g. {@code "logger@example.com"})
   * @param to         RFC 5321 email address of the message recipient
   * @param subject    subject line to use for every outgoing log email
   */
  public EmailAppender (String smtpServer, int smtpPort, String from, String to, String subject) {

    this(smtpServer, smtpPort);

    this.from = from;
    this.to = to;
    this.subject = subject;
  }

  /**
   * Sets the RFC 5321 email address used as the sender of every outgoing log message.
   *
   * @param from the sender email address (e.g. {@code "logger@example.com"})
   */
  public void setFrom (String from) {

    this.from = from;
  }

  /**
   * Sets the RFC 5321 email address of the recipient for every outgoing log message.
   *
   * @param to the recipient email address
   */
  public void setTo (String to) {

    this.to = to;
  }

  /**
   * Sets the subject line to use for every outgoing log email.
   *
   * @param subject the email subject text
   */
  public void setSubject (String subject) {

    this.subject = subject;
  }

  /**
   * Sends the formatted log text as the body of an email using the configured SMTP transport,
   * sender address, recipient address, and subject line.
   *
   * @param output the fully formatted log text to use as the email body; must not be {@code null}
   * @throws Exception if the SMTP connection fails, authentication is rejected, or the message
   *                   cannot be delivered
   */
  public void handleOutput (String output)
    throws Exception {

    postman.send(new Mail(from, to, subject, output));
  }
}
