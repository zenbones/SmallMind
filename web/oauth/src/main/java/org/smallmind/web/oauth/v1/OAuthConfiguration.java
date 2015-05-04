/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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

import java.util.Map;
import org.smallmind.nutsnbolts.time.Duration;

public class OAuthConfiguration {

  private Map<String, OAuthRegistration> registrationMap;
  private SecretService secretService;
  private Duration oauthProtocolLeaseDuration;
  private Duration oauthTokenGrantDuration;
  private String ssoCookieName;

  public SecretService getSecretService () {

    return secretService;
  }

  public void setSecretService (SecretService secretService) {

    this.secretService = secretService;
  }

  public Map<String, OAuthRegistration> getRegistrationMap () {

    return registrationMap;
  }

  public void setRegistrationMap (Map<String, OAuthRegistration> registrationMap) {

    this.registrationMap = registrationMap;
  }

  public Duration getOauthProtocolLeaseDuration () {

    return oauthProtocolLeaseDuration;
  }

  public void setOauthProtocolLeaseDuration (Duration oauthProtocolLeaseDuration) {

    this.oauthProtocolLeaseDuration = oauthProtocolLeaseDuration;
  }

  public Duration getOauthTokenGrantDuration () {

    return oauthTokenGrantDuration;
  }

  public void setOauthTokenGrantDuration (Duration oauthTokenGrantDuration) {

    this.oauthTokenGrantDuration = oauthTokenGrantDuration;
  }

  public String getSsoCookieName () {

    return ssoCookieName;
  }

  public void setSsoCookieName (String ssoCookieName) {

    this.ssoCookieName = ssoCookieName;
  }
}
