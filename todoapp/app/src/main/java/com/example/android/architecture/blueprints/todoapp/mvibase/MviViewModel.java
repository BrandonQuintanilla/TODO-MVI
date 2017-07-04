package com.example.android.architecture.blueprints.todoapp.mvibase;

import io.reactivex.Observable;
import timber.log.Timber;

public interface MviViewModel<IntentParentClass extends MviIntent> {
  void forwardIntents(Observable<IntentParentClass> intents);

  Observable<? extends MviViewState> states();

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
