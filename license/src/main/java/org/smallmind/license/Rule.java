package org.smallmind.license;

/**
 * Configuration describing how and where to apply a notice, including the stencil to use and the file types that
 * should be processed.
 */
public class Rule {

  private String[] fileTypes;
  private String[] excludes;
  private String id;
  private String stencilId;
  private String notice;

  /**
   * Retrieves the unique identifier for this rule.
   *
   * @return the rule id
   */
  public String getId () {

    return id;
  }

  /**
   * Sets the unique identifier for this rule.
   *
   * @param id the rule id to assign
   */
  public void setId (String id) {

    this.id = id;
  }

  /**
   * Provides the path to the notice text file that should be applied.
   *
   * @return the notice file path, or {@code null} when removal is intended
   */
  public String getNotice () {

    return notice;
  }

  /**
   * Defines the path to the notice text file that should be applied.
   *
   * @param notice the notice file path, or {@code null} to remove an existing notice
   */
  public void setNotice (String notice) {

    this.notice = notice;
  }

  /**
   * Returns the set of file type patterns eligible for processing under this rule.
   *
   * @return an array of file type patterns
   */
  public String[] getFileTypes () {

    return fileTypes;
  }

  /**
   * Specifies the file type patterns that should be processed by this rule.
   *
   * @param fileTypes an array of glob-like file name patterns
   */
  public void setFileTypes (String[] fileTypes) {

    this.fileTypes = fileTypes;
  }

  /**
   * Identifies the stencil used to format the notice within matching files.
   *
   * @return the stencil identifier
   */
  public String getStencilId () {

    return stencilId;
  }

  /**
   * Sets the stencil identifier used to format the notice within matching files.
   *
   * @param stencilId the stencil id to use
   */
  public void setStencilId (String stencilId) {

    this.stencilId = stencilId;
  }

  /**
   * Returns the file patterns that should be excluded from processing.
   *
   * @return an array of exclusion patterns, or {@code null} if none are defined
   */
  public String[] getExcludes () {

    return excludes;
  }

  /**
   * Defines file patterns that should be excluded from this rule.
   *
   * @param excludes an array of exclusion patterns
   */
  public void setExcludes (String[] excludes) {

    this.excludes = excludes;
  }
}
