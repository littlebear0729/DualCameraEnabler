package com.littlebear.dualcameraenabler;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    //0->disabled 1->enabled 2->no detected
    private Integer auxStatus = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch aSwitch = (Switch)findViewById(R.id.switch1);
        TextView tv = (TextView)findViewById(R.id.textView);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());

        Runtime runtime = Runtime.getRuntime();
        try {
            tvSetText("Checking SU...");
            runtime.exec("su");
        } catch (IOException e) {
            tvSetText("SU Check Failed.");
            e.printStackTrace();
            aSwitch.setClickable(false);
        }
        tvSetText("Checking prop file status...");
        //execShellCmd("su -c cat /vendor/build.prop");
        execShellCmd("su -c sed -n '/persist.vendor.camera.expose.aux/p' /vendor/build.prop");
        if (auxStatus == 1) {
            aSwitch.setChecked(true);
            tvSetText("Dual Camera is now enabled.");
        } else if (auxStatus == 0){
            aSwitch.setChecked(false);
            tvSetText("Dual Camera is now disabled.");
        } else if (auxStatus == 2) {
            aSwitch.setClickable(false);
            tvSetText("No Dual Camera Config Detected.");
        }
    }

    public void execShellCmd(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            String data = "";
            BufferedReader ie = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String error = "";
            while ((error = ie.readLine()) != null && !error.equals("null")) {
                data += error;
            }
            String line = "";
            while ((line = in.readLine()) != null && !line.equals("null")) {
                data += line;
            }
            Log.v("execShellCmd", data);
            tvSetText(data);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void tvSetText(String text) {
        TextView tv = (TextView)findViewById(R.id.textView);
        String temp = tv.getText().toString();
        tv.setText(temp + "\n" + text);
        if (text.equals("persist.vendor.camera.expose.aux=1")) {
            auxStatus = 1;
        } else if (text.equals("persist.vendor.camera.expose.aux=0")) {
            auxStatus = 0;
        }
}

    public void onClickRebootButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reboot RIGHT NOW?")
                .setCancelable(true)
                .setNegativeButton("NO", null)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Process process = Runtime.getRuntime().exec("su -c reboot");
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void onClickAboutButton(View view) {
        Toast.makeText(this,
                "This is a simple app made by Littlebear0729, " +
                "which is aim for fix the WeChat Video Call stuck when you switch the front camera to the back.",
                Toast.LENGTH_LONG).show();
    }
    public void onClickSwitch(View view) {
        Switch aSwitch = (Switch)findViewById(R.id.switch1);
        tvSetText("Mounting vendor...");
        execShellCmd("su -c mount -o remount,rw /vendor");
        tvSetText("Creating backup...");
        execShellCmd("su -c cp /vendor/build.prop /vendor/build.prop.bak");
        if (!aSwitch.isChecked()) {
            execShellCmd("su -c sed -i 's/persist.vendor.camera.expose.aux=1/persist.vendor.camera.expose.aux=0/g' /vendor/build.prop");
        } else {
            execShellCmd("su -c sed -i 's/persist.vendor.camera.expose.aux=0/persist.vendor.camera.expose.aux=1/g' /vendor/build.prop");
        }
        tvSetText("Checking Result...");
        execShellCmd("su -c sed -n '/persist.vendor.camera.expose.aux/p' /vendor/build.prop");
        tvSetText("Unmounting vendor...");
        execShellCmd("su -c mount -o remount,ro /vendor");
        tvSetText("Reboot to apply the changes.");
    }
}
