package io.github.gy7604.alarmtest;
import android.content.*;import android.os.*;
public class AlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){int id=i.getIntExtra("id",AlarmStore.TEST);if(id==AlarmStore.NEXT&&!AlarmStore.prefs(c).getBoolean("enabled",false))return;
  if(id==AlarmStore.NEXT)AlarmStore.restore(c);
  Intent ring=new Intent(c,RingService.class).putExtra("label",i.getStringExtra("label"));if(Build.VERSION.SDK_INT>=26)c.startForegroundService(ring);else c.startService(ring);
 }
}
