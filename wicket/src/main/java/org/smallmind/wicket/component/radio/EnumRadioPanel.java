package org.smallmind.wicket.component.radio;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.Loop;
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
            item.add(new Label("enumRadioLabel", new Model<String>(enumerations[item.getIteration()].toString())));
            item.add(new AjaxRadio<E>("enumRadioButton", new Model<E>(enumerations[item.getIteration()])) {

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