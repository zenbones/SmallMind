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
import org.smallmind.nutsnbolts.time.Duration;

@XmlRootElement(name = "token")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AccessToken {

  private UserLogin userLogin;
  private String clientId;
  private long expiration;

  public AccessToken () {

  }

  public AccessToken (AccessCode accessCode, Duration grantDuration) {

    clientId = accessCode.getClientId();
    userLogin = accessCode.getUserLogin();
    expiration = System.currentTimeMillis() + grantDuration.toMilliseconds();
  }

  public AccessToken (RefreshToken refreshToken, Duration grantDuration) {

    clientId = refreshToken.getClientId();
    userLogin = refreshToken.getUserLogin();
    expiration = System.currentTimeMillis() + grantDuration.toMilliseconds();
  }

  @XmlElement(name = "clientId", required = true, nillable = false)
  public String getClientId () {

    return clientId;
  }

  public void setClientId (String clientId) {

    this.clientId = clientId;
  }

  @XmlElement(name = "expiration", required = true, nillable = false)
  public long getExpiration () {

    return expiration;
  }

  public void setExpiration (long expiration) {

    this.expiration = expiration;
  }

  @XmlElementRef(required = true)
  public UserLogin getUserLogin () {

    return userLogin;
  }

  public void setUserLogin (UserLogin userLogin) {

    this.userLogin = userLogin;
  }
}
