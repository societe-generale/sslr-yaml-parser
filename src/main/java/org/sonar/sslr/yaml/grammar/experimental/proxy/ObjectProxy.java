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
package org.sonar.sslr.yaml.grammar.experimental.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.sonar.sslr.yaml.grammar.experimental.type.TypeDescriptor;
import org.sonar.sslr.yaml.grammar.JsonNode;

/**
 * TODO - implement the behavior in bytebuddy, so that we don't need the map key->value, and we can generate the methods
 * on the fly
 */
public class ObjectProxy implements NodeProxy, InvocationHandler {
  private final JsonNode node;
  private final Map<String, TypeDescriptor> methods = new HashMap();
  private final Map<String, TypeDescriptor> values = new HashMap();

  public ObjectProxy(JsonNode node) {
    this.node = node;
  }

  @Override
  public JsonNode getNode() {
    return node;
  }

  @Override
  public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
    Class<?> declaringClass = method.getDeclaringClass();
    if (declaringClass.equals(Object.class) || declaringClass.equals(NodeProxy.class)) {
      return method.invoke(node, objects);
    }

    Object value = values.get(method.getName());
    node.get(method.getName());
    if (value == null) {
      throw new IllegalAccessException("Method " + method + " is not a valid proxy");
    }

    return value;
  }


  // chaque méthode a un nom potentiellement différent (à cause de l'annotation Property/Key) -> il faut sauver la bonne valeur dans
  // le type descriptor

  // générer une méthode pour chaque fonction, qui sait aller chercher la bonne propriété et la retourner
  //  -> ie: charger la bonne instance de proxy, et le construire avec le noeud cible (ou null si la prop n'existe pas)
  // s'assurer que si elle est déjà construite, alors on la retoure direct (notamment pour les collections)
}
