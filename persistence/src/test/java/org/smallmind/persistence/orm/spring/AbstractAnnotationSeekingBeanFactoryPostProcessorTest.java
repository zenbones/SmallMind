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
package org.smallmind.persistence.orm.spring;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;
import org.mockito.Mockito;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.ManagedDao;
import org.smallmind.persistence.orm.MappedRelationships;
import org.smallmind.persistence.orm.MappedSubClasses;
import org.smallmind.persistence.orm.SessionSource;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link AbstractAnnotationSeekingBeanFactoryPostProcessor} through a concrete subclass that
 * declares a fixed set of DAO implementation types and one target annotation, driving the scan with a
 * Mockito-stubbed {@link ConfigurableListableBeanFactory}. Covers DAO detection, the durable-class
 * inference failure, recursion through {@link MappedSubClasses} and {@link MappedRelationships}, the
 * subclass-must-inherit validation, and {@link SessionSource}-keyed bucketing.
 *
 * <p>The class is unit-tested without a real Spring container: the bean factory mock only needs to
 * answer {@code getBeanDefinitionNames()} and {@code getType(name)}, which is all
 * {@code postProcessBeanFactory} consults.
 */
@Test(groups = "unit")
public class AbstractAnnotationSeekingBeanFactoryPostProcessorTest {

  private static ConfigurableListableBeanFactory beanFactoryWith (String beanName, Class<?> beanType) {

    ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);

    Mockito.when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[] {beanName});
    Mockito.doReturn(beanType).when(beanFactory).getType(beanName);

    return beanFactory;
  }

  private static Set<Class<?>> asSet (Class[] classes) {

    Set<Class<?>> set = new HashSet<>();

    for (Class type : classes) {
      set.add(type);
    }

    return set;
  }

  public void testDetectsDaoAndRecordsAnnotatedDurableUnderNullSessionKey () {

    SampleSeekingPostProcessor postProcessor = new SampleSeekingPostProcessor();

    postProcessor.postProcessBeanFactory(beanFactoryWith("widgetDao", WidgetDao.class));

    Assert.assertEquals(asSet(postProcessor.getAnnotatedClasses(null)), asSet(new Class[] {Widget.class}));
    // The default (null-keyed) accessor and the explicit null key return the same bucket.
    Assert.assertEquals(asSet(postProcessor.getAnnotatedClasses()), asSet(new Class[] {Widget.class}));
  }

  public void testIgnoresBeansThatAreNotDaoImplementations () {

    SampleSeekingPostProcessor postProcessor = new SampleSeekingPostProcessor();

    postProcessor.postProcessBeanFactory(beanFactoryWith("plainBean", String.class));

    Assert.assertEquals(postProcessor.getAnnotatedClasses().length, 0);
  }

  public void testIgnoresBeansWithUnresolvableType () {

    SampleSeekingPostProcessor postProcessor = new SampleSeekingPostProcessor();
    ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);

    Mockito.when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[] {"untyped"});
    Mockito.when(beanFactory.getType("untyped")).thenReturn(null);

    postProcessor.postProcessBeanFactory(beanFactory);

    Assert.assertEquals(postProcessor.getAnnotatedClasses().length, 0);
  }

  public void testUnannotatedDurableIsNotRecorded () {

    SampleSeekingPostProcessor postProcessor = new SampleSeekingPostProcessor();

    // PlainWidget carries no target annotation, so processClass records nothing even though the DAO is detected.
    postProcessor.postProcessBeanFactory(beanFactoryWith("plainWidgetDao", PlainWidgetDao.class));

    Assert.assertEquals(postProcessor.getAnnotatedClasses().length, 0);
  }

  @Test(groups = "unit", expectedExceptions = FatalBeanException.class)
  public void testThrowsWhenDurableClassCannotBeInferred () {

    SampleSeekingPostProcessor postProcessor = new SampleSeekingPostProcessor();

    // UninferableDao keeps its managed type as a type variable with no parameterized superclass, so
    // ManagedDaoSupport.findDurableClass returns null and the post-processor raises FatalBeanException.
    postProcessor.postProcessBeanFactory(beanFactoryWith("uninferableDao", UninferableDao.class));
  }

  public void testRecursesThroughMappedSubClasses () {

    SampleSeekingPostProcessor postProcessor = new SampleSeekingPostProcessor();

    postProcessor.postProcessBeanFactory(beanFactoryWith("animalDao", AnimalDao.class));

    Assert.assertEquals(asSet(postProcessor.getAnnotatedClasses()), asSet(new Class[] {Animal.class, Dog.class}));
  }

  public void testRecursesThroughMappedRelationships () {

    SampleSeekingPostProcessor postProcessor = new SampleSeekingPostProcessor();

    postProcessor.postProcessBeanFactory(beanFactoryWith("orderDao", OrderDao.class));

    Assert.assertEquals(asSet(postProcessor.getAnnotatedClasses()), asSet(new Class[] {Order.class, LineItem.class}));
  }

  @Test(groups = "unit", expectedExceptions = FatalBeanException.class)
  public void testThrowsWhenMappedSubclassDoesNotInheritFromParent () {

    SampleSeekingPostProcessor postProcessor = new SampleSeekingPostProcessor();

    // BadAnimal maps Mineral as a subclass, but Mineral does not extend BadAnimal.
    postProcessor.postProcessBeanFactory(beanFactoryWith("badAnimalDao", BadAnimalDao.class));
  }

  public void testBucketsDiscoveredClassesBySessionSourceKey () {

    SampleSeekingPostProcessor postProcessor = new SampleSeekingPostProcessor();

    postProcessor.postProcessBeanFactory(beanFactoryWith("alphaWidgetDao", AlphaWidgetDao.class));

    Assert.assertEquals(asSet(postProcessor.getAnnotatedClasses("alpha")), asSet(new Class[] {AlphaWidget.class}));
    // Nothing landed under the default (null) key.
    Assert.assertEquals(postProcessor.getAnnotatedClasses(null).length, 0);
    // An unknown key yields the empty array sentinel.
    Assert.assertEquals(postProcessor.getAnnotatedClasses("missing").length, 0);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface Persistent {

  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static class SampleSeekingPostProcessor extends AbstractAnnotationSeekingBeanFactoryPostProcessor {

    @Override
    public Class<? extends ManagedDao<?, ?>>[] getDaoImplementations () {

      return new Class[] {SampleDao.class};
    }

    @Override
    public Class<? extends Annotation>[] getTargetAnnotations () {

      return new Class[] {Persistent.class};
    }
  }

  public interface SampleDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends ManagedDao<I, D> {

  }

  public interface WidgetDao extends SampleDao<Long, Widget> {

    @Override
    Class<Widget> getManagedClass ();
  }

  public interface PlainWidgetDao extends SampleDao<Long, PlainWidget> {

    @Override
    Class<PlainWidget> getManagedClass ();
  }

  public interface AnimalDao extends SampleDao<Long, Animal> {

    @Override
    Class<Animal> getManagedClass ();
  }

  public interface BadAnimalDao extends SampleDao<Long, BadAnimal> {

    @Override
    Class<BadAnimal> getManagedClass ();
  }

  public interface OrderDao extends SampleDao<Long, Order> {

    @Override
    Class<Order> getManagedClass ();
  }

  @SessionSource("alpha")
  public interface AlphaWidgetDao extends SampleDao<Long, AlphaWidget> {

    @Override
    Class<AlphaWidget> getManagedClass ();
  }

  public interface GenericSampleDao<D extends Durable<Long>> extends SampleDao<Long, D> {

  }

  // Leaves getManagedClass inherited from the generic DAO interface, so its generic return type stays a
  // parameterized Class<D> with a type-variable argument and no parameterized superclass exposes a
  // concrete Durable. ManagedDaoSupport.findDurableClass therefore returns null. Declared abstract so it
  // need not supply the method body; the post-processor only inspects the type, never instantiates it.
  public abstract static class UninferableDao implements GenericSampleDao<Animal> {

  }

  @Persistent
  public static class Widget extends AbstractDurable<Long, Widget> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  public static class PlainWidget extends AbstractDurable<Long, PlainWidget> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  @Persistent
  public static class AlphaWidget extends AbstractDurable<Long, AlphaWidget> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  @Persistent
  @MappedSubClasses({Dog.class})
  public static class Animal extends AbstractDurable<Long, Animal> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  @Persistent
  public static class Dog extends Animal {

  }

  @Persistent
  @MappedSubClasses({Mineral.class})
  public static class BadAnimal extends AbstractDurable<Long, BadAnimal> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  @Persistent
  public static class Mineral extends AbstractDurable<Long, Mineral> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  @Persistent
  @MappedRelationships({LineItem.class})
  public static class Order extends AbstractDurable<Long, Order> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  @Persistent
  public static class LineItem extends AbstractDurable<Long, LineItem> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }
}
