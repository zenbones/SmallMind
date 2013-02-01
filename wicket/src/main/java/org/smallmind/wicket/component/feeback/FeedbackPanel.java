/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
