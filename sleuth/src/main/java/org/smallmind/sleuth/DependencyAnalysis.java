package org.smallmind.sleuth;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class DependencyAnalysis<T> {

  private HashMap<String, Dependency<T>> dependencyMap = new HashMap<>();
  private Class<T> clazz;

  public DependencyAnalysis (Class<T> clazz) {

    this.clazz = clazz;
  }

  public void add (Dependency<T> dependency, String[] dependsOn) {

    Dependency<T> mappedDependency;

    if ((mappedDependency = dependencyMap.putIfAbsent(dependency.getName(), dependency)).getValue() == null) {
      mappedDependency.setValue(dependency.getValue());
    }

    if ((dependsOn != null) && (dependsOn.length > 0)) {
      for (String parentName : dependsOn) {

        Dependency<T> parentDependency;

        if ((parentDependency = dependencyMap.get(parentName)) == null) {
          dependencyMap.put(parentName, parentDependency = new Dependency<>(parentName));
        }
        parentDependency.addChild(mappedDependency);
      }
    }
  }

  public LinkedList<T> calculate () {

    LinkedList<T> dependencyValueList = new LinkedList<>();

    while (!dependencyMap.isEmpty()) {

      Iterator<Dependency<T>> dependencyIter = dependencyMap.values().iterator();

      while (dependencyIter.hasNext()) {
        visit(dependencyIter.next(), dependencyIter, dependencyValueList);
      }
    }

    return dependencyValueList;
  }

  private void visit (Dependency<T> dependency, Iterator<Dependency<T>> dependencyIter, LinkedList<T> dependencyValueList) {

    if (dependency.isTemporary()) {
      throw new TestDependencyException("Cyclic dependency(%s) on node(%s)", clazz.getSimpleName(), dependency.getName());
    }
    if (!(dependency.isTemporary() || dependency.isPermanent())) {

      T value;

      dependency.setTemporary();
      for (Dependency<T> childDependency : dependency.getChildren()) {
        visit(childDependency, dependencyIter, dependencyValueList);
      }
      if ((value = dependency.getValue()) == null) {
        throw new TestDependencyException("Missing dependency(%s) on node(%s)", clazz.getSimpleName(), dependency.getName());
      }
      dependency.setPermanent();
      dependency.unsetTemporary();
      dependencyValueList.addFirst(value);
      dependencyIter.remove();
    }
  }
}