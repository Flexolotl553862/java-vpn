package com.stevesad.common.launcher;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TransactionalLauncher {

    protected List<Action> actions = new ArrayList<>();
    protected List<ThrowingRunnable> rollbacks = new ArrayList<>();

    protected void addAction(ThrowingRunnable work, ThrowingRunnable rollback) {
        actions.add(new Action(work, rollback));
    }

    protected void addActionWithNoRollback(ThrowingRunnable work) {
        actions.add(new Action(work, null));
    }

    protected void executeSequence() throws Exception {
        try {
            for (Action action : actions) {
                action.work.run();
                if (action.rollback != null) {
                    rollbacks.add(action.rollback);
                }
            }
        } catch (Exception e) {
            rollbackSequence();
            throw e;
        }

        actions.clear();
    }

    protected void rollbackSequence() {
        for (ThrowingRunnable rollback : rollbacks.reversed()) {
            try {
                rollback.run();
            } catch (Exception re) {
                log.warn("Failed to rollback action", re);
            }
        }

        rollbacks.clear();
    }

    protected record Action(ThrowingRunnable work, @Nullable ThrowingRunnable rollback) {}

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
