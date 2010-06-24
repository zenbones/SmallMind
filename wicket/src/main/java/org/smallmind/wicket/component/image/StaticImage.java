package org.smallmind.wicket.component.image;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.protocol.http.WebApplication;

public class StaticImage extends WebComponent {

   public StaticImage (String id) {

      super(id);
   }

   protected void onComponentTag (ComponentTag tag) {

      super.onComponentTag(tag);
      checkComponentTag(tag, "img");

      tag.put("src", ((WebApplication)getApplication()).getServletContext().getContextPath() + tag.getString("src"));
   }
}
