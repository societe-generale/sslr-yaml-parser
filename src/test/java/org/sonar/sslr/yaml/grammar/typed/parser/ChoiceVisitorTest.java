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

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.GrammarRuleBuilder;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.PropertyDescription;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
import org.sonar.sslr.yaml.grammar.typed.Choice;
import org.sonar.sslr.yaml.grammar.typed.Discriminant;
import org.sonar.sslr.yaml.grammar.typed.DiscriminantValue;
import org.sonar.sslr.yaml.grammar.typed.Discriminated;
import org.sonar.sslr.yaml.grammar.typed.GrammarGeneratorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.sslr.yaml.grammar.typed.parser.TypeTestUtils.annotationFromMethod;
import static org.sonar.sslr.yaml.grammar.typed.parser.TypeTestUtils.keyThatPrintsAs;
import static org.sonar.sslr.yaml.grammar.typed.parser.TypeTestUtils.returnTypeFromMethod;
import static org.sonar.sslr.yaml.grammar.typed.parser.TypeTestUtils.returnTypeNameWhenVisitingWith;
import static org.sonar.sslr.yaml.grammar.typed.parser.TypeTestUtils.returnTypeTransformationWhenVisitingWith;

public class ChoiceVisitorTest {
  private YamlGrammarBuilder builder;
  private TypeVisitor dispatcher;
  private TypeVisitor.Context context;
  private ChoiceVisitor visitor;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void prepareMocks() {
    builder = makeGrammarBuilder();

    dispatcher = mock(TypeDispatcher.class);
    context = new VisitorContext();

    visitor = new ChoiceVisitor(builder, dispatcher, context);
  }

  @Test
  public void visits_every_leaf_of_hierarchy() {
    returnTypeTransformationWhenVisitingWith(dispatcher, t -> context.makeTypeKey(t));
    Object result = visitor.visit(HierarchyRoot.class);

    assertThat(result).isEqualTo(context.makeTypeKey(HierarchyRoot.class));
    verify(dispatcher).visit(HierarchyLeaf.class);
    verify(dispatcher).visit(HierarchyLeaf2.class);
    verify(builder).firstOf(keyThatPrintsAs(HierarchyLeaf.class.getName()), keyThatPrintsAs(HierarchyLeaf2.class.getName()));
  }

  @Test
  public void throws_when_discriminated_child_doesnt_inherit_base() {
    expectedException.expect(GrammarGeneratorException.class);
    expectedException.expectMessage("org.sonar.sslr.yaml.grammar.typed.parser.ChoiceVisitorTest$BadHierarchyRoot references org.sonar.sslr.yaml.grammar.typed.parser.ChoiceVisitorTest$HierarchyLeaf but is not its parent.");

    visitor.visit(BadHierarchyRoot.class);
  }

  @Test
  public void visits_all_object_choices() {
    returnTypeNameWhenVisitingWith(dispatcher);
    visitor.visit(Object.class, annotationFromMethod(AnnotationBank.class, "multi_choice", Choice.class));

    verify(builder).firstOf("a", "b", "c", false, TypeA.class.getName(), TypeB.class.getName());
    verify(dispatcher).visit(TypeA.class);
    verify(dispatcher).visit(TypeB.class);
  }

  @Test
  public void includes_enum_in_object_choices() {
    returnTypeNameWhenVisitingWith(dispatcher);
    visitor.visit(Object.class, annotationFromMethod(AnnotationBank.class, "with_enum", Choice.class));

    verify(builder).firstOf("a", "b", "c", "value1", "value2");
    verify(dispatcher, never()).visit(EnumChoice.class);
  }

  @Test
  public void throws_when_no_choices_on_Object() {
    expectedException.expect(GrammarGeneratorException.class);
    expectedException.expectMessage("Using Choice annotation without any value!");

    visitor.visit(Object.class, annotationFromMethod(AnnotationBank.class, "empty_annotation", Choice.class));
  }

  @Test
  public void generates_choices_from_annotated_list() {
    returnTypeNameWhenVisitingWith(dispatcher);

    visitor.visit(returnTypeFromMethod(AnnotationBank.class, "list"), annotationFromMethod(AnnotationBank.class, "list", Choice.class));

    verify(dispatcher).visit(String.class, annotationFromMethod(AnnotationBank.class, "list", Choice.class));
    verify(builder).array("java.lang.String");
  }

  @Test
  public void generates_choices_from_annotated_derived_list() {
    returnTypeNameWhenVisitingWith(dispatcher);

    visitor.visit(SpecializedList.class);

    verify(dispatcher).visit(String.class, SpecializedList.class.getAnnotation(Choice.class));
    verify(builder).array("java.lang.String");
  }

  @Test
  public void generates_choices_from_annotated_map() {
    returnTypeNameWhenVisitingWith(dispatcher);
    PropertyDescription patternResult = mock(PropertyDescription.class);
    when(builder.patternProperty(anyString(), anyObject())).thenReturn(patternResult);

    visitor.visit(returnTypeFromMethod(AnnotationBank.class, "map"), annotationFromMethod(AnnotationBank.class, "map", Choice.class));

    verify(dispatcher).visit(String.class, annotationFromMethod(AnnotationBank.class, "map", Choice.class));
    verify(builder).patternProperty(".*", "java.lang.String");
    verify(builder).object(patternResult);
  }

  @Test
  public void generates_choices_from_annotated_derived_map() {
    returnTypeNameWhenVisitingWith(dispatcher);
    PropertyDescription patternResult = mock(PropertyDescription.class);
    when(builder.patternProperty(anyString(), anyObject())).thenReturn(patternResult);

    visitor.visit(SpecializedMap.class);

    verify(dispatcher).visit(String.class, SpecializedMap.class.getAnnotation(Choice.class));
    verify(builder).patternProperty(".*", "java.lang.String");
    verify(builder).object(patternResult);
  }

  @Test
  public void throws_when_jsonNode_has_choice() {
    expectedException.expect(GrammarGeneratorException.class);
    expectedException.expectMessage("Using type JsonNode with a Choice annotation");

    visitor.visit(JsonNode.class, annotationFromMethod(AnnotationBank.class, "multi_choice", Choice.class));
  }

  @Test
  public void generates_choices_for_string() {
    visitor.visit(String.class, annotationFromMethod(AnnotationBank.class, "strings", Choice.class));

    verify(builder).firstOf("a", "b", "c");
  }

  @Test
  public void includes_enum_in_string_choices() {
    visitor.visit(String.class, annotationFromMethod(AnnotationBank.class, "with_enum", Choice.class));

    verify(builder).firstOf("a", "b", "c", "value1", "value2");
    verify(dispatcher, never()).visit(EnumChoice.class);
  }

  @Test
  public void throws_when_wrong_choices_for_string() {
    expectedException.expect(GrammarGeneratorException.class);
    expectedException.expectMessage("Reducing the list of choices of a String with more than just strings or enums!");

    visitor.visit(String.class, annotationFromMethod(AnnotationBank.class, "multi_choice", Choice.class));
  }

  @Test
  public void generates_choices_for_boolean() {
    visitor.visit(Boolean.class, annotationFromMethod(AnnotationBank.class, "bool", Choice.class));

    verify(builder).bool(false);
  }

  @Test
  public void throws_when_several_boolean_choices() {
    expectedException.expect(GrammarGeneratorException.class);
    expectedException.expectMessage("Reducing the list of choices of a Boolean with more than one value! Use classes={Boolean.class} instead.");

    visitor.visit(Boolean.class, annotationFromMethod(AnnotationBank.class, "many_bools", Choice.class));
  }

  @Discriminated({HierarchyLeaf.class, HierarchyLeaf2.class})
  interface HierarchyRoot {
    @Discriminant
    String discriminant_field();
  }

  @DiscriminantValue({"value1"})
  interface HierarchyLeaf extends HierarchyRoot {

  }
  @DiscriminantValue({"value2"})
  interface HierarchyLeaf2 extends HierarchyRoot {

  }

  @Discriminated({HierarchyLeaf.class, HierarchyLeaf2.class})
  interface BadHierarchyRoot {
    @Discriminant
    String discriminant_field();
  }

  interface AnnotationBank {
    @Choice(value={"a", "b", "c"}, bools=false, classes = {TypeA.class, TypeB.class})
    Object multi_choice();
    @Choice(value={"a", "b", "c"}, classes = {EnumChoice.class})
    Object with_enum();
    @Choice(value={"a", "b", "c"})
    List<String> list();
    @Choice(value={"a", "b", "c"})
    Map<String, String> map();
    @Choice
    Object empty_annotation();
    @Choice(value={"a", "b", "c"})
    List<String> strings();
    @Choice(bools=false)
    Object bool();
    @Choice(bools={true,false})
    Object many_bools();
  }
  interface TypeA {
    String property();
  }
  interface TypeB {
    String property2();
  }
  @Choice(value={"a", "b", "c"})
  interface SpecializedList extends List<String> {}
  @Choice(value={"a", "b", "c"})
  interface SpecializedMap extends Map<String, String> {}

  enum EnumChoice {
    value1, value2
  }

  private static YamlGrammarBuilder makeGrammarBuilder() {
    YamlGrammarBuilder builder = mock(YamlGrammarBuilder.class);
    GrammarRuleBuilder ruleBuilder = mock(GrammarRuleBuilder.class);
    when(builder.rule(any(GrammarRuleKey.class))).thenReturn(ruleBuilder);
    return builder;
  }
}
