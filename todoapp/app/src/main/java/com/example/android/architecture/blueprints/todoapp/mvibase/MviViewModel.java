package com.example.android.architecture.blueprints.todoapp.mvibase;

import io.reactivex.Observable;

public interface MviViewModel<IntentParentClass extends MviIntent> {
  void forwardIntents(Observable<IntentParentClass> intents);

  Observable<? extends MviViewState> states();
}
