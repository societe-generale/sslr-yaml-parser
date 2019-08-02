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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.GrammarRuleBuilder;
import org.sonar.sslr.yaml.grammar.PropertyDescription;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
import org.sonar.sslr.yaml.grammar.typed.Discriminant;
import org.sonar.sslr.yaml.grammar.typed.DiscriminantValue;
import org.sonar.sslr.yaml.grammar.typed.Discriminated;
import org.sonar.sslr.yaml.grammar.typed.GrammarGeneratorException;
import org.sonar.sslr.yaml.grammar.typed.Key;
import org.sonar.sslr.yaml.grammar.typed.Mandatory;
import org.sonar.sslr.yaml.grammar.typed.Pattern;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;
import org.sonar.sslr.yaml.grammar.typed.TypeVisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.sslr.yaml.grammar.typed.impl2.TypeTestUtils.annotationFromMethod;
import static org.sonar.sslr.yaml.grammar.typed.impl2.TypeTestUtils.returnTypeFromMethod;

public class ObjectVisitorTest {

  private YamlGrammarBuilder builder;
  private TypeVisitor dispatcher;
  private TypeVisitor.Context context;
  private ObjectVisitor visitor;

  @Before
  public void prepareMocks() {
    builder = makeGrammarBuilder();

    dispatcher = mock(TypeDispatcher.class);
    context = new VisitorContext();

    visitor = new ObjectVisitor(builder, dispatcher, context);
  }

  @Test
  public void generates_a_new_rule() {
    Object result = visitor.visit(BasicObject.class);

    assertThat(result).isEqualTo(context.makeTypeKey(BasicObject.class));
    verify(builder).rule(TypeTestUtils.keyThatPrintsAs(BasicObject.class.getSimpleName()));
  }

  @Test(expected = GrammarGeneratorException.class)
  public void throws_if_not_received_class() {
    visitor.visit(returnTypeFromMethod(BasicObject.class, "string_list"));
  }

  @Test
  public void adds_each_method_with_visit_result() {
    TypeTestUtils.returnTypeNameWhenVisitingWith(dispatcher);

    visitor.visit(BasicObject.class);

    verify(builder).property("string_scalar", "java.lang.String");
    verify(builder).property("string_list", "java.util.List<java.lang.String>");
    verify(builder).property("recursive", BasicObject.class.getName());

    verify(builder).object(any(PropertyDescription.class), any(PropertyDescription.class), any(PropertyDescription.class));
  }

  @Test(expected = GrammarGeneratorException.class)
  public void throws_when_type_has_no_usable_method() {
    visitor.visit(EmptyObject.class);
  }

  @Test(expected = GrammarGeneratorException.class)
  public void throws_when_type_is_not_interface() {
    visitor.visit(ConcreteClass.class);
  }

  @Test
  public void applies_key_annotation_on_method() {
    visitor.visit(WithDecorators.class);

    verify(builder).property(eq("modified"), anyObject());
  }

  @Test
  public void registers_mandatory_properties() {
    visitor.visit(WithDecorators.class);

    verify(builder).mandatoryProperty(eq("mandatory"), anyObject());
  }

  @Test
  public void registers_pattern_properties() {
    TypeTestUtils.returnTypeNameWhenVisitingWith(dispatcher);
    visitor.visit(WithDecorators.class);

    verify(dispatcher).visit(Float.class, annotationFromMethod(WithDecorators.class, "pattern", Pattern.class));
    verify(builder).patternProperty("^x-.*", "java.lang.Float");
  }

  @Test(expected = GrammarGeneratorException.class)
  public void throws_on_pattern_with_bad_map() {
    visitor.visit(WrongPatternMapType.class);
  }

  @Test(expected = GrammarGeneratorException.class)
  public void throws_on_pattern_with_bad_type() {
    visitor.visit(WrongPatternType.class);
  }

  @Test(expected = GrammarGeneratorException.class)
  public void throws_if_method_is_not_getter_profile() {
    visitor.visit(WrongMethodProfile.class);
  }

  // Temporarily excluding map compatibility until I find a way to inject the proxied implementation of List/Map
  @Test(expected = GrammarGeneratorException.class)
  public void list_and_map_and_object_methods_are_excluded() {
    visitor.visit(DerivedFromMap.class);

    /*verify(builder, never()).property(eq("size"), anyObject());
    verify(builder, never()).property(eq("isEmpty"), anyObject());
    verify(builder, never()).property(eq("hashCode"), anyObject());
    */
  }

  @Test
  public void doesnt_parse_type_twice() {
    visitor.visit(BasicObject.class);
    verify(builder).rule(any(GrammarRuleKey.class));

    // Reset mocks so that we can test interactions with builder
    builder = makeGrammarBuilder();
    visitor = new ObjectVisitor(builder, dispatcher, context);

    visitor.visit(BasicObject.class);
    verifyZeroInteractions(builder);
  }

  @Test
  public void registers_discriminants_and_exposes_choices_to_discriminant_type() {
    TypeTestUtils.returnTypeNameWhenVisitingWith(dispatcher);

    visitor.visit(HierarchyLeaf.class);

    Annotation discriminantValue = HierarchyLeaf.class.getAnnotation(DiscriminantValue.class);
    Annotation discriminant = annotationFromMethod(HierarchyRoot.class, "discriminant_field", Discriminant.class);
    verify(dispatcher).visit(String.class, discriminant, discriminantValue);
    verify(builder).discriminant("discriminant_field", "java.lang.String");
  }

  @Test(expected = GrammarGeneratorException.class)
  public void throws_if_two_discriminants() {
    visitor.visit(WrongHierarchyLeaf.class);
  }

  @Test(expected = GrammarGeneratorException.class)
  public void throws_if_no_discriminant_value() {
    visitor.visit(NoDiscriminantValue.class);
  }

  @Test(expected = IllegalStateException.class)
  public void throws_if_visiting_discriminated_type() {
    visitor.visit(HierarchyRoot.class);
  }

  @Test
  public void generates_choice_for_resolvable_type() {
    TypeTestUtils.returnTypeNameWhenVisitingWith(dispatcher);

    visitor.visit(WithResolvable.class);

    // getName() because Resolvable is sent to the dispatcher
    verify(builder).firstOf(Resolvable.class.getName(), context.makeTypeKey(WithResolvable.class));
    verify(builder).rule(context.makeTypeKey(WithResolvable.class));
    verify(builder).property("value", "java.lang.String");
  }

  @Test
  public void generates_choice_for_resolvable_even_when_type_already_seen() {
    TypeTestUtils.returnTypeNameWhenVisitingWith(dispatcher);
    GrammarRuleKey typeKey = context.makeTypeKey(WithResolvable.class);
    context.add(typeKey);

    visitor.visit(WithResolvable.class);

    verify(builder).firstOf(Resolvable.class.getName(), typeKey);
    verify(builder, never()).rule(typeKey);
  }

  interface BasicObject {
    String string_scalar();
    List<String> string_list();
    BasicObject recursive();
  }

  interface EmptyObject {}

  static class ConcreteClass {
    String string_scalar() {
      return null;
    }
  }

  interface WithDecorators {
    @Key("modified")
    String unused_name();

    @Mandatory
    String mandatory();

    @Pattern("^x-.*")
    Map<String, Float> pattern();
  }

  interface WrongMethodProfile {
    String wrong_method(String parameter);
  }

  interface DerivedFromMap extends Map<String, BasicObject> {
  }

  @Discriminated({HierarchyLeaf.class})
  interface HierarchyRoot {
    @Discriminant
    String discriminant_field();
  }

  @DiscriminantValue({"value1", "value2"})
  interface HierarchyLeaf extends HierarchyRoot {

  }

  @DiscriminantValue({"value1", "value2"})
  interface WrongHierarchyLeaf extends HierarchyRoot {
    @Discriminant
    String other_discriminant();
  }

  interface NoDiscriminantValue extends HierarchyRoot {

  }

  public interface WrongPatternMapType {
    @Pattern("^x-.*")
    Map<Integer, BasicObject> bad_key_type();
  }

  public interface WrongPatternType {
    @Pattern("^x-.*")
    BasicObject bad_key_type();
  }

  public interface WithResolvable extends Resolvable<WithResolvable> {
    String value();
  }

  private static YamlGrammarBuilder makeGrammarBuilder() {
    YamlGrammarBuilder builder = mock(YamlGrammarBuilder.class);
    GrammarRuleBuilder ruleBuilder = mock(GrammarRuleBuilder.class);
    when(builder.rule(any(GrammarRuleKey.class))).thenReturn(ruleBuilder);
    return builder;
  }

}