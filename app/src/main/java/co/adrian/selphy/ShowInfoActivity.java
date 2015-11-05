package co.adrian.selphy;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by adrianzgaljic on 26/09/15.
 */
public class ShowInfoActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        RelativeLayout surfaceIntro = (RelativeLayout)findViewById(R.id.sufaceIntro);

        surfaceIntro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.info);
                RelativeLayout surfaceInfo = (RelativeLayout)findViewById(R.id.sufaceInfo);
                surfaceInfo.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ShowInfoActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });



    }

}
