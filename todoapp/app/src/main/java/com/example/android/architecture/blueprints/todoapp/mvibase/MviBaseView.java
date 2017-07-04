package com.example.android.architecture.blueprints.todoapp.mvibase;

import io.reactivex.Observable;

public interface MviBaseView<StateParentClass extends MviViewState> {
  Observable<? extends MviIntent> intents();

  void render(StateParentClass state);
}
