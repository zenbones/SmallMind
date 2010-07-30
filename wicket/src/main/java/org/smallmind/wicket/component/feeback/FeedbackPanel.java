package org.smallmind.wicket.component.feeback;

import java.util.List;
import java.util.Properties;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.smallmind.wicket.behavior.CssBehavior;
import org.smallmind.wicket.model.VisibilityModel;
import org.smallmind.wicket.skin.SkinManager;

public class FeedbackPanel extends Panel {

   public FeedbackPanel (String id, SkinManager skinManager) {

      super(id);

      Properties cssProperties;
      FeedbackMessageListModel feedbackMessageListModel;

      feedbackMessageListModel = new FeedbackMessageListModel();

      add(new FeedbackListView("feedbackRow", feedbackMessageListModel));
      add(new AttributeAppender("style", true, new FeedbackDisplayModel(feedbackMessageListModel), ";"));

      cssProperties = skinManager.getProperties((WebApplication)getApplication(), FeedbackPanel.class);
      add(new CssBehavior(cssProperties));
   }

   private class FeedbackListView extends ListView {

      public FeedbackListView (String id, IModel messageListModel) {

         super(id, messageListModel);
      }

      protected void populateItem (ListItem listItem) {

         listItem.add(new Label("messageLabel", ((FeedbackMessage)listItem.getModelObject()).getMessage().toString()));
      }
   }

   private class FeedbackMessageListModel extends AbstractReadOnlyModel {

      public Object getObject () {

         return Session.get().getFeedbackMessages().messages(null);
      }
   }

   private class FeedbackDisplayModel extends VisibilityModel {

      private FeedbackMessageListModel feedbackMessageListModel;

      public FeedbackDisplayModel (FeedbackMessageListModel feedbackMessageListModel) {

         this.feedbackMessageListModel = feedbackMessageListModel;
      }

      public boolean isVisible () {

         return !((List)feedbackMessageListModel.getObject()).isEmpty();
      }
   }
}
