package io.github.gy7604.alarmtest;
import android.app.Activity;import android.content.*;import android.graphics.Color;import android.os.*;import android.view.*;import android.widget.*;
public class AlarmActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable watch=new Runnable(){public void run(){if(!RingService.ringing){finish();return;}handler.postDelayed(this,500);}};
 @Override public void onCreate(Bundle state){super.onCreate(state);
 if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
 getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
 LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setPadding(32,48,32,48);box.setBackgroundColor(Color.rgb(16,24,40));setContentView(box);
 box.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(32+i.getSystemWindowInsetLeft(),48+i.getSystemWindowInsetTop(),32+i.getSystemWindowInsetRight(),48+i.getSystemWindowInsetBottom());return i;});
 TextClock clock=new TextClock(this);clock.setFormat24Hour("HH:mm");clock.setFormat12Hour("HH:mm");clock.setTextSize(64);clock.setTextColor(Color.WHITE);box.addView(clock);
 TextView label=new TextView(this);label.setText(getIntent().getStringExtra("label"));label.setTextColor(Color.WHITE);label.setTextSize(22);label.setGravity(Gravity.CENTER);box.addView(label);
 Button stop=new Button(this);stop.setText("알람 끄기");stop.setTextSize(28);box.addView(stop,new LinearLayout.LayoutParams(-1,120));stop.setOnClickListener(v->{stopService(new Intent(this,RingService.class));finish();});
 Button snooze=new Button(this);snooze.setText("5분 미루기");snooze.setTextSize(24);box.addView(snooze,new LinearLayout.LayoutParams(-1,120));snooze.setOnClickListener(v->{if(!AlarmStore.exact(this)){label.setText("정확한 알람 권한이 없어 미룰 수 없습니다. 알람 끄기를 눌러 주세요.");return;}AlarmStore.set(this,AlarmStore.SNOOZE,System.currentTimeMillis()+300000L,"5분 뒤 다시 알림");stopService(new Intent(this,RingService.class));finish();});
 }
 @Override protected void onResume(){super.onResume();handler.postDelayed(watch,500);}
 @Override protected void onPause(){handler.removeCallbacks(watch);super.onPause();}
}
