package io.github.gy7604.alarmtest;
import java.util.*;
final class AlarmTime {
 static long at(String date,String time){
 String[] d=date.split("-"),t=time.split(":");
 Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));c.clear();c.setLenient(false);
 c.set(Integer.parseInt(d[0]),Integer.parseInt(d[1])-1,Integer.parseInt(d[2]),Integer.parseInt(t[0]),Integer.parseInt(t[1]),0);
 return c.getTimeInMillis();
 }
}