/*
 * Copyright (C) 2020 Tobi.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package net.sf.jaer.util.textio;

import java.io.File;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.eventprocessing.EventFilter2D;

/**
 * Abstract class for text IO filters for DAVIS cameras.
 *
 * @author Tobi
 */
public abstract class AbstractDavisTextIo extends EventFilter2D {

    protected static String DEFAULT_FILENAME = "JAEERDavisTextIO.txt";
    protected final int LOG_EVERY_THIS_MANY_EVENTS = 10000; // for logging concole messages
    protected String lastFileName = getString("lastFileName", DEFAULT_FILENAME);
    protected File lastFile = null;
    protected int eventsProcessed = 0;
    protected int lastLineNumber = 0;
    protected boolean useCSV = getBoolean("useCSV", false);
    protected boolean useUsTimestamps = getBoolean("useUsTimestamps", false);
    protected boolean useSignedPolarity = getBoolean("useSignedPolarity", false);

    public AbstractDavisTextIo(AEChip chip) {
        super(chip);
        setPropertyTooltip("eventsProcessed", "READONLY, shows number of events read");
        setPropertyTooltip("useCSV", "use CSV (comma separated) format rather than space separated values");
        setPropertyTooltip("useUsTimestamps", "use us int timestamps rather than float time in seconds");
        setPropertyTooltip("useSignedPolarity", "use -1/+1 OFF/ON polarity rather than 0,1 OFF/ON polarity");
    }

    /**
     * @param useCSV the useCSV to set
     */
    public void setUseCSV(boolean useCSV) {
        this.useCSV = useCSV;
        putBoolean("useCSV", useCSV);
    }

    /**
     * @param useUsTimestamps the useUsTimestamps to set
     */
    public void setUseUsTimestamps(boolean useUsTimestamps) {
        this.useUsTimestamps = useUsTimestamps;
        putBoolean("useUsTimestamps", useUsTimestamps);
    }

    /**
     * @return the eventsProcessed
     */
    public int getEventsProcessed() {
        return eventsProcessed;
    }

    /**
     * @param eventsProcessed the eventsProcessed to set
     */
    public void setEventsProcessed(int eventsProcessed) {
        int old = this.eventsProcessed;
        this.eventsProcessed = eventsProcessed;
        if (eventsProcessed % LOG_EVERY_THIS_MANY_EVENTS == 0) {
            getSupport().firePropertyChange("eventsProcessed", old, eventsProcessed);
        }
    }

    /**
     * @return the useSignedPolarity
     */
    public boolean isUseSignedPolarity() {
        return useSignedPolarity;
    }

    /**
     * @param useSignedPolarity the useSignedPolarity to set
     */
    public void setUseSignedPolarity(boolean useSignedPolarity) {
        this.useSignedPolarity = useSignedPolarity;
        putBoolean("useSignedPolarity",useSignedPolarity);
    }

}
