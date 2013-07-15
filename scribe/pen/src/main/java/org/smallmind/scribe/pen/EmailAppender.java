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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen;

import org.smallmind.nutsnbolts.email.AuthType;
import org.smallmind.nutsnbolts.email.Authentication;
import org.smallmind.nutsnbolts.email.Mail;
import org.smallmind.nutsnbolts.email.Postman;

public class EmailAppender extends AbstractAppender {

  private Postman postman;
  private String from;
  private String to;
  private String subject;

  public EmailAppender (String smtpServer, int smtpPort) {

    this(smtpServer, smtpPort, new Authentication(AuthType.NONE), false);
  }

  public EmailAppender (String smtpServer, int smtpPort, Authentication authentication) {

    this(smtpServer, smtpPort, authentication, false);
  }

  public EmailAppender (String smtpServer, int smtpPort, boolean secure) {

    this(smtpServer, smtpPort, new Authentication(AuthType.NONE), secure);
  }

  public EmailAppender (String smtpServer, int smtpPort, Authentication authentication, boolean secure) {

    super();

    postman = new Postman(smtpServer, smtpPort, authentication, secure);
  }

  public EmailAppender (String smtpServer, int smtpPort, String from, String to, String subject) {

    this(smtpServer, smtpPort);

    this.from = from;
    this.to = to;

    setSubject(subject);
  }

  public void setFrom (String from) {

    this.from = from;
  }

  public void setTo (String to) {

    this.to = to;
  }

  public void setSubject (String subject) {

    this.subject = subject;
  }

  public void handleOutput (String output)
    throws Exception {

    postman.send(new Mail(from, to, subject, output));
  }

  public boolean requiresFormatter () {

    return true;
  }
}