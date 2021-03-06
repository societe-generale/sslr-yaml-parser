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

import com.sonar.sslr.api.Token;
import java.net.URI;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.YamlGrammar;
import org.sonar.sslr.yaml.snakeyaml.parser.Tokens;


public class MissingNode extends JsonNode {
  public static final JsonNode MISSING = new MissingNode();

  public MissingNode() {
    super(YamlGrammar.MISSING, "MISSING", Token.builder().setLine(1).setColumn(1).setURI(URI.create("file://unknown")).setType(Tokens.NULL).setValueAndOriginalValue("null").build());
  }

  @Override
  public boolean isMissing() {
    return true;
  }
}
