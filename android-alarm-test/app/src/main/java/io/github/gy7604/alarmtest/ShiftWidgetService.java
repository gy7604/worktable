package io.github.gy7604.alarmtest;
import android.appwidget.*;import android.content.*;import android.widget.*;import org.json.*;import java.text.*;import java.util.*;
public class ShiftWidgetService extends RemoteViewsService {
 @Override public RemoteViewsFactory onGetViewFactory(Intent intent){return new Factory(getApplicationContext());}
 static final class Factory implements RemoteViewsFactory {
 final Context c;final String[] dates=new String[7],labels=new String[7];JSONArray rows=new JSONArray();boolean ready;
 Factory(Context c){this.c=c;}
 public void onCreate(){onDataSetChanged();}
 public void onDataSetChanged(){
 ready=!AlarmStore.prefs(c).getString("token","").isEmpty()&&AlarmStore.prefs(c).contains("widget_synced");
 try{rows=ready?new JSONArray(AlarmStore.prefs(c).getString("widget_requests","[]")):new JSONArray();}catch(JSONException e){rows=new JSONArray();ready=false;}
 TimeZone zone=TimeZone.getTimeZone("Asia/Seoul");Calendar cal=Calendar.getInstance(zone);SimpleDateFormat key=new SimpleDateFormat("yyyy-MM-dd",Locale.US),label=new SimpleDateFormat("M/d (E)",Locale.KOREA);key.setTimeZone(zone);label.setTimeZone(zone);
 for(int i=0;i<7;i++){dates[i]=key.format(cal.getTime());labels[i]=label.format(cal.getTime());cal.add(Calendar.DATE,1);}
 }
 public void onDestroy(){rows=new JSONArray();}
 public int getCount(){return 7;}
 public RemoteViews getViewAt(int position){
 if(position<0||position>=7)return null;
 RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.shift_widget_day);v.setTextViewText(R.id.row_date,labels[position]+(position==0?" · 오늘":""));
 int[] ids={R.id.team_a,R.id.team_b,R.id.team_c,R.id.team_d};
 int[] colors={0xFFFDBA74,0xFF86EFAC,0xFF93C5FD,0xFFFDE047};
 for(int t=0;t<4;t++){String team=String.valueOf((char)('A'+t));StringBuilder b=new StringBuilder(ShiftCycle.regular(team,dates[position]));
 if(!ready)b.append("\n동기화 필요");
 else{
 boolean found=false;
 for(int j=0;j<rows.length();j++){
 JSONObject r=rows.optJSONObject(j);if(r==null||!dates[position].equals(r.optString("shift_date"))||!team.equals(r.optString("team")))continue;
 String who=r.optString("requested_by",""),sub=r.optString("filled_by","");
 b.append("\n");if(!r.optString("shift_type").equals(ShiftCycle.regular(team,dates[position])))b.append(r.optString("shift_type")).append(" ");
 b.append(r.optString("request_kind").equals("off")?"휴 ":"요청 ").append(who);
 b.append("\n대 ").append(r.optString("status").equals("FILLED")&&!sub.isEmpty()&&!sub.equals("null")?sub:"미정");found=true;
 }
 if(!found)b.append("\n휴무·대근 없음");
 }
 v.setTextViewText(ids[t],b.toString());v.setTextColor(ids[t],colors[t]);v.setContentDescription(ids[t],team+"조 "+labels[position]+" "+b);
 }
 v.setOnClickFillInIntent(R.id.day_root,new Intent());return v;
 }
 public RemoteViews getLoadingView(){return null;}
 public int getViewTypeCount(){return 1;}
 public long getItemId(int position){return position;}
 public boolean hasStableIds(){return false;}
 }
}