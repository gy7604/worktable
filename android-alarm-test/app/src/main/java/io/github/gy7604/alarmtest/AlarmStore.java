package io.github.gy7604.alarmtest;
import android.app.*;import android.app.job.*;import android.content.*;import android.os.*;import org.json.*;import java.net.*;import java.io.*;
final class AlarmStore {
 static final int NEXT=7605,TEST=7604,SNOOZE=7606,JOB=7607;
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("shift_alarm",Context.MODE_PRIVATE);}
 static boolean exact(Context c){return Build.VERSION.SDK_INT<31||((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();}
 static PendingIntent pi(Context c,int id,String label){Intent i=new Intent(c,AlarmReceiver.class).setAction("FIRE").putExtra("label",label).putExtra("id",id);return PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static void set(Context c,int id,long at,String label){if(!exact(c))return;PendingIntent show=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).setAlarmClock(new AlarmManager.AlarmClockInfo(at,show),pi(c,id,label));}
 static void cancel(Context c,int id){PendingIntent p=pi(c,id,"");((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(p);p.cancel();}

 static String wakeTime(Context c,boolean night){return prefs(c).getString(night?"night_time":"day_time",night?"17:10":"05:00");}
 static JSONArray plan(Context c)throws Exception{
 JSONArray raw=new JSONArray(prefs(c).getString("alarms","[]"));java.util.List<JSONObject> rows=new java.util.ArrayList<>();
 for(int i=0;i<raw.length();i++){JSONObject a=new JSONObject(raw.getJSONObject(i).toString());String shift=a.getString("shift");boolean night=shift.contains("야간");if(!night&&!shift.contains("주간"))continue;
 String time=wakeTime(c,night);a.put("time",time);a.put("at",AlarmTime.at(a.getString("date"),time));rows.add(a);}
 java.util.Collections.sort(rows,(a,b)->Long.compare(a.optLong("at"),b.optLong("at")));JSONArray out=new JSONArray();for(JSONObject a:rows)out.put(a);return out;
 }
 static int countFuture(Context c){int n=0;try{JSONArray a=plan(c);for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getLong("at")>System.currentTimeMillis())n++;}catch(Exception ignored){}return n;}
 static synchronized void restore(Context c){
  cancel(c,NEXT);prefs(c).edit().remove("next").apply();
  if(!prefs(c).getBoolean("enabled",false)||!exact(c))return;
  try{JSONArray list=plan(c);for(int n=0;n<list.length();n++){JSONObject a=list.getJSONObject(n);if(a.getLong("at")<=System.currentTimeMillis())continue;String label=a.getString("date")+" "+a.getString("time")+" "+a.getString("shift")+" · "+a.getString("source");set(c,NEXT,a.getLong("at"),label);prefs(c).edit().putString("next",label).apply();break;}}catch(Exception e){prefs(c).edit().putString("error","알람 예약 실패: "+e.getClass().getSimpleName()).apply();}
 }
 static JSONObject api(String action,String secret)throws Exception{
  HttpURLConnection cn=(HttpURLConnection)new URL("https://lqqjyedfwlqinppcrvjf.supabase.co/functions/v1/app-alarm").openConnection();
  try{cn.setRequestMethod("POST");cn.setConnectTimeout(10000);cn.setReadTimeout(15000);cn.setDoOutput(true);cn.setRequestProperty("Content-Type","application/json");if(action.equals("sync"))cn.setRequestProperty("x-alarm-token",secret);
   JSONObject body=new JSONObject().put("action",action);if(action.equals("sync"))body.put("widget",true);if(action.equals("exchange"))body.put("code",secret);
   try(OutputStream o=cn.getOutputStream()){o.write(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
   int code=cn.getResponseCode();if(code==401)throw new Revoked();if(code!=200)throw new IOException("서버에 연결하지 못했습니다 ("+code+")");
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=cn.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){bytes.write(buf,0,n);if(bytes.size()>524288)throw new IOException("응답 크기 초과");}}
   JSONObject data=new JSONObject(bytes.toString("UTF-8"));if(!data.optBoolean("success"))throw new IOException("연결 실패");return data;
  }finally{cn.disconnect();}
 }
 static class Revoked extends IOException {Revoked(){super("연결이 만료되었거나 해제되었습니다. 다시 연결해 주세요.");}}
 private static final Object SYNC_LOCK=new Object();
 static String sync(Context c)throws Exception {synchronized(SYNC_LOCK){return syncInternal(c);}}
 private static String syncInternal(Context c)throws Exception {
  String token; synchronized(AlarmStore.class){token=prefs(c).getString("token","");}
  if(token.isEmpty())return "먼저 교대근무 앱에서 연결 코드를 발급해 주세요.";
  try{
   JSONObject d=api("sync",token);JSONArray rows=d.getJSONArray("alarms");
   for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);row.getLong("at");row.getString("date");row.getString("time");row.getString("shift");row.getString("source");}
   synchronized(AlarmStore.class){
    if(!token.equals(prefs(c).getString("token","")))return "이전 연결의 응답을 무시했습니다.";
    prefs(c).edit().putString("alarms",rows.toString()).putString("workdays",d.optJSONArray("workdays")==null?"[]":d.getJSONArray("workdays").toString()).putString("name",d.getString("name")).putLong("synced",System.currentTimeMillis()).putString("error","").commit();
    if(d.optJSONArray("widget_requests")!=null)prefs(c).edit().putString("widget_requests",d.getJSONArray("widget_requests").toString()).putLong("widget_synced",System.currentTimeMillis()).putString("widget_error","").commit();
    else prefs(c).edit().putString("widget_error",d.optString("widget_error","위젯 정보를 갱신하지 못했습니다.")).commit();
    restore(c);ShiftWidget.refresh(c);
   }
   return "일정을 동기화했습니다.";
  }catch(Revoked e){
   synchronized(AlarmStore.class){
    if(!token.equals(prefs(c).getString("token","")))return "이전 연결의 응답을 무시했습니다.";
    prefs(c).edit().remove("token").putBoolean("enabled",false).putString("error",e.getMessage()).commit();
    restore(c);cancel(c,SNOOZE);c.stopService(new Intent(c,RingService.class));ShiftWidget.refresh(c);
   }throw e;
  }catch(Exception e){
   synchronized(AlarmStore.class){
    if(!token.equals(prefs(c).getString("token","")))return "이전 연결의 응답을 무시했습니다.";
    prefs(c).edit().putString("error","동기화 실패 — 마지막으로 받은 일정 유지").apply();ShiftWidget.refresh(c);
   }throw e;
  }
 }
 static synchronized void attach(Context c,String token,String name){stop(c);prefs(c).edit().putString("token",token).putString("name",name).putBoolean("enabled",true).remove("alarms").remove("workdays").remove("synced").remove("widget_requests").remove("widget_synced").remove("widget_error").commit();jobs(c);ShiftWidget.refresh(c);}
 static synchronized void detach(Context c){stop(c);cancel(c,TEST);prefs(c).edit().clear().commit();jobs(c);ShiftWidget.refresh(c);}

 static void jobs(Context c){JobScheduler j=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(prefs(c).getString("token","").isEmpty()){j.cancel(JOB);return;}j.schedule(new JobInfo.Builder(JOB,new ComponentName(c,SyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build());}
 static void stop(Context c){prefs(c).edit().putBoolean("enabled",false).commit();restore(c);cancel(c,SNOOZE);c.stopService(new Intent(c,RingService.class));}
}
