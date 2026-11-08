package io.github.gy7604.alarmtest;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {
 private static final int PAGE=Color.rgb(246,248,252);
 private static final int INK=Color.rgb(26,40,63);
 private static final int MUTED=Color.rgb(105,119,140);
 private static final int BLUE=Color.rgb(48,101,210);
 private static final int PALE_BLUE=Color.rgb(233,241,255);
 private static final int HERO=Color.rgb(22,42,78);
 private static final int WHITE=Color.WHITE;
 private static final int RED=Color.rgb(193,65,73);
 private TextView status,heroBadge,nextClock,nextDescription,dayTime,nightTime;
 private TextView planCount,syncState,accountName,permissionDescription,issueMessage,testPending;
 private Switch enabled;
 private EditText code;
 private LinearLayout accountSummary,accountForm,issueCard;
 private View testCancel;
 private boolean refreshing,editingConnection;

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=getPackageManager().PERMISSION_GRANTED)
   requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);
  getWindow().setStatusBarColor(PAGE);
  getWindow().setNavigationBarColor(PAGE);
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|(Build.VERSION.SDK_INT>=26?View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0));

  ScrollView scroll=new ScrollView(this);
  scroll.setBackgroundColor(PAGE);
  scroll.setFillViewport(true);
  scroll.setVerticalScrollBarEnabled(false);
  scroll.setClipToPadding(false);
  LinearLayout root=column();
  root.setPadding(dp(16),dp(18),dp(16),dp(36));
  scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
  setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,i)->{
   if(Build.VERSION.SDK_INT>=30){
    android.graphics.Insets z=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
    v.setPadding(z.left,z.top,z.right,z.bottom);
   }
   return i;
  });

  LinearLayout heading=row();
  LinearLayout iconFrame=column();
  iconFrame.setGravity(Gravity.CENTER);
  iconFrame.setBackground(round(PALE_BLUE,15));
  ImageView icon=new ImageView(this);
  icon.setImageResource(R.drawable.ic_shift_alarm);
  icon.setContentDescription("교대 기상 알람");
  iconFrame.addView(icon,new LinearLayout.LayoutParams(dp(32),dp(32)));
  heading.addView(iconFrame,new LinearLayout.LayoutParams(dp(50),dp(50)));
  LinearLayout titles=column();
  titles.addView(label("교대 기상 알람",23,INK,true));
  below(titles,label("내 근무에 맞춰 울리는 기상 알람",13,MUTED,false),5);
  LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(0,-2,1);
  titleLp.leftMargin=dp(12);
  heading.addView(titles,titleLp);
  root.addView(heading);

  LinearLayout hero=card(HERO,24);
  hero.setPadding(dp(20),dp(20),dp(20),dp(19));
  GradientDrawable heroBackground=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(31,65,118),HERO});
  heroBackground.setCornerRadius(dp(24));
  hero.setBackground(heroBackground);
  LinearLayout heroHead=row();
  TextView nextLabel=label("다음 기상 알람",14,Color.rgb(221,233,255),true);
  heroHead.addView(nextLabel,new LinearLayout.LayoutParams(0,-2,1));
  heroBadge=label("확인 중",12,WHITE,true);
  heroBadge.setGravity(Gravity.CENTER);
  heroBadge.setPadding(dp(11),dp(6),dp(11),dp(6));
  heroHead.addView(heroBadge);
  hero.addView(heroHead);

  nextClock=label("—",40,WHITE,true);
  below(hero,nextClock,19);
  nextDescription=label("근무 일정을 확인하고 있습니다.",14,Color.rgb(215,226,246),false);
  below(hero,nextDescription,5);
  LinearLayout switchRow=row();
  switchRow.setPadding(0,dp(15),0,dp(14));
  enabled=new Switch(this);
  enabled.setText("기상 알람 사용");
  enabled.setTextSize(16);
  enabled.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  enabled.setTextColor(WHITE);
  enabled.setThumbTintList(ColorStateList.valueOf(WHITE));
  enabled.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Color.rgb(91,160,247),Color.rgb(117,132,161)}));
  switchRow.addView(enabled,new LinearLayout.LayoutParams(-1,-2));
  below(hero,switchRow,12);
  View heroDivider=new View(this);
  heroDivider.setBackgroundColor(Color.rgb(77,103,145));
  hero.addView(heroDivider,new LinearLayout.LayoutParams(-1,dp(1)));
  LinearLayout facts=row();
  syncState=label("최근 동기화 —",12,Color.rgb(214,227,250),false);
  facts.addView(syncState,new LinearLayout.LayoutParams(0,-2,1));
  planCount=label("예정 0건",12,Color.rgb(214,227,250),true);
  facts.addView(planCount);
  below(hero,facts,14);
  TextView seeSchedule=label("예약 일정 전체 보기  ›",13,WHITE,true);
  seeSchedule.setGravity(Gravity.RIGHT);
  seeSchedule.setPadding(dp(4),dp(10),0,dp(3));
  seeSchedule.setContentDescription("예약된 알람 일정 보기");
  seeSchedule.setOnClickListener(v->showSchedule());
  below(hero,seeSchedule,5);
  below(root,hero,18);

  issueCard=card(Color.rgb(255,246,229),16);
  issueCard.setPadding(dp(15),dp(13),dp(15),dp(13));
  issueMessage=label("",13,Color.rgb(133,84,22),true);
  issueCard.addView(issueMessage);
  issueCard.setVisibility(View.GONE);
  below(root,issueCard,10);

  status=label("",13,BLUE,false);
  status.setPadding(dp(12),dp(11),dp(12),dp(11));
  status.setBackground(round(PALE_BLUE,12));
  status.setVisibility(View.GONE);
  below(root,status,10);

  section(root,"기상 시간","시간을 누르면 바로 변경할 수 있습니다.",23);
  LinearLayout times=row();
  LinearLayout dayCard=timeCard("☀","주간 · 주간 대근",false);
  LinearLayout nightCard=timeCard("☾","야간 · 야간 대근",true);
  addPair(times,dayCard,nightCard,12);
  root.addView(times);

  section(root,"빠른 실행","자주 사용하는 기능만 모았습니다.",24);
  LinearLayout quick1=row();
  addPair(quick1,quickAction("↻","일정 동기화","최신 근무표 받기",()->background(()->AlarmStore.sync(this))),
   quickAction("▦","예약 일정","앞으로 울릴 알람",this::showSchedule),12);
  root.addView(quick1);
  LinearLayout quick2=row();
  addPair(quick2,quickAction("▶","1분 테스트","실제 알람 울림 확인",this::scheduleTest),
   quickAction("✓","작동 점검","권한·음량 확인",this::diagnostics),12);
  below(root,quick2,12);
  LinearLayout cancelLine=card(PALE_BLUE,14);
  cancelLine.setPadding(dp(15),dp(12),dp(15),dp(12));
  testPending=label("예약된 테스트 알람 취소  ›",14,BLUE,true);
  cancelLine.addView(testPending);
  cancelLine.setOnClickListener(v->cancelTest());
  testCancel=cancelLine;
  testCancel.setVisibility(View.GONE);
  below(root,testCancel,10);

  section(root,"계정 연결","교대근무 앱과 연결하여 근무표를 가져옵니다.",24);
  LinearLayout accountCard=card(WHITE,21);
  accountSummary=column();
  TextView connectedLabel=label("연결된 계정",12,MUTED,false);
  accountSummary.addView(connectedLabel);
  accountName=label("—",20,INK,true);
  below(accountSummary,accountName,7);
  LinearLayout accountActions=row();
  TextView change=linkLabel("연결 코드 변경",BLUE);
  change.setOnClickListener(v->{editingConnection=true;refresh();note("새로 발급받은 연결 코드를 입력해 주세요.");});
  accountActions.addView(change,new LinearLayout.LayoutParams(0,-2,1));
  TextView disconnect=linkLabel("연결 해제",RED);
  disconnect.setOnClickListener(v->confirmDetach());
  accountActions.addView(disconnect);
  below(accountSummary,accountActions,12);
  accountCard.addView(accountSummary);

  accountForm=column();
  TextView link=label("교대근무 앱에서 연결 코드 발급하기  ↗",14,BLUE,true);
  link.setPadding(0,dp(7),0,dp(13));
  link.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://gy7604.github.io/index.html"))));
  accountForm.addView(link);
  code=new EditText(this);
  code.setSingleLine(true);
  code.setTextSize(14);
  code.setTextColor(INK);
  code.setHint("발급받은 연결 코드 붙여넣기");
  code.setHintTextColor(MUTED);
  code.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
  code.setPadding(dp(13),0,dp(13),0);
  code.setMinHeight(dp(52));
  code.setBackground(round(Color.rgb(241,245,251),13));
  if(Build.VERSION.SDK_INT>=26)code.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
  accountForm.addView(code,new LinearLayout.LayoutParams(-1,dp(52)));
  TextView connect=solidAction("내 계정 연결",true);
  connect.setOnClickListener(v->connectAccount());
  below(accountForm,connect,12);
  accountCard.addView(accountForm);
  root.addView(accountCard);

  section(root,"설정 및 관리","필요할 때만 확인하면 되는 항목입니다.",24);
  LinearLayout settingsCard=card(WHITE,21);
  settingsCard.setPadding(dp(8),dp(4),dp(8),dp(4));
  permissionDescription=label("",12,MUTED,false);
  settingsRow(settingsCard,"알람 권한",permissionDescription,this::permissionSettings);
  divider(settingsCard);
  settingsRow(settingsCard,"배터리 · 백그라운드","절전 및 실행 제한 설정",()->openPermission(Settings.ACTION_APPLICATION_DETAILS_SETTINGS));
  divider(settingsCard);
  settingsRow(settingsCard,"업데이트 확인","새 버전 확인 및 설치",()->UpdateCheck.check(this,true));
  divider(settingsCard);
  settingsRow(settingsCard,"추가 알람 제어","테스트 취소 · 울림 끄기 · 5분 미루기",this::extraControls);
  root.addView(settingsCard);

  TextView foot=label("일정은 앱을 열 때와 백그라운드에서 갱신되며, 통신이 실패하면 마지막으로 받은 31일 일정을 유지합니다. 급한 근무 변경 후에는 일정 동기화를 실행해 주세요.\n\n기상 알람은 삼성 시계 알람 및 교대근무 앱의 공지 알림과 별도로 작동합니다. 전원 끄기·앱 강제 종료·권한 해제 상태에서는 울리지 않을 수 있습니다.",12,MUTED,false);
  foot.setLineSpacing(dp(3),1f);
  below(root,foot,22);
  String version="";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
  TextView versionLabel=label("교대 기상 알람  ·  v"+version,12,MUTED,false);
  versionLabel.setGravity(Gravity.CENTER);
  below(root,versionLabel,17);

  enabled.setOnCheckedChangeListener((v,on)->{
   if(refreshing)return;
   if(on){
    if(AlarmStore.prefs(this).getString("token","").isEmpty()||!permissions()){
     note("알람을 켜려면 계정 연결과 알람 권한이 필요합니다.");
     refresh();
     return;
    }
    AlarmStore.prefs(this).edit().putBoolean("enabled",true).commit();
    AlarmStore.restore(this);
    refresh();
    background(()->AlarmStore.sync(this));
   }else{
    AlarmStore.stop(this);
    refresh();
    note("기상 알람을 껐습니다.");
   }
  });
  UpdateCheck.check(this,false);
 }

 @Override public void onResume(){
  super.onResume();
  AlarmStore.restore(this);
  AlarmStore.jobs(this);
  if(enabled!=null)refresh();
  if(!AlarmStore.prefs(this).getString("token","").isEmpty()&&enabled!=null)
   background(()->AlarmStore.sync(this),false);
 }

 private LinearLayout timeCard(String glyph,String title,boolean night){
  LinearLayout tile=card(WHITE,20);
  tile.setBackground(clickBackground(WHITE,20));
  tile.setMinimumHeight(dp(147));
  TextView caption=label(glyph+"  "+title,12,MUTED,true);
  tile.addView(caption);
  TextView time=label("00:00",28,INK,true);
  below(tile,time,15);
  if(night)nightTime=time;else dayTime=time;
  below(tile,label("시간 변경  ›",12,BLUE,true),9);
  tile.setContentDescription(title+" 기상 시간 변경");
  tile.setOnClickListener(v->pickTime(night));
  tile.setFocusable(true);
  return tile;
 }

 private LinearLayout quickAction(String icon,String title,String description,Runnable action){
  LinearLayout tile=card(WHITE,20);
  tile.setBackground(clickBackground(WHITE,20));
  tile.setMinimumHeight(dp(143));
  TextView symbol=label(icon,23,BLUE,true);
  symbol.setGravity(Gravity.CENTER);
  symbol.setBackground(round(PALE_BLUE,12));
  tile.addView(symbol,new LinearLayout.LayoutParams(dp(42),dp(42)));
  below(tile,label(title,16,INK,true),12);
  below(tile,label(description,12,MUTED,false),5);
  tile.setContentDescription(title+" · "+description);
  tile.setOnClickListener(v->action.run());
  tile.setFocusable(true);
  return tile;
 }

 private void section(LinearLayout parent,String title,String subtitle,int top){
  TextView heading=label(title,19,INK,true);
  below(parent,heading,top);
  TextView help=label(subtitle,12,MUTED,false);
  below(parent,help,5);
  View spacing=new View(this);
  parent.addView(spacing,new LinearLayout.LayoutParams(1,dp(13)));
 }

 private void settingsRow(LinearLayout parent,String title,String detail,Runnable action){
  settingsRow(parent,title,label(detail,12,MUTED,false),action);
 }
 private void settingsRow(LinearLayout parent,String title,TextView detail,Runnable action){
  LinearLayout item=row();
  item.setPadding(dp(10),dp(13),dp(10),dp(13));
  item.setMinimumHeight(dp(73));
  item.setBackground(clickBackground(WHITE,12));
  LinearLayout info=column();
  info.addView(label(title,15,INK,true));
  below(info,detail,6);
  item.addView(info,new LinearLayout.LayoutParams(0,-2,1));
  TextView arrow=label("›",26,MUTED,false);
  arrow.setPadding(dp(9),0,dp(2),0);
  item.addView(arrow);
  item.setContentDescription(title);
  item.setOnClickListener(v->action.run());
  item.setFocusable(true);
  parent.addView(item);
 }
 private void divider(LinearLayout parent){
  View v=new View(this);
  v.setBackgroundColor(Color.rgb(237,240,245));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));
  lp.leftMargin=dp(12);lp.rightMargin=dp(12);
  parent.addView(v,lp);
 }
 private void addPair(LinearLayout row,View a,View b,int gap){
  row.addView(a,new LinearLayout.LayoutParams(0,-2,1f));
  View spacer=new View(this);
  row.addView(spacer,new LinearLayout.LayoutParams(dp(gap),1));
  row.addView(b,new LinearLayout.LayoutParams(0,-2,1f));
 }
 private TextView solidAction(String text,boolean primary){
  TextView v=label(text,15,primary?WHITE:BLUE,true);
  v.setGravity(Gravity.CENTER);
  v.setMinHeight(dp(50));
  v.setBackground(clickBackground(primary?BLUE:PALE_BLUE,14));
  v.setFocusable(true);
  return v;
 }
 private TextView linkLabel(String text,int color){
  TextView v=label(text,13,color,true);
  v.setPadding(0,dp(8),dp(8),dp(8));
  v.setFocusable(true);
  return v;
 }

 private void connectAccount(){
  String value=code.getText().toString().trim();
  if(!value.matches("[0-9a-f]{64}")){note("교대근무 앱에서 발급한 64자리 연결 코드를 입력해 주세요.");return;}
  if(!permissions()){
   new AlertDialog.Builder(this).setTitle("알람 권한이 필요합니다")
    .setMessage("계정 연결 전에 알림과 정확한 알람 권한을 허용해 주세요.")
    .setPositiveButton("권한 설정",(d,w)->permissionSettings()).setNegativeButton("닫기",null).show();
   return;
  }
  background(()->{
   JSONObject response=AlarmStore.api("exchange",value);
   AlarmStore.attach(this,response.getString("token"),response.getString("name"));
   String message=AlarmStore.sync(this);
   runOnUiThread(()->{
    if(isFinishing()||isDestroyed())return;
    if(code.getText().toString().trim().equals(value))code.setText("");
    editingConnection=false;
    refresh();
   });
   return message;
  });
 }
 private void confirmDetach(){
  new AlertDialog.Builder(this).setTitle("기기 연결 해제")
   .setMessage("이 기기의 계정 연결과 기상 알람을 해제할까요? 저장된 연결 및 일정 정보가 이 기기에서 삭제됩니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("연결 해제",(d,w)->{
    AlarmStore.detach(this);
    editingConnection=false;
    refresh();
    note("이 기기의 연결을 해제했습니다.");
   }).show();
 }

 private void scheduleTest(){
  if(!permissions()){permissionSettings();return;}
  long at=System.currentTimeMillis()+60000L;
  AlarmStore.set(this,AlarmStore.TEST,at,"1분 테스트 알람");
  AlarmStore.prefs(this).edit().putLong("test_at",at).apply();
  refresh();
  note("1분 뒤 테스트 알람이 울리도록 예약했습니다.");
 }
 private void cancelTest(){
  AlarmStore.cancel(this,AlarmStore.TEST);
  AlarmStore.prefs(this).edit().remove("test_at").apply();
  refresh();
  note("테스트 알람을 취소했습니다.");
 }
 private void extraControls(){
  String[] actions={"테스트 알람 취소","울리는 알람 끄기","5분 미루기"};
  new AlertDialog.Builder(this).setTitle("추가 알람 제어")
   .setItems(actions,(d,which)->{
    if(which==0)cancelTest();
    else if(which==1){stopService(new Intent(this,RingService.class));note("울리는 알람을 정지했습니다.");}
    else{
     if(!AlarmStore.exact(this)){permissionSettings();return;}
     AlarmStore.set(this,AlarmStore.SNOOZE,System.currentTimeMillis()+300000L,"5분 뒤 다시 알림");
     stopService(new Intent(this,RingService.class));
     note("5분 뒤 다시 울리도록 예약했습니다.");
    }
   }).setNegativeButton("닫기",null).show();
 }

 private void refresh(){
  ShiftWidget.refresh(this);
  if(enabled==null||heroBadge==null)return;
  SharedPreferences p=AlarmStore.prefs(this);
  boolean connected=!p.getString("token","").isEmpty();
  boolean on=p.getBoolean("enabled",false);
  boolean permitted=permissions();
  refreshing=true;
  enabled.setChecked(on);
  enabled.setEnabled(connected);
  refreshing=false;
  dayTime.setText(AlarmStore.wakeTime(this,false));
  nightTime.setText(AlarmStore.wakeTime(this,true));
  accountName.setText(p.getString("name","미연결"));
  accountSummary.setVisibility(connected?View.VISIBLE:View.GONE);
  accountForm.setVisibility(!connected||editingConnection?View.VISIBLE:View.GONE);

  long last=p.getLong("synced",0);
  syncState.setText(last==0?"동기화 기록 없음":"최근 동기화 "+new SimpleDateFormat("MM/dd HH:mm",Locale.KOREA).format(new Date(last)));
  int count=AlarmStore.countFuture(this);
  planCount.setText("예정 "+count+"건");
  JSONObject upcoming=null;
  try{
   JSONArray rows=AlarmStore.plan(this);
   for(int n=0;n<rows.length();n++){
    JSONObject x=rows.getJSONObject(n);
    if(x.getLong("at")>System.currentTimeMillis()){upcoming=x;break;}
   }
  }catch(Exception ignored){}
  if(upcoming!=null){
   nextClock.setText(upcoming.optString("time","—"));
   nextDescription.setText(koreanDate(upcoming.optString("date",""))+" · "+upcoming.optString("shift","근무")+" · "+upcoming.optString("source","정규"));
  }else{
   nextClock.setText(connected?"예정 없음":"연결 필요");
   nextDescription.setText(connected?"근무 일정이 없거나 아직 동기화되지 않았습니다.":"교대근무 앱에서 연결 코드를 발급해 주세요.");
  }
  if(!connected)badge("미연결",Color.rgb(255,232,196),Color.rgb(113,80,51));
  else if(!on)badge("알람 꺼짐",Color.rgb(225,230,244),Color.rgb(79,95,123));
  else if(!permitted)badge("권한 확인",Color.rgb(255,228,174),Color.rgb(129,82,26));
  else if(upcoming==null)badge("일정 확인",Color.rgb(255,228,174),Color.rgb(129,82,26));
  else badge("정상 작동",Color.rgb(221,255,242),Color.rgb(21,105,78));

  NotificationManager manager=getSystemService(NotificationManager.class);
  boolean notificationAllowed=(Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==getPackageManager().PERMISSION_GRANTED)
   &&(Build.VERSION.SDK_INT<24||manager.areNotificationsEnabled());
  boolean exact=AlarmStore.exact(this);
  boolean full=(Build.VERSION.SDK_INT<34||manager.canUseFullScreenIntent());
  permissionDescription.setText(!notificationAllowed||!exact?"필수 권한 확인 필요":!full?"전체화면 표시 권한 확인 필요":"필수 알람 권한 허용됨");
  String issue=p.getString("error","");
  if(connected&&!permitted)issue="알람 권한이 필요합니다. 이 카드를 눌러 설정을 확인하세요."+(issue.isEmpty()?"":"\n"+issue);
  else if(connected&&!full)issue="전체화면 알람 표시 권한을 확인해 주세요."+(issue.isEmpty()?"":"\n"+issue);
  issueCard.setVisibility(issue.isEmpty()?View.GONE:View.VISIBLE);
  issueMessage.setText(issue);
  final boolean permissionIssue=(connected&&!permitted)||(connected&&!full);
  issueCard.setOnClickListener(v->{if(permissionIssue)permissionSettings();else background(()->AlarmStore.sync(this));});

  long testAt=p.getLong("test_at",0);
  testCancel.setVisibility(testAt>System.currentTimeMillis()?View.VISIBLE:View.GONE);
 }
 private String koreanDate(String value){
  try{
   SimpleDateFormat source=new SimpleDateFormat("yyyy-MM-dd",Locale.KOREA);
   source.setLenient(false);
   source.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
   SimpleDateFormat target=new SimpleDateFormat("M월 d일 (E)",Locale.KOREA);
   target.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
   return target.format(source.parse(value));
  }catch(Exception e){return value;}
 }
 private void badge(String text,int foreground,int background){
  heroBadge.setText(text);
  heroBadge.setTextColor(foreground);
  heroBadge.setBackground(round(background,30));
 }
 private void note(String message){
  if(status==null)return;
  status.setText(message==null?"":message);
  status.setVisibility(message==null||message.isEmpty()?View.GONE:View.VISIBLE);
 }

 private boolean permissions(){
  NotificationManager nm=getSystemService(NotificationManager.class);
  return AlarmStore.exact(this)
   &&(Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==getPackageManager().PERMISSION_GRANTED)
   &&(Build.VERSION.SDK_INT<24||nm.areNotificationsEnabled());
 }
 private void permissionSettings(){
  NotificationManager nm=getSystemService(NotificationManager.class);
  String exact=AlarmStore.exact(this)?"허용":"필요";
  String notifications=(Build.VERSION.SDK_INT<24||nm.areNotificationsEnabled())?"허용":"필요";
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=getPackageManager().PERMISSION_GRANTED)notifications="필요";
  String full=(Build.VERSION.SDK_INT<34||nm.canUseFullScreenIntent())?"허용":"필요";
  new AlertDialog.Builder(this).setTitle("알람 권한 설정")
   .setMessage("앱 알림: "+notifications+"\n정확한 알람: "+exact+"\n전체화면 알람: "+full+"\n\n확인이 필요한 권한을 아래 버튼으로 설정할 수 있습니다.")
   .setPositiveButton("전체화면 설정",(d,w)->openPermission(Build.VERSION.SDK_INT>=34?Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT:Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
   .setNeutralButton("정확한 알람",(d,w)->openPermission(Build.VERSION.SDK_INT>=31?Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM:Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
   .setNegativeButton("앱 알림",(d,w)->{
    try{
     if(Build.VERSION.SDK_INT>=26)startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
     else openPermission(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    }catch(Exception e){openPermission(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);}
   }).show();
 }
 private void openPermission(String action){
  try{startActivity(new Intent(action,Uri.parse("package:"+getPackageName())));}
  catch(Exception e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
 }

 private void showSchedule(){
  try{
   JSONArray rows=AlarmStore.plan(this);
   ScrollView scroll=new ScrollView(this);
   LinearLayout list=column();
   list.setPadding(dp(18),dp(10),dp(18),dp(12));
   scroll.addView(list);
   int count=0;
   for(int i=0;i<rows.length();i++){
    JSONObject x=rows.getJSONObject(i);
    if(x.getLong("at")<=System.currentTimeMillis())continue;
    LinearLayout item=column();
    item.setPadding(dp(13),dp(12),dp(13),dp(12));
    item.setBackground(round(count%2==0?Color.rgb(242,246,253):WHITE,13));
    item.addView(label(koreanDate(x.getString("date"))+"   "+x.getString("time"),16,INK,true));
    below(item,label(x.getString("shift")+" · "+x.getString("source"),13,MUTED,false),5);
    below(list,item,8);
    count++;
   }
   if(count==0)list.addView(label("예정된 알람이 없습니다.\n휴무일이거나 아직 동기화되지 않았습니다.",14,MUTED,false));
   new AlertDialog.Builder(this).setTitle("예약된 알람 · "+count+"건")
    .setView(scroll).setPositiveButton("확인",null).show();
  }catch(Exception e){
   new AlertDialog.Builder(this).setTitle("예약 일정").setMessage("일정 정보를 읽을 수 없습니다. 먼저 동기화해 주세요.").setPositiveButton("확인",null).show();
  }
 }
 private void diagnostics(){
  NotificationManager nm=getSystemService(NotificationManager.class);
  android.media.AudioManager audio=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);
  PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
  StringBuilder b=new StringBuilder();
  b.append("계정 연결: ").append(AlarmStore.prefs(this).getString("token","").isEmpty()?"필요":"유지 중");
  b.append("\n근무 알람: ").append(AlarmStore.prefs(this).getBoolean("enabled",false)?"켜짐":"꺼짐");
  b.append("\n정확한 알람 권한: ").append(AlarmStore.exact(this)?"허용":"필요");
  boolean notifications=(Build.VERSION.SDK_INT<24||nm.areNotificationsEnabled())&&(Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==getPackageManager().PERMISSION_GRANTED);
  b.append("\n앱 알림: ").append(notifications?"허용":"권한 확인 필요");
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel ch=nm.getNotificationChannel("wake_alarm_v3");
   b.append("\n알람 알림 채널: ").append(ch==null?"첫 테스트 시 생성":ch.getImportance()==NotificationManager.IMPORTANCE_NONE?"차단됨":ch.getImportance()<NotificationManager.IMPORTANCE_HIGH?"중요도 낮음 · 화면 표시 제한":"높음");
  }
  if(Build.VERSION.SDK_INT>=34)b.append("\n전체 화면: ").append(nm.canUseFullScreenIntent()?"허용":"필요 · 소리와 별개");
  b.append("\n알람 음량: ").append(audio.getStreamVolume(android.media.AudioManager.STREAM_ALARM)).append("/").append(audio.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM));
  b.append("\n배터리 최적화: ").append(power.isIgnoringBatteryOptimizations(getPackageName())?"제외됨":"적용 중 · 일정 갱신 지연 가능");
  if(Build.VERSION.SDK_INT>=28)b.append("\n백그라운드 제한: ").append(getSystemService(ActivityManager.class).isBackgroundRestricted()?"제한됨":"제한 없음");
  long last=AlarmStore.prefs(this).getLong("synced",0);
  b.append("\n일정 동기화: ").append(last==0?"아직 없음":(System.currentTimeMillis()-last)/3600000+"시간 전");
  b.append("\n다음 알람: ").append(AlarmStore.prefs(this).getString("next","없음"));
  b.append("\n최근 소리 오류: ").append(AlarmStore.prefs(this).getString("sound_error","없음"));
  b.append("\n\n전원 꺼짐·강제 종료 상태에서는 앱이 복구할 수 없습니다. 설정 변경 후 앱을 열고 잠금 상태에서 테스트하세요.");
  new AlertDialog.Builder(this).setTitle("알람 작동 점검").setMessage(b.toString())
   .setPositiveButton("확인",null)
   .setNeutralButton("알림 설정",(d,w)->openPermission(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)).show();
 }
 private void pickTime(boolean night){
  String[] t=AlarmStore.wakeTime(this,night).split(":");
  new TimePickerDialog(this,(v,h,m)->{
   String value=String.format(Locale.US,"%02d:%02d",h,m);
   AlarmStore.prefs(this).edit().putString(night?"night_time":"day_time",value).commit();
   AlarmStore.restore(this);
   refresh();
   note((night?"야간":"주간")+" 기상 시간을 "+value+"으로 변경했습니다.");
  },Integer.parseInt(t[0]),Integer.parseInt(t[1]),true).show();
 }
 private interface Task{String run()throws Exception;}
 private void background(Task task){background(task,true);}
 private void background(Task task,boolean visible){
  if(visible)note("일정을 확인하는 중입니다…");
  new Thread(()->{
   String message;boolean failed=false;
   try{message=task.run();}
   catch(Exception e){failed=true;message=e.getMessage()==null?"처리 중 오류가 발생했습니다.":e.getMessage();}
   final boolean error=failed;
   final String result=message;
   runOnUiThread(()->{
    if(isFinishing()||isDestroyed())return;
    refresh();
    if(visible||error)note(result);
   });
  }).start();
 }

 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
 private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 private LinearLayout card(int color,int radius){
  LinearLayout v=column();v.setPadding(dp(17),dp(17),dp(17),dp(17));
  v.setBackground(round(color,radius));return v;
 }
 private GradientDrawable round(int color,int radius){
  GradientDrawable d=new GradientDrawable();
  d.setColor(color);d.setCornerRadius(dp(radius));return d;
 }
 private RippleDrawable clickBackground(int color,int radius){
  return new RippleDrawable(ColorStateList.valueOf(0x283B77CC),round(color,radius),null);
 }
 private TextView label(String text,float size,int color,boolean bold){
  TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);
  v.setIncludeFontPadding(false);
  if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  return v;
 }
 private void below(LinearLayout parent,View child,int margin){
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
  lp.topMargin=dp(margin);
  parent.addView(child,lp);
 }
}
