package io.github.gy7604.alarmtest;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){AlarmStore.restore(c);AlarmStore.jobs(c);}
}
