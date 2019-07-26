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
import com.sonar.sslr.impl.ast.AlwaysSkipFromAst;
import com.sonar.sslr.impl.ast.NeverSkipFromAst;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.ValidationRule;

public class RuleDefinition implements GrammarRuleKey, ValidationRule {

  private final GrammarRuleKey ruleKey;
  private final String name;
  private ValidationRule validation;
  private AstNodeType astNodeSkippingPolicy = NeverSkipFromAst.INSTANCE;

  public RuleDefinition(GrammarRuleKey ruleKey) {
    this.ruleKey = ruleKey;
    this.name = ruleKey.toString();
  }

  public String getName() {
    return name;
  }

  public void skip() {
    astNodeSkippingPolicy = AlwaysSkipFromAst.INSTANCE;
  }

  public GrammarRuleKey getRuleKey() {
    return ruleKey;
  }

  public ValidationRule getValidation() {
    return validation;
  }

  public void setValidation(ValidationRule validation) {
    this.validation= validation;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public boolean visit(JsonNode node, Context context) {
    boolean valid = validation.visit(node, context);
    if (valid && astNodeSkippingPolicy == NeverSkipFromAst.INSTANCE) {
      node.decorate(ruleKey);
    }
    return valid;
  }
}
