package mmm.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionBarEventWrapper;
import mmm.MbmUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// Wrapper for MbmBarEventManager; unlike HubMissionBarEventWrapper, it does not lock itself onto a market once shown,
// and grants more control over the seed.
public class MbmHubMissionBarEventWrapper extends HubMissionBarEventWrapper {
    private static final Logger log = Global.getLogger(MbmHubMissionBarEventWrapper.class);
    static {
        if (MbmUtils.DEBUG) {
            log.setLevel(Level.ALL);
        }
    }

    public MbmHubMissionBarEventWrapper(String specId) {
        super(specId);
    }

    // Returns true if shouldShowAtMarket and createAndAbortIfFailed succeeds, then aborts the mission if not aborted.
    public boolean canCreate(MarketAPI market, long seed) {
        this.seed = seed;
        boolean succeeds = false;
        if (shouldShowAtMarket(market)) {
            mission.createAndAbortIfFailed(market, true);
            succeeds = !mission.isMissionCreationAborted();
        }
        abortMission();
        return succeeds;
    }

    // turn off the behavior where an active event locks itself onto a market
    @Override
    public void wasShownAtMarket(MarketAPI market) {}

    // Returning true here ensures it won't take up a slot in BarCMD.
    @Override
    public boolean isAlwaysShow() {
        return true;
    }

    @Override
    public String toString() {
        return specId;
    }
}
