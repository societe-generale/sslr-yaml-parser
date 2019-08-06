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

public class TypeUtils {
  static <T extends Annotation> T find(Annotation[] annotations, Class<T> annotationClass) {
    for (Annotation annotation : annotations) {
      if (annotationClass.isAssignableFrom(annotation.getClass())) {
        return (T)annotation;
      }
    }
    return null;
  }

  static Annotation[] combine(Annotation[] firstArray, Annotation... additionals) {
    if (firstArray == null && additionals == null) {
      return null;
    }
    if (additionals == null) {
      return firstArray;
    }
    if (firstArray == null) {
      return additionals;
    }
    Annotation[] result = new Annotation[firstArray.length + additionals.length];
    System.arraycopy(firstArray, 0, result, 0, firstArray.length);
    System.arraycopy(additionals, 0, result, firstArray.length, additionals.length);
    return result;
  }
}
