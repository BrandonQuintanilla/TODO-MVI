package com.example.android.architecture.blueprints.todoapp.mvibase;

import io.reactivex.Observable;

/**
 * Object representing a UI that will
 * a) emit its intents to a view model,
 * b) subscribes to a view model for rendering its UI.
 *
 * @param <I> Top class of the {@link MviIntent} that the {@link MviView} will be emitting.
 * @param <S> Top class of the {@link MviViewState} the {@link MviView} will be subscribing to.
 */
public interface MviView<I extends MviIntent, S extends MviViewState> {
    Observable<I> intents();

    void render(S state);
}
