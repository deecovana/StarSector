package mmm.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager.GenericBarEventCreator;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseGetCommodityBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.SpecBarEventCreator;
import com.fs.starfarer.api.loading.BarEventSpec;
import mmm.MbmUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.Random;

import static mmm.missions.MbmBarEventManager.MILLISECONDS_PER_DAY;

// A creator that simply forwards most methods to another creator except createBarEvent returns
// MbmHubMissionBarEventWrapper instead of HubMissionBarEventWrapper, and getBarEventId returns the event ID and not
// creator ID. Also provides canCreate and some convenience methods.
public class MbmBarEventCreator implements GenericBarEventCreator {
    private static final Logger log = Global.getLogger(MbmBarEventCreator.class);
    static {
        if (MbmUtils.DEBUG) {
            log.setLevel(Level.ALL);
        }
    }

    public GenericBarEventCreator delegate;
    public String event_id = null;  // Not used for BarEventSpecCreator
    public BarEventSpec spec = null;  // Only populated for BarEventSpecCreator
    // BarCMD has its own logic to limit the number of events. To bypass that we want to have both
    // PortsideBarEvent.isAlwaysShow and isPriority returns true. However that is not possible for all events as we
    // have no control over isAlwaysShow from non-spec events.
    public boolean isAlwaysShow;
    MbmBarEventCreator(String specId) {
        SpecBarEventCreator creator = new SpecBarEventCreator(specId);
        this.delegate = creator;
        this.spec = creator.getSpec();
        isAlwaysShow = true;
    }

    MbmBarEventCreator(GenericBarEventCreator delegate) {
        this.delegate = delegate;
        PortsideBarEvent event = delegate.createBarEvent();
        this.event_id = event.getBarEventId();
        isAlwaysShow = event.isAlwaysShow();
    }

    // Returns true if we can create the mission on this market with this seed. Here event must be a PortsideBarEvent
    // returned by this.
    boolean canCreate(MarketAPI market, PortsideBarEvent event, long seed) {
        // Unlock the event from any previously shown market
        try {
            event.wasShownAtMarket(null);
            if (event.shouldRemoveEvent()) return false;
            if (event instanceof MbmHubMissionBarEventWrapper) {
                return ((MbmHubMissionBarEventWrapper) event).canCreate(market, seed);
            }

            if (event instanceof BaseGetCommodityBarEvent) {
                MbmUtils.reflectionSet(BaseGetCommodityBarEvent.class, event, "seed", seed);
                MbmUtils.reflectionSet(BaseGetCommodityBarEvent.class, event, "random",
                        new Random(seed + market.getId().hashCode()));
            } else if (event instanceof BaseBarEventWithPerson) {
                MbmUtils.reflectionSet(BaseBarEventWithPerson.class, event, "seed", seed);
                MbmUtils.reflectionSet(BaseBarEventWithPerson.class, event, "random",
                        new Random(seed + market.getId().hashCode()));
            } else {
                log.warn(MessageFormat.format("Cannot manage PRNG of event_id={0}, class={1}",
                        event.getBarEventId(), event.getClass()));
            }
            return event.shouldShowAtMarket(market);
        } catch (Exception e) {
            log.error("canCreate Exception", e);
            return false;
        }
    }

    @Override
    public PortsideBarEvent createBarEvent() {
        if (delegate instanceof SpecBarEventCreator) {
            return new MbmHubMissionBarEventWrapper(delegate.getBarEventId());
        }
        return delegate.createBarEvent();
    }

    @Override
    public float getBarEventFrequencyWeight() {
        return delegate.getBarEventFrequencyWeight();
    }

    public long addDaysToTs(long ts, double days) {
        long result = ts + Math.round(days * MILLISECONDS_PER_DAY);
        return result < ts ? Long.MAX_VALUE : result;
    }

    @Override
    public float getBarEventActiveDuration() {
        return delegate.getBarEventActiveDuration();
    }

    public long getActiveExpirationTs(long ts) {
        return addDaysToTs(ts, getBarEventActiveDuration());
    }

    @Override
    public float getBarEventTimeoutDuration() {
        return delegate.getBarEventTimeoutDuration();
    }
    public long getTimeoutExpirationTs(long ts) {
        return addDaysToTs(ts, getBarEventTimeoutDuration());
    }

    @Override
    public float getBarEventAcceptedTimeoutDuration() {
        return delegate.getBarEventAcceptedTimeoutDuration();
    }
    public long getAcceptedTimeoutExpirationTs(long ts) {
        return addDaysToTs(ts, getBarEventAcceptedTimeoutDuration());
    }

    // See comment in isAlwaysShow; should not be used to see if this is priority (instead call delegate.isPriority)
    @Override
    public boolean isPriority() {
        return isAlwaysShow || delegate.isPriority();
    }

    @Override
    public String getBarEventId() {
        return event_id != null ? event_id : delegate.getBarEventId();
    }

    // Not used by MbmBarEventManager
    @Override
    public boolean wasAutoAdded() {
        return delegate.wasAutoAdded();
    }

    @Override
    public String toString() {
        return delegate.getBarEventId();
    }
}
