package co.adrian.cameratry;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by adrianzgaljic on 01/10/15.
 */
public class StartActivity extends Activity {

    // TODO: 01/10/15  napravi da ovo radi! sa shared pref možeš pamtit i zadnju fotku pa ju otvorit prvi put
    // TODO: 01/10/15 u cameraPreview se ne poziva surfacecreated -nakon detekcije prednje kamere i nakon vraćanja iz galerije! 
    

    public static final String TAG = "logIspis";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "start");

        SharedPreferences prefs = getSharedPreferences("selphy", 0);
        SharedPreferences.Editor editor = prefs.edit();
        Intent intent;
        String first = prefs.getString("first","");

        if (first.equals("isFirst"))
        {
            Log.i(TAG, "not initial");
            intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else
        {
            Log.i(TAG, "initial ");
            //First Time App launched, you are putting isInitialAppLaunch to false and calling create password activity.
            editor.putString("first","isFirst");
            editor.apply();
            intent = new Intent(this, ShowInfoActivity.class);
            startActivity(intent);
        }

    }


}

