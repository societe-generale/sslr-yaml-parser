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
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;

public class ScalarVisitor implements TypeVisitor {
  private final YamlGrammarBuilder builder;
  private Context context;

  public ScalarVisitor(YamlGrammarBuilder builder, Context context) {
    this.builder = builder;
    this.context = context;
  }

  @Override
  public Object visit(Type type, Annotation... annotations) {
    if (type == String.class) {
      return builder.string();
    } else if (type == Integer.class) {
      return builder.integer();
    } else if (type == Float.class) {
      return builder.floating();
    } else if (type == Number.class) {
      return builder.firstOf(builder.integer(), builder.floating());
    } else if (type == Boolean.class) {
      return builder.bool();
    } else if (type == JsonNode.class) {
      return builder.anything();
    } else {
      Class<?> values = (Class<?>) type;
      context.declareTypes(values);
      List<Object> collect = Arrays.stream(values.getEnumConstants()).map(Object::toString).collect(Collectors.toList());
      return declareFirstOf(collect);
    }
  }

  private Object declareFirstOf(List<Object> choices) {
    Object choiceRule;
    if (choices.size() == 1) {
      choiceRule = choices.get(0);
    } else if (choices.size() == 2) {
      choiceRule = builder.firstOf(choices.get(0), choices.get(1));
    } else {
      choiceRule = builder.firstOf(choices.get(0), choices.get(1),
          choices.subList(2, choices.size()).toArray(new Object[0]));
    }
    return choiceRule;
  }
}
