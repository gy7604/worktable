package io.github.gy7604.alarmtest;
import android.app.*;import android.appwidget.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.widget.RemoteViews;import org.json.*;import java.text.*;import java.util.*;
public class ShiftWidget extends AppWidgetProvider {
 static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,ShiftWidget.class)))render(c,m,id);}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)render(c,m,id);AlarmStore.jobs(c);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle b){render(c,m,id);}
 static void render(Context c,AppWidgetManager m,int id){
 RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.shift_widget);
 int[] dates={R.id.date0,R.id.date1,R.id.date2,R.id.date3,R.id.date4,R.id.date5,R.id.date6};
 int[][] cells={{R.id.a0,R.id.a1,R.id.a2,R.id.a3,R.id.a4,R.id.a5,R.id.a6},{R.id.b0,R.id.b1,R.id.b2,R.id.b3,R.id.b4,R.id.b5,R.id.b6},{R.id.c0,R.id.c1,R.id.c2,R.id.c3,R.id.c4,R.id.c5,R.id.c6},{R.id.d0,R.id.d1,R.id.d2,R.id.d3,R.id.d4,R.id.d5,R.id.d6}};
 boolean connected=!AlarmStore.prefs(c).getString("token","").isEmpty();
 TimeZone zone=TimeZone.getTimeZone("Asia/Seoul");Calendar cal=Calendar.getInstance(zone);
 SimpleDateFormat key=new SimpleDateFormat("yyyy-MM-dd",Locale.US);key.setTimeZone(zone);
 SimpleDateFormat label=new SimpleDateFormat("M/d\nE",Locale.KOREA);label.setTimeZone(zone);
 JSONArray personal;try{personal=new JSONArray(AlarmStore.prefs(c).getString("workdays","[]"));}catch(JSONException e){personal=new JSONArray();}
 for(int n=0;n<7;n++){
 String date=key.format(cal.getTime());String heading=label.format(cal.getTime());
 v.setTextViewText(dates[n],heading);v.setTextColor(dates[n],n==0?0xFFFDE047:0xFFCBD5E1);
 for(int team=0;team<4;team++){
 String group=String.valueOf((char)('A'+team));String regular=ShiftCycle.regular(group,date);
 String badge="",description=group+"조 "+date+" "+regular;
 if(team==1&&connected){
 JSONObject day=null;for(int j=0;j<personal.length();j++){JSONObject candidate=personal.optJSONObject(j);if(candidate!=null&&date.equals(candidate.optString("date"))){day=candidate;break;}}
 if(day==null){badge="갱신필요";description+=" · 본인 일정 갱신 필요";}
 else{
 JSONArray off=day.optJSONArray("off"),work=day.optJSONArray("work");
 boolean leave=off!=null&&off.length()>0,daySub=false,nightSub=false;
 if(work!=null)for(int j=0;j<work.length();j++){JSONObject w=work.optJSONObject(j);if(w==null||!"대근".equals(w.optString("source")))continue;if("야간".equals(w.optString("shift")))nightSub=true;else if("주간".equals(w.optString("shift")))daySub=true;}
 if(leave)description+=" · 본인 휴무";if(daySub)description+=" · 주간 대근";if(nightSub)description+=" · 야간 대근";
 if(leave&&(daySub||nightSub))badge="휴·대";else if(leave)badge="내휴";else if(daySub&&nightSub)badge="주야대";else if(daySub)badge="주대";else if(nightSub)badge="야대";
 }
 }
 v.setTextViewText(cells[team][n],regular+(badge.isEmpty()?"":"\n"+badge));
 v.setTextColor(cells[team][n],regular.equals("주간")?0xFFFDE68A:regular.equals("야간")?0xFFBFDBFE:0xFFCBD5E1);
 v.setContentDescription(cells[team][n],description);
 }
 cal.add(Calendar.DATE,1);
 }
 long synced=AlarmStore.prefs(c).getLong("synced",0);SimpleDateFormat stamp=new SimpleDateFormat("M/d HH:mm",Locale.KOREA);stamp.setTimeZone(zone);
 String info="오늘부터 일주일 · A B C D조 기본 근무표. ";
 info+=connected?(synced==0?"본인 일정 동기화 필요":"본인 일정 갱신 "+stamp.format(new Date(synced))):"본인 휴무와 대근을 보려면 앱에서 계정 연결 필요";
 if(!AlarmStore.prefs(c).getString("error","").isEmpty())info+=" · 갱신 실패, 이전 일정";
 v.setContentDescription(R.id.widget_root,info+" · 내휴는 본인 휴무, 주대는 주간 대근, 야대는 야간 대근 · 눌러서 교대근무 열기");
 Intent open=new Intent(Intent.ACTION_VIEW,Uri.parse("https://gy7604.github.io/index.html"));
 v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,7610,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
 m.updateAppWidget(id,v);
 }
}
