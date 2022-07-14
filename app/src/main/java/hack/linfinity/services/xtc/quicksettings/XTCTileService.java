package hack.linfinity.services.xtc.quicksettings;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import hack.linfinity.services.xtc.XTCService;

public class XTCTileService extends TileService {

    @Override
    public void onStartListening() {
        SharedPreferences sp = getSharedPreferences("xtc", 0);
        boolean en = sp.getBoolean("enabled", false);
        Tile tile = getQsTile();
        if(tile != null) {
            tile.setState((en ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE));
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        Log.d("XTCTile", "On Click");
        SharedPreferences sp = getSharedPreferences("xtc", 0);
        SharedPreferences.Editor speed = sp.edit();
        if(sp.getBoolean("enabled", false)) {
            speed.putBoolean("enabled", false);
            speed.commit();
            try {
                XTCService.thisService.stop();
                XTCService.thisService.stopSelf();
            } catch (Exception e) {
                e.printStackTrace();
            }
            requestListeningState(getApplicationContext(), new ComponentName(getApplicationContext(), XTCTileService.class));
        }
        else {
            speed.putBoolean("enabled", true);
            speed.commit();
            XTCService.startXTCService(getApplicationContext());
            requestListeningState(getApplicationContext(), new ComponentName(getApplicationContext(), XTCTileService.class));
        }
    }
}
