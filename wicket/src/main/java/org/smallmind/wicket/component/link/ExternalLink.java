/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.wicket.component.link;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

public class ExternalLink extends AbstractLink {

  public enum Target {

    BLANK("_blank", null), PARENT("_parent", "window.parent.location"), SELF("_self", "window.location"), TOP("_top", "window.top.location");

    private final String attribute;
    private final String window;

    Target (String attribute, String window) {

      this.attribute = attribute;
      this.window = window;
    }

    public String asAttribute () {

      return attribute;
    }

    public String asWindow () {

      if (window == null) {
        throw new WicketRuntimeException("Can not set the current 'window.blank' location via script");
      }

      return window;
    }
  }

  private final IModel<String> label;
  private final Target target;

  public ExternalLink (String id, IModel<String> hrefModel, Target target, IModel<String> labelModel) {

    super(id);

    this.target = target;

    setDefaultModel(wrap(hrefModel));
    label = wrap(labelModel);
  }

  protected void onComponentTag (ComponentTag tag) {

    super.onComponentTag(tag);

    if (tag.getName().equalsIgnoreCase("a") || tag.getName().equalsIgnoreCase("link") || tag.getName().equalsIgnoreCase("area")) {
      tag.put("href", Strings.replaceAll(getDefaultModelObjectAsString(), "&", "&amp;"));
      tag.put("target", target.asAttribute());
    } else {
      tag.put("onclick", target.asWindow() + "='" + getDefaultModelObjectAsString() + "';return false;");
    }
  }

  public void onComponentTagBody (MarkupStream markupStream, ComponentTag openTag) {

    if (!isLinkEnabled() && getBeforeDisabledLink() != null) {
      getResponse().write(getBeforeDisabledLink());
    }

    if ((label != null) && (label.getObject() != null)) {
      replaceComponentTagBody(markupStream, openTag, getDefaultModelObjectAsString(label.getObject()));
    } else {
      super.onComponentTagBody(markupStream, openTag);
    }

    if (!isLinkEnabled() && getAfterDisabledLink() != null) {
      getResponse().write(getAfterDisabledLink());
    }
  }
}
