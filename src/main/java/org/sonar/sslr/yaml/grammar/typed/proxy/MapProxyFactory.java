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

public class MapProxyFactory implements ProxyFactory {
  private final String pattern;
  private final ProxyFactory elementFactory;

  public MapProxyFactory(String pattern, ProxyFactory elementFactory) {
    this.pattern = pattern;
    this.elementFactory = elementFactory;
  }

  @Override
  public Object makeProxyFor(JsonNode node) {
    return new MapProxy(node, pattern, elementFactory);
  }
}
