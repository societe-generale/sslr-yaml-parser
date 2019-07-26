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

import com.sonar.sslr.api.AstNode;
import org.junit.Test;
import org.sonar.sslr.yaml.grammar.impl.ArrayNode;
import org.sonar.sslr.yaml.grammar.impl.ObjectNode;
import org.sonar.sslr.yaml.grammar.impl.PropertyNode;
import org.sonar.sslr.yaml.grammar.impl.ScalarNode;
import org.sonar.sslr.yaml.grammar.impl.SyntaxNode;
import org.sonar.sslr.yaml.grammar.impl.ValidationTestBase;

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.BLOCK_PROPERTY;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.FLOW_ARRAY_ELEMENT;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.SCALAR;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_END;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_MAPPING_START;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.FLOW_ENTRY;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.FLOW_SEQUENCE_END;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.FLOW_SEQUENCE_START;

public class YamlParserTest extends ValidationTestBase {

  @Test
  public void generates_object_structure() {
    JsonNode jsonNode = parseText("p1: v1");

    assertThat(jsonNode).isInstanceOf(ObjectNode.class);
    assertThat(jsonNode.getChildren()).extracting(AstNode::getClass, AstNode::getType).containsExactly(
        tuple(SyntaxNode.class, BLOCK_MAPPING_START),
        tuple(PropertyNode.class, BLOCK_PROPERTY),
        tuple(SyntaxNode.class, BLOCK_END),
        tuple(SyntaxNode.class, EOF)
    );
    JsonNode valueNode = jsonNode.at("/p1");
    assertThat(valueNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.propertyMap()).containsOnlyKeys("p1");
    assertThat(jsonNode.properties()).extracting(JsonNode::stringValue).containsOnly("v1");
  }

  @Test
  public void generates_array_structure() {
    JsonNode jsonNode = parseText("[ v1, v2 ]");

    assertThat(jsonNode).isInstanceOf(ArrayNode.class);
    assertThat(jsonNode.getChildren()).extracting(AstNode::getClass, AstNode::getType).containsExactly(
        tuple(SyntaxNode.class, FLOW_SEQUENCE_START),
        tuple(SyntaxNode.class, FLOW_ARRAY_ELEMENT),
        tuple(SyntaxNode.class, FLOW_ENTRY),
        tuple(SyntaxNode.class, FLOW_ARRAY_ELEMENT),
        tuple(SyntaxNode.class, FLOW_SEQUENCE_END),
        tuple(SyntaxNode.class, EOF)
    );
    JsonNode valueNode = jsonNode.elements().get(0);
    assertThat(valueNode).isInstanceOf(ScalarNode.class);
    assertThat(valueNode.stringValue()).isEqualTo("v1");
    valueNode = jsonNode.elements().get(1);
    assertThat(valueNode).isInstanceOf(ScalarNode.class);
    assertThat(valueNode.stringValue()).isEqualTo("v2");
  }


  @Test
  public void generates_integer_scalar_node() {
    JsonNode jsonNode = parseText("42");

    assertThat(jsonNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.getType()).isEqualTo(SCALAR);
    assertThat(jsonNode.intValue()).isEqualTo(42);
    assertThat(jsonNode.floatValue()).isEqualTo(42.0);
  }

  @Test
  public void generates_float_scalar_node() {
    JsonNode jsonNode = parseText("42.0");

    assertThat(jsonNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.getType()).isEqualTo(SCALAR);
    assertThat(jsonNode.floatValue()).isEqualTo(42.0);
  }

  @Test
  public void generates_boolean_scalar_node() {
    JsonNode jsonNode = parseText("y");

    assertThat(jsonNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.getType()).isEqualTo(SCALAR);
    assertThat(jsonNode.booleanValue()).isEqualTo(true);
  }

  @Test
  public void generates_null_scalar_node() {
    JsonNode jsonNode = parseText("null");

    assertThat(jsonNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.getType()).isEqualTo(SCALAR);
    assertThat(jsonNode.isNull()).isTrue();
  }
}