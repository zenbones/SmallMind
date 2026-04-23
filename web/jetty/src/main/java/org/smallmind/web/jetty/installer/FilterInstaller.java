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
import jakarta.servlet.Filter;

/**
 * Describes a servlet {@link Filter} to be registered with a Jetty context, including its URL mapping, init parameters, and async support setting.
 */
public class FilterInstaller extends JettyInstaller {

  private Filter filter;
  private Class<? extends Filter> filterClass;
  private Map<String, String> initParameters;
  private String displayName;
  private String urlPattern;
  private Boolean asyncSupported;
  private boolean matchAfter = false;

  /**
   * Returns the installer type for this class.
   *
   * @return {@link JettyInstallerType#FILTER}
   */
  @Override
  public JettyInstallerType getOptionType () {

    return JettyInstallerType.FILTER;
  }

  /**
   * Returns the display name registered for this filter in Jetty.
   *
   * @return filter display name, or {@code null} if unset
   */
  public String getDisplayName () {

    return displayName;
  }

  /**
   * Sets the display name for this filter.
   *
   * @param displayName human-readable name shown in Jetty
   */
  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  /**
   * Returns the filter instance, instantiating {@code filterClass} if no instance was supplied directly.
   *
   * @return the {@link Filter} to register
   * @throws InstantiationException if the filter class cannot be instantiated
   * @throws IllegalAccessException if the filter class or its no-arg constructor is inaccessible
   */
  public Filter getFilter ()
    throws InstantiationException, IllegalAccessException {

    return (filter != null) ? filter : filterClass.newInstance();
  }

  /**
   * Sets a concrete filter instance to register.
   *
   * @param filter the filter instance
   */
  public void setFilter (Filter filter) {

    this.filter = filter;
  }

  /**
   * Sets the filter class to instantiate when no instance is provided directly.
   *
   * @param filterClass implementation class for the filter
   */
  public void setFilterClass (Class<? extends Filter> filterClass) {

    this.filterClass = filterClass;
  }

  /**
   * Returns the initialization parameters applied to this filter.
   *
   * @return map of init parameters, or {@code null} if none configured
   */
  public Map<String, String> getInitParameters () {

    return initParameters;
  }

  /**
   * Sets the initialization parameters passed to the filter during startup.
   *
   * @param initParameters parameters to initialize the filter with
   */
  public void setInitParameters (Map<String, String> initParameters) {

    this.initParameters = initParameters;
  }

  /**
   * Returns whether this filter should be appended after existing filter mappings.
   *
   * @return {@code true} if the filter mapping is inserted after current entries
   */
  public boolean isMatchAfter () {

    return matchAfter;
  }

  /**
   * Controls whether this filter is inserted after existing mappings for the same URL pattern.
   *
   * @param matchAfter {@code true} to append after existing filter entries
   */
  public void setMatchAfter (boolean matchAfter) {

    this.matchAfter = matchAfter;
  }

  /**
   * Returns the URL pattern to which this filter is mapped.
   *
   * @return URL pattern, or {@code null} to default to {@code /*}
   */
  public String getUrlPattern () {

    return urlPattern;
  }

  /**
   * Sets the URL pattern to which this filter will be mapped.
   *
   * @param urlPattern the URL pattern, or {@code null} to use the default
   */
  public void setUrlPattern (String urlPattern) {

    this.urlPattern = urlPattern;
  }

  /**
   * Returns whether this filter supports asynchronous dispatch.
   *
   * @return {@code Boolean.TRUE} if async is supported, {@code Boolean.FALSE} if not, or {@code null} if unset
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * Configures whether this filter supports asynchronous dispatch.
   *
   * @param asyncSupported {@code true} to enable async support, {@code false} to disable, {@code null} to leave unset
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
