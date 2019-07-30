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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
import org.sonar.sslr.yaml.grammar.typed.Choice;
import org.sonar.sslr.yaml.grammar.typed.DiscriminantValue;
import org.sonar.sslr.yaml.grammar.typed.Discriminated;
import org.sonar.sslr.yaml.grammar.typed.GrammarGeneratorException;
import org.sonar.sslr.yaml.grammar.typed.TypeVisitor;

import static org.sonar.sslr.yaml.grammar.typed.impl2.TypeUtils.find;

public class ChoiceVisitor implements TypeVisitor {
  private final YamlGrammarBuilder builder;
  private final TypeVisitor typeDispatcher;
  private final Context context;

  public ChoiceVisitor(YamlGrammarBuilder builder, TypeVisitor typeDispatcher, Context context) {
    this.builder = builder;
    this.typeDispatcher = typeDispatcher;
    this.context = context;
  }

  @Override
  public Object visit(Type t, Annotation... annotations) {
    validateType(t);

    if (t instanceof Class) {
      Class<?> type = (Class<?>)t;
      if (type.getAnnotation(Discriminated.class) != null) {
        return visitTypeHierarchy(type);
      } else if(type.getAnnotation(Choice.class) != null) {
        if (List.class.isAssignableFrom(type)) {
          return visitListChoice(type, type.getAnnotation(Choice.class));
        } else if (Map.class.isAssignableFrom(type)) {
          return visitMapChoice(type, type.getAnnotation(Choice.class));
        }
      }
    }

    return visitChoice(t, annotations);
  }

  private void validateType(Type t) {
    if (t == JsonNode.class) {
      throw new GrammarGeneratorException("Using type JsonNode with a Choice annotation");
    }
  }

  private Object visitTypeHierarchy(Class<?> type) {
    Discriminated discriminated = type.getAnnotation(Discriminated.class);
    List<Object> keys = new ArrayList<>();
    for (Class<?> child : discriminated.value()) {
      if (!type.isAssignableFrom(child)) {
        throw new GrammarGeneratorException(type.getName() + " references " + child.getName() + " but is not its parent.");
      }
      Object childKey = typeDispatcher.visit(child);
      builder.rule((GrammarRuleKey) childKey).skip();
      keys.add(childKey);
    }
    GrammarRuleKey ruleKey = context.makeTypeKey(type);
    builder.rule(ruleKey).is(declareFirstOf(keys));
    return ruleKey;
  }

  private Object visitListChoice(Type type, Choice choice) {
    ParameterizedType p;
    if (type instanceof Class) {
      p = (ParameterizedType)Arrays.stream(((Class)type).getGenericInterfaces())
          .filter(t -> t.getTypeName().contains("List"))
          .findAny()
          .orElseThrow(()->new GrammarGeneratorException("Class " + type + " is annotated with Choice and doesn't derive from List or Map."));
    } else {
      p = (ParameterizedType) type;
    }
    Type componentType = p.getActualTypeArguments()[0];

    Object choices = typeDispatcher.visit(componentType, choice);
    return builder.array(choices);
  }

  private Object visitMapChoice(Type type, Choice choice) {
    ParameterizedType p;
    if (type instanceof Class) {
      p = (ParameterizedType)Arrays.stream(((Class)type).getGenericInterfaces())
          .filter(t -> t.getTypeName().contains("Map"))
          .findAny()
          .orElseThrow(()->new GrammarGeneratorException("Class " + type + " is annotated with Choice and doesn't derive from List or Map."));
    } else {
      p = (ParameterizedType) type;
    }
    Type componentType = p.getActualTypeArguments()[1];

    Object choices = typeDispatcher.visit(componentType, choice);
    // TODO - find how to declare the proxy (this is actually a mapped collection and not an object!) => special method?
    return builder.object(builder.patternProperty(".*", choices));
  }

  private Object visitChoice(Type type, Annotation[] annotations) {
    Choice choice = find(annotations, Choice.class);

    if (type == String.class) {
      return visitStringChoice(choice, find(annotations, DiscriminantValue.class));
    } else if (type == Boolean.class) {
      return visitBoolChoice(choice);
    } else if (type == Object.class) {
      return visitObjectChoice(choice);
    } else if (type instanceof ParameterizedType) {
      ParameterizedType p = (ParameterizedType) type;
      if (p.getRawType() == List.class) {
        return visitListChoice(type, choice);
      } else if (p.getRawType() == Map.class) {
        return visitMapChoice(type, choice);
      }
    } else if (type instanceof Class) {
      Class p = (Class) type;
      if (List.class.isAssignableFrom(p)) {
        return visitListChoice(type, choice);
      } else if (Map.class.isAssignableFrom(p)) {
        return visitMapChoice(type, choice);
      }
    }
    throw new GrammarGeneratorException("Choice annotation can only be put on the following types: String, Boolean, List, Map, but was found on " + type);
  }

  private Object visitObjectChoice(Choice choice) {
    if (choice.bools().length == 0 && choice.value().length == 0 && choice.classes().length == 0) {
      throw new GrammarGeneratorException("Using Choice annotation without any value!");
    }

    List<Object> keys = new ArrayList<>(Arrays.asList(choice.value()));
    for(boolean b : choice.bools()) {
      keys.add(b);
    }
    for (Class<?> child : choice.classes()) {
      if (child.isEnum()) {
        Arrays.stream(child.getEnumConstants()).map(Object::toString).forEach(keys::add);
      } else {
        keys.add(typeDispatcher.visit(child));
      }
    }

    return declareFirstOf(keys);
  }

  private Object visitBoolChoice(Choice choice) {
    if (choice.bools().length == 0) {
      throw new GrammarGeneratorException("Reducing the list of choices of a Boolean without providing values!");
    } else if (choice.bools().length > 1) {
      throw new GrammarGeneratorException("Reducing the list of choices of a Boolean with more than one value! Use classes={Boolean.class} instead.");
    }
    if (choice.value().length > 0 || choice.classes().length > 0) {
      throw new GrammarGeneratorException("Reducing the list of choices of a Boolean with more than just a boolean!");
    }
    return builder.bool(choice.bools()[0]);
  }

  private Object visitStringChoice(Choice choice, DiscriminantValue discriminant) {
    if (choice != null && discriminant != null) {
      throw new GrammarGeneratorException("Cannot use both DiscriminantValue on a class and Choice on the discriminant method.");
    }
    List<Object> choices = new ArrayList<>();
    if (choice != null) {
      Arrays.stream(choice.value()).forEach(choices::add);
      if (choice.bools().length > 0) {
        throw new GrammarGeneratorException("Reducing the list of choices of a String with more than just strings or enums!");
      } else if (choice.classes().length > 0){
        for (Class aClass : choice.classes()) {
          if (aClass.isEnum()) {
            Arrays.stream(aClass.getEnumConstants()).map(Object::toString).forEach(choices::add);
          } else {
            throw new GrammarGeneratorException("Reducing the list of choices of a String with more than just strings or enums!");
          }
        }
      }
      if (choices.isEmpty()) {
        throw new GrammarGeneratorException("Reducing the list of choices of a String without providing values!");
      }
    } else {
      Arrays.stream(discriminant.value()).forEach(choices::add);
    }
    return declareFirstOf(choices);
  }

  private Object declareFirstOf(List<Object> choices) {
    Object choiceRule;
    if (choices.size() == 1) {
      choiceRule = choices.get(0);
    } else if (choices.size() == 2) {
      choiceRule = builder.firstOf(choices.get(0), choices.get(1));
    } else {
      choiceRule = builder.firstOf(choices.get(0), choices.get(1), choices.subList(2, choices.size()).toArray(new Object[0]));
    }
    return choiceRule;
  }


}
