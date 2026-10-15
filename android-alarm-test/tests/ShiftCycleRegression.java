package io.github.gy7604.alarmtest;
import java.time.LocalDate;
public class ShiftCycleRegression {
 public static void main(String[] args){
 String[] teams={"A","B","C","D"},expected={"휴무","야간","주간","휴무"};
 for(int i=0;i<4;i++)if(!expected[i].equals(ShiftCycle.regular(teams[i],"2026-10-15")))throw new AssertionError("October 15 anchor "+teams[i]);
 for(LocalDate date=LocalDate.of(2026,9,1);date.isBefore(LocalDate.of(2027,2,1));date=date.plusDays(1)){
 int day=0,night=0,off=0;
 for(String team:teams){String shift=ShiftCycle.regular(team,date.toString());
 if(shift.equals("주간"))day++;else if(shift.equals("야간"))night++;else off++;
 if(!shift.equals(ShiftCycle.regular(team,date.plusDays(8).toString())))throw new AssertionError("8-day cycle");
 }
 if(day!=1||night!=1||off!=2)throw new AssertionError("Team coverage "+date);
 }
 System.out.println("All four teams, anchor, eight-day rotation and year rollover passed");
 }
}