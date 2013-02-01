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
package org.smallmind.wicket.component.radio;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public abstract class EnumRadioPanel<E extends Enum> extends Panel {

  private E selection;

  public EnumRadioPanel (String id, Class<E> enumClass) {

    this(id, enumClass, null);
  }

  public EnumRadioPanel (String id, Class<E> enumClass, E selection) {

    super(id);

    final E[] enumerations = enumClass.getEnumConstants();

    RadioGroup<E> radioGroup;

    this.selection = selection;

    add(radioGroup = new RadioGroup<E>("enumRadioGroup", new Model<E>(selection)));
    radioGroup.add(new Loop("enumRadioLoop", new Model<Integer>(enumerations.length)) {

      @Override
      protected void populateItem (LoopItem item) {

        item.add(new Label("enumRadioLabel", new Model<String>(enumerations[item.getIndex()].toString())));
        item.add(new AjaxRadio<E>("enumRadioButton", new Model<E>(enumerations[item.getIndex()])) {

          @Override
          public void onClick (E selection, AjaxRequestTarget target) {

            EnumRadioPanel.this.selection = selection;
            EnumRadioPanel.this.onClick(selection, target);
          }
        });
      }
    });
  }

  public E getSelection () {

    return selection;
  }

  public abstract void onClick (E selection, AjaxRequestTarget target);
}