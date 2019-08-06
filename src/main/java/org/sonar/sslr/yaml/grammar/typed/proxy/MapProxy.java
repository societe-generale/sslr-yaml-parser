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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.sonar.sslr.yaml.grammar.JsonNode;

public class MapProxy<V> implements Map<String, V>, NodeProxy {
  private Map<String, V> m;
  private JsonNode parent;
  private String pattern;
  private ProxyFactory factory;
  private transient Set<String> keySet;
  private transient Set<Entry<String, V>> entrySet;
  private transient Collection<V> values;

  public MapProxy(JsonNode parent, String pattern, ProxyFactory factory) {
    this.parent = parent;
    this.pattern = pattern;
    this.factory = factory;
  }

  @Override
  public JsonNode getNode() {
    return parent;
  }

  public int size() {
    return resolve().size();
  }

  public boolean isEmpty() {
    return resolve().isEmpty();
  }

  public boolean containsKey(Object var1) {
    return resolve().containsKey(var1);
  }

  public boolean containsValue(Object var1) {
    return resolve().containsValue(var1);
  }

  public V get(Object key) {
    return resolve().get(key);
  }

  public V put(String key, V value) {
    throw new UnsupportedOperationException();
  }

  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  public void putAll(Map<? extends String, ? extends V> other) {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public Set<String> keySet() {
    resolve();
    if (this.keySet == null) {
      this.keySet = Collections.unmodifiableSet(m.keySet());
    }

    return this.keySet;
  }

  public Set<Entry<String, V>> entrySet() {
    resolve();
    if (this.entrySet == null) {
      this.entrySet = Collections.unmodifiableSet(m.entrySet());
    }

    return this.entrySet;
  }

  public Collection<V> values() {
    resolve();
    if (this.values == null) {
      this.values = Collections.unmodifiableCollection(m.values());
    }

    return this.values;
  }

  public boolean equals(Object other) {
    return other == this || resolve().equals(other);
  }

  public int hashCode() {
    return resolve().hashCode();
  }

  public String toString() {
    return resolve().toString();
  }

  public V getOrDefault(Object key, V defaultValue) {
    return resolve().getOrDefault(key, defaultValue);
  }

  public void forEach(BiConsumer<? super String, ? super V> consumer) {
    this.m.forEach(consumer);
  }

  public void replaceAll(BiFunction<? super String, ? super V, ? extends V> fn) {
    throw new UnsupportedOperationException();
  }

  public V putIfAbsent(String key, V value) {
    throw new UnsupportedOperationException();
  }

  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  public boolean replace(String key, V oldValue, V newValue) {
    throw new UnsupportedOperationException();
  }

  public V replace(String key, V value) {
    throw new UnsupportedOperationException();
  }

  public V computeIfAbsent(String key, Function<? super String, ? extends V> transformer) {
    throw new UnsupportedOperationException();
  }

  public V computeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> transformer) {
    throw new UnsupportedOperationException();
  }

  public V compute(String var1, BiFunction<? super String, ? super V, ? extends V> var2) {
    throw new UnsupportedOperationException();
  }

  public V merge(String key, V value, BiFunction<? super V, ? super V, ? extends V> tranformer) {
    throw new UnsupportedOperationException();
  }

  private Map<String, V> resolve() {
    if (m != null) {
      return m;
    }
    Pattern compiled = Pattern.compile(pattern);
    Map<String, V> result = new HashMap<>();
    parent.propertyNames().stream()
        .filter(s -> compiled.matcher(s).matches())
        .forEach(k -> {
          JsonNode node = parent.get(k);
          result.put(k, (V)factory.makeProxyFor(node));
        });
    m = result;
    return m;
  }
}
