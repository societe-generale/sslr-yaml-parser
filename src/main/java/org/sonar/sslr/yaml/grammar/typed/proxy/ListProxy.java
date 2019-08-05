package org.sonar.sslr.yaml.grammar.typed.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.sslr.yaml.grammar.JsonNode;

import static org.sonar.sslr.yaml.grammar.YamlGrammar.BLOCK_ARRAY_ELEMENT;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.FLOW_ARRAY_ELEMENT;

public class ListProxy<E> implements NodeProxy, List<E> {
  private JsonNode node;
  private List<E> list;
  private ProxyFactory factory;

  public ListProxy(JsonNode node, ProxyFactory factory) {
    this.node = node;
    this.factory = factory;
  }

  @Override
  public JsonNode getNode() {
    return node;
  }

  @Override
  public int size() {
    return resolve().size();
  }

  @Override
  public boolean isEmpty() {
    return list == null ? node.hasDirectChildren(BLOCK_ARRAY_ELEMENT, FLOW_ARRAY_ELEMENT) : list.isEmpty();
  }

  @Override
  public boolean equals(Object other) {
    return other == this || resolve().equals(other);
  }

  @Override
  public int hashCode() {
    return resolve().hashCode();
  }

  @Override
  public String toString() {
    return resolve().toString();
  }

  @Override
  public boolean contains(Object element) {
    return resolve().contains(element);
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    return resolve().containsAll(collection);
  }

  @Override
  public Object[] toArray() {
    return resolve().toArray();
  }

  @Override
  public <T> T[] toArray(T[] array) {
    return resolve().toArray(array);
  }

  @Override
  public E get(int index) {
    return resolve().get(index);
  }

  @Override
  public E set(int index, E element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, E element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends E> collection) {
    throw new UnsupportedOperationException();
  }

  public boolean addAll(int index, Collection<? extends E> other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public E remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object element) {
    return resolve().indexOf(element);
  }

  @Override
  public int lastIndexOf(Object element) {
    return resolve().lastIndexOf(element);
  }

  @Override
  public void replaceAll(UnaryOperator<E> mutator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sort(Comparator<? super E> comparator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void forEach(Consumer<? super E> consumer) {
    resolve().forEach(consumer);
  }

  @Override
  public boolean removeIf(Predicate<? super E> predicate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Spliterator<E> spliterator() {
    return resolve().spliterator();
  }

  @Override
  public Stream<E> stream() {
    return resolve().stream();
  }

  @Override
  public Stream<E> parallelStream() {
    return resolve().parallelStream();
  }

  @Override
  public ListIterator<E> listIterator() {
    return this.listIterator(0);
  }

  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {
      private final Iterator<? extends E> i;

      {
        this.i = resolve().iterator();
      }

      public boolean hasNext() {
        return this.i.hasNext();
      }

      public E next() {
        return this.i.next();
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

      public void forEachRemaining(Consumer<? super E> consumer) {
        this.i.forEachRemaining(consumer);
      }
    };
  }

  @Override
  public ListIterator<E> listIterator(final int index) {
    return new ListIterator<E>() {
      private final ListIterator<? extends E> i;

      {
        this.i = resolve().listIterator(index);
      }

      @Override
      public boolean hasNext() {
        return this.i.hasNext();
      }

      @Override
      public E next() {
        return this.i.next();
      }

      @Override
      public boolean hasPrevious() {
        return this.i.hasPrevious();
      }

      @Override
      public E previous() {
        return this.i.previous();
      }

      @Override
      public int nextIndex() {
        return this.i.nextIndex();
      }

      @Override
      public int previousIndex() {
        return this.i.previousIndex();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void set(E element) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void add(E element) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void forEachRemaining(Consumer<? super E> consumer) {
        this.i.forEachRemaining(consumer);
      }
    };
  }

  @Override
  public List<E> subList(int from, int to) {
    return Collections.unmodifiableList(resolve().subList(from, to));
  }

  private List<E> resolve() {
    if (list != null) {
      return list;
    }
    List<E> result = new ArrayList<>();
    result = node.getChildren(BLOCK_ARRAY_ELEMENT, FLOW_ARRAY_ELEMENT).stream()
        .map(n -> (JsonNode)n.getFirstChild())
        .map(n -> (E)factory.makeProxyFor(n))
        .collect(Collectors.toList());
    node = null;
    factory = null;
    list = result;
    return list;
  }

}
