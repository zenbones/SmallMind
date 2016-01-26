/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.wicket.component.button;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.smallmind.wicket.skin.SkinManager;

public abstract class AjaxSubmitButton extends Button {

  public AjaxSubmitButton (String id, IModel labelModel, Form form, SkinManager skinManager) {

    super(id, labelModel, skinManager);

    addButtonBehavior(new OnClickAjaxFormSubmitBehavior(form, "onclick"));
  }

  public abstract void onSubmit (AjaxRequestTarget target, Form form);

  public void onError (AjaxRequestTarget target, Form form) {

  }

  private class OnClickAjaxFormSubmitBehavior extends AjaxFormSubmitBehavior {

    public OnClickAjaxFormSubmitBehavior (Form form, String event) {

      super(form, event);
    }

    protected void onEvent (AjaxRequestTarget target) {

      if (AjaxSubmitButton.this.isEnabled()) {
        super.onEvent(target);
      }
    }

    public void onSubmit (AjaxRequestTarget target) {

      AjaxSubmitButton.this.onSubmit(target, getForm());
    }

    public void onError (AjaxRequestTarget target) {

      AjaxSubmitButton.this.onError(target, getForm());
    }
  }
}
