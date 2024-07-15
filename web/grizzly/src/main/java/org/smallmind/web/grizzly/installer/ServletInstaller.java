/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.web.grizzly.installer;

import java.util.Map;
import jakarta.servlet.Servlet;

public class ServletInstaller extends GrizzlyInstaller {

  private Servlet servlet;
  private Class<? extends Servlet> servletClass;
  private Map<String, String> initParameters;
  private String displayName;
  private String urlPattern;
  private Integer loadOnStartup;
  private Boolean asyncSupported;

  @Override
  public GrizzlyInstallerType getOptionType () {

    return GrizzlyInstallerType.SERVLET;
  }

  public String getDisplayName () {

    return displayName;
  }

  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  public Servlet getServlet ()
    throws InstantiationException, IllegalAccessException {

    return (servlet != null) ? servlet : servletClass.newInstance();
  }

  public void setServlet (Servlet servlet) {

    this.servlet = servlet;
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

  public Integer getLoadOnStartup () {

    return loadOnStartup;
  }

  public void setLoadOnStartup (Integer loadOnStartup) {

    this.loadOnStartup = loadOnStartup;
  }

  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
