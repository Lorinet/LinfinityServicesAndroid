package hack.linfinity.services;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import hack.linfinity.services.xtc.XTCProcessMainClass;
import hack.linfinity.services.xtc.XTCService;
import hack.linfinity.services.xtc.restarter.XTCRestartServiceBroadcastReceiver;

public class MainActivity extends AppCompatActivity {

    public static String dataDir;
    public static String xtcBinary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("DEVICE", Build.DEVICE);
        try {
            dataDir = getApplicationContext().getApplicationInfo().dataDir;
            xtcBinary = dataDir + "/xtc";
            InputStream is = getAssets().open("xtc");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            OutputStream os = new FileOutputStream(xtcBinary);
            os.write(buffer);
            os.close();
            Runtime.getRuntime().exec("chmod a+rwx " + xtcBinary);
            Runtime.getRuntime().exec("mkdir " + dataDir + "/XTCAirShare");
            ((Button)findViewById(R.id.buttonXTCSettings)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, XTCConfigActivity.class));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //XTCService.startXTCService(getApplicationContext());
    }
}