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

import java.io.Reader;
import java.io.StringReader;
import jakarta.activation.DataSource;

/**
 * Mutable value object that describes an outgoing email message, including addressing (from, to, reply-to, cc, bcc), subject, body (as a {@link Reader} or inline {@link String}), content type, and optional attachments.
 */
public class Mail {

  private Reader bodyReader;
  private DataSource[] attachments;
  private String from;
  private String to;
  private String replyTo;
  private String cc;
  private String bcc;
  private String subject;
  private boolean html = false;

  /**
   * Constructs a message with sender, recipients, and optional attachments but no subject or body.
   *
   * @param from        sender address
   * @param to          comma-separated list of primary recipient addresses
   * @param attachments zero or more attachments to include
   */
  public Mail (String from, String to, DataSource... attachments) {

    this(from, to, null, null, null, null, (String)null, attachments);
  }

  /**
   * Constructs a message with sender, recipients, an inline string body, and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated list of primary recipient addresses
   * @param body        body text of the message
   * @param attachments zero or more attachments to include
   */
  public Mail (String from, String to, String body, DataSource... attachments) {

    this(from, to, null, null, null, null, new StringReader(body), attachments);
  }

  /**
   * Constructs a message with sender, recipients, a streamed body, and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated list of primary recipient addresses
   * @param bodyReader  reader supplying the body content
   * @param attachments zero or more attachments to include
   */
  public Mail (String from, String to, Reader bodyReader, DataSource... attachments) {

    this(from, to, null, null, null, null, bodyReader, attachments);
  }

  /**
   * Constructs a message with sender, recipients, subject, an inline string body, and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated list of primary recipient addresses
   * @param subject     subject line of the message
   * @param body        body text of the message
   * @param attachments zero or more attachments to include
   */
  public Mail (String from, String to, String subject, String body, DataSource... attachments) {

    this(from, to, null, null, null, subject, new StringReader(body), attachments);
  }

  /**
   * Constructs a message with sender, recipients, subject, a streamed body, and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated list of primary recipient addresses
   * @param subject     subject line of the message
   * @param bodyReader  reader supplying the body content
   * @param attachments zero or more attachments to include
   */
  public Mail (String from, String to, String subject, Reader bodyReader, DataSource... attachments) {

    this(from, to, null, null, null, subject, bodyReader, attachments);
  }

  /**
   * Constructs a message with sender, recipients, reply-to, cc, subject, an inline string body, and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated list of primary recipient addresses
   * @param replyTo     reply-to address, or {@code null} for none
   * @param cc          comma-separated list of CC addresses, or {@code null} for none
   * @param subject     subject line of the message
   * @param body        body text of the message
   * @param attachments zero or more attachments to include
   */
  public Mail (String from, String to, String replyTo, String cc, String subject, String body, DataSource... attachments) {

    this(from, to, replyTo, cc, null, subject, new StringReader(body), attachments);
  }

  /**
   * Constructs a message with sender, full recipient addressing (to, reply-to, cc, bcc), subject, an inline string body, and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated list of primary recipient addresses
   * @param replyTo     reply-to address, or {@code null} for none
   * @param cc          comma-separated list of CC addresses, or {@code null} for none
   * @param bcc         comma-separated list of BCC addresses, or {@code null} for none
   * @param subject     subject line of the message
   * @param body        body text of the message
   * @param attachments zero or more attachments to include
   */
  public Mail (String from, String to, String replyTo, String cc, String bcc, String subject, String body, DataSource... attachments) {

    this(from, to, replyTo, cc, bcc, subject, new StringReader(body), attachments);
  }

  /**
   * Constructs a message with sender, recipients, reply-to, cc, subject, a streamed body, and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated list of primary recipient addresses
   * @param replyTo     reply-to address, or {@code null} for none
   * @param cc          comma-separated list of CC addresses, or {@code null} for none
   * @param subject     subject line of the message
   * @param bodyReader  reader supplying the body content
   * @param attachments zero or more attachments to include
   */
  public Mail (String from, String to, String replyTo, String cc, String subject, Reader bodyReader, DataSource... attachments) {

    this(from, to, replyTo, cc, null, subject, bodyReader, attachments);
  }

  /**
   * Constructs a fully specified message with sender, full recipient addressing (to, reply-to, cc, bcc), subject, a streamed body, and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated list of primary recipient addresses
   * @param replyTo     reply-to address, or {@code null} for none
   * @param cc          comma-separated list of CC addresses, or {@code null} for none
   * @param bcc         comma-separated list of BCC addresses, or {@code null} for none
   * @param subject     subject line of the message
   * @param bodyReader  reader supplying the body content
   * @param attachments zero or more attachments to include
   */
  public Mail (String from, String to, String replyTo, String cc, String bcc, String subject, Reader bodyReader, DataSource... attachments) {

    this.from = from;
    this.to = to;
    this.replyTo = replyTo;
    this.cc = cc;
    this.bcc = bcc;
    this.subject = subject;
    this.bodyReader = bodyReader;
    this.attachments = attachments;
  }

  /**
   * Replaces the current body by wrapping the supplied string in a {@link java.io.StringReader}.
   *
   * @param body the new body text
   */
  public void setBody (String body) {

    setBodyReader(new StringReader(body));
  }

  /**
   * Appends a single attachment to the message, growing the attachments array while preserving any previously set attachments.
   *
   * @param attachment the additional {@link DataSource} to attach
   */
  public void addAttachment (DataSource attachment) {

    if (attachments == null) {
      attachments = new DataSource[] {attachment};
    } else {

      DataSource[] moreAttachments = new DataSource[attachments.length + 1];

      System.arraycopy(attachments, 0, moreAttachments, 0, attachments.length);
      moreAttachments[attachments.length] = attachment;
      attachments = moreAttachments;
    }
  }

  /**
   * Returns whether the message body should be transmitted as HTML content.
   *
   * @return {@code true} if the body is HTML; {@code false} for plain text
   */
  public boolean isHtml () {

    return html;
  }

  /**
   * Sets whether the body should be transmitted as HTML or plain text.
   *
   * @param html {@code true} to use the {@code text/html} content subtype
   * @return this instance for method chaining
   */
  public Mail setHtml (boolean html) {

    this.html = html;

    return this;
  }

  /**
   * Convenience method that marks the body as HTML content.
   *
   * @return this instance for method chaining
   */
  public Mail setHtml () {

    setHtml(true);

    return this;
  }

  /**
   * Returns the sender address.
   *
   * @return the from address
   */
  public String getFrom () {

    return from;
  }

  /**
   * Sets the sender address.
   *
   * @param from the from address
   */
  public void setFrom (String from) {

    this.from = from;
  }

  /**
   * Returns the comma-separated list of primary recipient addresses.
   *
   * @return to addresses, or {@code null}
   */
  public String getTo () {

    return to;
  }

  /**
   * Sets the comma-separated list of primary recipient addresses.
   *
   * @param to comma-separated recipient addresses
   */
  public void setTo (String to) {

    this.to = to;
  }

  /**
   * Returns the reply-to address.
   *
   * @return reply-to address, or {@code null} if not set
   */
  public String getReplyTo () {

    return replyTo;
  }

  /**
   * Sets the reply-to address.
   *
   * @param replyTo the reply-to address
   */
  public void setReplyTo (String replyTo) {

    this.replyTo = replyTo;
  }

  /**
   * Returns the comma-separated list of CC recipient addresses.
   *
   * @return CC addresses, or {@code null} if not set
   */
  public String getCc () {

    return cc;
  }

  /**
   * Sets the comma-separated list of CC recipient addresses.
   *
   * @param cc comma-separated CC addresses
   */
  public void setCc (String cc) {

    this.cc = cc;
  }

  /**
   * Returns the comma-separated list of BCC recipient addresses.
   *
   * @return BCC addresses, or {@code null} if not set
   */
  public String getBcc () {

    return bcc;
  }

  /**
   * Sets the comma-separated list of BCC recipient addresses.
   *
   * @param bcc comma-separated BCC addresses
   */
  public void setBcc (String bcc) {

    this.bcc = bcc;
  }

  /**
   * Returns the message subject line.
   *
   * @return subject, or {@code null} if not set
   */
  public String getSubject () {

    return subject;
  }

  /**
   * Sets the message subject line.
   *
   * @param subject the subject line text
   */
  public void setSubject (String subject) {

    this.subject = subject;
  }

  /**
   * Returns the reader from which the message body is read.
   *
   * @return body reader, or {@code null} if no body has been set
   */
  public Reader getBodyReader () {

    return bodyReader;
  }

  /**
   * Sets the reader from which the message body will be read.
   *
   * @param bodyReader reader supplying body content
   */
  public void setBodyReader (Reader bodyReader) {

    this.bodyReader = bodyReader;
  }

  /**
   * Returns the current array of attachments.
   *
   * @return attachments, or {@code null} if none have been set
   */
  public DataSource[] getAttachments () {

    return attachments;
  }

  /**
   * Replaces the entire attachments array with the supplied array.
   *
   * @param attachments new attachment array to use
   */
  public void setAttachments (DataSource[] attachments) {

    this.attachments = attachments;
  }
}
