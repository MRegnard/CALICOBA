package fr.irit.smac.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A queue with a fixed capacity. When adding a new element into a full queue,
 * the first element (head) will be popped out.
 * 
 * @author Damien Vergnet
 *
 * @param <E> the type of elements held in this collection
 */
public class FixedCapacityQueue<E> implements Queue<E> {
  private final int capacity;
  private Queue<E> values;

  public FixedCapacityQueue(final int capacity) {
    this.capacity = capacity;
    this.values = new LinkedList<>();
  }

  @Override
  public int size() {
    return this.values.size();
  }

  @Override
  public boolean isEmpty() {
    return this.values.isEmpty();
  }

  public boolean isFull() {
    return this.values.size() == this.capacity;
  }

  @Override
  public boolean contains(Object o) {
    return this.values.contains(o);
  }

  @Override
  public Iterator<E> iterator() {
    return this.values.iterator();
  }

  @Override
  public Object[] toArray() {
    return this.values.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return this.values.toArray(a);
  }

  /**
   * Unsupported operation.
   * 
   * @throws UnsupportedOperationException
   */
  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("remove");
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return this.values.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    return this.values.addAll(c);
  }

  /**
   * Unsupported operation.
   * 
   * @throws UnsupportedOperationException
   */
  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("removeAll");
  }

  /**
   * Unsupported operation.
   * 
   * @throws UnsupportedOperationException
   */
  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException("retainAll");
  }

  @Override
  public void clear() {
    this.values.clear();
  }

  /**
   * Inserts the specified element into this queue. If the queue is full, the
   * first element is removed.
   * 
   * @param e The element to add.
   * @return True if the element was added to this queue, else false.
   */
  @Override
  public boolean add(E e) {
    if (this.isFull()) {
      this.values.poll();
    }
    return this.values.add(e);
  }

  /**
   * Inserts the specified element into this queue. If the queue is full, the
   * first element is removed.
   * 
   * @param e The element to add.
   * @return True if the element was added to this queue, else false.
   */
  @Override
  public boolean offer(E e) {
    if (this.isFull()) {
      this.values.poll();
    }
    return this.values.offer(e);
  }

  @Override
  public E remove() {
    return this.values.remove();
  }

  @Override
  public E poll() {
    return this.values.poll();
  }

  @Override
  public E element() {
    return this.values.element();
  }

  @Override
  public E peek() {
    return this.values.peek();
  }

  @Override
  public String toString() {
    return this.values.toString();
  }
}
