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
package org.smallmind.web.jetty.installer;

import java.util.Map;
import jakarta.servlet.Servlet;

/**
 * Describes a servlet to be deployed into a Jetty context, including its URL mapping, init parameters, load order, and async support setting.
 */
public class ServletInstaller extends JettyInstaller {

  private Servlet servlet;
  private Class<? extends Servlet> servletClass;
  private Map<String, String> initParameters;
  private String displayName;
  private String urlPattern;
  private Integer loadOnStartup;
  private Boolean asyncSupported;

  /**
   * Returns the installer type for this class.
   *
   * @return {@link JettyInstallerType#SERVLET}
   */
  @Override
  public JettyInstallerType getOptionType () {

    return JettyInstallerType.SERVLET;
  }

  /**
   * Returns the display name registered for this servlet in Jetty.
   *
   * @return servlet display name, or {@code null} if unset
   */
  public String getDisplayName () {

    return displayName;
  }

  /**
   * Sets the display name for this servlet.
   *
   * @param displayName human-readable name shown in Jetty
   */
  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  /**
   * Returns the servlet instance, instantiating {@code servletClass} if no instance was supplied directly.
   *
   * @return the {@link Servlet} to register
   * @throws InstantiationException if the servlet class cannot be instantiated
   * @throws IllegalAccessException if the servlet class or its no-arg constructor is inaccessible
   */
  public Servlet getServlet ()
    throws InstantiationException, IllegalAccessException {

    return (servlet != null) ? servlet : servletClass.newInstance();
  }

  /**
   * Sets a concrete servlet instance to register.
   *
   * @param servlet the servlet instance
   */
  public void setServlet (Servlet servlet) {

    this.servlet = servlet;
  }

  /**
   * Sets the servlet class to instantiate when no instance is provided directly.
   *
   * @param servletClass implementation class for the servlet
   */
  public void setServletClass (Class<? extends Servlet> servletClass) {

    this.servletClass = servletClass;
  }

  /**
   * Returns the initialization parameters applied to this servlet.
   *
   * @return map of init parameters, or {@code null} if none configured
   */
  public Map<String, String> getInitParameters () {

    return initParameters;
  }

  /**
   * Sets the initialization parameters passed to the servlet during startup.
   *
   * @param initParameters parameters to initialize the servlet with
   */
  public void setInitParameters (Map<String, String> initParameters) {

    this.initParameters = initParameters;
  }

  /**
   * Returns the URL pattern to which this servlet is mapped.
   *
   * @return URL pattern, or {@code null} to default to {@code /*}
   */
  public String getUrlPattern () {

    return urlPattern;
  }

  /**
   * Sets the URL pattern that routes requests to this servlet.
   *
   * @param urlPattern the servlet mapping pattern
   */
  public void setUrlPattern (String urlPattern) {

    this.urlPattern = urlPattern;
  }

  /**
   * Returns the load-on-startup order value for this servlet.
   *
   * @return load order, or {@code null} if not configured
   */
  public Integer getLoadOnStartup () {

    return loadOnStartup;
  }

  /**
   * Sets the load-on-startup order for this servlet; lower values cause earlier loading.
   *
   * @param loadOnStartup the startup order value
   */
  public void setLoadOnStartup (Integer loadOnStartup) {

    this.loadOnStartup = loadOnStartup;
  }

  /**
   * Returns whether this servlet supports asynchronous processing.
   *
   * @return {@code Boolean.TRUE} if async is supported, {@code Boolean.FALSE} if not, or {@code null} if unset
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * Configures whether this servlet supports asynchronous processing.
   *
   * @param asyncSupported {@code true} to enable async support, {@code false} to disable, {@code null} to leave unset
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
