package co.adrian.cameratry;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by adrianzgaljic on 26/09/15.
 */
public class StartClass extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tip);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        RelativeLayout surface = (RelativeLayout)findViewById(R.id.suface);
        surface.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartClass.this, Custom_CameraActivity.class);
                startActivity(intent);
            }
        });

    }

}
