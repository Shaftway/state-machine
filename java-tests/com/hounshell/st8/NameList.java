package com.hounshell.st8;

import java.util.List;

/**
 * Live list of names that are "loaded" from somewhere.
 */
public interface NameList {

    interface HasContents extends NameList {
        List<String> getContents();
    }

    interface FullLoadRequested extends NameList {}
    interface LoadingFullPage extends NameList, HasContents {}
    interface Loaded extends NameList, HasContents {}
    interface LoadMoreRequested extends NameList, HasContents {}
    interface LoadingMore extends NameList, HasContents {}
    interface ErrorLoading extends NameList, HasContents {}

    static StateMachine<NameList> getStateMachine() {
        return getStateMachine(new FullLoadRequested() {});
    }

    static StateMachine<NameList> getStateMachine(NameList initialState) {
        return new StateMachine.Builder<NameList>()
                .addValidTransition(FullLoadRequested.class, LoadingFullPage.class)
                .addValidTransition(FullLoadRequested.class, ErrorLoading.class)
                .addValidTransition(LoadingMore.class, ErrorLoading.class)
                .addValidTransition(LoadingMore.class, Loaded.class)
                .addValidTransition(LoadingFullPage.class, ErrorLoading.class)
                .addValidTransition(LoadingFullPage.class, Loaded.class)
                .addValidTransition(Loaded.class, LoadMoreRequested.class)
                .addValidTransition(LoadMoreRequested.class, LoadingMore.class)
                .addValidTransition(LoadMoreRequested.class, ErrorLoading.class)
                .addValidTransitionFromAnythingTo(FullLoadRequested.class)
                .buildWithInitialState(initialState);
    }
}
