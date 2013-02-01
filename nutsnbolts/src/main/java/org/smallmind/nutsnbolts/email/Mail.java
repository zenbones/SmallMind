/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.email;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;

public class Mail {

  private Reader bodyReader;
  private File[] attachments;
  private String from = null;
  private String to = null;
  private String replyTo = null;
  private String cc = null;
  private String bcc = null;
  private String subject = null;

  public Mail (String from, String to, File... attachments) {

    this(from, to, null, null, null, null, null, attachments);
  }

  public Mail (String from, String to, String body, File... attachments) {

    this(from, to, null, null, null, null, new StringReader(body), attachments);
  }

  public Mail (String from, String to, Reader bodyReader, File... attachments) {

    this(from, to, null, null, null, null, bodyReader, attachments);
  }

  public Mail (String from, String to, String subject, String body, File... attachments) {

    this(from, to, null, null, null, subject, new StringReader(body), attachments);
  }

  public Mail (String from, String to, String subject, Reader bodyReader, File... attachments) {

    this(from, to, null, null, null, subject, bodyReader, attachments);
  }

  public Mail (String from, String to, String replyTo, String cc, String subject, String body, File... attachments) {

    this(from, to, replyTo, cc, null, subject, new StringReader(body), attachments);
  }

  public Mail (String from, String to, String replyTo, String cc, String subject, Reader bodyReader, File... attachments) {

    this(from, to, replyTo, cc, null, subject, bodyReader, attachments);
  }

  public Mail (String from, String to, String replyTo, String cc, String bcc, String subject, Reader bodyReader, File... attachments) {

    this.from = from;
    this.to = to;
    this.replyTo = replyTo;
    this.cc = cc;
    this.bcc = bcc;
    this.subject = subject;
    this.bodyReader = bodyReader;
    this.attachments = attachments;
  }

  public void setFrom (String from) {

    this.from = from;
  }

  public void setTo (String to) {

    this.to = to;
  }

  public void setReplyTo (String replyTo) {

    this.replyTo = replyTo;
  }

  public void setCc (String cc) {

    this.cc = cc;
  }

  public void setBcc (String bcc) {

    this.bcc = bcc;
  }

  public void setSubject (String subject) {

    this.subject = subject;
  }

  public void setBody (String body) {

    setBodyReader(new StringReader(body));
  }

  public void setBodyReader (Reader bodyReader) {

    this.bodyReader = bodyReader;
  }

  public void setAttachments (File[] attachments) {

    this.attachments = attachments;
  }

  public void addAttachment (File attachment) {

    if (attachments == null) {
      attachments = new File[] {attachment};
    }
    else {

      File[] moreAttachments = new File[attachments.length + 1];

      System.arraycopy(attachments, 0, moreAttachments, 0, attachments.length);
      moreAttachments[attachments.length] = attachment;
      attachments = moreAttachments;
    }
  }

  public String getFrom () {

    return from;
  }

  public String getTo () {

    return to;
  }

  public String getReplyTo () {

    return replyTo;
  }

  public String getCc () {

    return cc;
  }

  public String getBcc () {

    return bcc;
  }

  public String getSubject () {

    return subject;
  }

  public Reader getBodyReader () {

    return bodyReader;
  }

  public File[] getAttachments () {

    return attachments;
  }

}
