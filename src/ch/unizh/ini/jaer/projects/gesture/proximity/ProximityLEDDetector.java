/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.unizh.ini.jaer.projects.gesture.proximity;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import java.awt.Font;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;


import net.sf.jaer.Description;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.BasicEvent;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.event.PolarityEvent;
import net.sf.jaer.eventprocessing.EventFilter2D;
import net.sf.jaer.graphics.FrameAnnotater;
import net.sf.jaer.hardwareinterface.usb.cypressfx2.HasLEDControl;

import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Detects proximity of hand or object by looking for events generated by response of sensor to flashing LED which illuminates the nearby scene.
 * Fires PROXMITY PropertyChange events on change of detected proximity.
 * 
 * @author tobi
 * @see #PROXIMITY
 */
@Description("Proximity detection using flashing LED that illuminates nearby objects")
public class ProximityLEDDetector extends EventFilter2D implements Observer, FrameAnnotater {

	// parameters
	private int histNumBins = getInt("histNumBins", 10);
	private int histBinSizeUs = getInt("histBinSizeUs", 1000);
	private float histCountScale = getFloat("histCountScale", 0.1f);
	private long periodMs = getInt("periodMs", 20);
	private int countThreshold = getInt("countThreshold", 100);
	private float maxDeviationMs = getFloat("maxDeviationMs", 2);
	private int binDurationUs = getInt("binDurationUs", 100);
	private int durationAfterFlashToCountUs = getInt("durationAfterFlashToCountUs", 4000);
	// fields
	private HasLEDControl ledControl = null;
	private boolean proximityDetected = false;
	/** Event that is fired on change of proximity */
	public static final String PROXIMITY = "proximityDetected";
	private Timer ledTimer = null;
	private int lastLEDChangeTimestampUs = 0;
	private boolean lastLEDOn = true;
	private long lastCommandSentNs = 0;
	private Histogram histOn = new Histogram(), histOff = new Histogram();
	private TextRenderer renderer;
	private EventCounter eventCounter = new EventCounter();

	class EventCounter {

		public EventCounter() {
			init();
		}
		int[] eventCountBins = null;
		int nbins = 0;

		synchronized void init() {
			nbins = ((2 * durationAfterFlashToCountUs) / binDurationUs) + 1; // BEFORE  and after flash
			eventCountBins = new int[nbins];
		}

		void reset() {
			Arrays.fill(eventCountBins, 0);
		}

		synchronized void putEvent(int dt) {
			int bin = bin(dt);
			if (bin >= 0) {
				eventCountBins[bin]++;
			}
		}

		private int bin(int dt) {
			if ((dt >= 0) && (dt < durationAfterFlashToCountUs)) {
				return (nbins / 2) + (dt / binDurationUs);
			} else if ((dt < 0) && (dt > -durationAfterFlashToCountUs)) {
				return (nbins / 2) + (dt / binDurationUs);
			} else {
				return -1;
			}
		}

		void draw(GL2 gl) {
			gl.glColor3f(1, 1, 1);
			gl.glLineWidth(4);
			int x0 = 10, y0 = 10;
			float xsc = (chip.getSizeX() - (2 * x0)) / (float) (nbins);
			float ysc = (chip.getSizeY() - (2 * y0)) * histCountScale;
			gl.glBegin(GL.GL_LINE_STRIP);
			for (int i = 0; i < nbins; i++) {
				gl.glVertex2f(x0 + (i * xsc), y0 + (eventCountBins[i] * ysc));
				gl.glVertex2f(x0 + ((i + 1) * xsc), y0 + (eventCountBins[i] * ysc));
			}
			gl.glEnd();
		}
	}

	public enum Method {

		Histogram, RatioBeforeAfter
	};
	private Method method = Method.RatioBeforeAfter;

	{
		try {
			setMethod(Method.valueOf(getPrefs().get("method", Method.RatioBeforeAfter.toString())));
		} catch (Exception e) {
		}
	}

	public ProximityLEDDetector(AEChip chip) {
		super(chip);
		String hist = "Histogram", ratio = "RatioBeforeAfter";
		setPropertyTooltip(hist, "histCountScale", "scale for delta time histogram counts after sync pulses");
		setPropertyTooltip(hist, "histBinSizeUs", "histogram bin size in us after LED change");
		setPropertyTooltip(hist, "histNumBins", "number of histogram bins");
		setPropertyTooltip(hist, "maxDeviationMs", "maximum standard deviation in ms around mean delta time to consider events as resulting from LED change");
		setPropertyTooltip(hist, "countThreshold", "min number of events to capture after LED change to consider for proximity detection");
		setPropertyTooltip(ratio, "binDurationUs", "bin size in us to collect event counts before and after LED change");
		setPropertyTooltip(ratio, "durationAfterFlashToCountUs", "how long in us to count events after LED turns on or off");

		setPropertyTooltip("periodMs", "LED flashing period in ms");
		setPropertyTooltip("proximityDetected", "indicates detected proximity");
		chip.addObserver(this);
	}

	@Override
	public EventPacket<?> filterPacket(EventPacket<?> in) {
		switch (method) {
			case Histogram:
				for (BasicEvent o : in) {
					PolarityEvent e = (PolarityEvent) o;
					if (e.isSpecial()) { // got sync event indicating that camera has changed LED
						if (ledControl.getLEDState(0) == HasLEDControl.LEDState.ON) {
							lastLEDOn = true;
							histOn.reset();
						} else {
							lastLEDOn = false; // TODO sloppy
							histOff.reset();
						}
						lastLEDChangeTimestampUs = e.timestamp;
					} else {
						int dt = e.timestamp - lastLEDChangeTimestampUs;
						if (lastLEDOn) {
							histOn.put(dt);
						} else {
							histOff.put(dt);
						}
					}
				}
				histOn.computeStats();
				histOff.computeStats();
				if (histOn.proximityDetected() && histOff.proximityDetected()) {
					setProximityDetected(true);
				} else {
					setProximityDetected(false);
				}
				break;
			case RatioBeforeAfter:
				for (BasicEvent o : in) {
					PolarityEvent e = (PolarityEvent) o;
					if (e.isSpecial()) { // got sync event indicating that camera has changed LED
						if (ledControl.getLEDState(0) == HasLEDControl.LEDState.ON) {
							lastLEDOn = true;
						} else {
							lastLEDOn = false; // TODO sloppy
						}
						lastLEDChangeTimestampUs = e.timestamp;
						eventCounter.reset();
					} else {
						int dt = e.timestamp - lastLEDChangeTimestampUs;
						eventCounter.putEvent(dt); // TODO dt is always >0 here, should be <0 for events before flash
					}
				}
				// TODO decide proximity
				break;
		}
		return in;

	}

	@Override
	public void annotate(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		if (renderer == null) {
			renderer = new TextRenderer(new Font("Monospaced", Font.PLAIN, 24), true, true);
		}
		switch (method) {
			case Histogram:
				histOn.draw(gl, 80);
				histOn.draw(gl, 40);
				break;
			case RatioBeforeAfter:
				eventCounter.draw(gl);
		}
		if(proximityDetected){
			renderer.begin3DRendering();
			gl.glColor3f(0,1,0);
			renderer.draw3D("I see you!", 20, chip.getSizeY()/2,0,.7f);
			renderer.end3DRendering();
		}
	}

	/**
	 * @return the histNumBins
	 */
	public int getHistNumBins() {
		return histNumBins;
	}

	/**
	 * @param histNumBins the histNumBins to set
	 */
	synchronized public void setHistNumBins(int histNumBins) {
		this.histNumBins = histNumBins;
		histOn.init();
		histOff.init();
		putInt("histNumBins", histNumBins);
	}

	/**
	 * @return the histBinSizeUs
	 */
	public int getHistBinSizeUs() {
		return histBinSizeUs;
	}

	/**
	 * @param histBinSizeUs the histBinSizeUs to set
	 */
	public void setHistBinSizeUs(int histBinSizeUs) {
		this.histBinSizeUs = histBinSizeUs;
		putInt("histBinSizeUs", histBinSizeUs);
	}

	/**
	 * @return the histCountScale
	 */
	public float getHistCountScale() {
		return histCountScale;
	}

	/**
	 * @param histCountScale the histCountScale to set
	 */
	public void setHistCountScale(float histCountScale) {
		this.histCountScale = histCountScale;
		putFloat("histCountScale", histCountScale);
	}

	/**
	 * @return the periodMs
	 */
	public int getPeriodMs() {
		return (int) periodMs;
	}

	/**
	 * @param periodMs the periodMs to set
	 */
	public void setPeriodMs(int periodMs) {
		this.periodMs = periodMs;
		putInt("periodMs", periodMs);
		if (isFilterEnabled()) {
			setFilterEnabled(false);
			setFilterEnabled(true);
		}
	}

	/**
	 * @return the countThreshold
	 */
	public int getCountThreshold() {
		return countThreshold;
	}

	/**
	 * @param countThreshold the countThreshold to set
	 */
	public void setCountThreshold(int countThreshold) {
		this.countThreshold = countThreshold;
		putInt("countThreshold", countThreshold);
	}

	/**
	 * @return the maxDeviationMs
	 */
	public float getMaxDeviationMs() {
		return maxDeviationMs;
	}

	/**
	 * @param maxDeviationMs the maxDeviationMs to set
	 */
	public void setMaxDeviationMs(float maxDeviationMs) {
		this.maxDeviationMs = maxDeviationMs;
		putFloat("maxDeviationMs", maxDeviationMs);
	}

	private class Histogram {

		int[] counts = null;
		int overflowCount = 0;
		float meanCount = 0, meanBin = 0, stdCount = 0, stdBin = 0;
		int sumCounts = 0;

		public Histogram() {
			init();
		}

		private void init() {
			counts = new int[histNumBins];
			overflowCount = 0;
			meanCount = 0;
			meanBin = 0;
			stdCount = 0;
			stdBin = 0;
			sumCounts = 0;
		}

		synchronized void reset() {
			Arrays.fill(counts, 0);
			overflowCount = 0;
		}

		synchronized void put(int dt) {
			if (dt < 0) {
				return;
			}
			int bin = dt / histBinSizeUs;
			//            System.out.println("dt="+dt+" bin="+bin);
			if (bin >= histNumBins) {
				overflowCount++;
				return;
			}
			counts[bin]++;
		}

		synchronized void draw(GL2 gl, int y) {
			int x = 10;
			float histBinWidthPix = (float) (chip.getSizeX() - (2 * x)) / histNumBins;
			if (proximityDetected) {
				gl.glColor3f(1, 0, 0);
			} else {
				gl.glColor3f(0, 0, 1);
			}
			gl.glLineWidth(4);
			gl.glBegin(GL.GL_LINE_STRIP);
			for (int i = 0; i < histNumBins; i++) {
				float yy = y + (counts[i] * histCountScale);
				gl.glVertex2f(x + (i * histBinWidthPix), yy);
				gl.glVertex2f(1 + x + (i * histBinWidthPix), yy);
				//                System.out.print(bins[i]+" ");
			}
			//            System.out.println("");
			gl.glEnd();
			renderer.begin3DRendering();
			String s = String.format("count=%6.1f + %-6.1f mean dt=%6.1f+ %-6.1f ms", meanCount, stdCount, (histBinSizeUs * meanBin) / 1000, (histBinSizeUs * stdBin) / 1000);
			final float scale = .2f;
			renderer.draw3D(s, x, y - 5, 0, scale);
			//        Rectangle2D bounds=renderer.getBounds(s);
			renderer.end3DRendering();
		}

		// call this before gettings stats
		synchronized void computeStats() {
			sumCounts = 0;
			int sumCounts2 = 0;
			float weightedCount = 0;
			for (int i = 0; i < histNumBins; i++) {
				//                if (i == histNumBins / 2) {
				//                    counts[i] = 10;
				//                } else {
				//                    counts[i] = 10; // TODO debug
				//                }
				int b = counts[i];
				sumCounts += b;
				sumCounts2 += b * b;
				float wb = counts[i] * (.5f + i);
				weightedCount += wb;
			}
			meanCount = (float) sumCounts / histNumBins;
			if (sumCounts == 0) {
				meanBin = Float.NaN;
			} else {
				meanBin = weightedCount / sumCounts;
			}
			float norm = (histNumBins * (histNumBins - 1));
			stdCount = (float) Math.sqrt(((histNumBins * sumCounts2) - (sumCounts * sumCounts)) / norm);
			if (Float.isNaN(meanBin)) {
				stdBin = Float.NaN;
			} else {
				float sumSq = 0;
				for (int i = 0; i < histNumBins; i++) {
					float dev = ((i + 0.5f) - meanBin);
					float dev2 = dev * dev;
					sumSq += counts[i] * dev2;
				}
				float varBin = sumSq / sumCounts;
				stdBin = (float) Math.sqrt(varBin);
			}
			//            System.out.println("meanCount="+meanCount+"\tstdCount="+stdCount+"\tmeanBin="+meanBin+"\tstdBin="+stdBin+"\tstdCount="+stdCount);
		}

		synchronized float meanDeltaTimeUs() {
			return meanCount * histBinSizeUs;
		}

		synchronized float covBin() {
			if (Float.isNaN(meanBin)) {
				return Float.NaN;
			} else {
				return stdBin / meanBin;
			}
		}

		synchronized boolean proximityDetected() {
			if (sumCounts < countThreshold) {
				return false;
			}
			if (((stdBin * histBinSizeUs) / 1000) > maxDeviationMs) {
				return false;
			}
			if ((meanBin * histBinSizeUs) > 5000) { // TODO make parameter
				return false;
			}
			return true;
		}
	}

	private class LEDSetterTask extends TimerTask {

		@Override
		public void run() {
			if (ledControl != null) {
				switch (ledControl.getLEDState(0)) {
					case ON:
						ledControl.setLEDState(0, HasLEDControl.LEDState.OFF);
						break;
					case OFF:
					case UNKNOWN:
					case FLASHING:
						ledControl.setLEDState(0, HasLEDControl.LEDState.ON);
				}
				long ns = System.nanoTime();
				long dt = ns - lastCommandSentNs;
				lastCommandSentNs = ns;
				//                log.info(dt / 1000 + " us since last command sent");
			}
		}
	}

	public boolean isProximityDetected() {
		return proximityDetected;
	}

	public void setProximityDetected(boolean yes) {
		boolean old = proximityDetected;
		proximityDetected = yes;
		getSupport().firePropertyChange(PROXIMITY, old, proximityDetected); // updates GUI among others
	}

	@Override
	public void resetFilter() {
		//        getEnclosedFilterChain().reset();
		histOn.reset();
		histOff.reset();
	}

	@Override
	public void initFilter() {
	}

	@Override
	public synchronized void setFilterEnabled(boolean yes) {
		super.setFilterEnabled(yes);
		if (yes) {
			if (chip.getHardwareInterface() != null) {
				if (chip.getHardwareInterface() instanceof HasLEDControl) {
					ledControl = (HasLEDControl) chip.getHardwareInterface();
				}
			}
			ledTimer = new Timer("LED Flasher");
			ledTimer.schedule(new LEDSetterTask(), 0, periodMs / 2);
		} else {
			if (ledTimer != null) {
				ledTimer.cancel();
			}
			if (ledControl != null) {
				ledControl.setLEDState(0, HasLEDControl.LEDState.OFF);
			}
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof HasLEDControl) {
			ledControl = (HasLEDControl) arg;
			ledControl.setLEDState(0, HasLEDControl.LEDState.OFF);
		}
	}

	/**
	 * @return the method
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * @param method the method to set
	 */
	public void setMethod(Method method) {
		this.method = method;
		putString("method", method.toString());
	}

	/**
	 * @return the binDurationUs
	 */
	public int getBinDurationUs() {
		return binDurationUs;
	}

	/**
	 * @param binDurationUs the binDurationUs to set
	 */
	synchronized public void setBinDurationUs(int binDurationUs) {
		this.binDurationUs = binDurationUs;
		putInt("binDurationUs", binDurationUs);
		eventCounter.init();
	}

	/**
	 * @return the durationAfterFlashToCountUs
	 */
	public int getDurationAfterFlashToCountUs() {
		return durationAfterFlashToCountUs;
	}

	/**
	 * @param durationAfterFlashToCountUs the durationAfterFlashToCountUs to set
	 */
	synchronized public void setDurationAfterFlashToCountUs(int durationAfterFlashToCountUs) {
		this.durationAfterFlashToCountUs = durationAfterFlashToCountUs;
		putInt("durationAfterFlashToCountUs", durationAfterFlashToCountUs);
		eventCounter.init();
	}
}
