package com.example.android.architecture.blueprints.todoapp.util;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

public class ObservableUtils {
    private ObservableUtils() {
        // no implementation
    }

    /**
     * Emit an event immediately, then emit an other event after a delay has passed.
     * It is used for time limited UI state (e.g. a snackbar) to allow the stream to control
     * the timing for the showing and the hiding of a UI component.
     *
     * @param immediate Immediately emitted event
     * @param delayed   Event emitted after a delay
     */
    public static <T> Observable<T> pairWithDelay(T immediate, T delayed) {
        return Observable.timer(2, TimeUnit.SECONDS)
                .take(1)
                .map(ignored -> delayed)
                .startWith(immediate);
    }
}
