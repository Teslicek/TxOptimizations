package com.teslicek.txoptimizations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Predicate;

public final class EmptyHashSet<T> extends HashSet<T> {

    private static final EmptyHashSet<?> INSTANCE = new EmptyHashSet<>();

    private EmptyHashSet() {
    }

    @SuppressWarnings("unchecked")
    public static <T> EmptyHashSet<T> instance() {
        return (EmptyHashSet<T>) INSTANCE;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public boolean remove(Object element) {
        return false;
    }

    @Override
    public boolean add(T element) {
        throw new UnsupportedOperationException("The shared empty set cannot hold " + element);
    }

    @Override
    public boolean addAll(Collection<? extends T> elements) {
        throw new UnsupportedOperationException("The shared empty set cannot hold " + elements.size() + " elements");
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return false;
    }

    @Override
    public void clear() {
    }
}
