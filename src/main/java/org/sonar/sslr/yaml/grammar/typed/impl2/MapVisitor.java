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
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
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
    ParameterizedType type = (ParameterizedType) t;

    Object child = typeDispatcher.visit(type.getActualTypeArguments()[1], annotations);
    return builder.object(builder.patternProperty(".*", child));
  }

}
