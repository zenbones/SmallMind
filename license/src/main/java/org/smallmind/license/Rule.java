package org.smallmind.license;

/**
 * Maven plugin configuration bean that pairs a notice file with the file types that should receive
 * it and the stencil that controls how the notice is formatted.
 *
 * <p>Each rule must have a unique {@code id}, at least one entry in {@code fileTypes}, and a
 * {@code stencilId} that matches a registered {@link org.smallmind.license.stencil.Stencil}.
 * When {@code notice} is {@code null} and {@code allowNoticeRemoval} is enabled in the mojo, an
 * existing top-of-file notice is removed instead of replaced.
 */
public class Rule {

  private String[] fileTypes;
  private String[] excludes;
  private String id;
  private String stencilId;
  private String notice;

  /**
   * Returns the unique identifier for this rule, used in log messages and error reporting.
   *
   * @return the rule id; may be {@code null} if not configured
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
   * Returns the path to the notice text file that is applied to matching source files.
   *
   * <p>A relative path is resolved against the root project's base directory. When {@code null},
   * the mojo removes an existing notice (requires {@code allowNoticeRemoval = true} in the mojo
   * configuration).
   *
   * @return the notice file path, or {@code null} to trigger notice removal
   */
  public String getNotice () {

    return notice;
  }

  /**
   * Sets the path to the notice text file.
   *
   * @param notice the notice file path, or {@code null} to remove existing notices
   */
  public void setNotice (String notice) {

    this.notice = notice;
  }

  /**
   * Returns the glob-style file name patterns that select which files this rule processes.
   *
   * @return an array of file type patterns, or {@code null} if none have been configured
   */
  public String[] getFileTypes () {

    return fileTypes;
  }

  /**
   * Sets the glob-style file name patterns that select which files this rule processes.
   *
   * @param fileTypes an array of glob-style patterns such as {@code "*.java"} or {@code "*.xml"}
   */
  public void setFileTypes (String[] fileTypes) {

    this.fileTypes = fileTypes;
  }

  /**
   * Returns the identifier of the stencil used to format the notice in matching files.
   *
   * @return the stencil id; must match a registered {@link org.smallmind.license.stencil.Stencil}
   */
  public String getStencilId () {

    return stencilId;
  }

  /**
   * Sets the identifier of the stencil used to format the notice.
   *
   * @param stencilId the stencil id to use for this rule
   */
  public void setStencilId (String stencilId) {

    this.stencilId = stencilId;
  }

  /**
   * Returns the glob-style file name patterns that exempt files from this rule.
   *
   * @return an array of exclusion patterns, or {@code null} if none are defined
   */
  public String[] getExcludes () {

    return excludes;
  }

  /**
   * Sets the glob-style file name patterns that exempt files from this rule.
   *
   * @param excludes an array of glob-style patterns identifying files to skip
   */
  public void setExcludes (String[] excludes) {

    this.excludes = excludes;
  }
}
