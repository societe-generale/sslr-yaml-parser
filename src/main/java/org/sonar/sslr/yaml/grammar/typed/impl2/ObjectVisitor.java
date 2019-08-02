/*
 * Sonar SSLR :: YAML Parser
 * Copyright (C) 2018-2018 Societe Generale
 * vincent.girard-reydet AT socgen DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.sslr.yaml.grammar.typed.impl2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.GrammarRuleBuilder;
import org.sonar.sslr.yaml.grammar.PropertyDescription;
import org.sonar.sslr.yaml.grammar.ValidationRule;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
import org.sonar.sslr.yaml.grammar.typed.Discriminant;
import org.sonar.sslr.yaml.grammar.typed.DiscriminantValue;
import org.sonar.sslr.yaml.grammar.typed.Discriminated;
import org.sonar.sslr.yaml.grammar.typed.GrammarGeneratorException;
import org.sonar.sslr.yaml.grammar.typed.Key;
import org.sonar.sslr.yaml.grammar.typed.Mandatory;
import org.sonar.sslr.yaml.grammar.typed.Pattern;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;
import org.sonar.sslr.yaml.grammar.typed.TypeVisitor;

public class ObjectVisitor implements TypeVisitor {
  private final YamlGrammarBuilder builder;
  private final TypeVisitor typeDispatcher;
  private final Context context;
  private String discriminantProperty = null;
  private Annotation[] classAnnotations;
  private Class<?> type;

  public ObjectVisitor(YamlGrammarBuilder builder, TypeVisitor typeDispatcher, Context context) {
    this.builder = builder;
    this.typeDispatcher = typeDispatcher;
    this.context = context;
  }

  @Override
  public Object visit(Type t, Annotation... annotations) {
    type = validateType(t);
    GrammarRuleKey ruleKey = context.makeTypeKey(type);

    boolean isResolvable = isResolvable(type);
    boolean alreadySeen = !context.add(ruleKey);
    if (alreadySeen && !isResolvable) {
      return ruleKey;
    }
    if (!alreadySeen) {
      GrammarRuleBuilder rule = builder.rule(ruleKey);
      List<PropertyDescription> properties = parseMethodsInAllInterfaces();
      declarePropertiesInObject(rule, properties);
    }
    if (isResolvable) {
      Object resolvable = typeDispatcher.visit(Resolvable.class);
      return builder.firstOf(resolvable, ruleKey);
    } else {
      return ruleKey;
    }
  }

  private Class<?> validateType(Type t) {
    if (!(t instanceof Class)) {
      throw new GrammarGeneratorException("Expected a class to inspect, got generic type " + t);
    }
    Class<?> type = (Class<?>)t;
    if (!type.isInterface()) {
      throw new GrammarGeneratorException("Expected interface, got concrete class " + type);
    }
    Class<?>[] interfaces = type.getInterfaces();
    if (Arrays.stream(interfaces).anyMatch(i -> i == List.class || i == Collection.class || i == Map.class)) {

      throw new GrammarGeneratorException("Complex type " +type+ " cannot derive from a collection interface");
    }
    classAnnotations = type.getAnnotations();
    if (TypeUtils.find(classAnnotations, Discriminated.class) != null) {
      throw new IllegalStateException("Visiting discriminated class " + type + " when it should have been dispatched to a choice visitor!");
    }
    return type;
  }

  private void declarePropertiesInObject(GrammarRuleBuilder rule, List<PropertyDescription> properties) {
    ValidationRule objectRule;
    if (properties.size() == 1) {
      objectRule = builder.object(properties.get(0));
    } else if (properties.size() == 2) {
      objectRule = builder.object(properties.get(0), properties.get(1));
    } else {
      objectRule = builder.object(properties.get(0), properties.get(1), properties.subList(2, properties.size()).toArray(new PropertyDescription[0]));
    }
    rule.is(objectRule);
  }

  private List<PropertyDescription> parseMethodsInAllInterfaces() {
    List<PropertyDescription> properties = new ArrayList<>();

    Arrays.stream(type.getMethods())
        .filter(ObjectVisitor::isNotFromCollectionOrResolvable)
        .map(this::parseMethod)
        .forEach(properties::add);

    if (properties.isEmpty()) {
      throw new GrammarGeneratorException("Parsing interface " + type + " which has no usable methods!");
    }

    return properties;
  }

  private PropertyDescription parseMethod(Method method) {
    if (!hasGetterProfile(method)) {
      throw new GrammarGeneratorException("Parsed interfaces must only have getter-like methods. Wrong method " + method);
    }
    boolean isDiscriminant = checkDiscriminant(method);
    String pattern = checkPattern(method);
    // TODO - check that pattern is not combined with Discriminant or Mandatory

    Type returnType = method.getGenericReturnType();
    if (pattern != null) {
      returnType = ((ParameterizedType)returnType).getActualTypeArguments()[1];
    }
    Annotation[] annotations = computeDispatchAnnotations(method, classAnnotations);
    Object visited = typeDispatcher.visit(returnType, annotations);

    String propertyName = computePropertyName(method);
    boolean isMandatory = method.getAnnotation(Mandatory.class) != null;
    if (isDiscriminant) {
      return builder.discriminant(propertyName, visited);
    } else if (isMandatory) {
      return builder.mandatoryProperty(propertyName, visited);
    } else if (pattern != null) {
      // TODO - we will need to inject the property name at some point to generate the proxy
      return builder.patternProperty(pattern, visited);
    } else {
      return builder.property(propertyName, visited);
    }
  }

  private boolean checkDiscriminant(Method method) {
    boolean isDiscriminant = method.getAnnotation(Discriminant.class) != null;
    if (!isDiscriminant) {
      return false;
    }
    if (discriminantProperty != null) {
      throw new GrammarGeneratorException("Method " + method + " is a discriminant, but one has already been observed: " + discriminantProperty);
    }
    if (TypeUtils.find(classAnnotations, DiscriminantValue.class) == null) {
      throw new GrammarGeneratorException("Class " + type.getName() + " does not define a DiscriminantValue for discriminant " + method);
    }
    // TODO - check the method returns a String
    discriminantProperty = method.getName();
    return true;
  }

  private String checkPattern(Method method) {
    Pattern pattern = method.getAnnotation(Pattern.class);
    if (pattern == null) {
      return null;
    }
    Type returnType = method.getGenericReturnType();
    if (!(returnType instanceof ParameterizedType)) {
      throw new GrammarGeneratorException("Method " + method + " is decorated with @Pattern but doesn't return a Map<String, ...>.");
    }
    ParameterizedType mapType = (ParameterizedType) returnType;
    if (mapType.getRawType() != Map.class || mapType.getActualTypeArguments()[0] != String.class) {
      throw new GrammarGeneratorException("Method " + method + " is decorated with @Pattern but doesn't return a Map<String, ...>.");
    }
    return pattern.value();

  }

  private Annotation[] computeDispatchAnnotations(Method method, Annotation[] classAnnotations) {
    Discriminant discriminant = method.getAnnotation(Discriminant.class);
    if (discriminant != null) {
      Annotation additional = TypeUtils.find(classAnnotations, DiscriminantValue.class);
      return TypeUtils.combine(method.getAnnotations(), additional);
    } else {
      return method.getAnnotations();
    }
  }

  private String computePropertyName(Method method) {
    String propertyName = method.getName();
    Key key = method.getAnnotation(Key.class);
    if (key != null) {
      propertyName = key.value();
    }
    return propertyName;
  }

  private static boolean isResolvable(Class<?> type) {
    return Resolvable.class.isAssignableFrom(type);
  }

  private static boolean hasGetterProfile(Method method) {
    return method.getParameterCount() == 0 && method.getReturnType() != void.class;
  }

  private static boolean isNotFromCollectionOrResolvable(Method method) {
    Class<?> parent = method.getDeclaringClass();
    return parent != Collection.class && parent != Iterable.class && parent != List.class && parent != Map.class && parent != Resolvable.class;
  }
}
