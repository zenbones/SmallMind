/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Mail {

   private String from = null;
   private String to = null;
   private String replyTo = null;
   private String cc = null;
   private String bcc = null;
   private String subject = null;
   private String text = null;
   private String date = null;
   private DateFormat dateFormat = null;

   public Mail (String from, String to) {

      this(from, to, null, null, null, null, null);
   }

   public Mail (String from, String to, String text) {

      this(from, to, null, null, null, null, text);
   }

   public Mail (String from, String to, String subject, String text) {

      this(from, to, null, null, null, subject, text);
   }

   public Mail (String from, String to, String replyTo, String cc, String subject, String text) {

      this(from, to, replyTo, cc, null, subject, text);
   }

   public Mail (String from, String to, String replyTo, String cc, String bcc, String subject, String text) {

      this.from = from;
      this.to = to;
      this.replyTo = replyTo;
      this.cc = cc;
      this.bcc = bcc;
      this.subject = subject;
      this.text = text;
      this.dateFormat = new SimpleDateFormat("EEE, dd MMM yyy HH:mm:ss ZZZZ");
      this.date = dateFormat.format(new Date());
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

   public void setText (String text) {

      this.text = text;
   }

   public void setDate (Date date) {

      this.date = dateFormat.format(date);
   }

   public void setDate (String date) {

      this.date = date;
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

   public String getText () {

      return text;
   }

   public String getDate () {

      return date;
   }

}
