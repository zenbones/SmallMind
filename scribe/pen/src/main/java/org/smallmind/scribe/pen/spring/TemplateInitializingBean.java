/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.scribe.pen.spring;

import java.util.LinkedList;
import java.util.List;
import org.smallmind.scribe.pen.Template;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link InitializingBean} that accepts a list of pre-built {@link Template} instances and calls
 * {@link Template#register()} on each of them during Spring's post-construction callback, making the
 * templates active within the Scribe framework.
 */
public class TemplateInitializingBean implements InitializingBean {

  private final LinkedList<Template> initialTemplates;

  /**
   * Constructs a {@code TemplateInitializingBean} with an empty internal list, ready to accept templates
   * via {@link #setInitialTemplates(List)}.
   */
  public TemplateInitializingBean () {

    initialTemplates = new LinkedList<>();
  }

  /**
   * Provides the list of {@link Template} instances that this bean will register when
   * {@link #afterPropertiesSet()} is invoked; the templates are appended to the internal list.
   *
   * @param initialTemplates the templates to register; must not be {@code null}
   */
  public void setInitialTemplates (List<Template> initialTemplates) {

    this.initialTemplates.addAll(initialTemplates);
  }

  /**
   * Iterates over all templates provided via {@link #setInitialTemplates(List)} and calls
   * {@link Template#register()} on each one, making them active within the Scribe logging framework.
   */
  public void afterPropertiesSet () {

    for (Template template : initialTemplates) {
      template.register();
    }
  }
}
