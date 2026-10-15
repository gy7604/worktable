package io.github.gy7604.alarmtest;
final class ShiftCycle {
 private static final String[] PATTERN={"주간","주간","휴무","휴무","야간","야간","휴무","휴무"};
 static String regular(String team,String date){
 int offset;
 switch(team){case "A":offset=0;break;case "C":offset=2;break;case "D":offset=4;break;case "B":offset=6;break;default:throw new IllegalArgumentException("Unknown team");}
 long days=(AlarmTime.at(date,"00:00")-AlarmTime.at("2026-09-30","00:00"))/86400000L;
 return PATTERN[(int)(((days+offset)%8+8)%8)];
 }
}