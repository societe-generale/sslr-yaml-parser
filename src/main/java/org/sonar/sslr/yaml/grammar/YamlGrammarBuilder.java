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

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.impl.RuleDefinition;

/**
 * A builder for creating <a href="http://en.wikipedia.org/wiki/Parsing_expression_grammar">Parsing Expression Grammars</a>
 * for YAML-based grammars. Use it in combination with a {@link YamlParser} to parse and validate YAML documents.
 * <p>
 * Objects of following types can be used as an atomic parsing expressions:
 * <ul>
 * <li>GrammarRuleKey</li>
 * <li>TokenType</li>
 * <li>String</li>
 * </ul>
 */
public interface YamlGrammarBuilder {
  /**
   * Matches an object whose properties match the provided {@link #property(String, Object)} sub-expressions.
   * During the execution of this expression on an object, parser will try to match every property with one of the
   * provided sub-expressions, ensuring that no property is defined in double, that all {@link #mandatoryProperty(String, Object)}
   * and {@link #discriminant(String, Object)} are supplied.
   *
   * @param first first sub-expression
   * @param second second sub-expression
   * @param rest rest of sub-expressions
   * @return the built rule
   */
  ValidationRule object(PropertyDescription first, PropertyDescription second, PropertyDescription... rest);

  /**
   * Matches an object. For more details, see {@link #object(PropertyDescription, PropertyDescription, PropertyDescription...)}.
   * @param rule the description of the properties to match
   * @return the built rule
   */
  ValidationRule object(PropertyDescription rule);

  /**
   * Matches any YAML object. Equivalent for {@code object(patternProperty(".*", anything()))}.
   * @return the built rule
   */
  ValidationRule anyObject();

  /**
   * Matches any valid YAML content. Equivalent for {@code firstOf(scalar(), anyArray(), anyObject())}.
   * @return the built rule
   */
  ValidationRule anything();

  /**
   * Creates a parsing expression - "array".
   * During the execution of this expression, parser will apply the sub-expression to all entries of the array. This
   * expression succeeds only if the sub-expression succeeds on all entries of the array.
   *
   * @param rule the sub-expression
   * @return the built rule
   */
  ValidationRule array(Object rule);

  /**
   * Creates a parsing expression - "any array".
   * This expression matches any array, whatever the type of objects or the number of entries. Equivalent of
   * {@code array(anything())}.
   * @return the built rule
   */
  ValidationRule anyArray();

  /**
   * Describes an optional property that can appear in an {@link #object(PropertyDescription)}.
   * @param key the key of the property
   * @param rule the type of the property (can be any valid rule)
   * @return the built rule
   */
  PropertyDescription property(String key, Object rule);

  /**
   * Describes a mandatory property that can appear in an {@link #object(PropertyDescription)}.
   * @param key the key of the property
   * @param rule the type of the property (can be any valid rule)
   * @return the built rule
   */
  PropertyDescription mandatoryProperty(String key, Object rule);

  /**
   * Describes a rule to match properties by name in an {@link #object(PropertyDescription)}. When an object's property
   * key doesn't match a named property, it is evaluated against any declared pattern property, in the order in which
   * they have been declared (so order is important).
   *
   * Be aware that in {@code object(patternProperty(".*", anything()), patternProperty("^x-.*", anything())}, the second sub-expression will
   * never be executed.
   *
   * @param pattern the pattern to match against the property names
   * @param rule the sub-expression
   * @return the built rule
   */
  PropertyDescription patternProperty(String pattern, Object rule);

  /**
   * Describes a mandatory property that can appear in an {@link #object(PropertyDescription)}, and whose value is a
   * discriminant to identify the type of objects.
   * The object will be recognised only if the field exists and the validation rule is matched.
   *
   * @param key the key of the property
   * @param rule the type of the property (can be any valid rule)
   * @return the built rule
   */
  PropertyDescription discriminant(String key, Object rule);

  /**
   * Creates parsing expression - "first of".
   * During the execution of this expression parser execute sub-expressions in order until one succeeds.
   * This expressions succeeds if any sub-expression succeeds.
   * <p>
   * Be aware that in expression {@code firstOf("foo", firstOf("foo", "bar"))} second sub-expression will never be executed.
   *
   * @param first  first sub-expression
   * @param second  second sub-expression
   * @throws IllegalArgumentException if any of given arguments is not a parsing expression
   * @return the built rule
   */
  ValidationRule firstOf(Object first, Object second);

  /**
   * Creates parsing expression - "first of".
   * See {@link #firstOf(Object, Object)} for more details.
   *
   * @param first  first sub-expression
   * @param second  second sub-expression
   * @param rest  rest of sub-expressions
   * @throws IllegalArgumentException if any of given arguments is not a parsing expression
   * @return the built rule
   */
  ValidationRule firstOf(Object first, Object second, Object... rest);

  /**
   * Allows to describe rule.
   * Result of this method should be used only for execution of methods in it, i.e. you should not save reference on it.
   * No guarantee that this method always returns the same instance for the same key of rule.
   * @param ruleKey the key that will be used to refer to this rule in other rules
   * @return the builder, for continuation
   */
  GrammarRuleBuilder rule(GrammarRuleKey ruleKey);

  /**
   * Allows to specify that given rule should be root for grammar.
   * @param rootRuleKey a key that needs to be declared with {@link #rule(GrammarRuleKey)} before building
   */
  /**
   * Matches any scalar.
   * @return the built rule
   */
  ValidationRule scalar();

  /**
   * Matches any string scalar.
   * @return the built rule
   */
  ValidationRule string();

  /**
   * Matches an integer scalar.
   * @return the built rule
   */
  Object integer();

  /**
   * Matches a boolean scalar.
   * @return the built rule
   */
  Object bool();

  /**
   * Matches a boolean scalar with a specific value.
   * @param value the exact boolean to match
   * @return the built rule
   */
  Object bool(boolean value);

  /**
   * Matches a floating-point scalar.
   * @return the built rule
   */
  Object floating();

  RuleDefinition build();
}
