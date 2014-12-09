/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.web.oauth.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "code")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AccessCode {

  private UserLogin userLogin;
  private String clientId;
  private long created;

  public AccessCode () {

  }

  public AccessCode (String clientId, UserLogin userLogin) {

    this.clientId = clientId;
    this.userLogin = userLogin;

    created = System.currentTimeMillis();
  }

  @XmlElement(name = "clientId", required = true, nillable = false)
  public String getClientId () {

    return clientId;
  }

  public void setClientId (String clientId) {

    this.clientId = clientId;
  }

  @XmlElement(name = "created", required = true, nillable = false)
  public long getCreated () {

    return created;
  }

  public void setCreated (long created) {

    this.created = created;
  }

  @XmlElementRef(required = true)
  public UserLogin getUserLogin () {

    return userLogin;
  }

  public void setUserLogin (UserLogin userLogin) {

    this.userLogin = userLogin;
  }
}
