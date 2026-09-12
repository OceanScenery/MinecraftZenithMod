package com.oceanscenery.zenith.event;

public class ServerTickTask {
    private int ticks=0;
    private final Runnable task;
    private boolean shouldRun=true;

    public ServerTickTask(int ticks,Runnable task){
        this.task=task;
        this.ticks=ticks;
    }

    public void tick(){
        this.ticks--;
        if(this.ticks<=0 && this.shouldRun){
            this.task.run();
            this.shouldRun=false;
        }
    }

    public boolean isCompleted(){
        return !this.shouldRun;
    }
}
