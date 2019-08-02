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
package org.sonar.sslr.yaml.grammar.typed.impl;

import com.google.common.collect.Lists;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Predicate;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.GrammarRuleBuilder;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.ValidationRule;
import org.sonar.sslr.yaml.grammar.DefaultGrammarBuilder;
import org.sonar.sslr.yaml.grammar.impl.BooleanValidation;
import org.sonar.sslr.yaml.grammar.impl.FirstOfValidation;
import org.sonar.sslr.yaml.grammar.impl.ObjectValidation;
import org.sonar.sslr.yaml.grammar.impl.RuleDefinition;
import org.sonar.sslr.yaml.grammar.impl.TokenValueValidation;
import org.sonar.sslr.yaml.grammar.typed.Choice;
import org.sonar.sslr.yaml.grammar.typed.Discriminant;
import org.sonar.sslr.yaml.grammar.typed.Discriminated;
import org.sonar.sslr.yaml.grammar.typed.GrammarGeneratorException;
import org.sonar.sslr.yaml.grammar.typed.Key;
import org.sonar.sslr.yaml.grammar.typed.Mandatory;
import org.sonar.sslr.yaml.grammar.typed.Pattern;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;

public class GrammarGenerator {
  private final DefaultGrammarBuilder b = new DefaultGrammarBuilder();
  private final Map<Class, GrammarRuleKey> classToKey = new IdentityHashMap<>();

  public RuleDefinition parse(Class rootClass) {
    b.rule(getRuleKey(Integer.class)).is(b.integer());
    b.rule(getRuleKey(Float.class)).is(b.floating());
    b.rule(getRuleKey(Boolean.class)).is(b.bool());
    b.rule(getRuleKey(String.class)).is(b.string());
    buildGrammar(rootClass);
    return b.build();
  }

  private void buildGrammar(Class rootClass) {
    GrammarRuleKey key = parseClass(rootClass, false);
    b.setRootRule(key);
  }

  private GrammarRuleKey parseClass(Class clazz, boolean skip) {
    GrammarRuleKey ruleKey = classToKey.get(clazz);
    if (ruleKey != null) {
      // has already been parsed, or is currently being parsed
      return ruleKey;
    }
    // TODO - wont work for collections
    ruleKey = getRuleKey(clazz);
    GrammarRuleBuilder rule = b.rule(ruleKey);

    Annotation annotation = clazz.getAnnotation(Discriminated.class);
    Annotation choice = clazz.getAnnotation(Choice.class);
    if (annotation != null) {
      rule.is(parseDerivedClasses(clazz, (Discriminated) annotation));
    } else if (choice != null) {
      if (List.class.isAssignableFrom(clazz)) {
        Class aClass = Arrays.stream(clazz.getInterfaces()).filter(c -> c.equals(List.class)).findFirst().orElseThrow(() -> new GrammarGeneratorException("Couldn't find a List interface"));
        Type type = Arrays.stream(clazz.getGenericInterfaces()).filter(t -> t.getTypeName().contains("List")).findFirst().orElseThrow(() -> new GrammarGeneratorException("Couldn't find a List type"));
        rule.is(inspectCollectionClass(aClass, type, "Class " + clazz.getName(), new DefaultTypeVisitor<Object, Object>() {
          @Override
          public Object visitListOrArray(Object definition) {
            return definition;
          }
        }));
      } else if (Map.class.isAssignableFrom(clazz)) {
        Class aClass = Arrays.stream(clazz.getInterfaces()).filter(c -> c.equals(Map.class)).findFirst().orElseThrow(() -> new GrammarGeneratorException("Couldn't find a Map interface"));
        Type type = Arrays.stream(clazz.getGenericInterfaces()).filter(t -> t.getTypeName().contains("Map")).findFirst().orElseThrow(() -> new GrammarGeneratorException("Couldn't find a Map type"));
        rule.is(inspectCollectionClass(aClass, type, "Class " + clazz.getName(), new DefaultTypeVisitor<Object, Object>() {
          @Override
          public Object visitMap(Object definition) {
            return definition;
          }
        }));
      }
    } else {
      Object masterRule = parseMasterClass(clazz);
      rule.is(masterRule);
    }
    if (skip) {
      rule.skip();
    }
    return ruleKey;
  }

  private static class ObjectVisitor implements TypeVisitor<Object, Object> {

    @Override
    public Object visitMap(Object definition) {
      return null;
    }

    @Override
    public Object visitListOrArray(Object definition) {
      return null;
    }

    @Override
    public Object visitScalar(Object definition) {
      return null;
    }

    @Override
    public Object visitObject(Object definition) {
      return null;
    }
  }

  private Object parseMasterClass(Class clazz) {
    if (clazz == JsonNode.class) {
      return b.anything();
    } else if (clazz == Object.class) {
      return b.anything();
    }
    ObjectValidation masterRule = new ObjectValidation();
    Map<String, ParsedMethod> methods = new HashMap<>();
    Stack<Class> interfaces = new Stack<>();
    interfaces.push(clazz);
    while (!interfaces.isEmpty()) {
      Class aClass = interfaces.pop();
      if (aClass == Object.class || aClass == Resolvable.class) {
        continue;
      }
      for (Method method : aClass.getMethods()) {
        parseMethod(methods, method);
      }
      Class[] parents = aClass.getInterfaces();
      Arrays.stream(parents).forEach(interfaces::push);
    }
    for (Map.Entry<String, ParsedMethod> entry : methods.entrySet()) {
      ParsedMethod m = entry.getValue();
      if (m.isDiscriminant) {
        masterRule.addProperty(b.discriminant(entry.getKey(), m.rule));
      } else if (m.isMandatory) {
        masterRule.addProperty(b.mandatoryProperty(entry.getKey(), m.rule));
      } else if (m.pattern != null) {
        masterRule.addProperty(b.patternProperty(entry.getKey(), m.rule));
      } else {
        masterRule.addProperty(b.property(entry.getKey(), m.rule));
      }
    }
    return masterRule;
  }

  private ValidationRule parseDerivedClasses(Class baseClass, Discriminated choice) {
    if (choice.value().length == 0) {
      throw new GrammarGeneratorException("Class " + baseClass.getName() + " is annotated with Discriminated but defines no sub-class choices.");
    }
    // TODO - check at least one method defines a discriminant
    List<ValidationRule> rules = new ArrayList<>();
    for (Class derivedClass : choice.value()) {
      if (!baseClass.isAssignableFrom(derivedClass)) {
        throw new GrammarGeneratorException("Class " + baseClass.getName() + " is annotated with Discriminated but defines non sub-classes choices.");
      }
      rules.add(b.convertToRule(parseClass(derivedClass, true)));
    }
    return new FirstOfValidation(rules.toArray(new ValidationRule[0]));
  }

  private GrammarRuleKey getRuleKey(Class clazz) {
    GrammarRuleKey ruleKey = classToKey.get(clazz);
    if (ruleKey == null) {
      // TODO - wont work for collections
      ruleKey = new StringRuleKey(clazz.getName());
      classToKey.put(clazz, ruleKey);
    }
    return ruleKey;
  }

  private void parseMethod(Map<String, ParsedMethod> parsed, Method method) {
    // Must be marked at once, so that we know it's already being parsed in case of recursive classes
    String propertyKey = getPropertyKey(method);
    ParsedMethod parsedMethod = parsed.get(propertyKey);
    if (parsedMethod != null) {
      // We are observing the base declaration of an override - just check for a discriminant declaration
      parsedMethod.isDiscriminant |= isDiscriminant(method);
    } else {
      Object valueDefinition = getPropertyValueDefinition(method);
      // TODO- see how to handle more cleanly the Maps
      parsedMethod = (valueDefinition instanceof ParsedMethod) ? (ParsedMethod) valueDefinition : new ParsedMethod(valueDefinition, method);
      parsed.put(propertyKey, parsedMethod);
    }
  }

  private static String getPropertyKey(Method method) {
    Key annotation = method.getAnnotation(Key.class);
    if (annotation != null) {
      return annotation.value();
    } else {
      return method.getName();
    }
  }

  private static boolean isMandatory(Method method) {
    return method.getAnnotation(Mandatory.class) != null;
  }

  private static boolean isDiscriminant(Method method) {
    return method.getAnnotation(Discriminant.class) != null;
  }

  private static String getPropertyPattern(Method method) {
    Pattern annotation = method.getAnnotation(Pattern.class);
    if (annotation != null && !Map.class.isAssignableFrom(method.getReturnType())) {
      throw new GrammarGeneratorException("Method "+ method.getName() + " is annotated with Pattern annotation and doesn't return Map<String, X>");
    }
    if (annotation != null) {
      return annotation.value();
    } else  if (Map.class.isAssignableFrom(method.getReturnType())) {
      return ".*";
    } else {
      return null;
    }
  }

  private Object getPropertyValueDefinition(Method method) {
    Class<?> returnType = method.getReturnType();
    if (JsonNode.class.isAssignableFrom(returnType)) {
      return b.anything();
    }
    boolean isMap = Map.class.isAssignableFrom(returnType);
    boolean isList = List.class.isAssignableFrom(returnType);
    boolean isArray = returnType.isArray();
    boolean isCollection = isMap || isList || isArray;
    Choice choice = method.getAnnotation(Choice.class);
    if (choice == null && !isCollection) {
      return makePrimitiveValueDefinition(returnType);
    } else if (choice != null && !isCollection) {
      return makeChoicePrimitiveValueDefinition(method, returnType, choice);
    } else {
      return makeCollectionValueDefinition(method);
    }
  }

  interface TypeVisitor<T, R> {
    R visitMap(T definition);
    R visitListOrArray(T definition);
    R visitScalar(T definition);
    R visitObject(T definition);
  }
  private static class DefaultTypeVisitor<T, R> implements TypeVisitor<T,R> {

    @Override
    public R visitMap(T definition) {
      return null;
    }

    @Override
    public R visitListOrArray(T definition) {
      return null;
    }

    @Override
    public R visitScalar(T definition) {
      return null;
    }

    @Override
    public R visitObject(T definition) {
      return null;
    }
  }

  private Object inspectCollectionClass(Class<?> theClass, Type generic, String context, TypeVisitor<Object,Object> ruleConsumer) {
    if (theClass.isArray()) {
      Class<?> componentType = theClass.getComponentType();
      return ruleConsumer.visitListOrArray(b.array(parseClass(componentType, false)));
    }
    if (!(generic instanceof ParameterizedType)) {
      throw new GrammarGeneratorException(context + " uses class " + theClass + " as unparameterized collection type");
    }
    ParameterizedType returnType = (ParameterizedType) generic;
    Type[] arguments = returnType.getActualTypeArguments();
    if (Map.class.isAssignableFrom(theClass)) {
      return ruleConsumer.visitMap(parseClass(classFromType(arguments[1]), false));
    } else if (List.class.isAssignableFrom(theClass)) {
      Class<?> componentType = classFromType(arguments[0]);
      return ruleConsumer.visitListOrArray(b.array(parseClass(componentType, false)));
    } else {
      throw new GrammarGeneratorException("Unsupported collection type: " + returnType);
    }
  }

  private Object makeCollectionValueDefinition(Method method) {
    return inspectCollectionClass(method.getReturnType(), method.getGenericReturnType(), "Method " + method, new DefaultTypeVisitor<Object, Object>() {
      @Override
      public Object visitMap(Object definition) {
        // TODO - ugly, but works for want of a better structure
        return new ParsedMethod(definition, method);
      }

      @Override
      public Object visitListOrArray(Object definition) {
        return definition;
      }
    });
  }

  public static Class<?> classFromType(Type argument) {
    try {

      return Class.forName(argument.getTypeName());
    } catch (ClassNotFoundException e) {
      throw new GrammarGeneratorException("Cannot load class " + argument.getTypeName());
    }
  }

  private Object makePrimitiveValueDefinition(Class<?> returnType) {
    if (Integer.class.isAssignableFrom(returnType)) {
      return b.integer();
    } else if (Float.class.isAssignableFrom(returnType)) {
      return b.floating();
    } else if (Boolean.class.isAssignableFrom(returnType)) {
      return b.bool();
    } else if (String.class.isAssignableFrom(returnType)) {
      return b.string();
    } else {
      // just assume it's a class definition that needs to be parsed
      return b.convertToRule(parseClass(returnType, false));
    }
  }

  private Object makeChoicePrimitiveValueDefinition(Method method, Class<?> returnType, Choice choice) {
    String[] strings = choice.value();
    Class[] classes = choice.classes();
    boolean[] bools = choice.bools();
    long countNonEmpty = Lists.newArrayList(strings.length > 0, classes.length > 0, bools.length > 0).stream().filter(Predicate.isEqual(true)).count();
    if (countNonEmpty > 1 && returnType != Object.class) {
      throw new GrammarGeneratorException("Method " + method.getDeclaringClass().getName() + "." + method.getName() + " does not return Object and is annotated with multi-typed Choice annotation.");
    } else if (countNonEmpty == 1 && strings.length > 0 && returnType != String.class) {
      throw new GrammarGeneratorException("Method " + method.getDeclaringClass().getName() + "." + method.getName() + " does not return String and defines string choices.");
    } else if (countNonEmpty == 1 && bools.length > 0 && returnType != Boolean.class) {
      throw new GrammarGeneratorException("Method " + method.getDeclaringClass().getName() + "." + method.getName() + " does not return Boolean and defines boolean choices.");
    } else if (countNonEmpty == 1 && classes.length > 0 && returnType != Object.class) {
      throw new GrammarGeneratorException("Method " + method.getDeclaringClass().getName() + "." + method.getName() + " does not return Object and defines class choices.");
    }

    List<ValidationRule> allRules = new ArrayList<>();
    Arrays.stream(strings).map(TokenValueValidation::new).forEach(allRules::add);
    if (bools.length == 1) {
      allRules.add(new BooleanValidation(bools[0]));
    } else if (bools.length > 1) {
      allRules.add(new BooleanValidation(null));
    }
    Arrays.stream(classes).forEach(c -> this.parseClass(c, false));
    Arrays.stream(classes).map(this::getRuleKey).map(b::convertToRule).forEach(allRules::add);
    return new FirstOfValidation(allRules.toArray(new ValidationRule[0]));
  }

  public void print() {
    b.print(System.out);
  }

  private static class StringRuleKey implements GrammarRuleKey {
    private final String name;

    private StringRuleKey(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private static class ParsedMethod {
    public final Object rule;
    public boolean isDiscriminant;
    public final boolean isMandatory;
    public final String pattern;

    public ParsedMethod(Object rule, Method method) {
      this.rule = rule;
      isMandatory = isMandatory(method);
      isDiscriminant = isDiscriminant(method);
      pattern = getPropertyPattern(method);
    }
  }
}
