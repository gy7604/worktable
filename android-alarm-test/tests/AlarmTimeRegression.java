package io.github.gy7604.alarmtest;
import java.time.Instant;import java.util.TimeZone;
public class AlarmTimeRegression {
 static void check(String date,String time,String expected){if(AlarmTime.at(date,time)!=Instant.parse(expected).toEpochMilli())throw new AssertionError(date+" "+time);}
 public static void main(String[] args){
 TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
 check("2026-10-14","06:30","2026-10-13T21:30:00Z");
 check("2026-10-14","18:25","2026-10-14T09:25:00Z");
 check("2026-12-31","23:59","2026-12-31T14:59:00Z");
 check("2028-02-29","00:00","2028-02-28T15:00:00Z");
 boolean rejected=false;try{AlarmTime.at("2026-02-29","05:00");}catch(IllegalArgumentException e){rejected=true;}if(!rejected)throw new AssertionError("Invalid dates accepted");
 if(AlarmTime.at("2026-10-14","18:25")==AlarmTime.at("2026-10-14","17:10"))throw new AssertionError("Custom time ignored");
 System.out.println("Custom day/night times, timezone, year rollover and invalid dates: passed");
 }
}