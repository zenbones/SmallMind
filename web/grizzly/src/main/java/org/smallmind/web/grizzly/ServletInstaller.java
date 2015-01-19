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
package org.smallmind.web.grizzly;

import java.util.Map;
import javax.servlet.Servlet;

public class ServletInstaller {

  private Map<String, String> initParameters;
  private Class<? extends Servlet> servletClass;
  private String displayName;
  private String urlPattern;

  public String getDisplayName () {

    return displayName;
  }

  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  public Class<? extends Servlet> getServletClass () {

    return servletClass;
  }

  public void setServletClass (Class<? extends Servlet> servletClass) {

    this.servletClass = servletClass;
  }

  public Map<String, String> getInitParameters () {

    return initParameters;
  }

  public void setInitParameters (Map<String, String> initParameters) {

    this.initParameters = initParameters;
  }

  public String getUrlPattern () {

    return urlPattern;
  }

  public void setUrlPattern (String urlPattern) {

    this.urlPattern = urlPattern;
  }
}
