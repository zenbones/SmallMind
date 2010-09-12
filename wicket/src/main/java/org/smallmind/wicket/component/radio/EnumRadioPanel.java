package org.smallmind.wicket.component.radio;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.smallmind.wicket.event.OnChangeListener;

public abstract class EnumRadioPanel<E extends Enum> extends Panel {

   public EnumRadioPanel (String id, Class<E> enumClass) {

      this(id, enumClass, null, null);
   }

   public EnumRadioPanel (String id, Class<E> enumClass, E selectedEnum) {

      this(id, enumClass, selectedEnum, null);
   }

   public EnumRadioPanel (String id, Class<E> enumClass, OnChangeListener onChangeListener) {

      this(id, enumClass, null, onChangeListener);
   }

   public EnumRadioPanel (String id, Class<E> enumClass, E selectedEnum, OnChangeListener onChangeListener) {

      super(id);

      final E[] enumerations = enumClass.getEnumConstants();

      RadioGroup<E> radioGroup;

      add(radioGroup = new RadioGroup<E>("enumRadioGroup", new Model<E>(selectedEnum)));
      radioGroup.add(new Loop("enumRadioLoop", new Model<Integer>(enumerations.length)) {

         @Override
         protected void populateItem (LoopItem item) {
            item.add(new Label("enumRadioLabel", new Model<String>(enumerations[item.getIteration()].toString())));
            item.add(new AjaxRadio<E>("enumRadioButton", new Model<E>(enumerations[item.getIteration()])) {

               @Override
               public void onClick (E selection, AjaxRequestTarget target) {

                  EnumRadioPanel.this.onClick(selection, target);
               }
            });
         }
      });
   }

   public abstract void onClick (E selection, AjaxRequestTarget target);
}