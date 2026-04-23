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
import jakarta.servlet.Filter;

/**
 * Descriptor for a servlet filter to be registered in a Grizzly web application context.
 */
public class FilterInstaller extends GrizzlyInstaller {

  private Filter filter;
  private Class<? extends Filter> filterClass;
  private Map<String, String> initParameters;
  private String displayName;
  private String urlPattern;
  private Boolean asyncSupported;
  private boolean matchAfter = false;

  /**
   * Returns the installer type discriminator.
   *
   * @return {@link GrizzlyInstallerType#FILTER}
   */
  @Override
  public GrizzlyInstallerType getOptionType () {

    return GrizzlyInstallerType.FILTER;
  }

  /**
   * Returns the display name used when registering the filter with the servlet context.
   *
   * @return filter display name
   */
  public String getDisplayName () {

    return displayName;
  }

  /**
   * Sets the display name used when registering the filter with the servlet context.
   *
   * @param displayName friendly registration name
   */
  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  /**
   * Returns the filter to deploy, instantiating from the configured class if no instance was set.
   *
   * @return filter instance ready for deployment
   * @throws InstantiationException if the filter class cannot be instantiated
   * @throws IllegalAccessException if the filter class or no-arg constructor is not accessible
   */
  public Filter getFilter ()
    throws InstantiationException, IllegalAccessException {

    return (filter != null) ? filter : filterClass.newInstance();
  }

  /**
   * Sets a pre-built filter instance to use instead of constructing one from a class.
   *
   * @param filter concrete filter instance to register
   */
  public void setFilter (Filter filter) {

    this.filter = filter;
  }

  /**
   * Sets the filter class to instantiate when no filter instance has been provided.
   *
   * @param filterClass filter implementation class
   */
  public void setFilterClass (Class<? extends Filter> filterClass) {

    this.filterClass = filterClass;
  }

  /**
   * Returns the init parameters to supply to the filter registration.
   *
   * @return init parameter map, or {@code null} if none
   */
  public Map<String, String> getInitParameters () {

    return initParameters;
  }

  /**
   * Sets the init parameters to supply to the filter registration.
   *
   * @param initParameters filter init parameters
   */
  public void setInitParameters (Map<String, String> initParameters) {

    this.initParameters = initParameters;
  }

  /**
   * Returns whether this filter's URL mapping should be added after existing mappings.
   *
   * @return {@code true} to map after existing filters
   */
  public boolean isMatchAfter () {

    return matchAfter;
  }

  /**
   * Controls whether the filter's URL mapping is added after existing mappings.
   *
   * @param matchAfter {@code true} to place the mapping after existing ones
   */
  public void setMatchAfter (boolean matchAfter) {

    this.matchAfter = matchAfter;
  }

  /**
   * Returns the URL pattern to map this filter to.
   *
   * @return URL pattern, or {@code null} to default to {@code /*}
   */
  public String getUrlPattern () {

    return urlPattern;
  }

  /**
   * Sets the URL pattern to map this filter to.
   *
   * @param urlPattern URL pattern for the filter mapping
   */
  public void setUrlPattern (String urlPattern) {

    this.urlPattern = urlPattern;
  }

  /**
   * Returns whether the filter declares async support.
   *
   * @return {@code Boolean.TRUE} if async is supported, {@code Boolean.FALSE} if not, or {@code null} to leave the
   * container default
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * Sets whether the filter declares async support.
   *
   * @param asyncSupported async-support flag, or {@code null} to use the container default
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
