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
import java.lang.reflect.Type;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.DefaultGrammarBuilder;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;
import org.sonar.sslr.yaml.grammar.typed.TypeVisitor;

public class ResolvableVisitor implements TypeVisitor {
  private final DefaultGrammarBuilder builder;
  private final Context context;

  public ResolvableVisitor(DefaultGrammarBuilder builder, Context context) {
    this.builder = builder;
    this.context = context;
  }

  @Override
  public Object visit(Type type, Annotation... annotations) {
    GrammarRuleKey ruleKey = context.makeTypeKey(Resolvable.class);
    if (!context.add(ruleKey)) {
      return ruleKey;
    }
    builder.rule(ruleKey).is(builder.object(builder.mandatoryProperty("$ref", builder.string())));
    return ruleKey;
  }
}
