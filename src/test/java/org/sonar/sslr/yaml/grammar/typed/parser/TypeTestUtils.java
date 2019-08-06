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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.stubbing.Answer;
import org.sonar.sslr.grammar.GrammarRuleKey;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

public class TypeTestUtils {
  private static Method methodFromClass(Class<?> ownerType, String methodName) {
    return Arrays.stream(ownerType.getMethods())
          .filter(m -> m.getName().equals(methodName))
          .findFirst()
          .orElseThrow(()->new RuntimeException("Could not find method " + methodName + " in type " + ownerType));
  }

  static Type returnTypeFromMethod(Class<?> ownerType, String methodName) {
    Method method = methodFromClass(ownerType, methodName);
    return method.getGenericReturnType();
  }

  static <T extends Annotation> Annotation annotationFromMethod(Class<?> ownerType, String methodName, Class<T> annotationType) {
    Method method = methodFromClass(ownerType, methodName);
    return method.getAnnotation(annotationType);
  }

  static void returnTypeNameWhenVisitingWith(TypeVisitor dispatcher) {
    returnTypeTransformationWhenVisitingWith(dispatcher, Type::getTypeName);
  }

  static void returnTypeTransformationWhenVisitingWith(TypeVisitor dispatcher, Function<Type, Object> transformation) {
    Answer<Object> objectAnswer = invocation -> transformation.apply((Type)invocation.getArguments()[0]);
    when(dispatcher.visit(any(Type.class), any(Annotation[].class))).thenAnswer(objectAnswer);
    when(dispatcher.visit(any(Type.class), any(Annotation.class), any(Annotation.class))).thenAnswer(objectAnswer);
    when(dispatcher.visit(any(Type.class))).thenAnswer(objectAnswer);
  }

  static GrammarRuleKey keyThatPrintsAs(String name) {
    return argThat(new TypeSafeMatcher<GrammarRuleKey>() {
      @Override
      protected boolean matchesSafely(GrammarRuleKey item) {
        return item.toString().contains(name);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Key for type ").appendValue(name);
      }
    });
  }
}
