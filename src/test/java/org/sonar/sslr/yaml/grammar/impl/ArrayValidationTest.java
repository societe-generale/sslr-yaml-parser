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
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.STRING;

public class ArrayValidationTest extends ValidationTestBase {

  @Test
  public void matches_element_format() {
    ArrayValidation validation = new ArrayValidation(new AlwaysTrueValidation());

    JsonNode rootNode = parseText("[ v1, v2 ]");
    validation.visit(rootNode, context);

    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void fails_if_element_validation_fails() {
    ArrayValidation validation = new ArrayValidation(new TokenTypeValidation(STRING));
    JsonNode rootNode = parseText("[ v1, 1.0 ]");

    validation.visit(rootNode, context);

    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: STRING, got: SCALAR");
  }

  @Test
  public void matches_empty_arrays() {
    ArrayValidation validation = new ArrayValidation(new TokenTypeValidation(STRING));

    JsonNode rootNode = parseText("[ ]");
    validation.visit(rootNode, context);

    assertThat(context.captured()).isEmpty();
  }
}
