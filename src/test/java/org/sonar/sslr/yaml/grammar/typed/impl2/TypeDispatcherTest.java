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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.typed.Choice;
import org.sonar.sslr.yaml.grammar.typed.Discriminant;
import org.sonar.sslr.yaml.grammar.typed.DiscriminantValue;
import org.sonar.sslr.yaml.grammar.typed.Discriminated;
import org.sonar.sslr.yaml.grammar.typed.GrammarGeneratorException;
import org.sonar.sslr.yaml.grammar.typed.TypeVisitor;
import org.sonar.sslr.yaml.grammar.typed.VisitorFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assume.assumeThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TypeDispatcherTest.SimpleTypes.class,
    TypeDispatcherTest.ArrayTypes.class,
    TypeDispatcherTest.ListTypes.class,
    TypeDispatcherTest.MapTypes.class,
    TypeDispatcherTest.ObjectTypes.class,
    TypeDispatcherTest.Choices.class
})
public class TypeDispatcherTest {
  private static final Object VISITOR_TOKEN = new Object();

  @RunWith(Theories.class)
  public static class SimpleTypes {
    @DataPoints
    public static List<Class<?>> ACCEPTED_SCALAR_TYPES =
      Arrays.asList(String.class, Integer.class, Float.class, Boolean.class, Number.class, JsonNode.class);

    private TypeVisitor v;
    private VisitorFactory f;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void prepareMocks() {
      v = makeVisitor();
      f = mock(VisitorFactory.class);
      when(f.scalar()).thenReturn(v);
    }

    @Theory
    public void dispatches_boxed_plain_to_scalar(Class<?> type) {
      Object result = new TypeDispatcher(f).visit(type);

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).scalar();
      verify(v).visit(type);
    }

    @Test
    public void dispatches_enums_to_scalar() {
      Object result = new TypeDispatcher(f).visit(ChoiceEnum.class);

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).scalar();
      verify(v).visit(ChoiceEnum.class);
    }
  }

  /**
   *
   */
  public static class ArrayTypes {
    private TypeVisitor v;
    private VisitorFactory f;

    @Before
    public void prepareMocks() {
      v = makeVisitor();
      f = mock(VisitorFactory.class);
      when(f.list()).thenReturn(v);
    }

    @Test
    public void dispatches_arrays_of_scalars_to_list() {
      Object result = new TypeDispatcher(f).visit(TypeTestUtils.returnTypeFromMethod(WithArrays.class, "integer"));

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).list();
      verify(v).visit(Integer[].class);
    }

    @Test
    public void dispatches_arrays_of_collections_to_array() {
      Object result = new TypeDispatcher(f).visit(TypeTestUtils.returnTypeFromMethod(WithArrays.class, "map_integer"));

      verify(f).list();
      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(v).visit(TypeTestUtils.returnTypeFromMethod(WithArrays.class, "map_integer"));
    }
  }

  /**
   *
   */
  public static class MapTypes {
    TypeVisitor v;
    VisitorFactory f;

    @Before
    public void prepareMocks() {
      v = makeVisitor();
      f = mock(VisitorFactory.class);
      when(f.map()).thenReturn(v);
    }

    @Test
    public void dispatches_simple_maps_to_map() {
      Object result = new TypeDispatcher(f).visit(TypeTestUtils.returnTypeFromMethod(WithMaps.class, "integer"));

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).map();
      verify(v).visit(TypeTestUtils.returnTypeFromMethod(WithMaps.class, "integer"));
    }

    @Test
    public void dispatches_maps_of_collections_with_correct_generic_type() {
      Object result = new TypeDispatcher(f).visit(TypeTestUtils.returnTypeFromMethod(WithMaps.class, "list_integer"));

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).map();
      verify(v).visit(TypeTestUtils.returnTypeFromMethod(WithMaps.class, "list_integer"));
    }
  }


  /**
   *
   */
  public static class ListTypes {
    TypeVisitor v;
    VisitorFactory f;

    @Before
    public void prepareMocks() {
      v = makeVisitor();
      f = mock(VisitorFactory.class);
      when(f.list()).thenReturn(v);
    }

    @Test
    public void dispatches_simple_lists_to_list() {
      Object result = new TypeDispatcher(f).visit(TypeTestUtils.returnTypeFromMethod(WithLists.class, "integer"));

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).list();
      verify(v).visit(TypeTestUtils.returnTypeFromMethod(WithLists.class, "integer"));
    }

    @Test
    public void dispatches_lists_of_collections_with_correct_generic_type() {
      Object result = new TypeDispatcher(f).visit(TypeTestUtils.returnTypeFromMethod(WithLists.class, "map_integer"));

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).list();
      verify(v).visit(TypeTestUtils.returnTypeFromMethod(WithLists.class, "map_integer"));
    }
  }

  /**
   *
   */
  public static class ObjectTypes {
    TypeVisitor v;
    VisitorFactory f;

    @Before
    public void prepareMocks() {
      v = makeVisitor();
      f = mock(VisitorFactory.class);
      when(f.object()).thenReturn(v);
    }

    @Test
    public void dispatches_plain_objects_to_object() {
      Object result = new TypeDispatcher(f).visit(RawRule.class);

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).object();
      verify(v).visit(RawRule.class);
    }

    @Test
    public void dispatches_objects_derived_from_maps_to_object() {
      Object result = new TypeDispatcher(f).visit(MapOfInts.class);

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).object();
      verify(v).visit(MapOfInts.class);
    }
  }

  /**
   *
   */
  @RunWith(Theories.class)
  public static class Choices {
    @DataPoints
    public static List<Class<?>> ACCEPTED_SCALAR_TYPES =
        Arrays.asList(String.class, Integer.class, Float.class, Boolean.class, Number.class, List.class, Map.class);

    private Annotation[] choice = choice("string_choice");
    private TypeVisitor v;
    private VisitorFactory f;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void prepareMocks() {
      v = makeVisitor();
      f = mock(VisitorFactory.class);
      when(f.choice()).thenReturn(v);
      when(f.scalar()).thenReturn(v);
      when(f.list()).thenReturn(v);
      when(f.map()).thenReturn(v);
      when(f.object()).thenReturn(v);
    }

    @Theory
    public void dispatches_boxed_plain_to_choice_when_annotated(Class<?> type) {
      assumeThat(type, CoreMatchers.<Class>allOf(not(List.class), not(Map.class)));

      Object result = new TypeDispatcher(f).visit(type, choice);

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).choice();
      verify(v).visit(type, choice);
    }

    @Test
    public void dispatches_to_choice_when_object() {
      Object result = new TypeDispatcher(f).visit(Object.class, choice);

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).choice();
      verify(v).visit(Object.class, choice);
    }

    @Test
    public void throws_when_object_has_no_choice() {
      expectedException.expect(GrammarGeneratorException.class);

      new TypeDispatcher(f).visit(Object.class);
    }

    @Test
    public void pushes_choice_for_discriminated_type() {
      Object result = new TypeDispatcher(f).visit(HierarchyRoot.class);

      assertThat(result).isSameAs(VISITOR_TOKEN);
      verify(f).choice();
      verify(v).visit(HierarchyRoot.class);

      verifyNoMoreInteractions(f, v);
    }

  }

  /*
   * Test Helpers
   */

  private static Annotation[] choice(String choice) {
    return new Annotation[]{ TypeTestUtils.annotationFromMethod(Annotations.class, choice, Choice.class)};
  }

  private static Type container(Class<?> rawContainer, Class<?> componentType, int componentIndex) {
    return argThat(new TypeSafeMatcher<Type>() {
      @Override
      protected boolean matchesSafely(Type item) {
        boolean isParam = item instanceof ParameterizedType;
        if (!isParam)
          return false;
        ParameterizedType t = (ParameterizedType) item;
        return t.getRawType() == rawContainer && t.getActualTypeArguments()[componentIndex] == componentType;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(rawContainer.getSimpleName());
        description.appendValue(componentType.getSimpleName());
      }
    });
  }

  private static TypeVisitor makeVisitor() {
    TypeVisitor v = mock(TypeVisitor.class);
    when(v.visit(any(Type.class), any(Annotation[].class))).thenReturn(VISITOR_TOKEN);
    when(v.visit(any(Type.class))).thenReturn(VISITOR_TOKEN);
    return v;
  }

  public interface WithMaps {
    Map<String, Integer> integer();
    Map<String, List<Integer>> list_integer();
    MapOfInts map_of_ints();
  }

  public interface WithLists {
    List<Integer> integer();
    List<Map<String, Integer>> map_integer();
  }

  public interface WithArrays {
    Integer[] integer();
    Map<String, Integer>[] map_integer();
  }

  public interface Annotations {
    @Choice({"a", "b", "c"})
    void string_choice();
  }

  public interface MapOfInts extends Map<String, Integer> {

  }
  public interface RawRule {}

  public enum ChoiceEnum {
    value1, value2, value3;
  }

  @Discriminated({HierarchyLeaf.class, HierarchyLeaf2.class})
  interface HierarchyRoot {
    @Discriminant
    String discriminant_field();
  }

  @DiscriminantValue({"value1"})
  interface HierarchyLeaf extends ObjectVisitorTest.HierarchyRoot {

  }
  @DiscriminantValue({"value2"})
  interface HierarchyLeaf2 extends ObjectVisitorTest.HierarchyRoot {

  }
}