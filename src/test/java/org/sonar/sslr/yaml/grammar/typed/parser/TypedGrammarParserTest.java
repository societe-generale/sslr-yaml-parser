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
package org.sonar.sslr.yaml.grammar.typed.parser;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.typed.Choice;
import org.sonar.sslr.yaml.grammar.typed.Discriminant;
import org.sonar.sslr.yaml.grammar.typed.DiscriminantValue;
import org.sonar.sslr.yaml.grammar.typed.Discriminated;
import org.sonar.sslr.yaml.grammar.typed.Key;
import org.sonar.sslr.yaml.grammar.typed.Mandatory;
import org.sonar.sslr.yaml.grammar.typed.Pattern;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;
import org.sonar.sslr.yaml.grammar.typed.TypedYamlParser;

import static org.assertj.core.api.Assertions.assertThat;

public class TypedGrammarParserTest {
  @Test
  //@Ignore
  public void can_generate_grammar_and_parse_file() {
    TypedYamlParser<RootType> parser = TypedYamlParser.builder(RootType.class).withStrictValidation(true).build();
    RootType rootType = parser.parse(new File("src/test/resources/typed-grammar.yaml"));
    //assertTrue(parsed.is(rootKey));

    assertThat(rootType.children().get("child-b").resolve().details().get("detail a").get(1)).isEqualTo("element 2");
    assertThat(rootType.leaf()).isInstanceOf(Leaf2.class);
  }

  interface Child extends Resolvable<Child>  {
    //@Mandatory
    Float floating();
    Map<String, List<String>> details();
    List<Child> children();
  }

  @Discriminated({Leaf1.class, Leaf2.class})
  interface HierarchyBase {
    @Discriminant
    String in();
  }

  @DiscriminantValue({"leaf1"})
  interface Leaf1 extends HierarchyBase {
    Integer integer();
  }
  @DiscriminantValue({"leaf2", "leaf2-old"})
  interface Leaf2 extends HierarchyBase {
    Float floating();
  }
  interface ListOfPossibleTypes extends List<PossibleTypes> {}
  enum PossibleTypes {
    type1,
    type2,
    type3
  }
  interface MapOfPossibleTypes extends Map<String, PossibleTypes> {}
  interface RootType {
    Child[] array();

    HierarchyBase leaf();

    ListOfPossibleTypes listType();
    MapOfPossibleTypes mapType();

    @Mandatory
    @Choice(classes={PossibleTypes.class,ListOfPossibleTypes.class})
    @Key("type")
    Object _type();

    @Pattern("^x-.*")
    Map<String, JsonNode> extensions();

    @Pattern("child-.*")
    Map<String, Child> children();
  }

}
