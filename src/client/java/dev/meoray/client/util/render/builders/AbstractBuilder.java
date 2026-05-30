package dev.meoray.client.util.render.builders;

public abstract class AbstractBuilder<T> {
    public AbstractBuilder() {
        this.reset();
    }

    @SuppressWarnings("unchecked")
    public final T build() {
        T instance = (T)this._build();
        this.reset();
        return instance;
    }

    protected abstract void reset();

    protected abstract T _build();
}
