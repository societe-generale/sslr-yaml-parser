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
package org.sonar.sslr.yaml.grammar;

import com.sonar.sslr.api.TokenType;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import org.sonar.sslr.grammar.GrammarException;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.impl.AlwaysTrueValidation;
import org.sonar.sslr.yaml.grammar.impl.ArrayValidation;
import org.sonar.sslr.yaml.grammar.impl.BooleanValidation;
import org.sonar.sslr.yaml.grammar.impl.FirstOfValidation;
import org.sonar.sslr.yaml.grammar.impl.FloatValidation;
import org.sonar.sslr.yaml.grammar.impl.IntegerValidation;
import org.sonar.sslr.yaml.grammar.impl.NodeTypeValidation;
import org.sonar.sslr.yaml.grammar.impl.ObjectValidation;
import org.sonar.sslr.yaml.grammar.impl.PropertyDescriptionImpl;
import org.sonar.sslr.yaml.grammar.impl.RuleDefinition;
import org.sonar.sslr.yaml.grammar.impl.TokenTypeValidation;
import org.sonar.sslr.yaml.grammar.impl.TokenValueValidation;
import org.sonar.sslr.yaml.snakeyaml.parser.Tokens;

import static org.sonar.sslr.yaml.grammar.YamlGrammar.SCALAR;

/**
 * Implementation of YamlGrammarBuilder for grammars built from DSL.
 */
public class DefaultGrammarBuilder implements YamlGrammarBuilder {
  private final Map<GrammarRuleKey, RuleDefinition> definitions = new LinkedHashMap<>();
  private GrammarRuleKey rootRuleKey;

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule object(PropertyDescription first, PropertyDescription second, PropertyDescription... rest) {
    ObjectValidation masterRule = new ObjectValidation();
    masterRule.addProperty(first);
    masterRule.addProperty(second);
    for (PropertyDescription otherRule : rest) {
      masterRule.addProperty(otherRule);
    }
    return masterRule;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule object(PropertyDescription rule) {
    ObjectValidation masterRule = new ObjectValidation();
    masterRule.addProperty(rule);
    return masterRule;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule anyObject() {
    ObjectValidation validation = new ObjectValidation();
    validation.addProperty(patternProperty(".*", new AlwaysTrueValidation()));
    return validation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule anything() {
    return firstOf(
        SCALAR,
        anyArray(),
        anyObject()
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule array(Object rule) {
    return new ArrayValidation(convertToRule(rule));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule anyArray() {
    return new ArrayValidation(new AlwaysTrueValidation());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PropertyDescription property(String key, Object rule) {
    return new PropertyDescriptionImpl(key, false, false, false, convertToRule(rule));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PropertyDescription mandatoryProperty(String key, Object rule) {
    return new PropertyDescriptionImpl(key, false, true, false, convertToRule(rule));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PropertyDescription patternProperty(String pattern, Object rule) {
    return new PropertyDescriptionImpl(pattern, true, false, false, convertToRule(rule));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PropertyDescription discriminant(String key, Object rule) {
    return new PropertyDescriptionImpl(key, false, true, true, convertToRule(rule));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule firstOf(Object first, Object second) {
    return new FirstOfValidation(convertToRule(first), convertToRule(second));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule firstOf(Object first, Object second, Object... rest) {
    return new FirstOfValidation(convertToRules(first, second, rest));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GrammarRuleBuilder rule(GrammarRuleKey ruleKey) {
    RuleDefinition rule = definitions.computeIfAbsent(ruleKey, RuleDefinition::new);
    return new RuleBuilder(this, rule);
  }

  /**
   * Allows to specify that given rule should be root for grammar. The key must have been declared when {@link #build()}
   * is called.
   * 
   * @param rootRuleKey a rule key
   */
  public void setRootRule(GrammarRuleKey rootRuleKey) {
    this.rootRuleKey = rootRuleKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule scalar() {
    return new NodeTypeValidation(YamlGrammar.SCALAR);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValidationRule string() {
    return new TokenTypeValidation(Tokens.STRING, Tokens.NULL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object integer() {
    return new IntegerValidation();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object bool() {
    return new BooleanValidation(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object bool(boolean value) {
    return new BooleanValidation(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object floating() {
    return new FloatValidation();
  }

  @Override
  public RuleDefinition build() {
    return definitions.get(rootRuleKey);
  }

  public void print(PrintStream s) {
    SortedSet<GrammarRuleKey> keys = new TreeSet<>(Comparator.comparing(Object::toString));
    keys.addAll(definitions.keySet());
    for (GrammarRuleKey key : keys) {
      RuleDefinition value = definitions.get(key);
      s.println(key + " ::= " + value.describe());
    }

  }

  private ValidationRule convertToRule(Object e) {
    Objects.requireNonNull(e, "Validation rule can't be null");
    final ValidationRule result;
    if (e instanceof ValidationRule) {
      result = (ValidationRule) e;
    } else if (e instanceof YamlGrammar) {
      result = new NodeTypeValidation((YamlGrammar)e);
    } else if (e instanceof GrammarRuleKey) {
      GrammarRuleKey ruleKey = (GrammarRuleKey) e;
      rule(ruleKey);
      result = definitions.get(ruleKey);
    } else if (e instanceof TokenType) {
      result = new TokenTypeValidation((TokenType) e);
    } else if (e instanceof String) {
      result = new TokenValueValidation((String) e);
    } else {
      throw new IllegalArgumentException("Incorrect type of validation rule: " + e.getClass().toString());
    }
    return result;
  }

  private ValidationRule[] convertToRules(Object e1, Object e2, Object[] rest) {
    ValidationRule[] result = new ValidationRule[2 + rest.length];
    result[0] = convertToRule(e1);
    result[1] = convertToRule(e2);
    for (int i = 0; i < rest.length; i++) {
      result[2 + i] = convertToRule(rest[i]);
    }
    return result;
  }

  private static class RuleBuilder implements GrammarRuleBuilder {

    private final DefaultGrammarBuilder b;
    private final RuleDefinition delegate;

    public RuleBuilder(DefaultGrammarBuilder b, RuleDefinition delegate) {
      this.b = b;
      this.delegate = delegate;
    }

    @Override
    public GrammarRuleBuilder is(Object e) {
      if (delegate.getValidation() != null) {
        throw new GrammarException("The rule '" + delegate.getRuleKey() + "' has already been defined somewhere in the grammar.");
      }
      delegate.setValidation(b.convertToRule(e));
      return this;
    }

    @Override
    public void skip() {
      delegate.skip();
    }
  }
}
