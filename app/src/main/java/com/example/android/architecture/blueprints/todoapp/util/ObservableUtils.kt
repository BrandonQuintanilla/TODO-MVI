package com.example.android.architecture.blueprints.todoapp.util

import io.reactivex.Observable
import java.util.concurrent.TimeUnit

/**
 * Emit an event immediately, then emit an other event after a delay has passed.
 * It is used for time limited UI state (e.g. a snackbar) to allow the stream to control
 * the timing for the showing and the hiding of a UI component.
 *
 * @param immediate Immediately emitted event
 * @param delayed   Event emitted after a delay
 */
fun <T> pairWithDelay(immediate: T, delayed: T): Observable<T> {
  return Observable.timer(2, TimeUnit.SECONDS)
      .map { delayed }
      .startWith(immediate)
}
