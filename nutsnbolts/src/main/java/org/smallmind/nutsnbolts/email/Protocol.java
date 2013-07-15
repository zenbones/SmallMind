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
package org.smallmind.nutsnbolts.email;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Session;

public enum Protocol {

  SMTP("smtp", "smtp", false), SMTPS("smtps", "smtp", true);

  private String name;
  private String selector;
  private boolean secure;

  private Protocol (String name, String selector, boolean secure) {

    this.name = name;
    this.selector = selector;
    this.secure = secure;
  }

  public Session getSession (String host, int port, Authentication authentication) {

    Properties properties = new Properties();
    Authenticator authenticator;

    properties.setProperty("mail.transport.protocol", name);
    properties.setProperty("mail." + selector + ".host", host);
    properties.put("mail." + selector + ".port", port);

    if (secure) {
      properties.setProperty("mail." + selector + ".starttls.enable", "true");
    }

    if ((authenticator = authentication.getAuthenticator()) != null) {
      properties.setProperty("mail." + selector + ".auth", "true");
    }

    return Session.getInstance(properties, authenticator);
  }
}
