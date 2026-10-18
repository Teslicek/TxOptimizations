package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public final class EntityValues<T> implements Iterable<T> {

    private final ObjectCollection<T> values;

    public EntityValues(ObjectCollection<T> values) {
        this.values = values;
    }

    @Override
    public Iterator<T> iterator() {
        return new Values<>(this.values.iterator());
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        this.values.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return this.values.spliterator();
    }

    @Override
    public String toString() {
        return this.values.toString();
    }

    private static final class Values<T> implements Iterator<T> {

        private final ObjectIterator<T> iterator;

        private Values(ObjectIterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        @Override
        public T next() {
            return this.iterator.next();
        }
    }
}
