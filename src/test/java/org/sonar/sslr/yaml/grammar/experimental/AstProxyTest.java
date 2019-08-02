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
package org.sonar.sslr.yaml.grammar.experimental;

import com.sonar.sslr.api.AstNodeType;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.DefaultGrammarBuilder;
import org.sonar.sslr.yaml.grammar.YamlParser;
import org.sonar.sslr.yaml.grammar.typed.Choice;
import org.sonar.sslr.yaml.grammar.typed.GrammarRule;
import org.sonar.sslr.yaml.grammar.typed.Key;
import org.sonar.sslr.yaml.grammar.typed.Pattern;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;

import static org.assertj.core.api.Assertions.assertThat;


public class AstProxyTest {

  public enum Rules implements GrammarRuleKey {
    ROOT,
    CHILD,
    EXTENDED,
    REF,
  }

  public interface SimpleTypes {
    String stringProperty();
    Integer integerProperty();
    Float floatObjectProperty();
    int intProperty();
    float floatProperty();
  }

  public interface Child extends GrammarRule {
    AstNodeType __TYPE = Rules.CHILD;

    String p1();
  }

  public interface ObjectType extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;

    Child child();
  }

  public interface TypeWithMap extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    Map<String, Child> children();
    Map<String, List<String>> complexMap();
  }

  public interface TypeWithAnnotatedMap extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    @Pattern("x-.*")
    Map<String, Child> extensions();
  }

  public interface ExtendedType extends Child {
    AstNodeType __TYPE = Rules.EXTENDED;
    String p2();
  }

  public interface TypeWithList extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    List<Child> elements();
  }

  public interface TypeWithArray extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    // Cannot be proxied, will throw
    Child[] elements();
  }

  public interface TypeWithSet extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    // Cannot be proxied, will throw
    Set<Child> elements();
  }

  public interface TypeWithPropertyOverride extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    @Key("enum")
    String _enum();
  }

  public interface TypeWithRawJson extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    JsonNode raw();
    List<JsonNode> list();
    Map<String, JsonNode> map();
  }

  public interface TypeWithPolymorphic extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    @Choice(classes={Boolean.class, Child.class})
    Object polymorphic();
    @Choice(classes={Boolean.class, Child.class})
    List<Object> list();
    @Choice(classes={Boolean.class, Child.class})
    Map<String, Object> map();
  }

  public interface ResolvableType extends GrammarRule, Resolvable<ResolvableType> {
    AstNodeType[] __TYPE = {Rules.REF, Rules.CHILD};
    String p1();
  }
  public interface TypeWithResolvable extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    ResolvableType child();
  }

  public enum SomeEnum {
    value1,
    value2
  }
  public interface TypeWithEnum extends GrammarRule {
    AstNodeType __TYPE = Rules.ROOT;
    SomeEnum myEnum();
  }

  @Test
  public void can_proxy_simple_type_methods() {
    JsonNode n = parse("stringProperty: some string\nintegerProperty: 12\nfloatObjectProperty: 12.0\nintProperty: 42\nfloatProperty: 133.0");
    SimpleTypes p = AstProxy.forNode(SimpleTypes.class, n);

    assertThat(p.stringProperty()).isEqualTo("some string");
    assertThat(p.integerProperty()).isEqualTo(12);
    assertThat(p.floatObjectProperty()).isEqualTo(12.0f);
    assertThat(p.intProperty()).isEqualTo(42);
    assertThat(p.floatProperty()).isEqualTo(133.0f);
  }

  @Test
  public void can_proxy_simple_type_with_name_override() {
    JsonNode n = parse("enum: some string");
    TypeWithPropertyOverride p = AstProxy.forNode(TypeWithPropertyOverride.class, n);

    assertThat(p._enum()).isEqualTo("some string");
  }

  @Test
  public void can_proxy_grammar_types() {
    JsonNode n = parse("child:\n  p1: v1");
    ObjectType p = AstProxy.forNode(ObjectType.class, n);

    assertThat(p.child()).isInstanceOf(Child.class);
    assertThat(p.child().p1()).isEqualTo("v1");
  }

  @Test
  @Ignore
  public void grammar_types_are_equal_if_nodes_are_equal() {
    ObjectType p1 = AstProxy.forNode(ObjectType.class, parse("child:\n  p1: v1"));
    ObjectType p2 = AstProxy.forNode(ObjectType.class, parse("child:\n  p1: v1"));

    assertThat(p1.child()).isNotEqualTo(p2.child());
    assertThat(p1.child()).isEqualTo(p1.child());
  }

  @Test
  public void can_proxy_maps_as_properties() {
    TypeWithMap p = AstProxy.forNode(TypeWithMap.class, parse(
        "children:\n" +
        "  first:\n" +
        "    p1: v1\n" +
        "  second:\n" +
        "    p1: v2\n" +
        "complexMap:\n" +
        "  first: [ a, b ]\n" +
        "  second: [ a, b ]"));

    Map<String, Child> children = p.children();

    assertThat(children).containsOnlyKeys("first", "second");
    assertThat(children.values()).extracting(Child::p1).containsOnly("v1", "v2");
    assertThat(children).hasSize(2);

    // TODO - this case is failing for now
    // Map<String, List<String>> map = p.complexMap();
    // assertThat(map).containsOnlyKeys("first", "second");
  }

  @Test
  public void can_proxy_annotated_maps_as_proxy() {
    TypeWithAnnotatedMap p = AstProxy.forNode(TypeWithAnnotatedMap.class, parse("x-first:\n    p1: v1\nx-second:\n    p1: v2"));

    Map<String, Child> children = p.extensions();

    assertThat(children).containsOnlyKeys("x-first", "x-second");
    assertThat(children.values()).extracting(Child::p1).containsOnly("v1", "v2");
    assertThat(children).hasSize(2);
  }


  @Test
  public void can_proxy_list_as_properties() {
    TypeWithList p = AstProxy.forNode(TypeWithList.class, parse("elements:\n  - p1: v1\n  - p1: v2"));

    List<Child> children = p.elements();

    assertThat(children).extracting(Child::p1).containsOnly("v1", "v2");
    assertThat(children).hasSize(2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void cannot_proxy_array_as_properties() {
    AstProxy.forNode(TypeWithArray.class, parse("elements:\n  - p1: v1\n  - p1: v2"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void cannot_proxy_set_as_properties() {
    AstProxy.forNode(TypeWithSet.class, parse("p1: v1\np2: v2"));
  }

  @Test
  public void can_proxy_raw_json() {
    TypeWithRawJson p = AstProxy.forNode(TypeWithRawJson.class, parse("raw:\n  p1: v1\n  p2: v2\nlist:\n  - p1: v1\n  - p2: v2\nmap:\n  p1: v1"));

    JsonNode raw = p.raw();

    assertThat(raw.isObject()).isTrue();
    assertThat(raw.propertyMap()).containsOnlyKeys("p1", "p2");
    assertThat(raw.propertyMap().values()).extracting(JsonNode::getTokenValue).containsExactlyInAnyOrder("v1", "v2");

    List<JsonNode> list = p.list();

    assertThat(list).hasSize(2);
    assertThat(list).extracting(JsonNode::isObject).containsOnly(true, true);

    Map<String, JsonNode> map = p.map();

    assertThat(map).hasSize(1);
    assertThat(map.get("p1")).extracting(JsonNode::getTokenValue).containsOnly("v1");
  }

  @Test
  @Ignore("Failing on TypeWithPolymorphic because Boolean is not an interface")
  public void can_proxy_polymorphic_objects() {
    TypeWithPolymorphic p = AstProxy.forNode(TypeWithPolymorphic.class, parse("polymorphic:\n  p1: v1"));
    Object c = p.polymorphic();
    assertThat(c).isInstanceOf(Child.class);

    p = AstProxy.forNode(TypeWithPolymorphic.class, parse("polymorphic:\n  true"));
    c = p.polymorphic();
    assertThat(c).isInstanceOf(Boolean.class);

    // lists

    p = AstProxy.forNode(TypeWithPolymorphic.class, parse("list:\n  - p1: v1"));
    c = p.list().get(0);
    assertThat(c).isInstanceOf(Child.class);

    p = AstProxy.forNode(TypeWithPolymorphic.class, parse("list:\n  - true"));
    c = p.list().get(0);
    assertThat(c).isInstanceOf(Boolean.class);

    // maps

    p = AstProxy.forNode(TypeWithPolymorphic.class, parse("map:\n  first:\n    p1: v1"));
    c = p.map().get("first");
    assertThat(c).isInstanceOf(Child.class);

    p = AstProxy.forNode(TypeWithPolymorphic.class, parse("map:\n  first:\n    true"));
    c = p.map().get("first");
    assertThat(c).isInstanceOf(Boolean.class);
  }

  @Test
  public void can_proxy_type_hierarchies() {
    ExtendedType p = AstProxy.forNode(ExtendedType.class, parse("p1: v1\np2: v2"));

    assertThat(p.p1()).isEqualTo("v1");
    assertThat(p.p2()).isEqualTo("v2");
  }

  @Test
  public void can_resolve_resolvables() {
    TypeWithResolvable p = AstProxy.forNode(TypeWithResolvable.class, parse("child:\n  $ref: '#/refs/toto'\nrefs:\n  toto:\n    p1: v1"));

    ResolvableType res = p.child();
    Assertions.assertThat(res.isRef()).isTrue();
    res = res.resolve();
    assertThat(res.p1()).isEqualTo("v1");
  }

  @Test
  public void can_proxy_enums() {
    TypeWithEnum p = AstProxy.forNode(TypeWithEnum.class, parse("myEnum: value1"));

    SomeEnum res = p.myEnum();
    assertThat(res).isEqualTo(SomeEnum.value1);
  }

  private JsonNode parse(String text) {
    DefaultGrammarBuilder b = new DefaultGrammarBuilder();
    b.rule(Rules.ROOT).is(b.object(
        b.property("child", b.firstOf(Rules.REF, Rules.CHILD)),
        b.property("children", b.anything()),
        b.property("polymorphic", b.firstOf(b.bool(), Rules.CHILD)),
        b.patternProperty(".*", b.anything())
    ));
    b.rule(Rules.REF).is(b.object(
        b.mandatoryProperty("$ref", b.string())));
    b.rule(Rules.CHILD).is(b.anything());
    b.setRootRule(Rules.ROOT);
    YamlParser p = new YamlParser(Charset.forName("UTF-8"), b.build(), true);
    return p.parse(text);
  }
}