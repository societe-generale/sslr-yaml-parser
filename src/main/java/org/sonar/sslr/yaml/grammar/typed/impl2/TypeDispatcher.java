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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.typed.Choice;
import org.sonar.sslr.yaml.grammar.typed.DiscriminantValue;
import org.sonar.sslr.yaml.grammar.typed.Discriminated;
import org.sonar.sslr.yaml.grammar.typed.GrammarGeneratorException;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;
import org.sonar.sslr.yaml.grammar.typed.TypeVisitor;
import org.sonar.sslr.yaml.grammar.typed.VisitorFactory;

public class TypeDispatcher implements TypeVisitor {
  private final VisitorFactory factory;

  public TypeDispatcher(VisitorFactory factory) {
    this.factory = factory;
  }

  public Object visit(Type type, Annotation... annotations) {
    if(type == Resolvable.class) {
      return factory.resolvable().visit(type, annotations);
    } else if (hasChoice(annotations) || hasDiscriminantValue(annotations) || hasChoice(type) || hasDiscriminated(type)) {
      return factory.choice().visit(type, annotations);
    } else if (type == String.class) {
      return factory.scalar().visit(type, annotations);
    } else if (type == Integer.class) {
      return factory.scalar().visit(type, annotations);
    } else if (type == Float.class) {
      return factory.scalar().visit(type, annotations);
    } else if (type == Number.class) {
      return factory.scalar().visit(type, annotations);
    } else if (type == Boolean.class) {
      return factory.scalar().visit(type, annotations);
    } else if (type == JsonNode.class) {
      return factory.scalar().visit(type, annotations);
    } else if (type == Object.class) {
      throw new GrammarGeneratorException("Using type Object without a Choice annotation");
    } else if (type instanceof Class) {
      Class clazz = (Class)type;
      if (clazz.isEnum()) {
        return factory.scalar().visit(type, annotations);
      } else if (clazz.isArray() || List.class.isAssignableFrom(clazz)) {
        // TODO - in the case of List, we're not checking that it doesn't define any more methods...
        return factory.list().visit(clazz, annotations);
      } else if (Map.class.isAssignableFrom(clazz)){
        // TODO - we're not checking that it doesn't define any more methods...
        return factory.map().visit(clazz, annotations);
      } else {
        return factory.object().visit(type, annotations);
        // 4 cases here:
        // - regular object to scan
        // - object deriving from List -> must pass it below
        // - object deriving from Map -> must defer any property not matched in other methods to the default get()
        //               (like an object with a catch-all ".*' at the end)
      }
    } else if (type instanceof GenericArrayType) {
      GenericArrayType generic = (GenericArrayType) type;
      return factory.list().visit(type, annotations);
    } else if (type instanceof ParameterizedType){
      ParameterizedType parameterized = (ParameterizedType)type;
      if (Map.class == parameterized.getRawType()) {
        return factory.map().visit(type, annotations);
        // 2 cases here:
        //   Map with a @Pattern -> a generic set of properties in parent object
        //   Map with no annotation -> a generic object with no identity (like AuthFlow.scopes() or Link.parameters())
      } else if (List.class == parameterized.getRawType()) {
        return factory.list().visit(type, annotations);
      }
    }
    throw new GrammarGeneratorException("Found an unsupported type: " + type);
  }

  private static boolean hasChoice(Annotation[] annotations) {
    return Arrays.stream(annotations).anyMatch(a -> a instanceof Choice);
  }
  private static boolean hasDiscriminantValue(Annotation[] annotations) {
    return Arrays.stream(annotations).anyMatch(a -> a instanceof DiscriminantValue);
  }
  private static boolean hasDiscriminated(Type type) {
    return type instanceof Class && ((Class)type).getAnnotation(Discriminated.class) != null;
  }
  private static boolean hasChoice(Type type) {
    if (!(type instanceof Class)) {
      return false;
    }
    Class clazz = (Class) type;
    return clazz.getAnnotation(Choice.class) != null;
  }
}
