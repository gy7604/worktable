package io.github.gy7604.alarmtest;
import android.app.job.*;
public class SyncJob extends JobService {
 private Thread task;
 @Override public boolean onStartJob(JobParameters p){task=new Thread(()->{boolean retry=false;try{AlarmStore.sync(this);}catch(Exception e){retry=!(e instanceof AlarmStore.Revoked);}jobFinished(p,retry);});task.start();return true;}
 @Override public boolean onStopJob(JobParameters p){if(task!=null)task.interrupt();return true;}
}
