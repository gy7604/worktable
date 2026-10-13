package io.github.gy7604.alarmtest;
import android.app.*;import android.appwidget.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.widget.RemoteViews;import org.json.*;import java.text.*;import java.util.*;
public class ShiftWidget extends AppWidgetProvider {
 static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,ShiftWidget.class));for(int id:ids)render(c,m,id);}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)render(c,m,id);AlarmStore.jobs(c);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle b){render(c,m,id);}
 static void render(Context c,AppWidgetManager m,int id){
 RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.shift_widget);
 boolean connected=!AlarmStore.prefs(c).getString("token","").isEmpty();
 v.setTextViewText(R.id.widget_title,connected?AlarmStore.prefs(c).getString("name","")+" · 일주일 근무":"교대근무 · 계정 연결 필요");
 String[] lines=new String[7];Arrays.fill(lines,"");
 TimeZone zone=TimeZone.getTimeZone("Asia/Seoul");Calendar cal=Calendar.getInstance(zone);
 SimpleDateFormat key=new SimpleDateFormat("yyyy-MM-dd",Locale.US);key.setTimeZone(zone);
 SimpleDateFormat label=new SimpleDateFormat("M/d (E)",Locale.KOREA);label.setTimeZone(zone);
 try{
 JSONArray days=new JSONArray(AlarmStore.prefs(c).getString("workdays","[]"));
 for(int n=0;n<7;n++){String date=key.format(cal.getTime());String value="일정 갱신 필요";
 for(int j=0;j<days.length();j++){JSONObject d=days.getJSONObject(j);if(!date.equals(d.optString("date")))continue;
 JSONArray work=d.getJSONArray("work"),off=d.getJSONArray("off");StringBuilder b=new StringBuilder();
 for(int k=0;k<work.length();k++){JSONObject w=work.getJSONObject(k);if(b.length()>0)b.append(" / ");b.append(w.getString("shift"));if("대근".equals(w.getString("source")))b.append(" 대근");b.append(" ").append(w.getString("hours"));}
 if(work.length()==0)b.append(off.length()>0?"본인 휴무":"휴무");
 if(off.length()>0){if(work.length()>0)b.append(" · 본인 휴무");b.append(" (");for(int k=0;k<off.length();k++){if(k>0)b.append("/");b.append(off.getString(k));}b.append(")");}
 value=b.toString();break;}
 lines[n]=label.format(cal.getTime())+"  "+value;cal.add(Calendar.DATE,1);}
 }catch(Exception e){lines[0]="일정을 읽을 수 없습니다. 앱에서 동기화해 주세요.";}
 int[] rows={R.id.day0,R.id.day1,R.id.day2,R.id.day3,R.id.day4,R.id.day5,R.id.day6};
 for(int n=0;n<7;n++)v.setTextViewText(rows[n],connected?lines[n]:(n==0?"교대 기상 알람 앱에서 계정을 연결해 주세요.":""));
 long synced=AlarmStore.prefs(c).getLong("synced",0);
 SimpleDateFormat stamp=new SimpleDateFormat("M/d HH:mm",Locale.KOREA);stamp.setTimeZone(zone);
 String footer=synced==0?"아직 동기화되지 않음":"갱신 "+stamp.format(new Date(synced));
 if(!AlarmStore.prefs(c).getString("error","").isEmpty())footer+=" · 갱신 실패";
 else if(synced>0&&System.currentTimeMillis()-synced>3600000)footer+=" · 이전 일정";
 v.setTextViewText(R.id.widget_footer,footer+" · 눌러서 교대근무 열기");
 Intent open=new Intent(Intent.ACTION_VIEW,Uri.parse("https://gy7604.github.io/index.html"));
 v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,7610,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
 m.updateAppWidget(id,v);
 }
}
