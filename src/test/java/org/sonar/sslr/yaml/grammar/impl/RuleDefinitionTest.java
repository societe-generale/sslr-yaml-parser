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
package org.sonar.sslr.yaml.grammar.impl;

import org.junit.Test;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.ValidationIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.SCALAR;

public class RuleDefinitionTest extends ValidationTestBase {

  @Test
  public void matches_if_delegate_matches() {
    RuleDefinition validation = new RuleDefinition(FAKE_RULE);
    validation.setValidation(new IntegerValidation());

    validation.visit(parseText("42"), context);
    assertThat(context.captured()).isEmpty();

    context.capture();
    validation.visit(parseText("some string"), context);
    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: INTEGER, got: \"some string\"");
  }

  @Test
  public void reuse_delegate_result_with_original_type_if_is_skipped() {
    RuleDefinition validation = new RuleDefinition(FAKE_RULE);
    validation.setValidation(new IntegerValidation());
    validation.skip();

    JsonNode node = parseText("42");
    validation.visit(node, context);

    assertThat(node.getType()).isEqualTo(SCALAR);
    assertThat(node).isInstanceOf(ScalarNode.class);
    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void reuse_delegate_and_overrides_type_if_not_skipped() {
    RuleDefinition validation = new RuleDefinition(FAKE_RULE);
    validation.setValidation(new IntegerValidation());

    JsonNode node = parseText("42");
    validation.visit(node, context);

    assertThat(node.getType()).isEqualTo(FAKE_RULE);
    assertThat(node).isInstanceOf(ScalarNode.class);
    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void reuse_delegate_and_overrides_type_if_skipped_and_overridden() {
    RuleDefinition validation = new RuleDefinition(FAKE_RULE);
    validation.skip();
    validation.setValidation(new IntegerValidation());

    JsonNode node = parseText("42");
    validation.visit(node, context);

    assertThat(node.getType()).isEqualTo(SCALAR);
    assertThat(node).isInstanceOf(ScalarNode.class);
    assertThat(context.captured()).isEmpty();
  }
}
