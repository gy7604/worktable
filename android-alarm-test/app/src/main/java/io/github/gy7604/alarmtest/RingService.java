package io.github.gy7604.alarmtest;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;import android.provider.Settings;
public class RingService extends Service {
 static volatile boolean ringing;
 private Ringtone fallback;
 private MediaPlayer player;private Vibrator vibrator;private PowerManager.WakeLock wake;
 private final Handler handler=new Handler(Looper.getMainLooper());private final Runnable timeout=this::stopSelf;
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  String action=intent==null?"":intent.getAction();if("STOP".equals(action)){stopSelf();return START_NOT_STICKY;}if("SNOOZE".equals(action)){if(!AlarmStore.exact(this))return START_NOT_STICKY;AlarmStore.set(this,AlarmStore.SNOOZE,System.currentTimeMillis()+5*60*1000L,"5분 뒤 다시 알림");stopSelf();return START_NOT_STICKY;}
  String label=intent==null?"기상 알람":intent.getStringExtra("label");NotificationManager nm=getSystemService(NotificationManager.class);
  if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel("wake_alarm_v3","근무 기상 알람",NotificationManager.IMPORTANCE_HIGH);ch.setSound(null,null);nm.createNotificationChannel(ch);}
  PendingIntent open=PendingIntent.getActivity(this,2,new Intent(this,AlarmActivity.class).putExtra("label",label),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,3,new Intent(this,RingService.class).setAction("STOP"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent snooze=PendingIntent.getService(this,4,new Intent(this,RingService.class).setAction("SNOOZE"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"wake_alarm_v3"):new Notification.Builder(this);b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("교대근무 기상 알람").setContentText(label).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_MAX).setOngoing(true).setContentIntent(open).addAction(android.R.drawable.ic_media_pause,"5분 미루기",snooze).addAction(android.R.drawable.ic_menu_close_clear_cancel,"끄기",stop);
  if(Build.VERSION.SDK_INT<34||nm.canUseFullScreenIntent())b.setFullScreenIntent(open,true);
  ringing=true;startForeground(7604,b.build());
  if(player==null){try{wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"shift:alarm");wake.acquire(11*60*1000L);player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());player.setDataSource(this,Settings.System.DEFAULT_ALARM_ALERT_URI);player.setLooping(true);player.prepare();player.start();AlarmStore.prefs(this).edit().remove("sound_error").apply();}catch(Exception e){if(player!=null){player.release();player=null;}AlarmStore.prefs(this).edit().putString("sound_error","기본 알람음 재생 실패 · 대체음 시도: "+e.getClass().getSimpleName()).apply();try{fallback=RingtoneManager.getRingtone(this,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));if(fallback!=null){fallback.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());if(Build.VERSION.SDK_INT>=28)fallback.setLooping(true);fallback.play();}}catch(Exception f){AlarmStore.prefs(this).edit().putString("sound_error","대체 알람음도 실패: "+f.getClass().getSimpleName()).apply();}}
   vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(vibrator!=null){long[] pattern={0,500,500};if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createWaveform(pattern,0));else vibrator.vibrate(pattern,0);}}
  handler.removeCallbacks(timeout);handler.postDelayed(timeout,10*60*1000L);return START_NOT_STICKY;
 }
 @Override public void onDestroy(){ringing=false;if(fallback!=null)fallback.stop();handler.removeCallbacks(timeout);if(player!=null){player.release();player=null;}if(vibrator!=null)vibrator.cancel();if(wake!=null&&wake.isHeld())wake.release();stopForeground(true);super.onDestroy();}
}
