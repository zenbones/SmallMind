/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.wicket.behavior.form;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

public abstract class AjaxFormProcessingBehavior extends Behavior {

  private FeedbackPanel feedbackPanel;
  private Form form;

  public AjaxFormProcessingBehavior () {

  }

  public AjaxFormProcessingBehavior (FeedbackPanel feedbackPanel) {

    this.feedbackPanel = feedbackPanel;
  }

  @Override
  public boolean getStatelessHint (Component component) {

    return false;
  }

  @Override
  public void bind (Component component) {

    if (!(component instanceof Form)) {
      throw new IllegalArgumentException("Not a Form");
    }

    form = (Form)component;

    super.bind(component);
  }

  public FeedbackPanel getFeedbackPanel () {

    return feedbackPanel;
  }

  public Form getForm () {

    return form;
  }

  public void process (final AjaxRequestTarget target) {

    form.process(new IFormSubmitter() {
      @Override
      public Form<?> getForm () {

        return form;
      }

      @Override
      public boolean getDefaultFormProcessing () {

        return false;
      }

      @Override
      public void onSubmit () {

        AjaxFormProcessingBehavior.this.onSubmit(target);
      }

      @Override
      public void onAfterSubmit () {

      }

      @Override
      public void onError () {

        AjaxFormProcessingBehavior.this.onError(target);
      }
    });
  }

  public void onError (AjaxRequestTarget target) {

    if (feedbackPanel != null) {
      target.add(feedbackPanel);
    }
  }

  public void onSubmit (AjaxRequestTarget target) {

    if (feedbackPanel != null) {
      target.add(feedbackPanel);
    }
  }
}