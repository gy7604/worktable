package io.github.gy7604.alarmtest;
import android.app.*;import android.content.*;import android.net.Uri;import java.net.*;import java.io.*;import org.json.*;
final class UpdateCheck {
 private static final String BASE="https://github.com/gy7604/worktable/releases/download/shift-alarm-current/";
 static void check(Activity a,boolean manual){
 new Thread(()->{try{
 HttpURLConnection cn=(HttpURLConnection)new URL(BASE+"update.json?ts="+System.currentTimeMillis()).openConnection();
 JSONObject d;try{cn.setConnectTimeout(8000);cn.setReadTimeout(8000);cn.setRequestProperty("Cache-Control","no-cache");if(cn.getResponseCode()!=200)throw new IOException();
 try(InputStream in=cn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>16384)throw new IOException();}d=new JSONObject(out.toString("UTF-8"));}}finally{cn.disconnect();}
 int installed=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionCode;
 int latest=d.getInt("versionCode");String name=d.getString("versionName");
 a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;
 if(latest>installed)new AlertDialog.Builder(a).setTitle("새 업데이트가 있습니다").setMessage(name+" 버전을 설치할 수 있습니다.\n기존 계정 연결과 알람 설정은 유지됩니다.\n다운로드한 APK를 열어 설치해 주세요.").setPositiveButton("업데이트",(x,y)->{try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(BASE+"app-release.apk")));}catch(ActivityNotFoundException e){new AlertDialog.Builder(a).setMessage("다운로드를 열 수 있는 브라우저가 필요합니다.").setPositiveButton("확인",null).show();}}).setNegativeButton("나중에",null).show();
 else if(manual)new AlertDialog.Builder(a).setMessage("현재 최신 버전입니다.").setPositiveButton("확인",null).show();});
 }catch(Exception e){if(manual)a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())new AlertDialog.Builder(a).setMessage("업데이트를 확인하지 못했습니다. 인터넷 연결을 확인해 주세요.").setPositiveButton("확인",null).show();});}}).start();
 }
}
