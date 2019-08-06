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

import org.junit.Before;
import org.junit.Test;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.GrammarRuleBuilder;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScalarVisitorTest {
  private YamlGrammarBuilder builder;
  private ScalarVisitor visitor;
  private TypeVisitor.Context context;

  @Before
  public void prepareMocks() {
    builder = makeGrammarBuilder();
    context = new VisitorContext();
    visitor = new ScalarVisitor(builder, context);
  }

  @Test
  public void declares_enum_constants() {
    Object result = visitor.visit(SomeEnum.class);

    verify(builder).firstOf("value1", "value2", "value3", "value4");
  }

  @Test
  public void declares_String() {
    Object result = visitor.visit(String.class);

    verify(builder).string();
  }

  @Test
  public void declares_Integer() {
    Object result = visitor.visit(Integer.class);

    verify(builder).integer();
  }

  @Test
  public void declares_Float() {
    Object result = visitor.visit(Float.class);

    verify(builder).floating();
  }

  @Test
  public void declares_Number() {
    when(builder.integer()).thenReturn("integer");
    when(builder.floating()).thenReturn("float");
    Object result = visitor.visit(Number.class);

    verify(builder).firstOf("integer", "float");
  }

  @Test
  public void declares_Boolean() {
    Object result = visitor.visit(Boolean.class);

    verify(builder).bool();
  }

  enum SomeEnum {
    value1, value2, value3, value4
  }

  private static YamlGrammarBuilder makeGrammarBuilder() {
    YamlGrammarBuilder builder = mock(YamlGrammarBuilder.class);
    GrammarRuleBuilder ruleBuilder = mock(GrammarRuleBuilder.class);
    when(builder.rule(any(GrammarRuleKey.class))).thenReturn(ruleBuilder);
    return builder;
  }
}