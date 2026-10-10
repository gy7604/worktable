package io.github.gy7604.alarmtest;
import android.Manifest;import android.app.*;import android.content.*;import android.graphics.Color;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;import java.text.*;import java.util.*;
public class MainActivity extends Activity {
 private TextView status;private EditText code;private Switch enabled;private boolean refreshing;
 @Override public void onCreate(Bundle state){super.onCreate(state);if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=getPackageManager().PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);
  ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(20),dp(20),dp(28));box.setBackgroundColor(Color.WHITE);scroll.addView(box);setContentView(scroll);scroll.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets z=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(z.left,z.top,z.right,z.bottom);}return i;});
  text(box,"교대근무 기상 알람",24);text(box,"주간·야간 기상 시간은 직접 변경할 수 있습니다.\n휴무 신청과 대근을 실제 일정에서 반영합니다.",16);
  button(box,"교대근무 앱 열기 · 연결 코드 발급",()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://gy7604.github.io/index.html"))));
  code=new EditText(this);code.setSingleLine(true);code.setHint("기상 알람 연결 코드를 붙여넣으세요");code.setTextSize(14);code.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);if(Build.VERSION.SDK_INT>=26)code.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);box.addView(code);
  button(box,"내 계정 연결",()->{String value=code.getText().toString().trim();if(!value.matches("[0-9a-f]{64}")){status.setText("교대근무 앱에서 발급한 연결 코드를 입력해 주세요.");return;}if(!permissions()){new AlertDialog.Builder(this).setTitle("알람 권한이 필요합니다").setMessage("계정 연결 전에 알림과 정확한 알람 권한을 허용해 주세요. 권한 설정 후 내 계정 연결을 다시 눌러 주세요.").setPositiveButton("권한 설정",(d,w)->permissionSettings()).setNegativeButton("닫기",null).show();return;}background(()->{org.json.JSONObject d=AlarmStore.api("exchange",value);AlarmStore.stop(this);AlarmStore.prefs(this).edit().putString("token",d.getString("token")).putString("name",d.getString("name")).putBoolean("enabled",true).remove("alarms").commit();AlarmStore.jobs(this);return AlarmStore.sync(this);});code.setText("");});
  enabled=new Switch(this);enabled.setText("근무 기상 알람 켜기");box.addView(enabled);enabled.setOnCheckedChangeListener((b,on)->{if(refreshing)return;if(on){if(AlarmStore.prefs(this).getString("token","").isEmpty()||!permissions()){status.setText("계정 연결과 알람 권한을 먼저 확인해 주세요.");refresh();return;}AlarmStore.prefs(this).edit().putBoolean("enabled",true).commit();AlarmStore.restore(this);background(()->AlarmStore.sync(this));}else{AlarmStore.stop(this);refresh();}});
  button(box,"주간·주간 대근 시간 변경",()->pickTime(false));
  button(box,"야간·야간 대근 시간 변경",()->pickTime(true));
  button(box,"지금 일정 동기화",()->background(()->AlarmStore.sync(this)));
  button(box,"예약 일정·알람 상태 확인",()->showSchedule());
  button(box,"알림·정확한 알람 권한",this::permissionSettings);
  button(box,"알람 작동 점검",this::diagnostics);
  button(box,"배터리·백그라운드 설정",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
  button(box,"2분 뒤 테스트 알람",()->{if(!permissions()){permissionSettings();return;}AlarmStore.set(this,AlarmStore.TEST,System.currentTimeMillis()+120000L,"2분 테스트 알람");status.setText("2분 뒤 테스트 알람을 등록했습니다.");});
  button(box,"테스트 알람 취소",()->{AlarmStore.cancel(this,AlarmStore.TEST);status.setText("테스트 알람을 취소했습니다.");});
  button(box,"울리는 알람 끄기",()->stopService(new Intent(this,RingService.class)));
  button(box,"5분 미루기",()->{AlarmStore.set(this,AlarmStore.SNOOZE,System.currentTimeMillis()+300000L,"5분 뒤 다시 알림");stopService(new Intent(this,RingService.class));status.setText("5분 뒤 다시 울립니다.");});
  button(box,"이 기기 연결 해제",()->new AlertDialog.Builder(this).setMessage("이 기기의 근무 기상 알람과 계정 연결을 해제할까요?").setPositiveButton("해제",(d,w)->{AlarmStore.stop(this);AlarmStore.prefs(this).edit().clear().commit();AlarmStore.jobs(this);refresh();}).setNegativeButton("취소",null).show());
  status=text(box,"",16);box.removeView(status);box.addView(status,2);text(box,"일정은 앱을 열 때와 백그라운드에서 주기적으로 갱신합니다. 절전·인터넷 상태에 따라 갱신이 늦을 수 있으니 급한 일정 변경 후에는 ‘지금 일정 동기화’를 눌러 주세요.\n인터넷 연결 실패 시 마지막으로 받은 31일 일정을 유지합니다. 휴대폰 전원 끄기·앱 강제 종료·알람 권한 해제 상태에서는 울리지 않을 수 있습니다.\n이 앱의 기상 알람은 삼성 시계 및 교대근무 앱의 공지 알림과 별개입니다.",14);
 }
 @Override public void onResume(){super.onResume();AlarmStore.restore(this);AlarmStore.jobs(this);refresh();if(!AlarmStore.prefs(this).getString("token","").isEmpty())background(()->AlarmStore.sync(this));}
 private boolean permissions(){return AlarmStore.exact(this)&&(Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==getPackageManager().PERMISSION_GRANTED)&&(Build.VERSION.SDK_INT<24||getSystemService(NotificationManager.class).areNotificationsEnabled());}
 private void permissionSettings(){if(Build.VERSION.SDK_INT>=31&&!AlarmStore.exact(this)){startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));return;}if(Build.VERSION.SDK_INT>=24&&!getSystemService(NotificationManager.class).areNotificationsEnabled()){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));return;}if(Build.VERSION.SDK_INT>=34&&!getSystemService(NotificationManager.class).canUseFullScreenIntent()){startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())));return;}status.setText("알람 권한이 허용되어 있습니다. 휴대폰 설정에서 알람 음량도 확인해 주세요.");}
 private void refresh(){if(status==null)return;refreshing=true;boolean on=AlarmStore.prefs(this).getBoolean("enabled",false);enabled.setChecked(on);refreshing=false;long synced=AlarmStore.prefs(this).getLong("synced",0);String last=synced==0?"아직 없음":new SimpleDateFormat("MM/dd HH:mm",Locale.KOREA).format(new Date(synced));status.setText("설정 시간: 주간 "+AlarmStore.wakeTime(this,false)+" / 야간 "+AlarmStore.wakeTime(this,true)+"\n기상 알람: "+(on?(AlarmStore.exact(this)?"켜짐":"권한 필요 · 예약 불가"):"꺼짐")+"\n향후 알람 일정: "+AlarmStore.countFuture(this)+"개 (기기에 다음 1개 예약)\n연결 계정: "+AlarmStore.prefs(this).getString("name","미연결")+"\n다음 알람: "+AlarmStore.prefs(this).getString("next","없음")+"\n최근 동기화: "+last+"\n"+AlarmStore.prefs(this).getString("error","")+(permissions()?"":"\n알림·정확한 알람 권한을 확인해 주세요."));}
 private void showSchedule(){try{org.json.JSONArray a=AlarmStore.plan(this);StringBuilder b=new StringBuilder();int n=0;for(int i=0;i<a.length();i++){org.json.JSONObject x=a.getJSONObject(i);if(x.getLong("at")<=System.currentTimeMillis())continue;b.append(x.getString("date")).append("  ").append(x.getString("time")).append("  ").append(x.getString("shift")).append("  ").append(x.getString("source")).append("\n");n++;}if(n==0)b.append("예정된 알람이 없습니다.\n휴무일이거나 아직 동기화되지 않았습니다.");new AlertDialog.Builder(this).setTitle("예약 일정·알람 상태").setMessage(b.toString()).setPositiveButton("확인",null).show();}catch(Exception e){new AlertDialog.Builder(this).setTitle("예약 일정").setMessage("일정 정보를 읽을 수 없습니다. 먼저 동기화해 주세요.").setPositiveButton("확인",null).show();}}

 private void diagnostics(){
 NotificationManager nm=getSystemService(NotificationManager.class);
 android.media.AudioManager audio=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);
 PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
 StringBuilder b=new StringBuilder();
 b.append("계정 연결: ").append(AlarmStore.prefs(this).getString("token","").isEmpty()?"필요":"유지 중");
 b.append("\n근무 알람: ").append(AlarmStore.prefs(this).getBoolean("enabled",false)?"켜짐":"꺼짐");
 b.append("\n정확한 알람 권한: ").append(AlarmStore.exact(this)?"허용":"필요");
 b.append("\n앱 알림: ").append(permissions()?"허용":"권한 확인 필요");
 if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=nm.getNotificationChannel("wake_alarm_v3");b.append("\n알람 알림 채널: ").append(ch==null?"첫 테스트 시 생성":ch.getImportance()==NotificationManager.IMPORTANCE_NONE?"차단됨":ch.getImportance()<NotificationManager.IMPORTANCE_HIGH?"중요도 낮음 · 화면 표시 제한":"높음");}
 if(Build.VERSION.SDK_INT>=34)b.append("\n전체 화면: ").append(nm.canUseFullScreenIntent()?"허용":"필요 · 소리와 별개");
 b.append("\n알람 음량: ").append(audio.getStreamVolume(android.media.AudioManager.STREAM_ALARM)).append("/").append(audio.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM));
 b.append("\n배터리 최적화: ").append(power.isIgnoringBatteryOptimizations(getPackageName())?"제외됨":"적용 중 · 일정 갱신 지연 가능");
 if(Build.VERSION.SDK_INT>=28)b.append("\n백그라운드 제한: ").append(getSystemService(ActivityManager.class).isBackgroundRestricted()?"제한됨":"제한 없음");
 long last=AlarmStore.prefs(this).getLong("synced",0);
 b.append("\n일정 동기화: ").append(last==0?"아직 없음":(System.currentTimeMillis()-last)/3600000+"시간 전");
 b.append("\n다음 알람: ").append(AlarmStore.prefs(this).getString("next","없음"));
 b.append("\n최근 소리 오류: ").append(AlarmStore.prefs(this).getString("sound_error","없음"));
 b.append("\n\n전원 꺼짐·강제 종료 상태에서는 앱이 복구할 수 없습니다. 설정 변경 후 앱을 열고 잠금 상태에서 테스트하세요.");
 new AlertDialog.Builder(this).setTitle("알람 작동 점검").setMessage(b.toString()).setPositiveButton("확인",null).setNeutralButton("알림 설정",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).show();
 }

 private void pickTime(boolean night){String[] t=AlarmStore.wakeTime(this,night).split(":");new TimePickerDialog(this,(v,h,m)->{String value=String.format(Locale.US,"%02d:%02d",h,m);AlarmStore.prefs(this).edit().putString(night?"night_time":"day_time",value).commit();AlarmStore.restore(this);refresh();status.append("\n시간을 저장했습니다. 이미 지난 시각은 울리지 않습니다.");},Integer.parseInt(t[0]),Integer.parseInt(t[1]),true).show();}
 interface Task{String run()throws Exception;}
 private void background(Task task){status.setText("일정을 확인하는 중입니다…");new Thread(()->{String message;try{message=task.run();}catch(Exception e){message=e.getMessage();}final String result=message;runOnUiThread(()->{if(isFinishing()||isDestroyed())return;refresh();status.append("\n"+result);});}).start();}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView text(LinearLayout box,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.rgb(28,35,51));t.setPadding(0,dp(10),0,dp(10));box.addView(t);return t;}
 private void button(LinearLayout box,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(Color.rgb(30,58,110));box.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->action.run());}
}
