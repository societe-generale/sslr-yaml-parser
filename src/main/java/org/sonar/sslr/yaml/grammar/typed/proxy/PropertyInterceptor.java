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

import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.PropertyDescription;
import org.sonar.sslr.yaml.snakeyaml.parser.Tokens;

class PropertyInterceptor implements ProxyFactory, PropertyPointer {
  private final String methodName;
  private final String propertyName;
  private final ProxyFactory factory;

  PropertyInterceptor(String methodName, String propertyName, ProxyFactory factory) {
    this.methodName = methodName;
    this.propertyName = propertyName;
    this.factory = factory;
  }

  @Override
  public Object makeProxyFor(JsonNode parent) {
    JsonNode node = parent.get(propertyName);
    if (node.isMissing() || node.is(Tokens.NULL)) {
      return null;
    }
    return factory.makeProxyFor(node);
  }

  @Override
  public String getMethodName() {
    return methodName;
  }
}
