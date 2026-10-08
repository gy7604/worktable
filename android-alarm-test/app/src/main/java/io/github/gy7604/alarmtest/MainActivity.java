package io.github.gy7604.alarmtest;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends Activity {
 static final int REQUEST_CODE=7604;
 private TextView status;
 private SharedPreferences prefs;

 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  prefs=getSharedPreferences("alarm_test",MODE_PRIVATE);
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=getPackageManager().PERMISSION_GRANTED)
   requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);
  ScrollView scroll=new ScrollView(this);
  LinearLayout box=new LinearLayout(this);
  box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(20),dp(20),dp(28)); box.setBackgroundColor(Color.WHITE);
  scroll.addView(box); setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);}return i;});
  text(box,"교대 알람 자체 관리 테스트",24);
  text(box,"이 앱이 만든 테스트 알람만 등록하고 취소합니다. 삼성 시계의 기존 알람은 변경하지 않습니다.",16);
  button(box,"① 2분 뒤 테스트 알람 등록",this::scheduleAlarm);
  button(box,"② 테스트 알람 취소",this::cancelAlarm);
  button(box,"정확한 알람 권한 확인",this::openExactAlarmSettings);
  status=text(box,prefs.getString("status","2분 뒤 알람을 등록한 다음, 취소 버튼으로 울리지 않는지 확인해 주세요."),16);
  text(box,"테스트 1: 등록 후 기다려 알림음·진동 확인\n테스트 2: 다시 등록한 뒤 곧바로 취소하고 2분간 울리지 않는지 확인",15);
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView text(LinearLayout b,String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.rgb(28,35,51));t.setPadding(0,dp(10),0,dp(10));b.addView(t);return t;}
 private void button(LinearLayout b,String s,Runnable r){Button x=new Button(this);x.setText(s);x.setTextColor(Color.rgb(30,58,110));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);b.addView(x,p);x.setOnClickListener(v->r.run());}
 static PendingIntent alarmIntent(Context c){
  Intent i=new Intent(c,AlarmReceiver.class).setAction("io.github.gy7604.alarmtest.FIRE");
  return PendingIntent.getBroadcast(c,REQUEST_CODE,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private boolean canExact(){
  AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);
  return Build.VERSION.SDK_INT<31||a.canScheduleExactAlarms();
 }
 private void scheduleAlarm(){
  if(!canExact()){record("정확한 알람 권한을 허용한 뒤 등록 버튼을 다시 눌러 주세요.");openExactAlarmSettings();return;}
  long at=System.currentTimeMillis()+120000L;
  AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);
  Intent show=new Intent(this,MainActivity.class);
  PendingIntent showPi=PendingIntent.getActivity(this,REQUEST_CODE,show,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  a.setAlarmClock(new AlarmManager.AlarmClockInfo(at,showPi),alarmIntent(this));
  String when=DateFormat.getTimeInstance(DateFormat.SHORT,Locale.KOREA).format(new Date(at));
  record(when+" 테스트 알람을 등록했습니다.");
 }
 private void cancelAlarm(){
  AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);
  a.cancel(alarmIntent(this)); alarmIntent(this).cancel();
  record("테스트 앱이 만든 알람을 취소했습니다.");
 }
 private void openExactAlarmSettings(){
  if(Build.VERSION.SDK_INT>=31&&!canExact()){
   try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())));}
   catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}
  } else record("정확한 알람 권한이 허용되어 있습니다.");
 }
 private void record(String s){status.setText(s);prefs.edit().putString("status",s).apply();}
}
