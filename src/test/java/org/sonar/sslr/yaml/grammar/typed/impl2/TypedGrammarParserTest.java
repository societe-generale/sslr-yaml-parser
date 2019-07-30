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

import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.DefaultGrammarBuilder;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.YamlParser;
import org.sonar.sslr.yaml.grammar.typed.Choice;
import org.sonar.sslr.yaml.grammar.typed.Discriminant;
import org.sonar.sslr.yaml.grammar.typed.DiscriminantValue;
import org.sonar.sslr.yaml.grammar.typed.Discriminated;
import org.sonar.sslr.yaml.grammar.typed.Key;
import org.sonar.sslr.yaml.grammar.typed.Mandatory;
import org.sonar.sslr.yaml.grammar.typed.Pattern;

import static org.junit.Assert.assertTrue;

public class TypedGrammarParserTest {
  @Test
  //@Ignore
  public void can_generate_grammar_and_parse_file() {
    DefaultGrammarBuilder builder = new DefaultGrammarBuilder();
    VisitorFactoryImpl factory = new VisitorFactoryImpl(builder);
    TypeDispatcher dispatcher = new TypeDispatcher(factory);
    factory.setDispatcher(dispatcher);

    GrammarRuleKey rootKey = (GrammarRuleKey) dispatcher.visit(RootType.class);
    builder.setRootRule(rootKey);
    builder.print(System.out);

    YamlParser parser = YamlParser.builder().withGrammar(builder).withStrictValidation(true).build();
    JsonNode parsed = parser.parse(new File("src/test/resources/typed-grammar.yaml"));
    assertTrue(parsed.is(rootKey));
  }

  interface Child {
    @Mandatory
    Float floating();
    Map<String, String> details();
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
  interface RootType {
    Child[] array();

    HierarchyBase leaf();

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
