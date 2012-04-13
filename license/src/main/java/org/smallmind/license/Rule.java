package org.smallmind.license;

public class Rule {

  private String[] fileTypes;
  private String[] excludes;
  private String id;
  private String stencilId;
  private String notice;

  public String getId () {

    return id;
  }

  public void setId (String id) {

    this.id = id;
  }

  public String getNotice () {

    return notice;
  }

  public void setNotice (String notice) {

    this.notice = notice;
  }

  public String[] getFileTypes () {

    return fileTypes;
  }

  public void setFileTypes (String[] fileTypes) {

    this.fileTypes = fileTypes;
  }

  public String getStencilId () {

    return stencilId;
  }

  public void setStencilId (String stencilId) {

    this.stencilId = stencilId;
  }

  public String[] getExcludes () {

    return excludes;
  }

  public void setExcludes (String[] excludes) {

    this.excludes = excludes;
  }
}
