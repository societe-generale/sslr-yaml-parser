/*
 * Sonar SSLR :: YAML Parser
 * Copyright (C) 2018-2019 Societe Generale
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
package org.sonar.sslr.yaml.grammar.typed.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
import org.sonar.sslr.yaml.grammar.typed.GrammarGeneratorException;

public class ListVisitor implements TypeVisitor {
  private final YamlGrammarBuilder builder;
  private final TypeVisitor typeDispatcher;
  private Context context;

  public ListVisitor(YamlGrammarBuilder builder, TypeVisitor typeDispatcher, Context context) {
    this.builder = builder;
    this.typeDispatcher = typeDispatcher;
    this.context = context;
  }

  @Override
  public Object visit(Type t, Annotation... annotations) {
    Type componentType;
    Class declaredType;
    if (t instanceof ParameterizedType) {
      ParameterizedType type = (ParameterizedType) t;
      declaredType = (Class)type.getRawType();
      componentType = type.getActualTypeArguments()[0];
    } else if (t instanceof Class) {
      Class type = (Class) t;
      declaredType = type;
      if (type.isArray()) {
        componentType = type.getComponentType();
      } else {
        Type listType = Arrays.stream(type.getGenericInterfaces())
            .filter(i -> i.getTypeName().contains("List"))
            .findFirst()
            .orElseThrow(() -> new GrammarGeneratorException("Class " + type + " derives from an unparameterized List."));
        componentType = ((ParameterizedType)listType).getActualTypeArguments()[0];
      }
    } else if (t instanceof GenericArrayType) {
      GenericArrayType type = (GenericArrayType) t;
      componentType = type.getGenericComponentType();
      // TODO
      declaredType = Object.class;
    } else {
      throw new IllegalStateException("Visiting type " + t + " as list but it doesn't seem to be a list or array!");
    }
    Object child = typeDispatcher.visit(componentType, annotations);
    context.declareTypes(declaredType);
    return builder.array(child);
  }

}
