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
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
import org.sonar.sslr.yaml.grammar.typed.GrammarGeneratorException;
import org.sonar.sslr.yaml.grammar.typed.TypeVisitor;

public class MapVisitor implements TypeVisitor {
  private final YamlGrammarBuilder builder;
  private final TypeVisitor typeDispatcher;

  public MapVisitor(YamlGrammarBuilder builder, TypeVisitor typeDispatcher) {
    this.builder = builder;
    this.typeDispatcher = typeDispatcher;
  }

  @Override
  public Object visit(Type t, Annotation... annotations) {
    Type componentType;
    if (t instanceof ParameterizedType) {
      ParameterizedType type = (ParameterizedType) t;
      componentType = type.getActualTypeArguments()[1];
    } else if (t instanceof Class) {
      Class type = (Class) t;
      Type mapType = Arrays.stream(type.getGenericInterfaces())
          .filter(i -> i.getTypeName().contains("Map"))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException(new GrammarGeneratorException("Class " + type + " derives from an unparameterized Map.")));
      componentType = ((ParameterizedType)mapType).getActualTypeArguments()[1];
    } else {
      throw new IllegalStateException("Visiting type " + t + " as map but it doesn't seem to be a map!");
    }
    Object child = typeDispatcher.visit(componentType, annotations);
    return builder.object(builder.patternProperty(".*", child));
  }

}
