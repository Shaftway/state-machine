package com.hounshell.st8.demos.turnstile;

import com.hounshell.st8.ReadOnlyStateMachine;
import com.hounshell.st8.StateMachine;

import java.util.Scanner;

/**
 * Simple demo of StateMachine using a turnstile as an example.
 *
 * A turnstile is a gate used to ensure that people accessing a resource have paid. Normally the
 * turnstile is locked. A user inserts a coin and it unlocks. They can then push the turnstile and
 * it will open. Pushing the turnstile without inserting a coin wil; result in it not opening. This
 * is a particularly fancy turnstile; if you insert more than one coin it will remain unlocked
 * until you have used up all of your credits.
 */
public class TurnstileDemo {
    /** Sentinel class for identifying turnstile states. */
    public interface TurnstileState {}

    /** State indicating that the turnstile is locked. */
    public static final class LockedState implements TurnstileState {
        @Override
        public String toString() {
            return "Locked";
        }
    }

    /** State indicating that the turnstile is unlocked and how many credits are available. */
    public static final class UnlockedState implements TurnstileState {
        public final int credits;

        public UnlockedState(int credits) {
            this.credits = credits;
        }

        @Override
        public String toString() {
            return String.format("Unlocked (%s credits)", credits);
        }
    }

    /**
     * State machine for the turnstile.
     *
     * The base class (ReadOnlyStateMachine) holds the actual StateMachine instance that is used
     * for all the underlying functionality. Using this layer of indirection hides the actual
     * StateMachine implementation. This helps prevent invalid state transitions. The
     * {@link StateMachine.Builder} can ensure that only transitions between specific pairs of
     * types are allowed, but it can't actually validate business logic. This pattern ensures that
     * callers don't have the ability to call {@link StateMachine#transition} which means they
     * can't cause state transitions that break business logic.
     *
     * Instead, this class exposes action methods that callers can use to change the state. These
     * actions can ensure that business logic is followed properly. Callers can still subscribe to
     * state changes and query the current state.
     */
    public static final class Turnstile extends ReadOnlyStateMachine<TurnstileState> {
        // We use a single static instance of this class because it has no parameters.
        private static final LockedState LOCKED = new LockedState();

        public Turnstile() {
            /*
             * Create the underlying state machine and pass it to the base class. These aren't
             * technically necessary, since we have business logic protecting the transitions
             * in the action methods below, but it's nice to have the guardrails. Note that a
             * transition from Unlocked to Unlocked has to be explicitly allowed, as otherwise
             * there would be an error when adding a token to an unlocked turnstile.
             */
            super(
                    StateMachine.<TurnstileState>newBuilder()
                            .addValidTransition(LockedState.class, UnlockedState.class)
                            .addValidTransition(UnlockedState.class, LockedState.class)
                            .addValidTransition(UnlockedState.class, UnlockedState.class)
                            .buildWithInitialState(LOCKED));
        }

        /**
         * Action for the user pushing on the turnstile.
         *
         * @return {@code true} if the user was allowed in, {@code false} otherwise.
         */
        public boolean push() {
            if (isState(UnlockedState.class)) {
                // If the turnstile is unlocked we need to deduct a credit and maybe switch to Locked.
                int credits = ((UnlockedState) getCurrentState()).credits;
                if (credits <= 1) {
                    transition(LOCKED);
                } else {
                    transition(new UnlockedState(credits - 1));
                }

                return true;

            } else {
                // If the turnstile wasn't unlocked, then indicate that pushing did nothing.
                return false;
            }
        }

        /** Action for the user inserting a coin into the turnstile. */
        public void insertCoin() {
            // Determine how many credits the turnstile has, add one, and set it to Unlocked.
            int credits =
                    isState(UnlockedState.class) ? ((UnlockedState) getCurrentState()).credits : 0;

            transition(new UnlockedState(credits + 1));
        }
    }
    /** Simple main() that prompts for a user action and relays it to a turnstile. */
    public static void main(String[] args) {
        Turnstile turnstile = new Turnstile();
        Scanner input = new Scanner(System.in);

        System.out.println("\nTurnstile is currently " + turnstile.getCurrentState().toString());

        turnstile.addCallbackForAnything((from, to) -> {
            System.out.println("");
            System.out.println("Turnstile state has changed");
            System.out.println("  Was: " + from.toString());
            System.out.println("  Now: " + to.toString());
        });

        while (true) {
            System.out.println("");
            System.out.println("  1 - Push");
            System.out.println("  2 - Insert Coin");
            System.out.println("  3 - Quit");
            System.out.print("--> ");

            switch (input.nextInt()) {
                case 1:
                    boolean opened = turnstile.push();
                    if (opened) {
                        System.out.println("\nThe turnstile opened");
                    } else {
                        System.out.println("\nThe turnstile did not open");
                    }
                    break;

                case 2:
                    turnstile.insertCoin();
                    break;

                case 3:
                    return;
            }
        }
    }
}
