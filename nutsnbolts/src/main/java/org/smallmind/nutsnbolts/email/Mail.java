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
 * Simple value object representing an email message, including addressing, subject, body, and attachments.
 * Bodies can be provided as a {@link Reader} or {@link String}; attachments are optional.
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
   * Constructs a message with minimal addressing and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated recipient list
   * @param attachments optional attachments
   */
  public Mail (String from, String to, DataSource... attachments) {

    this(from, to, null, null, null, null, (String)null, attachments);
  }

  /**
   * Constructs a message with string body and optional attachments.
   *
   * @param from        sender address
   * @param to          comma-separated recipient list
   * @param body        message body
   * @param attachments optional attachments
   */
  public Mail (String from, String to, String body, DataSource... attachments) {

    this(from, to, null, null, null, null, new StringReader(body), attachments);
  }

  /**
   * Constructs a message with a supplied {@link Reader} for the body.
   *
   * @param from        sender address
   * @param to          comma-separated recipient list
   * @param bodyReader  reader for the message body
   * @param attachments optional attachments
   */
  public Mail (String from, String to, Reader bodyReader, DataSource... attachments) {

    this(from, to, null, null, null, null, bodyReader, attachments);
  }

  /**
   * Constructs a message with subject, string body, and attachments.
   *
   * @param from        sender address
   * @param to          comma-separated recipient list
   * @param subject     subject line
   * @param body        message body
   * @param attachments optional attachments
   */
  public Mail (String from, String to, String subject, String body, DataSource... attachments) {

    this(from, to, null, null, null, subject, new StringReader(body), attachments);
  }

  /**
   * Constructs a message with subject, body reader, and attachments.
   *
   * @param from        sender address
   * @param to          comma-separated recipient list
   * @param subject     subject line
   * @param bodyReader  reader for the message body
   * @param attachments optional attachments
   */
  public Mail (String from, String to, String subject, Reader bodyReader, DataSource... attachments) {

    this(from, to, null, null, null, subject, bodyReader, attachments);
  }

  /**
   * Constructs a message with reply-to and cc recipients.
   *
   * @param from        sender address
   * @param to          comma-separated recipient list
   * @param replyTo     reply-to address
   * @param cc          comma-separated carbon copy addresses
   * @param subject     subject line
   * @param body        message body
   * @param attachments optional attachments
   */
  public Mail (String from, String to, String replyTo, String cc, String subject, String body, DataSource... attachments) {

    this(from, to, replyTo, cc, null, subject, new StringReader(body), attachments);
  }

  /**
   * Constructs a message with reply-to, cc, and bcc recipients.
   *
   * @param from        sender address
   * @param to          comma-separated recipient list
   * @param replyTo     reply-to address
   * @param cc          comma-separated carbon copy addresses
   * @param bcc         comma-separated blind carbon copy addresses
   * @param subject     subject line
   * @param body        message body
   * @param attachments optional attachments
   */
  public Mail (String from, String to, String replyTo, String cc, String bcc, String subject, String body, DataSource... attachments) {

    this(from, to, replyTo, cc, bcc, subject, new StringReader(body), attachments);
  }

  /**
   * Constructs a message with reply-to and cc recipients, using a body reader.
   *
   * @param from        sender address
   * @param to          comma-separated recipient list
   * @param replyTo     reply-to address
   * @param cc          comma-separated carbon copy addresses
   * @param subject     subject line
   * @param bodyReader  reader for the message body
   * @param attachments optional attachments
   */
  public Mail (String from, String to, String replyTo, String cc, String subject, Reader bodyReader, DataSource... attachments) {

    this(from, to, replyTo, cc, null, subject, bodyReader, attachments);
  }

  /**
   * Constructs a message with full addressing, subject, body reader, and attachments.
   *
   * @param from        sender address
   * @param to          comma-separated recipient list
   * @param replyTo     reply-to address
   * @param cc          comma-separated carbon copy addresses
   * @param bcc         comma-separated blind carbon copy addresses
   * @param subject     subject line
   * @param bodyReader  reader for the message body
   * @param attachments optional attachments
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
   * Replaces the message body using a string value.
   *
   * @param body new body content
   */
  public void setBody (String body) {

    setBodyReader(new StringReader(body));
  }

  /**
   * Adds a new attachment, preserving existing attachments.
   *
   * @param attachment additional attachment to include
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
   * @return {@code true} if the body should be sent as HTML, {@code false} for plain text
   */
  public boolean isHtml () {

    return html;
  }

  /**
   * Flags whether the body should be treated as HTML.
   *
   * @param html {@code true} to send HTML content
   * @return this for chaining
   */
  public Mail setHtml (boolean html) {

    this.html = html;

    return this;
  }

  /**
   * Marks the body as HTML content.
   *
   * @return this for chaining
   */
  public Mail setHtml () {

    setHtml(true);

    return this;
  }

  /**
   * @return sender address
   */
  public String getFrom () {

    return from;
  }

  /**
   * Sets the sender address.
   *
   * @param from sender address
   */
  public void setFrom (String from) {

    this.from = from;
  }

  /**
   * @return comma-separated list of primary recipients
   */
  public String getTo () {

    return to;
  }

  /**
   * Sets the primary recipients.
   *
   * @param to comma-separated recipient addresses
   */
  public void setTo (String to) {

    this.to = to;
  }

  /**
   * @return reply-to address or {@code null}
   */
  public String getReplyTo () {

    return replyTo;
  }

  /**
   * Sets the reply-to address.
   *
   * @param replyTo reply-to address
   */
  public void setReplyTo (String replyTo) {

    this.replyTo = replyTo;
  }

  /**
   * @return comma-separated CC recipients or {@code null}
   */
  public String getCc () {

    return cc;
  }

  /**
   * Sets CC recipients.
   *
   * @param cc comma-separated CC addresses
   */
  public void setCc (String cc) {

    this.cc = cc;
  }

  /**
   * @return comma-separated BCC recipients or {@code null}
   */
  public String getBcc () {

    return bcc;
  }

  /**
   * Sets BCC recipients.
   *
   * @param bcc comma-separated BCC addresses
   */
  public void setBcc (String bcc) {

    this.bcc = bcc;
  }

  /**
   * @return subject line or {@code null}
   */
  public String getSubject () {

    return subject;
  }

  /**
   * Sets the subject line.
   *
   * @param subject subject line
   */
  public void setSubject (String subject) {

    this.subject = subject;
  }

  /**
   * @return reader providing the body content
   */
  public Reader getBodyReader () {

    return bodyReader;
  }

  /**
   * Sets the body content as a reader.
   *
   * @param bodyReader reader supplying body content
   */
  public void setBodyReader (Reader bodyReader) {

    this.bodyReader = bodyReader;
  }

  /**
   * @return current attachments or {@code null}
   */
  public DataSource[] getAttachments () {

    return attachments;
  }

  /**
   * Replaces all attachments.
   *
   * @param attachments attachments to include
   */
  public void setAttachments (DataSource[] attachments) {

    this.attachments = attachments;
  }
}
