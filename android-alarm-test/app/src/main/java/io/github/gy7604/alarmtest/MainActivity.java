package io.github.gy7604.alarmtest;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;
public class MainActivity extends Activity {
 private TextView status, time;
 private int hour, minute;
 private String label;
 private android.content.SharedPreferences prefs;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  prefs=getSharedPreferences("test",MODE_PRIVATE);
  label=prefs.getString("label",null);
  if(label==null) { label="교대연동테스트-"+UUID.randomUUID().toString().substring(0,8); prefs.edit().putString("label",label).apply(); }
  Calendar next=Calendar.getInstance(); next.add(Calendar.MINUTE,5);
  hour=prefs.getInt("hour",next.get(Calendar.HOUR_OF_DAY)); minute=prefs.getInt("minute",next.get(Calendar.MINUTE));
  ScrollView scroll=new ScrollView(this);
  LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(20),dp(20),dp(20)); box.setBackgroundColor(Color.WHITE);
  scroll.addView(box); setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,insets)->{
   if(android.os.Build.VERSION.SDK_INT>=30) {
    android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
    v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
   } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
   return insets;
  }); scroll.requestApplyInsets();
  text(box,"교대 알람 연동 테스트",24);
  text(box,"삼성 시계에 일회성 알람을 등록하고 취소를 요청합니다. 근무 자동 연동은 테스트 결과 확인 후 추가합니다.",16);
  text(box,"테스트 알람 이름: "+label,15);
  time=text(box,"",22); updateTime();
  button(box,"시간 선택",()->new TimePickerDialog(this,(p,h,m)->{hour=h;minute=m;saveTime();updateTime();},hour,minute,true).show());
  button(box,"5분 뒤로 맞추기",()->{Calendar c=Calendar.getInstance();c.add(Calendar.MINUTE,5);hour=c.get(Calendar.HOUR_OF_DAY);minute=c.get(Calendar.MINUTE);saveTime();updateTime();});
  button(box,"① 테스트 알람 등록·켜기",this::setAlarm);
  button(box,"② 테스트 알람 취소 요청",()->new AlertDialog.Builder(this).setTitle("테스트 알람 취소").setMessage(label+" 이름의 일회성 알람만 취소를 요청합니다. 시계가 선택 화면을 띄우면 같은 이름인지 확인하세요.").setNegativeButton("돌아가기",null).setPositiveButton("취소 요청",(d,w)->dismissAlarm()).show());
  button(box,"③ 시계에서 결과 확인",()->send(new Intent(AlarmClock.ACTION_SHOW_ALARMS),"시계 알람 목록 열기"));
  status=text(box,prefs.getString("status","등록 버튼을 누른 뒤 삼성 시계에서 이름과 시간이 맞는지 확인하세요."),16);
  text(box,"취소 후 시계에서 이 알람이 꺼졌거나 삭제됐는지 확인해 주세요. 알람 변경 결과는 앱이 직접 조회할 수 없으므로 요청 전송과 실제 성공을 구분합니다.",15);
 }
 private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private TextView text(LinearLayout box,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.rgb(28,35,51));t.setPadding(0,dp(10),0,dp(10));box.addView(t);return t;}
 private void button(LinearLayout box,String value,Runnable action){Button b=new Button(this);b.setText(value);b.setTextColor(Color.rgb(30,58,110));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);box.addView(b,lp);b.setOnClickListener(v->action.run());}
 private void saveTime(){prefs.edit().putInt("hour",hour).putInt("minute",minute).apply();}
 private void updateTime(){time.setText(String.format(Locale.KOREA,"알람 시간 %02d:%02d (일회성)",hour,minute));}
 private void setAlarm(){
  saveTime();
  Intent i=new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,hour).putExtra(AlarmClock.EXTRA_MINUTES,minute).putExtra(AlarmClock.EXTRA_MESSAGE,label).putExtra(AlarmClock.EXTRA_SKIP_UI,false);
  send(i,"테스트 알람 등록 요청");
 }
 private void dismissAlarm(){
  Intent i=new Intent(AlarmClock.ACTION_DISMISS_ALARM).putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE,AlarmClock.ALARM_SEARCH_MODE_LABEL).putExtra(AlarmClock.EXTRA_MESSAGE,label).putExtra(AlarmClock.EXTRA_SKIP_UI,false);
  send(i,"테스트 알람 취소 요청");
 }
 private void send(Intent intent,String action){
  // 삼성 시계가 이 동작을 지원하면 우선 사용합니다. 지원하지 않으면 시스템 선택 화면을 사용합니다.
  Intent samsung=new Intent(intent).setPackage("com.sec.android.app.clockpackage");
  Intent target=samsung.resolveActivity(getPackageManager())!=null?samsung:intent;
  ComponentName handler=target.resolveActivity(getPackageManager());
  if(handler==null){record(action+": 이 기기의 시계가 해당 기능을 지원하지 않습니다. 시계에서 수동 확인해 주세요.");return;}
  try{record(action+" 전송 → "+handler.getPackageName()+"\n실제 성공 여부는 시계에서 확인하세요.");startActivity(target);}
  catch(ActivityNotFoundException|SecurityException e){record(action+" 실패: "+e.getClass().getSimpleName());}
 }
 private void record(String value){status.setText(value);prefs.edit().putString("status",value).apply();}
}
