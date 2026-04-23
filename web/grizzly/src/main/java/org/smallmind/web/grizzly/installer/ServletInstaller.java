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
 * Descriptor for a servlet to be registered in a Grizzly web application context.
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
   * Returns the installer type discriminator.
   *
   * @return {@link GrizzlyInstallerType#SERVLET}
   */
  @Override
  public GrizzlyInstallerType getOptionType () {

    return GrizzlyInstallerType.SERVLET;
  }

  /**
   * Returns the display name used when registering the servlet.
   *
   * @return servlet display name
   */
  public String getDisplayName () {

    return displayName;
  }

  /**
   * Sets the display name used when registering the servlet.
   *
   * @param displayName friendly registration name
   */
  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  /**
   * Returns the servlet to deploy, instantiating from the configured class if no instance was set.
   *
   * @return servlet instance ready for deployment
   * @throws InstantiationException if the servlet class cannot be instantiated
   * @throws IllegalAccessException if the servlet class or no-arg constructor is not accessible
   */
  public Servlet getServlet ()
    throws InstantiationException, IllegalAccessException {

    return (servlet != null) ? servlet : servletClass.newInstance();
  }

  /**
   * Sets a pre-built servlet instance to use instead of constructing one from a class.
   *
   * @param servlet concrete servlet instance
   */
  public void setServlet (Servlet servlet) {

    this.servlet = servlet;
  }

  /**
   * Sets the servlet class to instantiate when no servlet instance has been provided.
   *
   * @param servletClass servlet implementation class
   */
  public void setServletClass (Class<? extends Servlet> servletClass) {

    this.servletClass = servletClass;
  }

  /**
   * Returns the init parameters to set on the servlet registration.
   *
   * @return init parameter map, or {@code null} if none
   */
  public Map<String, String> getInitParameters () {

    return initParameters;
  }

  /**
   * Sets the init parameters to pass to the servlet registration.
   *
   * @param initParameters servlet init parameters
   */
  public void setInitParameters (Map<String, String> initParameters) {

    this.initParameters = initParameters;
  }

  /**
   * Returns the URL pattern to map this servlet to.
   *
   * @return URL pattern, or {@code null} to default to {@code /*}
   */
  public String getUrlPattern () {

    return urlPattern;
  }

  /**
   * Sets the URL pattern to map this servlet to.
   *
   * @param urlPattern URL mapping for the servlet
   */
  public void setUrlPattern (String urlPattern) {

    this.urlPattern = urlPattern;
  }

  /**
   * Returns the load-on-startup order for this servlet.
   *
   * @return load-on-startup value, or {@code null} if not specified
   */
  public Integer getLoadOnStartup () {

    return loadOnStartup;
  }

  /**
   * Sets the load-on-startup order for this servlet.
   *
   * @param loadOnStartup startup order value
   */
  public void setLoadOnStartup (Integer loadOnStartup) {

    this.loadOnStartup = loadOnStartup;
  }

  /**
   * Returns whether the servlet declares async support.
   *
   * @return {@code Boolean.TRUE} if async is supported, {@code Boolean.FALSE} if not, or {@code null} to leave the
   * container default
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * Sets whether the servlet declares async support.
   *
   * @param asyncSupported async-support flag, or {@code null} to use the container default
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
