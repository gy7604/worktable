package io.github.gy7604.alarmtest;
import android.app.Activity;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.widget.Toast;
public class WidgetOpenActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b);try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://gy7604.github.io/index.html")));}catch(ActivityNotFoundException e){Toast.makeText(this,"웹앱을 열 브라우저가 필요합니다.",Toast.LENGTH_LONG).show();}finish();}
}