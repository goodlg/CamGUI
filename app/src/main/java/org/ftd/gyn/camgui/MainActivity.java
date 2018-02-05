package org.ftd.gyn.camgui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.ftd.gyn.camlibrary.MyCamera;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Cam-Gui";

    // -----------------------------------------------------
    // ui
    private TextView tvStat = null;
    private TextView tvData = null;
    private EditText mEditOpen = null;

    private MyCamera mCamera = null;

    private int mDefaultCameraId = 0;//0: BACK, 1: FRONT
    private int mCurrentCameraId   = mDefaultCameraId;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        MyCamera.apiInit();
        mainScreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyCamera.apiDeinit();
    }

    //-----------------------------------------------------
    // create main screen
    private void mainScreen() {
        setContentView(R.layout.activity_main);

        mEditOpen = (EditText) findViewById(R.id.editOpen);

        ((Button) findViewById(R.id.open)).setOnClickListener(mOpenListener);
        ((Button) findViewById(R.id.close)).setOnClickListener(mCloseListener);

        // ui items
        tvStat = (TextView) findViewById(R.id.textStatus);
        tvData = (TextView) findViewById(R.id.textData);
    }

    // ------------------------------------------------------
    // callback for OPEN button press
    private View.OnClickListener mOpenListener = new View.OnClickListener() {
        public void onClick(View v) {
            String str = mEditOpen.getText().toString();
            if (!TextUtils.isEmpty(str) && TextUtils.isDigitsOnly(str)) {
                mCurrentCameraId = Integer.parseInt(str);
            }
            doOpen(mCurrentCameraId);
        }
    };

    // ------------------------------------------------------
    // callback for CLOSE button press
    private View.OnClickListener mCloseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doClose();
        }
    };

    // ----------------------------------------
    // open camera
    private void doOpen(int camId) {
        mCamera = MyCamera.open(camId);
        if (mCamera == null)
            dspErr("open camera failed");
        else
            dspStat("camera opened");
    }

    // ----------------------------------------
    // close camera
    private void doClose() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            dspStat("camera closed");
        }
    }

    // ----------------------------------------
    // display error msg
    private void dspErr(String s) {
        tvStat.setText("ERROR: " + s);
    }

    // ----------------------------------------
    // display status string
    private void dspStat(String s) {
        tvStat.setText(s);
    }

    // ----------------------------------------
    // display data string
    private void dspData(String s) {
        tvData.setText(s);
    }
}
