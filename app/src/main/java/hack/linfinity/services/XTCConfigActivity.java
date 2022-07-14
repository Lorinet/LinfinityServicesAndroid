package hack.linfinity.services;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class XTCConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xtcconfig);
        ((Button)findViewById(R.id.buttonXTCSaveLogin)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Process ps = Runtime.getRuntime().exec(MainActivity.xtcBinary + " passwd", new String[]{"HOME=" + MainActivity.dataDir}, new File(MainActivity.dataDir));
                    Log.d("XTCSettings", "Process launched");
                    DataOutputStream dos = new DataOutputStream(ps.getOutputStream());
                    dos.writeBytes(((TextView)findViewById(R.id.textInputXTCUsername)).getText() + "\n");
                    dos.writeBytes(((TextView)findViewById(R.id.textInputXTCPassword)).getText() + "\n");
                    dos.writeBytes(((TextView)findViewById(R.id.textInputXTCPassword)).getText() + "\n");
                    dos.flush();
                    ps.waitFor();
                    Toast.makeText(getApplicationContext(), "Settings saved", Toast.LENGTH_SHORT).show();
                    Log.d("XTCSettings", "Settings saved");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}