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
    public sealed interface TurnstileState {}

    /** State indicating that the turnstile is locked. */
    public record LockedState() implements TurnstileState {}

    /** State indicating that the turnstile is unlocked and how many credits are available. */
    public record UnlockedState(int credits) implements TurnstileState {}

    /**
     * State machine for the turnstile.
     *
     * The base class (ReadOnlyStateMachine) holds the actual StateMachine instance that is used
     * for all of the underlying functionality. Using this layer of indirection hides the actual
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

    private static final class TurnstileStateMachine extends ReadOnlyStateMachine<TurnstileState> {
        // We use a single static instance of this class because it has no parameters.
        private static final LockedState LOCKED = new LockedState();

        public TurnstileStateMachine() {
            // Create the underlying state machine and pass it to the base class.
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
            // If the turnstile is unlocked we need to deduct a credit and maybe switch to Locked.
            if (isState(UnlockedState.class)) {
                UnlockedState state = (UnlockedState) getCurrentState();
                if (state.credits <= 1) {
                    stateMachine.transition(LOCKED);
                } else {
                    stateMachine.transition(new UnlockedState(state.credits - 1));
                }

                return true;
            }

            // If the turnstile wasn't unlocked, then indicate that pushing did nothing.
            return false;
        }

        /** Action for the user inserting a coin into the turnstile. */
        public void insertCoin() {
            // First determine how many credits the turnstile has, add one, and set it to Unlocked.
            int currentCredit =
                    isState(UnlockedState.class) ? ((UnlockedState) getCurrentState()).credits : 0;

            stateMachine.transition(new UnlockedState(currentCredit + 1));
        }
    }

    /** Simple main() that prompts for a user action and relays it to a turnstile. */
    public static void main(String[] args) {
        TurnstileStateMachine turnstile = new TurnstileStateMachine();
        Scanner input = new Scanner(System.in);

        while (true) {
            System.out.println("");
            System.out.println("Turnstile State: " + turnstile.getCurrentState().toString());
            System.out.println("  1 - Push");
            System.out.println("  2 - Insert Coin");
            System.out.println("  3 - Exit");
            System.out.print("--> ");

            switch (input.nextInt()) {
                case 1:
                    boolean opened = turnstile.push();
                    if (opened) {
                        System.out.println("The turnstile opened");
                    } else {
                        System.out.println("The turnstile did not open");
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
