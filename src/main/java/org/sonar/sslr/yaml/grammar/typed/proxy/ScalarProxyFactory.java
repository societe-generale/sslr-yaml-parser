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
import org.sonar.sslr.yaml.snakeyaml.parser.Tokens;

import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.FLOAT;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.INTEGER;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.STRING;

public class ScalarProxyFactory implements ProxyFactory {
  @Override
  public Object makeProxyFor(JsonNode node) {
    if (node.is(STRING)) {
      return node.stringValue();
    } else if (node.is(FLOAT)) {
      return node.floatValue();
    } else if (node.is(INTEGER)) {
      return node.intValue();
    } else if (node.is(Tokens.TRUE)) {
      return true;
    } else if (node.is(Tokens.FALSE)) {
      return false;
    } else {
      return null;
    }
  }
}
