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
package org.sonar.sslr.yaml.grammar.typed.proxy;

import com.google.common.collect.Lists;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.GrammarRuleBuilder;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.PropertyDescription;
import org.sonar.sslr.yaml.grammar.ValidationRule;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
import org.sonar.sslr.yaml.grammar.impl.FirstOfValidation;
import org.sonar.sslr.yaml.grammar.impl.RuleDefinition;
import org.sonar.sslr.yaml.grammar.typed.parser.TypeVisitor;

public class ProxyFactoryGenerator implements YamlGrammarBuilder, TypeVisitor.Context {
  private final Deque<ProxyFactory> currentStack = new ArrayDeque<>();
  private final Deque<String> methodStack = new ArrayDeque<>();
  private final Deque<Class[]> typeStack = new ArrayDeque<>();
  private final Map<GrammarRuleKey, ProxyFactory> rules = new HashMap<>();
  private final YamlGrammarBuilder delegate;
  private TypeVisitor.Context contextDelegate;
  private ProxyFactory rootFactory;

  public ProxyFactoryGenerator(YamlGrammarBuilder delegate) {
    this.delegate = delegate;
  }

  public void setContextDelegate(TypeVisitor.Context contextDelegate) {
    this.contextDelegate = contextDelegate;
  }

  public ProxyFactory getRootFactory() {
    return rootFactory;
  }

  private ProxyFactory popOrDelegate(Object rule) {
    if (rule instanceof GrammarRuleKey) {
      return new DeferredProxyFactory(rules, (GrammarRuleKey) rule);
    } else if (rule instanceof String) {
      return new StringProxyFactory();
    } else {
      return currentStack.pop();
    }
  }

  @Override
  public ValidationRule object(PropertyDescription first, PropertyDescription second, PropertyDescription... rest) {
    Class[] pop = typeStack.pop();
    if (pop[0] == Map.class) {
      throw new IllegalStateException("Should not be proxying a map with several properties!");
    }
    ObjectProxyFactory factory = new ObjectProxyFactory(pop);
    factory.addProperty(asPointer(popOrDelegate(first)));
    factory.addProperty(asPointer(popOrDelegate(second)));
    Arrays.stream(rest).forEach(p -> factory.addProperty(asPointer(popOrDelegate(rest))));
    currentStack.push(factory);
    return delegate.object(first, second, rest);
  }

  private static PropertyPointer asPointer(ProxyFactory factory) {
    if (!(factory instanceof PropertyPointer)) {
      throw new IllegalStateException("Expected a property pointer, got " + factory.getClass());
    }
    return (PropertyPointer) factory;
  }

  @Override
  public ValidationRule object(PropertyDescription rule) {
    Class[] pop = typeStack.pop();
    if (pop[0] == Map.class) {
      return delegate.object(rule);
    }
    ObjectProxyFactory factory = new ObjectProxyFactory(pop);
    factory.addProperty(asPointer(popOrDelegate(rule)));
    currentStack.push(factory);
    return delegate.object(rule);
  }

  @Override
  public ValidationRule anyObject() {
    return object(patternProperty(".*", anything()));
  }

  @Override
  public ValidationRule anything() {
    currentStack.push(new IdentityProxyFactory());
    return delegate.anything();
  }

  @Override
  public ValidationRule array(Object rule) {
    Class elementType = typeStack.pop()[0];
    ProxyFactory elementFactory = popOrDelegate(rule);
    if (elementType.isArray()) {
      currentStack.push(new ArrayProxyFactory(elementType, elementFactory));
    } else {
      currentStack.push(new ListProxyFactory(elementFactory));
    }
    return delegate.array(rule);
  }

  @Override
  public ValidationRule anyArray() {
    currentStack.push(new ArrayProxyFactory(JsonNode.class, new IdentityProxyFactory()));
    return delegate.anyArray();
  }

  @Override
  public PropertyDescription property(String key, Object rule) {
    currentStack.push(new PropertyInterceptor(methodStack.pop(), key, popOrDelegate(rule)));
    return delegate.property(key, rule);
  }

  @Override
  public PropertyDescription mandatoryProperty(String key, Object rule) {
    currentStack.push(new PropertyInterceptor(methodStack.pop(), key, popOrDelegate(rule)));
    return delegate.mandatoryProperty(key, rule);
  }

  @Override
  public PropertyDescription patternProperty(String pattern, Object rule) {
    currentStack.push(new PatternPropertyInterceptor(methodStack.pop(), pattern, popOrDelegate(rule)));
    return delegate.patternProperty(pattern, rule);
  }

  @Override
  public PropertyDescription discriminant(String key, Object rule) {
    currentStack.push(new PropertyInterceptor(methodStack.pop(), key, popOrDelegate(rule)));
    return delegate.discriminant(key, rule);
  }

  @Override
  public ValidationRule firstOf(Object first, Object second) {
    FirstOfValidation validationRule = (FirstOfValidation) delegate.firstOf(first, second);
    List<ProxyFactory> factories = Lists.newArrayList(popOrDelegate(first), popOrDelegate(second));
    generateFirstOfProxyFactory(validationRule, factories);
    return validationRule;
  }

  @Override
  public ValidationRule firstOf(Object first, Object second, Object... rest) {
    List<ProxyFactory> factories = Lists.newArrayList(popOrDelegate(first), popOrDelegate(second));
    Arrays.stream(rest).forEach(o -> factories.add(this.popOrDelegate(o)));
    FirstOfValidation validationRule = (FirstOfValidation) delegate.firstOf(first, second, rest);
    generateFirstOfProxyFactory(validationRule, factories);
    return validationRule;
  }

  private void generateFirstOfProxyFactory(FirstOfValidation validationRule, List<ProxyFactory> factories) {
    Class[] peeked = typeStack.peek();
    if (peeked != null && peeked[0].isEnum()) {
      currentStack.push(new EnumProxyFactory(peeked[0]));
    } else {
      ChoiceProxyFactory factory = new ChoiceProxyFactory(factories);
      currentStack.push(factory);
      validationRule.setObserver(factory);
    }
  }

  @Override
  public GrammarRuleBuilder rule(GrammarRuleKey ruleKey) {
    final GrammarRuleBuilder delegateBuilder = delegate.rule(ruleKey);
    return new GrammarRuleBuilder() {
      @Override
      public GrammarRuleBuilder is(Object e) {
        ProxyFactory pop = popOrDelegate(e);
        rules.put(ruleKey, pop);
        delegateBuilder.is(e);
        return this;
      }

      @Override
      public void skip() {
        delegateBuilder.skip();
      }
    };
  }

  @Override
  public ValidationRule scalar() {
    currentStack.push(new ScalarProxyFactory());
    return delegate.scalar();
  }

  @Override
  public ValidationRule string() {
    currentStack.push(new StringProxyFactory());
    return delegate.string();
  }

  @Override
  public Object integer() {
    currentStack.push(new IntegerProxyFactory());
    return delegate.integer();
  }

  @Override
  public Object bool() {
    currentStack.push(new BooleanProxyFactory());
    return delegate.bool();
  }

  @Override
  public Object bool(boolean value) {
    bool();
    return delegate.bool(value);
  }

  @Override
  public Object floating() {
    currentStack.push(new FloatProxyFactory());
    return delegate.floating();
  }

  @Override
  public RuleDefinition build() {
    RuleDefinition definition = delegate.build();
    this.rootFactory = rules.get(definition.getRuleKey());
    return definition;
  }

  @Override
  public GrammarRuleKey makeTypeKey(Type type) {
    return contextDelegate.makeTypeKey(type);
  }

  @Override
  public boolean add(GrammarRuleKey ruleKey) {
    return contextDelegate.add(ruleKey);
  }

  @Override
  public void declareMethod(String name) {
    methodStack.push(name);
  }

  @Override
  public void declareTypes(Class type) {
    Class[] implementedInterfaces = type.getInterfaces();
    Class[] interfaces = new Class[1+implementedInterfaces.length];
    System.arraycopy(implementedInterfaces, 0, interfaces, 1, implementedInterfaces.length);
    interfaces[0] = type;

    typeStack.push(interfaces);
  }
}
