package org.smallmind.wicket.component.link;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.string.Strings;

public class ExternalLink extends AbstractLink {

   public static enum Target {

      BLANK("_blank", null), PARENT("_parent", "window.parent"), SELF("_self", "window.location"), TOP("_top", "window.top");

      private String attribute;
      private String window;

      private Target (String attribute, String window) {

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
   private Target target;

   public ExternalLink (String id, IModel<String> hrefModel, Target target, IModel<String> labelModel) {

      super(id);

      this.target = target;

      setDefaultModel(wrap(hrefModel));
      label = wrap(labelModel);
   }

   protected void onComponentTag (ComponentTag tag) {

      super.onComponentTag(tag);
      checkComponentTag(tag, "img");

      tag.put("src", ((WebApplication)getApplication()).getServletContext().getContextPath() + tag.getString("src"));

      if (tag.getName().equalsIgnoreCase("a") || tag.getName().equalsIgnoreCase("link") || tag.getName().equalsIgnoreCase("area")) {
         tag.put("href", Strings.replaceAll(getDefaultModelObjectAsString(), "&", "&amp;"));
         tag.put("target", target.asAttribute());
      }
      else {
         tag.put("onclick", target.asWindow() + "='" + getDefaultModelObjectAsString() + "';return false;");
      }
   }

   protected void onComponentTagBody (MarkupStream markupStream, ComponentTag openTag) {

      if (!isLinkEnabled() && getBeforeDisabledLink() != null) {
         getResponse().write(getBeforeDisabledLink());
      }

      if ((label != null) && (label.getObject() != null)) {
         replaceComponentTagBody(markupStream, openTag, getDefaultModelObjectAsString(label.getObject()));
      }
      else {
         renderComponentTagBody(markupStream, openTag);
      }

      if (!isLinkEnabled() && getAfterDisabledLink() != null) {
         getResponse().write(getAfterDisabledLink());
      }
   }
}
