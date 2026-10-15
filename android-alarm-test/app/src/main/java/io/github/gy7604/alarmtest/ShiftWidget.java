package io.github.gy7604.alarmtest;
import android.app.*;import android.appwidget.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.widget.RemoteViews;import java.text.*;import java.util.*;
public class ShiftWidget extends AppWidgetProvider {
 static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,ShiftWidget.class));for(int id:ids)render(c,m,id);m.notifyAppWidgetViewDataChanged(ids,R.id.week_list);}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)render(c,m,id);m.notifyAppWidgetViewDataChanged(ids,R.id.week_list);AlarmStore.jobs(c);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle b){render(c,m,id);}
 static void render(Context c,AppWidgetManager m,int id){
 RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.shift_widget);
 Intent adapter=new Intent(c,ShiftWidgetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);adapter.setData(Uri.parse("shiftwidget://week/"+id));
 v.setRemoteAdapter(R.id.week_list,adapter);v.setEmptyView(R.id.week_list,R.id.widget_empty);
 String info="계정 연결 후 일정 동기화";
 boolean connected=!AlarmStore.prefs(c).getString("token","").isEmpty();long synced=AlarmStore.prefs(c).getLong("widget_synced",0);
 if(connected&&synced>0){SimpleDateFormat f=new SimpleDateFormat("M/d HH:mm",Locale.KOREA);f.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));info="갱신 "+f.format(new Date(synced))+" · 위아래로 스크롤";}
 String error=AlarmStore.prefs(c).getString("widget_error","");
 if(connected&&(!error.isEmpty()||!AlarmStore.prefs(c).getString("error","").isEmpty()))info="갱신 실패 · 이전 일정 · 앱에서 동기화";
 v.setTextViewText(R.id.widget_status,info);
 Intent open=new Intent(c,WidgetOpenActivity.class);
 v.setOnClickPendingIntent(R.id.widget_heading,PendingIntent.getActivity(c,7610,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
 v.setPendingIntentTemplate(R.id.week_list,PendingIntent.getActivity(c,7611,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE));
 m.updateAppWidget(id,v);
 }
}