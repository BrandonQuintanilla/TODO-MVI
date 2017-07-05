package com.example.android.architecture.blueprints.todoapp.mvibase;

import io.reactivex.Observable;
import timber.log.Timber;

/**
 * Object that will subscribes to a view's intents, process it and emit a state back.
 *
 * @param <I> Top class of the {@link MviIntent} that the {@link MviViewModel} will be subscribing
 * to.
 * @param <S> Top class of the {@link MviViewState} the {@link MviViewModel} will be emitting.
 */
public interface MviViewModel<I extends MviIntent, S extends MviViewState> {
  void processIntents(Observable<I> intents);

  Observable<S> states();

  static void logIntent(MviIntent intent) {
    Timber.d("Intent: " + intent);
  }

  static void logAction(MviAction action) {
    Timber.d("Action: " + action);
  }

  static void logResult(MviResult result) {
    Timber.d("Result: " + result);
  }

  static void logState(MviViewState state) {
    Timber.d("State: " + state);
  }
}
