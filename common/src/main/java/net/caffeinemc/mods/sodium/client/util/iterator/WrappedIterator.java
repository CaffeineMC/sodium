package net.caffeinemc.mods.sodium.client.util.iterator;

import java.util.Iterator;

public class WrappedIterator<T> implements Iterator<T> {
    private final Iterator<T> delegate;

    private WrappedIterator(Iterator<T> delegate) {
        this.delegate = delegate;
    }

    public static <T> WrappedIterator<T> create(Iterable<T> iterable) {
        return new WrappedIterator<>(iterable.iterator());
    }

    @Override
    public boolean hasNext() {
        try {
            return this.delegate.hasNext();
        } catch (Throwable t) {
            throw new Exception("Iterator#hasNext() threw unhandled exception", t);
        }
    }

    @Override
    public T next() {
        try {
            return this.delegate.next();
        } catch (Throwable t) {
            throw new Exception("Iterator#next() threw unhandled exception", t);
        }
    }

    public static class Exception extends RuntimeException {
        private Exception(String message, Throwable t) {
            super(message, t);
        }
    }
}
