/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * BinsPanel.java
 *
 * Created on 25.03.2009, 16:52:05
 */
package ch.unizh.ini.jaer.projects.cochsoundloc;

/**
 * Displays the ITD histogram.
 *
 * @author Holger
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.util.logging.Logger;

import javax.swing.JPanel;

import net.sf.jaer.util.chart.Axis;
import net.sf.jaer.util.chart.Category;
import net.sf.jaer.util.chart.Series;
import net.sf.jaer.util.chart.XYChart;

public class ITDBinsPanel extends JPanel {

    private Logger log = Logger.getLogger("JAERITDViewer");
    public ITDBins myBins;
    volatile boolean stopflag = false;
    // activity
    private int numOfBins = 16;
    private int ACTVITY_SECONDS_TO_SHOW = 16;
    public Series activitySeries;
    private Axis binAxis;
    private Axis activityAxis;
    private Category activityCategory;
    private XYChart activityChart;
    public double maxActivity = 0;
    private Series locSeries;
    private Category locCategory;
    private float localisedSoundPos = 0;
    private boolean displaySoundDetected = true;
    private boolean displayNormalize = true;

    /** Creates new form BinsPanel */
    public ITDBinsPanel() {
        initComponents();
        init();
    }

    public void init() {
        try {
            initComponents();

            activitySeries = new Series(2, numOfBins);

            binAxis = new Axis(0, ACTVITY_SECONDS_TO_SHOW);
            binAxis.setTitle("bins");
            binAxis.setUnit("#bin");

            activityAxis = new Axis(0, 1); // will be normalized
            activityAxis.setTitle("activity");

            activityCategory = new Category(activitySeries, new Axis[]{binAxis, activityAxis});
            activityCategory.setColor(new float[]{1.0f, 1.0f, 1.0f}); // white for visibility
            activityCategory.setLineWidth(3f);

            locSeries = new Series(2, 2);
            locCategory = new Category(locSeries, new Axis[]{binAxis, activityAxis});
            locCategory.setColor(new float[]{1.0f, 0.0f, 0.0f});
            locCategory.setLineWidth(5f);
            
            activityChart = new XYChart("");
            activityChart.setBackground(Color.black);
            activityChart.setForeground(Color.white);
            activityChart.setGridEnabled(false);
            
            activityChart.addCategory(activityCategory);
            if (this.displaySoundDetected) {
                activityChart.addCategory(locCategory);
            }
            
            activityPanel.setLayout(new BorderLayout());
            activityPanel.add(activityChart, BorderLayout.CENTER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void updateBins(ITDBins newBins) {
        myBins = newBins;
        numOfBins = myBins.getNumOfBins() + 1;

        activitySeries.setCapacity(numOfBins);
        locSeries.setCapacity(3);
        //activityPanel.remove(activityChart);
        //init();
//        activitySeries = null;
//        activitySeries = new Series(2, myBins.getNumOfBins() + 1);
        this.repaint();
    }

    public void setLocalisedPos(int ITD) {
        this.localisedSoundPos = myBins.convertITD2BIN(ITD);
    }

    @Override
    synchronized public void paint(Graphics g) {
        super.paint(g);
        try {
            if (myBins != null) {
                if (isDisplayNormalize()) {
                    maxActivity = 0;
                }

                activitySeries.clear();
                
                //log.info("numbins="+myBins.numOfBins);
                for (int i = 0; i < myBins.getNumOfBins(); i++) {
                    if (maxActivity < myBins.getBin(i)) {
                        maxActivity = myBins.getBin(i);
                    }
                    activitySeries.add(i, myBins.getBin(i));
                }
                activitySeries.add(myBins.getNumOfBins(), 0);

                if (isDisplaySoundDetected()) {
                    locSeries.clear();
                    locSeries.add(localisedSoundPos, 0);
                    locSeries.add(localisedSoundPos, (int) maxActivity / 2);
                }

                binAxis.setMaximum(myBins.getNumOfBins());
                binAxis.setMinimum(0);
                activityAxis.setMaximum(maxActivity);
            } else {
                log.warning("myBins==null");
            }
        } catch (Exception e) {
            log.warning("while displaying bins chart caught " + e);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        activityPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(0, 0, 0));
        setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        setAutoscrolls(true);
        setDoubleBuffered(false);
        setLayout(new java.awt.BorderLayout());

        activityPanel.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout activityPanelLayout = new javax.swing.GroupLayout(activityPanel);
        activityPanel.setLayout(activityPanelLayout);
        activityPanelLayout.setHorizontalGroup(
            activityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 384, Short.MAX_VALUE)
        );
        activityPanelLayout.setVerticalGroup(
            activityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 334, Short.MAX_VALUE)
        );

        add(activityPanel, java.awt.BorderLayout.CENTER);

        getAccessibleContext().setAccessibleParent(this);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel activityPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the displaySoundDetected
     */
    public boolean isDisplaySoundDetected() {
        return displaySoundDetected;
    }

    /**
     * @param displaySoundDetected the displaySoundDetected to set
     */
    public void setDisplaySoundDetected(boolean displaySoundDetected) {
        this.displaySoundDetected = displaySoundDetected;
        activityChart.clear();
        activityChart.addCategory(activityCategory);
        if (displaySoundDetected) {
            activityChart.addCategory(locCategory);
        }
    }

    /**
     * @return the displayNormalize
     */
    public boolean isDisplayNormalize() {
        return displayNormalize;
    }

    /**
     * @param displayNormalize the displayNormalize to set
     */
    public void setDisplayNormalize(boolean displayNormalize) {
        this.displayNormalize = displayNormalize;
    }
}
