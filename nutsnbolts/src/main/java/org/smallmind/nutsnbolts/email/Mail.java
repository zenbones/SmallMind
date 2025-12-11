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

  public Mail (String from, String to, DataSource... attachments) {

    this(from, to, null, null, null, null, (String)null, attachments);
  }

  public Mail (String from, String to, String body, DataSource... attachments) {

    this(from, to, null, null, null, null, new StringReader(body), attachments);
  }

  public Mail (String from, String to, Reader bodyReader, DataSource... attachments) {

    this(from, to, null, null, null, null, bodyReader, attachments);
  }

  public Mail (String from, String to, String subject, String body, DataSource... attachments) {

    this(from, to, null, null, null, subject, new StringReader(body), attachments);
  }

  public Mail (String from, String to, String subject, Reader bodyReader, DataSource... attachments) {

    this(from, to, null, null, null, subject, bodyReader, attachments);
  }

  public Mail (String from, String to, String replyTo, String cc, String subject, String body, DataSource... attachments) {

    this(from, to, replyTo, cc, null, subject, new StringReader(body), attachments);
  }

  public Mail (String from, String to, String replyTo, String cc, String bcc, String subject, String body, DataSource... attachments) {

    this(from, to, replyTo, cc, bcc, subject, new StringReader(body), attachments);
  }

  public Mail (String from, String to, String replyTo, String cc, String subject, Reader bodyReader, DataSource... attachments) {

    this(from, to, replyTo, cc, null, subject, bodyReader, attachments);
  }

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

  public void setBody (String body) {

    setBodyReader(new StringReader(body));
  }

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

  public boolean isHtml () {

    return html;
  }

  public Mail setHtml (boolean html) {

    this.html = html;

    return this;
  }

  public Mail setHtml () {

    setHtml(true);

    return this;
  }

  public String getFrom () {

    return from;
  }

  public void setFrom (String from) {

    this.from = from;
  }

  public String getTo () {

    return to;
  }

  public void setTo (String to) {

    this.to = to;
  }

  public String getReplyTo () {

    return replyTo;
  }

  public void setReplyTo (String replyTo) {

    this.replyTo = replyTo;
  }

  public String getCc () {

    return cc;
  }

  public void setCc (String cc) {

    this.cc = cc;
  }

  public String getBcc () {

    return bcc;
  }

  public void setBcc (String bcc) {

    this.bcc = bcc;
  }

  public String getSubject () {

    return subject;
  }

  public void setSubject (String subject) {

    this.subject = subject;
  }

  public Reader getBodyReader () {

    return bodyReader;
  }

  public void setBodyReader (Reader bodyReader) {

    this.bodyReader = bodyReader;
  }

  public DataSource[] getAttachments () {

    return attachments;
  }

  public void setAttachments (DataSource[] attachments) {

    this.attachments = attachments;
  }
}
