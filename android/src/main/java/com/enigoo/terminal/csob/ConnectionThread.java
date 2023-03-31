package com.enigoo.terminal.csob;

import androidx.annotation.Nullable;

public class ConnectionThread extends Thread{
    private boolean passivating;
    public ConnectionThread(@Nullable Runnable target) {
        super(target);
        passivating = false;
    }

    public boolean isPassivating() {
        return passivating;
    }

    public void setPassivating(boolean passivating) {
        this.passivating = passivating;
    }

    @Override
    public void run() {
        super.run();

    }
}
