package co.adrian.cameratry;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;



public class StartActivity extends Activity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("selphy", 0);
        SharedPreferences.Editor editor = prefs.edit();
        Intent intent;
        String first = prefs.getString("first","");

        if (first.equals("isFirst"))
        {
            intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else
        {
            //First Time App launched, you are putting isInitialAppLaunch to false and calling create password activity.
            editor.putString("first","isFirst");
            editor.apply();
            intent = new Intent(this, ShowInfoActivity.class);
            startActivity(intent);
        }

    }


}

