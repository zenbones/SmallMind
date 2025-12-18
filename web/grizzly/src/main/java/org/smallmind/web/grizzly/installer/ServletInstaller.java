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
package org.smallmind.web.grizzly.installer;

import java.util.Map;
import jakarta.servlet.Servlet;

/**
 * Describes a servlet to be installed into a Grizzly web application context.
 */
public class ServletInstaller extends GrizzlyInstaller {

  private Servlet servlet;
  private Class<? extends Servlet> servletClass;
  private Map<String, String> initParameters;
  private String displayName;
  private String urlPattern;
  private Integer loadOnStartup;
  private Boolean asyncSupported;

  /**
   * @return {@link GrizzlyInstallerType#SERVLET}
   */
  @Override
  public GrizzlyInstallerType getOptionType () {

    return GrizzlyInstallerType.SERVLET;
  }

  /**
   * @return display name used when registering the servlet
   */
  public String getDisplayName () {

    return displayName;
  }

  /**
   * @param displayName friendly registration name for the servlet
   */
  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  /**
   * Instantiates the servlet if only a class was provided, otherwise returns the supplied instance.
   *
   * @return servlet instance to deploy
   * @throws InstantiationException if the servlet cannot be constructed
   * @throws IllegalAccessException if the constructor is not accessible
   */
  public Servlet getServlet ()
    throws InstantiationException, IllegalAccessException {

    return (servlet != null) ? servlet : servletClass.newInstance();
  }

  /**
   * @param servlet concrete servlet instance
   */
  public void setServlet (Servlet servlet) {

    this.servlet = servlet;
  }

  /**
   * @param servletClass servlet implementation class to instantiate
   */
  public void setServletClass (Class<? extends Servlet> servletClass) {

    this.servletClass = servletClass;
  }

  /**
   * @return init parameters to set on the servlet registration
   */
  public Map<String, String> getInitParameters () {

    return initParameters;
  }

  /**
   * @param initParameters servlet init parameters
   */
  public void setInitParameters (Map<String, String> initParameters) {

    this.initParameters = initParameters;
  }

  /**
   * @return mapping pattern to use when registering the servlet
   */
  public String getUrlPattern () {

    return urlPattern;
  }

  /**
   * @param urlPattern URL mapping for the servlet
   */
  public void setUrlPattern (String urlPattern) {

    this.urlPattern = urlPattern;
  }

  /**
   * @return desired load-on-startup order; {@code null} leaves it unset
   */
  public Integer getLoadOnStartup () {

    return loadOnStartup;
  }

  /**
   * @param loadOnStartup servlet load-on-startup order
   */
  public void setLoadOnStartup (Integer loadOnStartup) {

    this.loadOnStartup = loadOnStartup;
  }

  /**
   * @return whether the servlet supports async; {@code null} leaves container default
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * @param asyncSupported flag indicating async support
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
