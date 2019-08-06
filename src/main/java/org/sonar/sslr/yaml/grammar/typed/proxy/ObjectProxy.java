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
package org.sonar.sslr.yaml.grammar.typed.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;

public class ObjectProxy implements NodeProxy, Resolvable, InvocationHandler {
  private JsonNode node;
  private final Map<String, ProxyFactory> methods;
  private final Map<String, Object> values = new HashMap<>();
  private JsonNode resolved;

  public ObjectProxy(JsonNode node, Map<String, ProxyFactory> methods) {
    this.node = node;
    this.methods = methods;
  }

  @Override
  public JsonNode getNode() {
    return node;
  }

  @Override
  public final Object invoke(Object o, Method method, Object[] parameters) throws Throwable {
    Class<?> declaringClass = method.getDeclaringClass();
    if (declaringClass.equals(Object.class) || declaringClass.equals(NodeProxy.class)) {
      return method.invoke(this, parameters);
    } else if  (method.getName().equals("resolve")) {
      doResolve();
      return o;
    } else if (method.getName().equals("$ref")) {
      return $ref();
    } else if (method.getName().equals("isRef")) {
      return isRef();
    }

    doResolve();
    Object value = values.get(method.getName());
    if (value == null) {
      value = methods.get(method.getName()).makeProxyFor(resolved);
      values.put(method.getName(), value);
      // No need for it anymore
      //methods.remove(method.getName());
    }
    return value;
  }

  @Override
  public Resolvable resolve() {
    doResolve();
    return this;
  }

  private void doResolve() {
    if (resolved == null) {
      resolved = node.resolve();
      if (resolved.isMissing()) {
        throw new IllegalArgumentException("Cannot resolve reference " + $ref());
      }
    }
  }

  @Override
  public boolean isRef() {
    return node.isRef();
  }

  @Override
  public String $ref() {
    JsonNode ref = node.get("$ref");
    if (ref.isMissing()) {
      throw new NoSuchElementException("Not a reference!");
    }
    return ref.stringValue();
  }
}
