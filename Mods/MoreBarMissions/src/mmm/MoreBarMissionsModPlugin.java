package mmm;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import mmm.missions.MbmBarEventManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class MoreBarMissionsModPlugin extends BaseModPlugin {
    private static final Logger log = Global.getLogger(mmm.MoreBarMissionsModPlugin.class);
    static {
        log.setLevel(Level.ALL);
    }

    @Override
    public void onApplicationLoad() {
        MbmUtils.loadSettings();
        if (MbmUtils.LUNA_LIB_ENABLED) {
            lunalib.lunaSettings.LunaSettings.addSettingsListener(new lunalib.lunaSettings.LunaSettingsListener() {
                @Override
                public void settingsChanged(String s) {
                    MbmUtils.loadSettings();
                }
            });
        }
    }

    @Override
    public void beforeGameSave() {
        MbmBarEventManager.unload();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        // Note that onGameLoad is called after CoreLifecyclePluginImpl.onGameLoad, so the vanilla code to load
        // BarEventManager has already been executed.
        Global.getSector().addTransientListener(new MbmBarEventManager());
    }
}
