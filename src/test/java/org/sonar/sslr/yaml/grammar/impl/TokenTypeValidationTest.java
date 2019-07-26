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
import org.sonar.sslr.yaml.grammar.ValidationIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.FLOAT;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.NULL;

public class TokenTypeValidationTest extends ValidationTestBase {

  @Test
  public void matches_correct_token_type() {
    TokenTypeValidation validation = new TokenTypeValidation(NULL);
    validation.visit(parseText("null"), context);
  }

  @Test
  public void fails_on_wrong_token_type() {
    TokenTypeValidation validation = new TokenTypeValidation(FLOAT);

    validation.visit(parseText("some string"), context);

    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: FLOAT, got: SCALAR");
  }

  @Test
  public void fails_on_non_scalar() {
    TokenTypeValidation validation = new TokenTypeValidation(FLOAT);

    validation.visit(parseText("p1: v1"), context);

    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: FLOAT, got: BLOCK_MAPPING");
  }
}
