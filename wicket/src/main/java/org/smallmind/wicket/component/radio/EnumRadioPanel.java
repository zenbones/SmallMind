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
package org.smallmind.wicket.component.radio;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.smallmind.nutsnbolts.util.StringUtilities;

public abstract class EnumRadioPanel<E extends Enum> extends Panel {

  public EnumRadioPanel (String id, Class<E> enumClass) {

    this(id, enumClass, new Model<E>());
  }

  public EnumRadioPanel (String id, Class<E> enumClass, final IModel<E> selectionModel) {

    super(id, selectionModel);

    final E[] enumerations = enumClass.getEnumConstants();

    RadioGroup<E> radioGroup;

    setOutputMarkupId(true);

    add(radioGroup = new RadioGroup<E>("enumRadioGroup", selectionModel));
    radioGroup.add(new Loop("enumRadioLoop", new Model<>(enumerations.length)) {

      @Override
      protected void populateItem (LoopItem item) {

        item.add(new Label("enumRadioLabel", new Model<>(StringUtilities.toDisplayCase(enumerations[item.getIndex()].name(), '_'))));
        item.add(new AjaxRadio<E>("enumRadioButton", new Model<E>(enumerations[item.getIndex()])) {

          @Override
          public void onClick (E selection, AjaxRequestTarget target) {

            selectionModel.setObject(selection);
            target.add(EnumRadioPanel.this);

            EnumRadioPanel.this.onClick(selection, target);
          }
        });
      }
    });
  }

  public IModel<E> getSelectionModel () {

    return (IModel<E>)getDefaultModel();
  }

  public E getSelection () {

    return (E)getDefaultModelObject();
  }

  public abstract void onClick (E selection, AjaxRequestTarget target);
}