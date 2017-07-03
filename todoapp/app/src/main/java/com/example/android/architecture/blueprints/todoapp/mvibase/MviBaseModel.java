package com.example.android.architecture.blueprints.todoapp.mvibase;

import io.reactivex.Observable;

public interface MviBaseModel {
  void forwardIntents(Observable<? extends MviIntent> intents);

  Observable<? extends MviUiState> states();
}
