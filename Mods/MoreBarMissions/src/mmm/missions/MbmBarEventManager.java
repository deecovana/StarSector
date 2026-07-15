package mmm.missions;

import java.text.MessageFormat;
import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarData;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager.GenericBarEventCreator;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.loading.BarEventSpec;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import mmm.MbmUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

public class MbmBarEventManager extends BaseCampaignEventListener {
    private static final Logger log = Global.getLogger(mmm.missions.MbmBarEventManager.class);
    static {
        if (MbmUtils.DEBUG) {
            log.setLevel(Level.ALL);
        }
    }

    // How many events to show. Note that priority events take up half a slot.
    public static int MAX_ACTIVE_EVENTS;
    // Bonus bar events for player owned colonies.
    public static final int PLAYER_BAR_EVENTS_BONUS = Global.getSettings().getInt("MmmBmPlayerBarEventsBonus");
    // For each market add new bar missions this number of days after the last time anything was added. Note that
    // priority events ignore this limit if they're not in timeout.
    public static float ADD_ACTIVE_COOL_DOWN_DAYS;
    // Beware of overflow if you convert this to float instead of double.
    public static final long MILLISECONDS_PER_DAY = 86400000L;
    // Decay global stats once every this number of days.
    public static final int STATS_DECAY_DAYS = 180;
    // Grants additional weight to events whose frequency is below expectation; this is the maximum frequency ratio.
    public static final float MAX_FREQ_BONUS = 4f;
    // 1/10 of getBarEventAcceptedTimeoutDuration of PlanetaryShieldBarEventCreator or ScientistAICoreBarEventCreator.
    public static final float TIMEOUT_LIMIT = 1000000000f;

    // Map from event id to all the supported BarEventSpec
    public Map<String, MbmBarEventCreator> creators_map = new HashMap<>();
    public float freq_sum;
    public boolean reloaded = false;

    // In order to make our mod a utility mod (can be safely removed without breaking saves), we do not want to store
    // anything that has a class created by our mod. So we will store data in a HashMap<String, long[]> instead on each
    // market.
    // The state machine is as follows:
    // (0) READY: expiration is set to MIN_VALUE.
    //     can transition to ACTIVE if canCreate returns true
    //     updates seed when seed expiration expires (seed expiration is only a thing in READY).
    // (1) ACTIVE: expiration > now and canCreate returns true
    //     transitions to TIMEOUT when accepted, or when expiration <= now, or when canCreate returns false
    //     updates seed when you transitions out of ACTIVE
    // (2) TIMEOUT: expiration > now
    //     transitions to READY when expiration <= now
    //     can transition directly to ACTIVE when canCreate returns true and the manager cannot find enough active
    //     events otherwise.
    //
    // Note that the seed expiration is only checked in READY because its purpose to ensure that if the current seed
    // results in canCreate returning false, it has a chance to change its seed and isn't stuck in READY forever.
    enum State {
        READY,
        ACTIVE,
        TIMEOUT
    }

    // A single EventDataAccessor provides access to data for a single event on a single market.
    public static class EventDataAccessor {
        public static final int STATE_INDEX = 0;
        // In ACTIVE this is when it should transition to TIMEOUT. In TIMEOUT this is when it should transition to
        // READY. In READY this is when the seed should be updated.
        public static final int EXPIRATION_INDEX = 1;
        public static final int SEED_INDEX = 2;
        public static final int ARRAY_SIZE = 3;

        // creator is not used by the accessor itself, but is passed in for convenience for the caller.
        public MbmBarEventCreator creator = null;
        public String event_id;
        public long[] data;

        public EventDataAccessor(String event_id, long[] data) {
            this.event_id = event_id;
            this.data = data;

            // Normalize the data; note that the seed will be updated in READY anyway so we don't bother doing it here.
            if (data[STATE_INDEX] < 0 || data[STATE_INDEX] > 2) {
                data[STATE_INDEX] = 0;
            }
        }

        public EventDataAccessor(MbmBarEventCreator creator, long[] data) {
            this(creator.getBarEventId(), data);
            this.creator = creator;
        }

        // Gets/Sets the state and expiration.
        public State getState() {
            switch ((int) data[STATE_INDEX]) {
                case 0: return State.READY;
                case 1: return State.ACTIVE;
                default: return State.TIMEOUT;
            }
        }
        public void setState(State state, long now, long expiration) {
            State from = getState();
            switch (state) {
                case READY: {
                    data[STATE_INDEX] = 0;
                    break;
                }
                case ACTIVE: {
                    data[STATE_INDEX] = 1;
                    break;
                }
                case TIMEOUT: {
                    data[STATE_INDEX] = 2;
                    break;
                }
            }
            data[EXPIRATION_INDEX] = expiration;
            if (state != from) {  // too noisy otherwise
                log.debug(MessageFormat.format("Updating state from {0} to {1}, exp to {2}, for spec={3}",
                        from, state, getDaysFromNowStr(now, expiration), event_id));
            }
        }

        public long getExpiration() {
            return data[EXPIRATION_INDEX];
        }

        public long getSeed() {
            return data[SEED_INDEX];
        }
        public void updateSeed() {
            data[SEED_INDEX] = Misc.genRandomSeed();
        }
        public void updateSeed(Random random) {
            data[SEED_INDEX] += random.nextLong();
        }

        public String toString(long now) {
            String[] arr = new String[ARRAY_SIZE];
            arr[STATE_INDEX] = getState().toString();
            arr[EXPIRATION_INDEX] = getDaysFromNowStr(now, getExpiration());
            arr[SEED_INDEX] = "" + getSeed();
            return Arrays.toString(arr);
        }
    }

    // A single BarDataAccessor provides access to all data from all events on a single market.
    public static class BarDataAccessor {
        public static final String DATA_KEY = "$mmm_mbm_data";
        public static final String LAST_ADDED_KEY = "$mmm_mbm_last_added";

        public MarketAPI market;
        public Random random;
        public long last_add_ts = Long.MIN_VALUE;
        public Map<String, long[]> data_mapping;
        public Map<String, PortsideBarEvent> events_map;

        BarDataAccessor(Set<String> event_ids, MarketAPI market) {
            this.market = market;
            MemoryAPI memory = market.getMemoryWithoutUpdate();

            // Get/create Random from market memory; we store it to market memory to avoid save scumming.
            final String RANDOM_KEY = "$mmm_mbm_random";
            random = (Random) memory.get(RANDOM_KEY);
            if (random == null) {
                long seed = BarEventManager.getInstance().getSeed(market.getPrimaryEntity(), null, null);
                random = new Random(seed);
                memory.set(RANDOM_KEY, random);
            }

            // Get the last_add_ts if it exists.
            Long ts = (Long) memory.get(LAST_ADDED_KEY);
            if (ts != null) {
                last_add_ts = ts;
            }

            // Populate data_mapping
            data_mapping = (Map<String, long[]>) memory.get(DATA_KEY);
            if (data_mapping == null) {
                data_mapping = new HashMap<>();
                memory.set(DATA_KEY, data_mapping);
            }

            // Adds active events from PortsideBarData to avoid regenerating the same events unnecessarily.
            events_map = new HashMap<>();
            for (PortsideBarEvent event : PortsideBarData.getInstance().getEvents()) {
                if (event_ids.contains(event.getBarEventId())) {
                    events_map.put(event.getBarEventId(), event);
                }
            }
        }

        // Never returns null.
        public EventDataAccessor getAccessor(MbmBarEventCreator creator) {
            // Ensure the array have the same length.
            long[] value = data_mapping.get(creator.getBarEventId());
            if (value != null) {
                if (value.length < EventDataAccessor.ARRAY_SIZE) {
                    long[] new_value = Arrays.copyOf(value, EventDataAccessor.ARRAY_SIZE);
                    Arrays.fill(new_value, value.length, new_value.length, Long.MIN_VALUE);
                    value = new_value;
                    data_mapping.put(creator.getBarEventId(), new_value);
                }
            } else {
                value = new long[EventDataAccessor.ARRAY_SIZE];
                Arrays.fill(value, Long.MIN_VALUE);
                data_mapping.put(creator.getBarEventId(), value);
            }
            return new EventDataAccessor(creator, value);
        }

        // Get or create an PortsideBarEvent associated with the event data.
        public PortsideBarEvent getOrCreateEvent(EventDataAccessor accessor) {
            PortsideBarEvent event = events_map.get(accessor.event_id);
            if (event == null) {
                event = accessor.creator.createBarEvent();
                events_map.put(accessor.event_id, event);
            }
            return event;
        }

        public void updateCoolDownExp(long now) {
            market.getMemoryWithoutUpdate().set(LAST_ADDED_KEY, now);
        }

        // Dump the memory content for this market.
        public String toString(long now) {
            TreeMap<String, String> repr = new TreeMap<>();
            for (Map.Entry<String, long[]> entry : data_mapping.entrySet()) {
                EventDataAccessor accessor = new EventDataAccessor(entry.getKey(), entry.getValue());
                repr.put(entry.getKey(), accessor.toString(now));
            }
            return MessageFormat.format("{0}={1}; {2}={3}", LAST_ADDED_KEY,
                    getDaysFromNowStr(now, last_add_ts), DATA_KEY, repr.toString());
        }
    }

    // Class for keeping and accessing statistics about event frequency.
    public static class StatsAccessor {
        public static final String STATS_KEY = "$mmm_mbm_stats";
        public static final String LAST_DECAYED_KEY = "$mmm_mbm_last_decayed";

        public float freq_sum;  // Sum of BarEventSpec.getFreq().
        public HashMap<String, Float> stats;
        public float value_sum = 0f;  // Sum of stats.

        public StatsAccessor(long now, float freq_sum) {
            this.freq_sum = freq_sum;

            MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
            stats = (HashMap<String, Float>) memory.get(STATS_KEY);
            if (stats == null) {
                stats = new HashMap<>();
                memory.set(STATS_KEY, stats);
            }

            Long last_decayed = (Long) memory.get(LAST_DECAYED_KEY);
            if (last_decayed == null || last_decayed + MILLISECONDS_PER_DAY * STATS_DECAY_DAYS < now) {
                memory.set(LAST_DECAYED_KEY, now);
                for (Map.Entry<String, Float> entry : stats.entrySet()) {
                    entry.setValue(entry.getValue() / 2);
                }
                log.info("Decaying stats");
            }

            for (float value : stats.values()) {
                value_sum += value;
            }
        }

        public void inc(String event_id) {
            value_sum += 1;
            Float value = stats.get(event_id);
            if (value == null) {
                value = 1f;
            } else {
                value += 1;
            }
            stats.put(event_id, value);
        }

        // Adjust freq to obtain weight, giving a boost to events whose actual frequency is lower than expected.
        public float getWeight(String event_id, float freq, float prob) {
            float bonus = MAX_FREQ_BONUS;
            Float value = stats.get(event_id);
            if (value != null) {
                bonus = Math.max(Math.min(freq * prob * value_sum / value / freq_sum, MAX_FREQ_BONUS), 1f);
            }
            return bonus * freq;
        }

        public String toString(long now) {
            TreeMap<String, String> repr = new TreeMap<>();
            for (Map.Entry<String, Float> entry : stats.entrySet()) {
                repr.put(entry.getKey(), String.format("%.2f", entry.getValue()));
            }

            Long last_decayed = (Long) Global.getSector().getMemoryWithoutUpdate().get(LAST_DECAYED_KEY);
            if (last_decayed == null) {
                last_decayed = Long.MIN_VALUE;
            }
            return String.format("%s=%s; freq_sum=%.1f; %s: value_sum=%.2f, stats=%s",
                    LAST_DECAYED_KEY, getDaysFromNowStr(now, last_decayed), freq_sum, STATS_KEY, value_sum, repr);
        }
    }

    // Representing a string representing timestamp ts.
    public static String getDaysFromNowStr(long now, long ts) {
        if (ts == Long.MIN_VALUE) return "-inf";
        if (ts == Long.MAX_VALUE) return "inf";
        return String.format("%.1f days", (ts - now) / (double) MILLISECONDS_PER_DAY);
    }

    public static long getSeedExpirationTs(long now, Random random) {
        // Matches BAR_EVENT_MIN_TIME_BEFORE_CHANGING/BAR_EVENT_MAX_TIME_BEFORE_CHANGING in BarCMD and tracker2 in
        // BarEventManager.
        double days = 20f + 20f * random.nextFloat();
        return now + Math.round(days * MILLISECONDS_PER_DAY);
    }

    public MbmBarEventManager() {
        super(false);  // Don't register permanently.
        load();
    }


    public void load() {
        // Look for supported BarEventSpec (bar_events.csv) and put them in creators_map.
        List<BarEventSpec> event_specs = Global.getSettings().getAllBarEventSpecs();
        int mission_events = 0;
        for (BarEventSpec spec : event_specs) {
            if (spec.isMission() && spec.getFreq() > 0f
                    && spec.getMinTimeout() < TIMEOUT_LIMIT && spec.getMaxTimeout() < TIMEOUT_LIMIT
                    && spec.getMinAcceptedTimeout() < TIMEOUT_LIMIT && spec.getMaxAcceptedTimeout() < TIMEOUT_LIMIT) {
                ++mission_events;
                creators_map.put(spec.getId(), new MbmBarEventCreator(spec.getId()));
            }
        }

        HashSet<String> supported_creators = new HashSet<>();
        try {
            JSONArray array = Global.getSettings().getJSONArray("MmmBmSupportedCreators");
            for (int i = 0; i < array.length(); ++i) {
                supported_creators.add(array.getString(i));
            }

        } catch (JSONException e) {
            log.error("Failed to read MmmBmSupportedCreators: ", e);
        }

        // Remove the supported creators from the BarEventManager.creator. Also look for non-BarEventSpec creators that
        // is whitelisted. We don't have to worry about undoing the change because BarEventManager.readResolve will add
        // them back.
        TreeSet<String> unsupported_creator_ids = new TreeSet<>();
        int specBarEventCreatorsRemoved = 0;
        int creatorsStolen = 0;
        BarEventManager manager = BarEventManager.getInstance();
        Iterator<GenericBarEventCreator> creator_itr = manager.getCreators().iterator();
        while (creator_itr.hasNext()) {
            GenericBarEventCreator creator = creator_itr.next();
            String creator_id = creator.getBarEventId();
            if (creators_map.containsKey(creator_id)) {
                ++specBarEventCreatorsRemoved;
                creator_itr.remove();
            } else if (supported_creators.contains(creator_id)) {
                ++creatorsStolen;
                creator_itr.remove();

                // Note that creator.getBarEventId returns DeliveryBarEventCreator, but the event ID is DeliveryBarEvent
                MbmBarEventCreator mbm_creator = new MbmBarEventCreator(creator);
                creators_map.put(mbm_creator.getBarEventId(), mbm_creator);
            } else {
                unsupported_creator_ids.add(creator_id);
            }
        }

        // Remove the supported events from the BarEventManager.barEventCreators
        Object obj = MbmUtils.reflectionGet(BarEventManager.class, manager, "barEventCreators");
        LinkedHashMap<PortsideBarEvent, GenericBarEventCreator> barEventCreators =
                (LinkedHashMap<PortsideBarEvent, GenericBarEventCreator>) obj;
        int barEventCreatorsRemoved = 0;
        Iterator<PortsideBarEvent> itr0 = barEventCreators.keySet().iterator();
        while (itr0.hasNext()) {
            PortsideBarEvent event = itr0.next();
            if (creators_map.containsKey(event.getBarEventId())) {
                ++barEventCreatorsRemoved;
                itr0.remove();
            }
        }

        // Remove the supported events from the BarEventManager.active
        int active_removed = 0;
        for (PortsideBarEvent event : manager.getActive().getItems()) {
            if (creators_map.containsKey(event.getBarEventId())) {
                ++active_removed;
                manager.getActive().remove(event);
            }
        }

        // Remove the supported creators from the BarEventManager.timeout
        int timeout_removed = 0;
        for (GenericBarEventCreator creator : manager.getTimeout().getItems()) {
            if (creators_map.containsKey(creator.getBarEventId()) ||
                    supported_creators.contains(creator.getBarEventId())) {
                ++timeout_removed;
                manager.getTimeout().remove(creator);
            }
        }

        // If the mod is added to an existing save then PortsideBarData might have the same events with the vanilla
        // wrapper (HubMissionBarEventWrapper), so we need to remove them.
        int psbd_removed = 0;
        Iterator<PortsideBarEvent> itr1 = PortsideBarData.getInstance().getEvents().iterator();
        while (itr1.hasNext()) {
            PortsideBarEvent event = itr1.next();
            if (creators_map.containsKey(event.getBarEventId())) {
                ++psbd_removed;
                itr1.remove();
            }
        }

        // Compute frequency sum
        freq_sum = 0f;
        for (MbmBarEventCreator creator : creators_map.values()) {
            freq_sum += creator.getBarEventFrequencyWeight();
        }

        log.info(MessageFormat.format(
                "MbmBarEventManager: {0} bar event specs, {1} mission events, {2} supported events, " +
                        "{3} supported creators, {4} unsupported creators; stolen {5} creators; " +
                        "removed {6} specBarEventCreators, {7} barEventCreators, {8} active, {9} timeout {10} PortsideBarData",
                event_specs.size(), mission_events, creators_map.size(), supported_creators.size(),
                unsupported_creator_ids.size(), creatorsStolen, specBarEventCreatorsRemoved,
                barEventCreatorsRemoved, active_removed, timeout_removed, psbd_removed));
        log.info("Supported bar event ids: " + new TreeSet<>(creators_map.keySet()));
        log.info("Unsupported bar event ids: " + unsupported_creator_ids);
    }

    // Clear all references to MbmHubMissionBarEventWrapper inside PortsideBarData so the mod can be safely removed.
    public static void unload() {
        Iterator<PortsideBarEvent> itr = PortsideBarData.getInstance().getEvents().iterator();
        while (itr.hasNext()) {
            PortsideBarEvent event = itr.next();
            if (event instanceof MbmHubMissionBarEventWrapper) {
                itr.remove();
            }
        }
    }

    public static String toString(List<PortsideBarEvent> events) {
        ArrayList<String> event_ids = new ArrayList();
        for (PortsideBarEvent event : events) {
            event_ids.add(event.getBarEventId());
        }
        Collections.sort(event_ids);
        return MessageFormat.format("[{0}]", Misc.getAndJoined(event_ids));
    }

    // Computes the active events for this market.
    List<PortsideBarEvent> computeActiveEvents(MarketAPI market) {
        ArrayList<PortsideBarEvent> active_events = new ArrayList<>();
        ArrayList<PortsideBarEvent> priority_events = new ArrayList<>();

        long now = Global.getSector().getClock().getTimestamp();

        BarDataAccessor bar_accessor = new BarDataAccessor(creators_map.keySet(), market);
        Random random = bar_accessor.random;
        log.debug(bar_accessor.toString(now));

        StatsAccessor stats = new StatsAccessor(now, freq_sum);
        log.debug(stats.toString(now));

        // Similar to BarCMD, we want to exclude contacts from bar missions or else our canCreate result might be
        // different from BarCMD; see BarCMD.showOptions for reference.
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        ip.resetExcludeFromGetPerson();
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(ContactIntel.class)) {
            ip.excludeFromGetPerson(((ContactIntel)intel).getPerson());
        }

        for (MbmBarEventCreator creator : creators_map.values()) {
            EventDataAccessor accessor = bar_accessor.getAccessor(creator);
            switch (accessor.getState()) {
                case READY: {
                    // For READY we only need to update seed
                    if (accessor.getExpiration() <= now) {
                        // We set the seed using the random associated with the market here to prevent save scumming.
                        accessor.updateSeed(random);
                        accessor.setState(State.READY, now, getSeedExpirationTs(now, random));
                    }
                    break;
                }
                case ACTIVE: {
                    // For ACTIVE we need to check expiration and canCreate, also update seed if we reach timeout
                    boolean timeout = false;
                    if (accessor.getExpiration() <= now) {
                        timeout = true;
                    } else {
                        PortsideBarEvent event = bar_accessor.getOrCreateEvent(accessor);
                        timeout = !creator.canCreate(market, event, accessor.getSeed());
                    }
                    if (timeout) {
                        accessor.setState(State.TIMEOUT, now, creator.getTimeoutExpirationTs(now));
                        accessor.updateSeed();
                    }
                    break;
                }
                case TIMEOUT: {
                    if (accessor.getExpiration() <= now) {
                        accessor.setState(State.READY, now, getSeedExpirationTs(now, random));
                    }
                    break;
                }
            }
            if (accessor.getState() == State.ACTIVE) {
                if (accessor.creator.delegate.isPriority()) {
                    priority_events.add(bar_accessor.getOrCreateEvent(accessor));
                } else {
                    active_events.add(bar_accessor.getOrCreateEvent(accessor));
                }
            }
        }
        log.debug("Existing active events: " + toString(active_events));
        log.debug("Existing priority active events: " + toString(priority_events));

        ip.resetExcludeFromGetPerson();

        // Now if there's less active events then the max, find more with the following priority:
        // 0. READY priority events
        // 1. READY non-priority events
        // 2. TIMEOUT priority events (if not 1 off)
        // 3. TIMEOUT non-priority events (if not 1 off)
        //
        // If it has been less than ADD_ACTIVE_COOL_DOWN_DAYS since the last time anything was added, only
        // READY priority events can be added. This timestamp is only updated for non-priority events.
        int max_events = market.isPlayerOwned() ? MAX_ACTIVE_EVENTS + PLAYER_BAR_EVENTS_BONUS : MAX_ACTIVE_EVENTS;
        if (active_events.size() + priority_events.size() / 2 < max_events) {
            boolean added = false;
            long ts = bar_accessor.last_add_ts + Math.round((double) ADD_ACTIVE_COOL_DOWN_DAYS * MILLISECONDS_PER_DAY);
            int size = ts <= now ? 4 : 1;
            WeightedRandomPicker<EventDataAccessor>[] pickers = new WeightedRandomPicker[size];
            for (int i = 0; i < pickers.length; ++i) {
                pickers[i] = new WeightedRandomPicker<>(random);
            }

            TreeMap<String, String> repr = new TreeMap<>();
            for (MbmBarEventCreator creator : creators_map.values()) {
                EventDataAccessor accessor = bar_accessor.getAccessor(creator);
                if (accessor.getState() == State.ACTIVE) {
                    continue;
                }

                int index = accessor.getState() == State.TIMEOUT ? 2 : 0;
                if (!creator.delegate.isPriority()) {
                    ++index;
                }
                if (index < pickers.length) {
                    float prob = creator.spec == null ? 1f : creator.spec.getProb();
                    float weight = stats.getWeight(accessor.event_id, creator.getBarEventFrequencyWeight(), prob);
                    repr.put(accessor.event_id, String.format("%.1f", weight));
                    pickers[index].add(accessor, weight);

                }
            }
            log.debug("weights: " + repr);

            ArrayList<String> cannot_creates = new ArrayList<>();
            for (WeightedRandomPicker<EventDataAccessor> picker : pickers) {
                while (active_events.size() + priority_events.size() / 2 < max_events && !picker.isEmpty()) {
                    EventDataAccessor accessor = picker.pickAndRemove();
                    PortsideBarEvent event = bar_accessor.getOrCreateEvent(accessor);
                    if (accessor.creator.canCreate(market, event, accessor.getSeed())) {
                        accessor.setState(State.ACTIVE, now, accessor.creator.getActiveExpirationTs(now));
                        if (accessor.creator.delegate.isPriority()) {
                            priority_events.add(event);
                        } else {
                            active_events.add(event);
                        }
                        stats.inc(accessor.event_id);
                        if (!accessor.creator.delegate.isPriority()) {
                            added = true;
                        }
                    } else {
                        cannot_creates.add(accessor.event_id);
                    }
                }
            }
            log.debug("cannot_creates: " + cannot_creates);

            if (added) {
                bar_accessor.updateCoolDownExp(now);
            }
        }
        active_events.addAll(priority_events);
        log.debug("All active events: " + toString(active_events));
        return active_events;
    }

    // Sanity check for weird "markets" that can cause crash with some certain missions.
    public static boolean marketIsEligible(MarketAPI market) {
        return market != null && market.getStarSystem() != null && market.getPrimaryEntity() != null &&
                market.getFaction() != null && !market.getFactionId().equals(Factions.NEUTRAL) &&
                !market.getMemoryWithoutUpdate().getBoolean("$noBar");
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        // Call load again upon first market visit in case some mod added more supported creators.
        if (!reloaded) {
            load();
            reloaded = true;
        }

        if (!marketIsEligible(market)) return;

        // Go to the market and update the state machine for each event.
        List<PortsideBarEvent> active_events = computeActiveEvents(market);

        // Now that we have our active events, ensure that they are in $BarCMD_shownEvents if the key exists.
        final String KEY = "$BarCMD_shownEvents";
        MemoryAPI memory = market.getMemoryWithoutUpdate();
        if (memory.contains(KEY)) {
            List<String> event_ids = (List<String>) memory.get(KEY);
//            log.debug("$BarCMD_shownEvents=" + event_ids);
            for (PortsideBarEvent event : active_events) {
                if (!event_ids.contains(event.getBarEventId())) {
                    event_ids.add(event.getBarEventId());
//                    log.debug("Added " + event.getBarEventId() + " to $BarCMD_shownEvents" + market.getName());
                }
            }
        }

        // Finally synchronize with PortsideBarData.
        PortsideBarData data = PortsideBarData.getInstance();
        Iterator<PortsideBarEvent> event_itr = data.getEvents().iterator();
        while (event_itr.hasNext()) {
            PortsideBarEvent event = event_itr.next();
            if (creators_map.containsKey(event.getBarEventId())) {
                if (!active_events.remove(event)) {
                    event_itr.remove();
                }
            }
        }
        for (PortsideBarEvent event : active_events) {
            data.addEvent(event);
        }

        ArrayList<PortsideBarEvent> events = new ArrayList(data.getEvents());
        // Same sort order as BarCMD
        Collections.sort(events, new Comparator<PortsideBarEvent>() {
            public int compare(PortsideBarEvent o1, PortsideBarEvent o2) {
                boolean p1 = o1.isAlwaysShow();
                boolean p2 = o2.isAlwaysShow();
                if (p1 && !p2) return -1;
                if (p2 && !p1) return 1;
                return 0;
            }
        });
        TreeSet<String> event_ids = new TreeSet<>();
        for (PortsideBarEvent event : events) {
//            if (event.shouldShowAtMarket(market)) {
                event_ids.add(event.getBarEventId());
//            }
        }

        log.debug("reportPlayerOpenedMarket done; PortsideBarData: " + event_ids);
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        if (!marketIsEligible(market)) return;
        long now = Global.getSector().getClock().getTimestamp();

        // BarCMD calls BarEventManager.notifyWasInteractedWith, which removes the event from PortsideBarData.
        // So we can look for active events missing from PortsideBarData to know what has been accepted.
        BarDataAccessor bar_accessor = new BarDataAccessor(creators_map.keySet(), market);
//        Random random = bar_accessor.random;
        ArrayList<PortsideBarEvent> accepted_events = new ArrayList<>();
        for (MbmBarEventCreator creator : creators_map.values()) {
            EventDataAccessor accessor = bar_accessor.getAccessor(creator);
            if (accessor.getState() == State.ACTIVE) {
                accepted_events.add(bar_accessor.getOrCreateEvent(accessor));
            }
        }
        for (PortsideBarEvent event : PortsideBarData.getInstance().getEvents()) {
            accepted_events.remove(event);
        }

        // For newly accepted events, we want to transition them to TIMEOUT while updating seed.
        for (PortsideBarEvent event : accepted_events) {
            EventDataAccessor accessor = bar_accessor.getAccessor(creators_map.get(event.getBarEventId()));
            // Note that in BarEventManager both timeout and accepted timeout are used, effectively, as an event leaving
            // active also triggers timeout.
            accessor.setState(State.TIMEOUT, now, accessor.creator.getAcceptedTimeoutExpirationTs(now));
            accessor.updateSeed();
        }
    }
}
