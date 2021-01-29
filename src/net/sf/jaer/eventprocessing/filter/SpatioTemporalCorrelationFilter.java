/* RetinaBackgrondActivityFilter.java
 *
 * Created on October 21, 2005, 12:33 PM */
package net.sf.jaer.eventprocessing.filter;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Random;

import net.sf.jaer.Description;
import net.sf.jaer.DevelopmentStatus;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.ApsDvsEvent;
import net.sf.jaer.event.BasicEvent;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.eventio.AEInputStream;
import static net.sf.jaer.eventprocessing.EventFilter.log;
import net.sf.jaer.util.RemoteControlCommand;

/**
 * A BA noise filter derived from BackgroundActivityFilter that only passes
 * events that are supported by at least some fraction of neighbors in the past
 * {@link #setDt dt} in the immediate spatial neighborhood, defined by a
 * subsampling bit shift.
 *
 * @author tobi, with discussion with Moritz Milde, Dave Karpul, Elisabetta
 * Chicca, Chiara Bartolozzi Telluride 2017
 */
@Description("Filters out uncorrelated noise events based on work at Telluride 2017 with discussion with Moritz Milde, Dave Karpul, Elisabetta\n"
        + " * Chicca, and Chiara Bartolozzi ")
@DevelopmentStatus(DevelopmentStatus.Status.Stable)
public class SpatioTemporalCorrelationFilter extends AbstractNoiseFilter {

    private int numMustBeCorrelated = getInt("numMustBeCorrelated", 5);
    protected boolean favorLines = getBoolean("favorLines", false);

    private int sx; // size of chip minus 1
    private int sy;
    private int ssx; // size of subsampled timestamp map
    private int ssy;

    private class LastEvent {

        boolean wasSent = false;
        BasicEvent event = new ApsDvsEvent();

        public LastEvent() {
            event.timestamp=DEFAULT_TIMESTAMP;
        }
    }
    
//    private int[][] lastTimesMap;
    private LastEvent[][] lastEventMap; // 2d array of most recent events at each pixel, used for correlation checking and anticausal filtering
    private int ts = 0; // used to reset filter

    public SpatioTemporalCorrelationFilter(AEChip chip) {
        super(chip);
        setPropertyTooltip(TT_FILT_CONTROL, "numMustBeCorrelated", "At least this number of 9 (3x3) neighbors (including our own event location) must have had event within past dt");
        setPropertyTooltip(TT_FILT_CONTROL, "favorLines", "add condition that events in 8-NNb must lie along line crossing pixel to pass");
        getSupport().addPropertyChangeListener(AEInputStream.EVENT_REWOUND, this);
    }

    /**
     * filters in to out. if filtering is enabled, the number of out may be less
     * than the number putString in
     *
     * @param in input events can be null or empty.
     * @return the processed events, may be fewer in number. filtering may occur
     * in place in the in packet.
     */
    @Override
    synchronized public EventPacket<? extends BasicEvent> filterPacket(EventPacket<? extends BasicEvent> in) {
        super.filterPacket(in);
        ArrayList<LastEvent> possiblePastEventsList = new ArrayList();
//        if (lastEventMap == null) {
//            allocateMaps(chip);
//        }
        int dt = (int) Math.round(getCorrelationTimeS() * 1e6f);
        ssx = sx >> subsampleBy;
        ssy = sy >> subsampleBy;
        // for each event only keep it if it is within dt of the last time
        // an event happened in the direct neighborhood
        final boolean record = recordFilteredOutEvents; // to speed up loop, maybe
        final boolean fhp = filterHotPixels;
        final NnbRange nnbRange = new NnbRange();
        int firstTs = in.getFirstTimestamp();
        if (record) { // branch here to save a tiny bit if not instrumenting denoising
            for (BasicEvent e : in) {
//                if (e == null) {
//                    continue;
//                }
                if (e.isSpecial()) {
                    continue;
                }
                totalEventCount++;
                final int ts = e.timestamp;
                final int x = (e.x >> subsampleBy), y = (e.y >> subsampleBy); // subsampling address
                if ((x < 0) || (x > ssx) || (y < 0) || (y > ssy)) { // out of bounds, discard (maybe bad USB or something)
                    filterOut(e);
                    continue;
                }
                if (lastEventMap[x][y].event.timestamp == DEFAULT_TIMESTAMP) {
                    lastEventMap[x][y].event.copyFrom(e);
                    if (letFirstEventThrough) {
                        filterIn(e);
                        continue;
                    } else {
                        filterOut(e);
                        continue;
                    }
                }

                // finally the real denoising starts here
                int ncorrelated = 0;
                byte nnb = 0;
                int bit = 0;
                nnbRange.compute(x, y, ssx, ssy);
                possiblePastEventsList.clear();
                outerloop:
                for (int xx = nnbRange.x0; xx <= nnbRange.x1; xx++) {
                    final LastEvent[] col = lastEventMap[xx];
                    for (int yy = nnbRange.y0; yy <= nnbRange.y1; yy++) {
                        if (fhp && xx == x && yy == y) {
                            continue; // like BAF, don't correlate with ourself
                        }
                        final int lastT = col[yy].event.timestamp;
                        final int deltaT = (ts - lastT); // note deltaT will be very negative for DEFAULT_TIMESTAMP because of overflow

                        boolean occupied = false;
                        if (lastT != DEFAULT_TIMESTAMP && deltaT < dt && deltaT >= 0) { // ignore correlations for DEFAULT_TIMESTAMP that are neighbors which never got event so far
                            ncorrelated++;
                            occupied = true;
                            possiblePastEventsList.add(lastEventMap[xx][yy]); // save previous event if this event turns out to be filtered in
                        }
                        if (occupied) {
                            // nnb bits are like this
                            // 0 3 5
                            // 1 x 6
                            // 2 4 7
                            nnb |= (0xff & (1 << bit));
                        }
                        bit++;
                    }
                }
                if (ncorrelated < numMustBeCorrelated) {
                    filterOutWithNNb(e, nnb);
                } else {
                    if (!favorLines) {
                        filterInWithNNb(e, nnb);
                        if (antiCasualEnabled) {
                            for (LastEvent pe : possiblePastEventsList) {
                                // if there was past event in tau window that was not already filtered in, filter it in
                                // but only if it is definitely in current packet
                                if (pe.wasSent) {
                                    continue;
                                }
                                
                                BasicEvent oe=filterIn(pe.event); // put back the previous event
                                oe.timestamp=ts; // and give it current timstamp to make it monotonic in time
                                pe.wasSent = true;
                            }
                        }
                    } else {
                        // only pass events that have bits set that form line with current pixel, at 45 degrees on 8 NNbs
                        if ((nnb & 0x81) == 0x81 || (nnb & 0x18) == 0x18 || (nnb & 0x24) == 0x24 || (nnb & 0x42) == 0x42) {
                            filterInWithNNb(e, nnb);
                            if (antiCasualEnabled) {
                                for (LastEvent pe : possiblePastEventsList) {
                                    // if there was past event in tau window that was not already filtered in, filter it in
                                    if (pe.wasSent) {
                                        continue;
                                    }
                                    BasicEvent oe=filterIn(pe.event);
                                    oe.timestamp=ts;
                                    pe.wasSent = true;
                                }
                            }
                        }
                    }
                }
                lastEventMap[x][y].event.copyFrom(e);
                // the event is no longer valid to filter in.
                // when we filter it back in anti-causially, then we need to make sure it is within current packet
                lastEventMap[x][y].wasSent = !e.isFilteredOut();
            } // event packet loop
        } else { // not keep stats
            for (BasicEvent e : in) {
                if (e == null) {
                    continue;
                }
                if (e.isSpecial()) {
                    continue;
                }
                totalEventCount++;
                final int x = (e.x >> subsampleBy), y = (e.y >> subsampleBy); // subsampling address
                if ((x < 0) || (x > ssx) || (y < 0) || (y > ssy)) { // out of bounds, discard (maybe bad USB or something)
                    filterOut(e);
                    continue;
                }
                if (lastEventMap[x][y].event.timestamp == DEFAULT_TIMESTAMP) {
                    lastEventMap[x][y].event = e;
                    if (letFirstEventThrough) {
                        filterIn(e);
                        continue;
                    } else {
                        filterOut(e);
                        continue;
                    }
                }

                // finally the real denoising starts here
                int ncorrelated = 0;
                nnbRange.compute(x, y, ssx, ssy);
                outerloop:
                for (int xx = nnbRange.x0; xx <= nnbRange.x1; xx++) {
                    final LastEvent[] col = lastEventMap[xx];
                    for (int yy = nnbRange.y0; yy <= nnbRange.y1; yy++) {
                        if (fhp && xx == x && yy == y) {
                            continue; // like BAF, don't correlate with ourself
                        }
                        final int lastT = col[yy].event.timestamp;
                        final int deltaT = (ts - lastT); // note deltaT will be very negative for DEFAULT_TIMESTAMP because of overflow

                        if (deltaT < dt && lastT != DEFAULT_TIMESTAMP) { // ignore correlations for DEFAULT_TIMESTAMP that are neighbors which never got event so far
                            ncorrelated++;
                            if (ncorrelated >= numMustBeCorrelated) {
                                break outerloop; // csn stop checking now
                            }
                        }
                    }
                }
                if (ncorrelated < numMustBeCorrelated) {
                    filterOut(e);
                } else {
                    filterIn(e);
                }
                lastEventMap[x][y].event = e;
            }
        }
        getNoiseFilterControl().maybePerformControl(in);
        return out;
    }

    @Override
    public synchronized final void resetFilter() {
        super.resetFilter();
        log.info("resetting SpatioTemporalCorrelationFilter");
        for (LastEvent[] r : lastEventMap) {
            for (LastEvent e : r) {
                e.event.timestamp = DEFAULT_TIMESTAMP;
                e.wasSent = true;  // make sure not to send any of old events
            }
        }
    }

    @Override
    public final void initFilter() {
        sx = chip.getSizeX() - 1;
        sy = chip.getSizeY() - 1;
        ssx = sx >> subsampleBy;
        ssy = sy >> subsampleBy;
        allocateMaps(chip);
        resetFilter();
    }

    private void allocateMaps(AEChip chip) {
        if ((chip != null) && (chip.getNumCells() > 0) && (lastEventMap == null || lastEventMap.length != chip.getSizeX() >> subsampleBy)) {
            lastEventMap = new LastEvent[chip.getSizeX()][chip.getSizeY()]; // TODO handle subsampling to save memory (but check in filterPacket for range check optomization)
            for (LastEvent[] r : lastEventMap) {
                for (int i = 0; i < r.length; i++) {
                    r[i] = new LastEvent();
                }
            }
        }
    }

    /**
     * Fills lastTimesMap with waiting times drawn from Poisson process with
     * rate noiseRateHz
     *
     * @param noiseRateHz rate in Hz
     * @param lastTimestampUs the last timestamp; waiting times are created
     * before this time
     */
    @Override
    public void initializeLastTimesMapForNoiseRate(float noiseRateHz, int lastTimestampUs) {
        Random random = new Random();
        for (final LastEvent[] arrayRow : lastEventMap) {
            for (int i = 0; i < arrayRow.length; i++) {
                final double p = random.nextDouble();
                final double t = -noiseRateHz * Math.log(1 - p);
                final int tUs = (int) (1000000 * t);
                arrayRow[i].event.timestamp = lastTimestampUs - tUs;
            }
        }
    }

    // </editor-fold>
    /**
     * @return the letFirstEventThrough
     */
    public boolean isLetFirstEventThrough() {
        return letFirstEventThrough;
    }

    /**
     * @param letFirstEventThrough the letFirstEventThrough to set
     */
    public void setLetFirstEventThrough(boolean letFirstEventThrough) {
        this.letFirstEventThrough = letFirstEventThrough;
        putBoolean("letFirstEventThrough", letFirstEventThrough);
    }

    /**
     * @return the numMustBeCorrelated
     */
    public int getNumMustBeCorrelated() {
        return numMustBeCorrelated;
    }

    /**
     * @param numMustBeCorrelated the numMustBeCorrelated to set
     */
    public void setNumMustBeCorrelated(int numMustBeCorrelated) {
        if (numMustBeCorrelated < 1) {
            numMustBeCorrelated = 1;
        } else if (numMustBeCorrelated > getNumNeighbors()) {
            numMustBeCorrelated = getNumNeighbors();
        }
        putInt("numMustBeCorrelated", numMustBeCorrelated);
        this.numMustBeCorrelated = numMustBeCorrelated;
        getSupport().firePropertyChange("numMustBeCorrelated", this.numMustBeCorrelated, numMustBeCorrelated);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        if (evt.getPropertyName() == AEInputStream.EVENT_REWOUND) {
            resetFilter();
        }
    }

    private String USAGE = "SpatioTemporalFilter needs at least 2 arguments: noisefilter <command> <args>\nCommands are: setParameters dt xx numMustBeCorrelated xx\n";

    // remote control for experiments e.g. with python / UDP remote control 
    @Override
    public String setParameters(RemoteControlCommand command, String input) {
        String[] tok = input.split("\\s");

        if (tok.length < 3) {
            return USAGE;
        }
        try {

            if ((tok.length - 1) % 2 == 0) {
                for (int i = 1; i < tok.length; i++) {
                    if (tok[i].equals("dt")) {
                        setCorrelationTimeS(1e-6f * Integer.parseInt(tok[i + 1]));
                    } else if (tok[i].equals("num")) {
                        setNumMustBeCorrelated(Integer.parseInt(tok[i + 1]));
                    }
                }
                String out = "successfully set SpatioTemporalFilter parameters dt " + String.valueOf(getCorrelationTimeS()) + " and numMustBeCorrelated " + String.valueOf(numMustBeCorrelated);
                return out;
            } else {
                return USAGE;
            }

        } catch (Exception e) {
            return "IOExeption in remotecontrol" + e.toString() + "\n";
        }
    }

    @Override
    public String infoString() {
        String s = super.infoString() + " k=" + numMustBeCorrelated;
        return s;
    }

    /**
     * @return the favorLines
     */
    public boolean isFavorLines() {
        return favorLines;
    }

    /**
     * @param favorLines the favorLines to set
     */
    public void setFavorLines(boolean favorLines) {
        this.favorLines = favorLines;
        putBoolean("favorLines", favorLines);
    }

}
