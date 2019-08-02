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
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Test;
import org.sonar.sslr.grammar.GrammarRuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.BLOCK_PROPERTY;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.SCALAR;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.KEY;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.VALUE;

public class JsonNodeTest {
  private static final GrammarRuleKey ROOT = new GrammarRuleKey() {
  };

  @Test
  public void can_get_self() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anything());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "some scalar");

    JsonNode at = node.at("");

    assertSame(node, at);
  }

  @Test
  public void can_get_property() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: some scalar");

    JsonNode at = node.at("/p1");

    assertEquals(SCALAR, at.getType());
    assertEquals("p1", at.key().getTokenValue());
    assertEquals("some scalar", at.getTokenValue());
  }

  @Test
  public void can_get_nested_property() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1:\n  p2: some scalar");

    JsonNode at = node.at("/p1/p2");

    assertEquals(SCALAR, at.getType());
    assertEquals("p2", at.key().getTokenValue());
    assertEquals("some scalar", at.getTokenValue());
  }

  @Test
  public void can_get_array_element() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyArray());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "[ 1, 2, 3 ]");

    JsonNode at = node.at("/1");

    assertEquals(SCALAR, at.getType());
    assertEquals("2", at.getTokenValue());
  }

  @Test
  public void can_get_nested_array_element() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: [ 1, 2, 3 ]");

    JsonNode at = node.at("/p1/1");

    assertEquals(SCALAR, at.getType());
    assertEquals("2", at.getTokenValue());
  }

  @Test
  public void can_get_key_from_value() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: some scalar");

    JsonNode key = node.at("/p1").key();

    assertEquals(SCALAR, key.getType());
    assertEquals("p1", key.getTokenValue());
  }

  @Test
  public void can_get_value_from_key() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: some scalar");

    JsonNode key = (JsonNode)node.getFirstDescendant(KEY).getNextSibling();
    JsonNode value = key.value();

    assertEquals(SCALAR, value.getType());
    assertEquals("some scalar", value.getTokenValue());
  }

  @Test
  public void can_get_value_from_property() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: some scalar");

    JsonNode property = (JsonNode)node.getFirstDescendant(BLOCK_PROPERTY);
    JsonNode value = property.value();

    assertEquals(SCALAR, value.getType());
    assertEquals("some scalar", value.getTokenValue());
  }

  @Test
  public void can_get_key_from_property() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: some scalar");

    JsonNode property = (JsonNode)node.getFirstDescendant(BLOCK_PROPERTY);
    JsonNode key = property.key();

    assertEquals(SCALAR, key.getType());
    assertEquals("p1", key.getTokenValue());
  }

  @Test
  public void can_get_key_from_key() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: some scalar");

    JsonNode n = (JsonNode)node.getFirstDescendant(KEY).getNextSibling();
    JsonNode key = n.key();

    assertEquals(SCALAR, key.getType());
    assertEquals("p1", key.getTokenValue());
  }

  @Test
  public void can_get_value_from_value() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: some scalar");

    JsonNode n = (JsonNode)node.getFirstDescendant(VALUE).getNextSibling();
    JsonNode value = n.value();

    assertEquals(SCALAR, value.getType());
    assertEquals("some scalar", value.getTokenValue());
  }

  @Test
  public void can_get_all_properties() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: a\np2: b\np3: c");

    Collection<JsonNode> properties = node.properties();

    assertThat(properties).extracting(n -> n.key().getTokenValue()).containsExactly("p1", "p2", "p3");
  }

  @Test
  public void can_get_all_properties_by_map() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: a\np2: b\np3: c");

    Map<String, JsonNode> properties = node.propertyMap();

    assertThat(properties.entrySet())
        .extracting(Map.Entry::getKey, e -> e.getValue().getTokenValue())
        .containsExactlyInAnyOrder(
            tuple("p1", "a"),
            tuple("p2", "b"),
            tuple("p3", "c"));
  }

  @Test
  public void can_transform_all_properties() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "p1: a\np2: b\np3: c");

    Map<String, String> properties = node.propertyMap(AstNode::getTokenValue);

    assertThat(properties.entrySet())
        .extracting(Map.Entry::getKey, Map.Entry::getValue)
        .containsExactlyInAnyOrder(
            tuple("p1", "a"),
            tuple("p2", "b"),
            tuple("p3", "c"));
  }
  @Test
  public void can_get_all_items() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyArray());
    b.setRootRule(ROOT);
    JsonNode node = parseText(b, "[ a, b, c ]");

    Collection<JsonNode> properties = node.elements();

    assertThat(properties).extracting(JsonNode::getTokenValue).containsExactly("a", "b", "c");
  }

  @Test
  public void can_get_items_in_inline_array() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.setRootRule(ROOT);
    b.rule(ROOT).is(b.object(
        b.mandatoryProperty("p1", b.array(b.string()))));
    JsonNode node = parseText(b, "p1: [ a, b, c ]");

    List<JsonNode> properties = node.at("/p1").elements();

    assertThat(properties).extracting(JsonNode::getTokenValue).containsExactly("a", "b", "c");
  }

  @Test
  public void can_get_element_in_inline_array() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.setRootRule(ROOT);
    b.rule(ROOT).is(b.object(
        b.mandatoryProperty("p1", b.array(b.string()))));
    JsonNode node = parseText(b, "p1: [ a, b, c ]");

    JsonNode element = node.at("/p1/1");

    assertEquals("b", element.getTokenValue());
  }

  @Test
  public void can_get_properties_in_inline_object() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.setRootRule(ROOT);
    b.rule(ROOT).is(b.object(
        b.mandatoryProperty("p1", b.object(b.patternProperty(".*", b.string())))));
    JsonNode node = parseText(b, "p1:\n  p2: a\n  p3: b");

    Collection<JsonNode> properties = node.at("/p1").properties();

    assertThat(properties).extracting(n -> n.key().getTokenValue()).containsExactly("p2", "p3");
  }

  @Test
  public void can_get_property_in_inline_object() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.setRootRule(ROOT);
    b.rule(ROOT).is(b.object(
        b.mandatoryProperty("p1", b.object(b.patternProperty(".*", b.string())))));
    JsonNode node = parseText(b, "p1:\n  p2: a\n  p3: b");

    JsonNode element = node.at("/p1/p2");

    assertEquals("a", element.getTokenValue());
  }

  @Test
  public void can_get_properties_as_map() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);

    JsonNode node = parseText(b, "p1:  a\np2: b");

    assertThat(node.propertyMap()).hasEntrySatisfying("p1", scalar("a"));
    assertThat(node.propertyMap()).hasEntrySatisfying("p2", scalar("b"));

  }

  @Test
  public void can_resolve_references() {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(ROOT).is(b.anyObject());
    b.setRootRule(ROOT);

    JsonNode node = parseText(b, "p1: v1\np2:\n  $ref: '#/p1'");
    JsonNode refNode = node.at("/p2");
    assertThat(refNode.isRef()).isTrue();
    assertThat(refNode.resolve()).isSameAs(node.at("/p1"));

  }

  private static Consumer<JsonNode> scalar(String value) {
    return n -> assertEquals(value, n.getTokenValue(), value);
  }

  private JsonNode parseText(DefaultGrammarBuilder b, String s) {
    YamlParser parser = YamlParser.builder().withCharset(Charset.forName("UTF-8")).withGrammar(b).build();
    return parser.parse(s);
  }

}