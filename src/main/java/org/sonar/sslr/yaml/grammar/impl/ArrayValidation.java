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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.ValidationRule;
import org.sonar.sslr.yaml.grammar.YamlGrammar;

import static org.sonar.sslr.yaml.grammar.YamlGrammar.BLOCK_SEQUENCE;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.FLOW_SEQUENCE;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.INDENTLESS_SEQUENCE;

public class ArrayValidation extends ValidationBase {
  private final ValidationRule elementValidation;

  public ArrayValidation(ValidationRule elementValidation) {
    this.elementValidation = elementValidation;
  }

  @Override
  protected boolean validate(JsonNode node, Context context) {
    AstNodeType type = node.getType();
    if (type != FLOW_SEQUENCE && type != BLOCK_SEQUENCE && type != INDENTLESS_SEQUENCE) {
      context.recordFailure(node, "Expected array, got: " + node.getType());
      return false;
    }
    boolean valid = true;
    for (AstNode child : node.getChildren(YamlGrammar.FLOW_ARRAY_ELEMENT, YamlGrammar.BLOCK_ARRAY_ELEMENT)) {
      valid &= elementValidation.visit((JsonNode)child.getFirstChild(), context);
    }
    return valid;
  }

  @Override
  public String toString() {
    return "array of <" + elementValidation.toString() + ">";
  }

  @Override
  public String describe() {
    return "array of <" + (elementValidation instanceof RuleDefinition ? ((RuleDefinition) elementValidation).getRuleKey() : elementValidation.describe()) + ">";
  }
}
