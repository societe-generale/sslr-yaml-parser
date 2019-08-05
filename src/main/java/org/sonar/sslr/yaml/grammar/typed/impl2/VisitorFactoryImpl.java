/*
 * Sonar SSLR :: YAML Parser
 * Copyright (C) 2018-2018 Societe Generale
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
package org.sonar.sslr.yaml.grammar.typed.impl2;

import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
import org.sonar.sslr.yaml.grammar.typed.TypeVisitor;
import org.sonar.sslr.yaml.grammar.typed.VisitorFactory;
import org.sonar.sslr.yaml.grammar.typed.proxy.ProxyFactoryGenerator;

public class VisitorFactoryImpl implements VisitorFactory {
  private final YamlGrammarBuilder builder;
  private final TypeVisitor.Context context;
  private TypeDispatcher dispatcher;

  public VisitorFactoryImpl(ProxyFactoryGenerator builder) {
    this.builder = builder;
    this.context = builder;
    builder.setContextDelegate(new VisitorContext());
  }

  @Override
  public TypeVisitor object() {
    return new ObjectVisitor(builder, dispatcher, context);
  }

  @Override
  public TypeVisitor scalar() {
    return new ScalarVisitor(builder);
  }

  @Override
  public TypeVisitor map() {
    return new MapVisitor(builder, dispatcher, context);
  }

  @Override
  public TypeVisitor list() {
    return new ListVisitor(builder, dispatcher, context);
  }

  @Override
  public TypeVisitor choice() {
    return new ChoiceVisitor(builder, dispatcher, context);
  }

  @Override
  public TypeVisitor array() {
    return null;
  }

  @Override
  public TypeVisitor resolvable() {
    return new ResolvableVisitor(builder, context);
  }

  public void setDispatcher(TypeDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }
}
