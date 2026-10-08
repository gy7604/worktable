package io.github.gy7604.alarmtest;
import android.app.*;import android.app.job.*;import android.content.*;import android.os.*;import org.json.*;import java.net.*;import java.io.*;
final class AlarmStore {
 static final int NEXT=7605,TEST=7604,SNOOZE=7606,JOB=7607;
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("shift_alarm",Context.MODE_PRIVATE);}
 static boolean exact(Context c){return Build.VERSION.SDK_INT<31||((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();}
 static PendingIntent pi(Context c,int id,String label){Intent i=new Intent(c,AlarmReceiver.class).setAction("FIRE").putExtra("label",label).putExtra("id",id);return PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static void set(Context c,int id,long at,String label){if(!exact(c))return;PendingIntent show=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).setAlarmClock(new AlarmManager.AlarmClockInfo(at,show),pi(c,id,label));}
 static void cancel(Context c,int id){PendingIntent p=pi(c,id,"");((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(p);p.cancel();}
 static int countFuture(Context c){int n=0;try{JSONArray a=new JSONArray(prefs(c).getString("alarms","[]"));for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getLong("at")>System.currentTimeMillis())n++;}catch(Exception ignored){}return n;}\n static synchronized void restore(Context c){
  cancel(c,NEXT);prefs(c).edit().remove("next").apply();
  if(!prefs(c).getBoolean("enabled",false))return;
  try{JSONArray list=new JSONArray(prefs(c).getString("alarms","[]"));for(int n=0;n<list.length();n++){JSONObject a=list.getJSONObject(n);if(a.getLong("at")<=System.currentTimeMillis())continue;String label=a.getString("date")+" "+a.getString("time")+" "+a.getString("shift")+" · "+a.getString("source");set(c,NEXT,a.getLong("at"),label);prefs(c).edit().putString("next",label).apply();break;}}catch(Exception ignored){}
 }
 static JSONObject api(String action,String secret)throws Exception{
  HttpURLConnection cn=(HttpURLConnection)new URL("https://lqqjyedfwlqinppcrvjf.supabase.co/functions/v1/app-alarm").openConnection();
  try{cn.setRequestMethod("POST");cn.setConnectTimeout(10000);cn.setReadTimeout(15000);cn.setDoOutput(true);cn.setRequestProperty("Content-Type","application/json");if(action.equals("sync"))cn.setRequestProperty("x-alarm-token",secret);
   JSONObject body=new JSONObject().put("action",action);if(action.equals("exchange"))body.put("code",secret);
   try(OutputStream o=cn.getOutputStream()){o.write(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
   int code=cn.getResponseCode();if(code==401)throw new Revoked();if(code!=200)throw new IOException("서버에 연결하지 못했습니다 ("+code+")");
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=cn.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){bytes.write(buf,0,n);if(bytes.size()>131072)throw new IOException("응답 크기 초과");}}
   JSONObject data=new JSONObject(bytes.toString("UTF-8"));if(!data.optBoolean("success"))throw new IOException("연결 실패");return data;
  }finally{cn.disconnect();}
 }
 static class Revoked extends IOException {Revoked(){super("연결이 만료되었거나 해제되었습니다. 다시 연결해 주세요.");}}
 static synchronized String sync(Context c)throws Exception {
  String token=prefs(c).getString("token","");if(token.isEmpty())return "먼저 교대근무 앱에서 연결 코드를 발급해 주세요.";
  try{JSONObject d=api("sync",token);JSONArray a=d.getJSONArray("alarms");for(int i=0;i<a.length();i++){JSONObject row=a.getJSONObject(i);row.getLong("at");row.getString("date");row.getString("time");row.getString("shift");row.getString("source");}
   prefs(c).edit().putString("alarms",a.toString()).putString("name",d.getString("name")).putLong("synced",System.currentTimeMillis()).putString("error","").commit();restore(c);return "일정을 동기화했습니다.";
  }catch(Revoked e){prefs(c).edit().remove("token").putBoolean("enabled",false).putString("error",e.getMessage()).commit();restore(c);cancel(c,SNOOZE);c.stopService(new Intent(c,RingService.class));throw e;}
  catch(Exception e){prefs(c).edit().putString("error","동기화 실패 — 마지막으로 받은 일정 유지").apply();throw e;}
 }
 static void jobs(Context c){JobScheduler j=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(prefs(c).getString("token","").isEmpty()){j.cancel(JOB);return;}j.schedule(new JobInfo.Builder(JOB,new ComponentName(c,SyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build());}
 static void stop(Context c){prefs(c).edit().putBoolean("enabled",false).commit();restore(c);cancel(c,SNOOZE);c.stopService(new Intent(c,RingService.class));}
}
