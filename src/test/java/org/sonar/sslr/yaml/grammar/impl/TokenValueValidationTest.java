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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.ValidationIssue;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenValueValidationTest extends ValidationTestBase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void matches_any_scalar() {
    TokenValueValidation textual = new TokenValueValidation("some string");
    textual.visit(parseText("some string"), context);

    TokenValueValidation numeric = new TokenValueValidation("42.036");
    numeric.visit(parseText("42.036"), context);

    TokenValueValidation bool = new TokenValueValidation("yes");
    bool.visit(parseText("yes"), context);
  }

  @Test
  public void fails_on_wrong_value() {
    TokenValueValidation validation = new TokenValueValidation("some string");
    JsonNode node = parseText("wrong string");

    validation.visit(node, context);

    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: \"some string\", got: \"wrong string\"");
  }

  @Test
  public void fails_on_non_scalar() {
    TokenValueValidation validation = new TokenValueValidation("some string");
    JsonNode node = parseText("p1: v1");

    validation.visit(node, context);

    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: \"some string\", got: \"{\"");
  }
}
