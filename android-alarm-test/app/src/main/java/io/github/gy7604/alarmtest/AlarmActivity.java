package io.github.gy7604.alarmtest;

import android.app.Activity;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

public class AlarmActivity extends Activity {
 private static final int NAVY=Color.rgb(19,39,74);
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable watch=new Runnable(){
  @Override public void run(){
   if(!RingService.ringing){finish();return;}
   handler.postDelayed(this,500);
  }
 };

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
  else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  getWindow().setStatusBarColor(NAVY);
  getWindow().setNavigationBarColor(NAVY);
  getWindow().getDecorView().setSystemUiVisibility(0);

  ScrollView scroll=new ScrollView(this);
  scroll.setFillViewport(true);
  scroll.setVerticalScrollBarEnabled(false);
  LinearLayout box=new LinearLayout(this);
  box.setOrientation(LinearLayout.VERTICAL);
  box.setGravity(Gravity.CENTER);
  box.setPadding(dp(20),dp(44),dp(20),dp(44));
  GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
   new int[]{Color.rgb(31,70,124),NAVY,Color.rgb(12,29,56)});
  box.setBackground(background);
  scroll.addView(box,new ScrollView.LayoutParams(-1,-2));
  setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,i)->{
   if(Build.VERSION.SDK_INT>=30){
    android.graphics.Insets z=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
    v.setPadding(z.left,z.top,z.right,z.bottom);
   }else{
    v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());
   }
   return i;
  });

  TextView eyebrow=text("교대 기상 알람",15,Color.rgb(197,219,253),true);
  eyebrow.setGravity(Gravity.CENTER);
  box.addView(eyebrow);

  TextClock clock=new TextClock(this);
  clock.setFormat24Hour("HH:mm");
  clock.setFormat12Hour("HH:mm");
  clock.setTextSize(72);
  clock.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  clock.setTextColor(Color.WHITE);
  clock.setGravity(Gravity.CENTER);
  LinearLayout.LayoutParams clockLp=new LinearLayout.LayoutParams(-1,-2);
  clockLp.topMargin=dp(26);
  box.addView(clock,clockLp);

  TextView title=text("일어날 시간입니다",27,Color.WHITE,true);
  title.setGravity(Gravity.CENTER);
  addBelow(box,title,13);
  String received=getIntent()==null?null:getIntent().getStringExtra("label");
  TextView detail=text(received==null||received.trim().isEmpty()?"설정한 교대근무 기상 알람이 울리고 있습니다.":received,
   16,Color.rgb(216,230,251),false);
  detail.setGravity(Gravity.CENTER);
  detail.setLineSpacing(dp(5),1f);
  addBelow(box,detail,16);

  LinearLayout actions=new LinearLayout(this);
  actions.setOrientation(LinearLayout.HORIZONTAL);
  actions.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout stop=action("■","알람 끄기","지금 종료",Color.rgb(185,63,80));
  LinearLayout snooze=action("Zz","5분 미루기","다시 울림",Color.rgb(50,111,207));
  actions.addView(stop,new LinearLayout.LayoutParams(0,dp(137),1));
  actions.addView(new View(this),new LinearLayout.LayoutParams(dp(12),1));
  actions.addView(snooze,new LinearLayout.LayoutParams(0,dp(137),1));
  LinearLayout.LayoutParams actionsLp=new LinearLayout.LayoutParams(-1,-2);
  actionsLp.topMargin=dp(42);
  box.addView(actions,actionsLp);

  stop.setOnClickListener(v->{stopService(new Intent(this,RingService.class));finish();});
  snooze.setOnClickListener(v->{
   if(!AlarmStore.exact(this)){
    detail.setText("정확한 알람 권한이 없어 미룰 수 없습니다.\n알람 끄기를 눌러 주세요.");
    return;
   }
   AlarmStore.set(this,AlarmStore.SNOOZE,System.currentTimeMillis()+300000L,"5분 뒤 다시 알림");
   stopService(new Intent(this,RingService.class));
   finish();
  });
  TextView hint=text("알람 끄기는 현재 울림을 종료하고,\n5분 미루기는 5분 후 다시 울립니다.",13,Color.rgb(173,195,226),false);
  hint.setGravity(Gravity.CENTER);
  addBelow(box,hint,25);
 }

 private LinearLayout action(String icon,String title,String subtitle,int background){
  LinearLayout tile=new LinearLayout(this);
  tile.setOrientation(LinearLayout.VERTICAL);
  tile.setGravity(Gravity.CENTER);
  tile.setPadding(dp(6),dp(12),dp(6),dp(12));
  GradientDrawable shape=new GradientDrawable();
  shape.setColor(background);
  shape.setCornerRadius(dp(20));
  tile.setBackground(new RippleDrawable(ColorStateList.valueOf(0x55FFFFFF),shape,null));
  TextView symbol=text(icon,24,Color.WHITE,true);
  symbol.setGravity(Gravity.CENTER);
  tile.addView(symbol);
  TextView label=text(title,18,Color.WHITE,true);
  label.setGravity(Gravity.CENTER);
  addBelow(tile,label,10);
  TextView description=text(subtitle,12,Color.rgb(233,240,255),false);
  description.setGravity(Gravity.CENTER);
  addBelow(tile,description,6);
  tile.setClickable(true);
  tile.setFocusable(true);
  tile.setContentDescription(title+" · "+subtitle);
  return tile;
 }
 private TextView text(String value,int size,int color,boolean bold){
  TextView view=new TextView(this);
  view.setText(value);
  view.setTextSize(size);
  view.setTextColor(color);
  view.setIncludeFontPadding(false);
  if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  return view;
 }
 private void addBelow(LinearLayout parent,View child,int margin){
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
  lp.topMargin=dp(margin);
  parent.addView(child,lp);
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 @Override protected void onResume(){super.onResume();handler.postDelayed(watch,500);}
 @Override protected void onPause(){handler.removeCallbacks(watch);super.onPause();}
}
