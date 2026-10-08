package io.github.gy7604.alarmtest;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  String channelId="shift_alarm_test";
  NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  Uri sound=android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel ch=new NotificationChannel(channelId,"교대 알람 테스트",NotificationManager.IMPORTANCE_HIGH);
   ch.setDescription("교대 근무 알람 테스트");
   ch.enableVibration(true);
   ch.setSound(sound,new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
   nm.createNotificationChannel(ch);
  }
  Intent open=new Intent(context,MainActivity.class);
  PendingIntent pi=PendingIntent.getActivity(context,MainActivity.REQUEST_CODE,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification n=new NotificationCompat.Builder(context,channelId)
   .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
   .setContentTitle("교대 알람 테스트")
   .setContentText("보조 앱이 설정한 테스트 알람입니다.")
   .setPriority(NotificationCompat.PRIORITY_MAX)
   .setCategory(NotificationCompat.CATEGORY_ALARM)
   .setSound(sound).setVibrate(new long[]{0,800,400,800})
   .setAutoCancel(true).setContentIntent(pi).build();
  nm.notify(MainActivity.REQUEST_CODE,n);
 }
}
