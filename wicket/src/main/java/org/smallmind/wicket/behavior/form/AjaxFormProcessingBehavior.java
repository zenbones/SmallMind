package org.smallmind.wicket.behavior.form;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

public abstract class AjaxFormProcessingBehavior extends Behavior {

  private final FeedbackPanel feedbackPanel;
  private Form form;

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

    target.add(feedbackPanel);
  }

  public void onSubmit (AjaxRequestTarget target) {

    target.add(feedbackPanel);
  }
}