/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * Head6DOF_GUI.java
 *
 * Created on Jul 7, 2011, 11:51:37 PM
 */
package org.ine.telluride.jaer.tell2011.head6axis;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import net.sf.jaer.hardwareinterface.HardwareInterfaceException;

/**
 * Allows mouse control of head.
 * @author tobi
 * @editor philipp
 */
public class Head6DOF_GUI extends javax.swing.JFrame implements PropertyChangeListener {

    private static Logger log = Logger.getLogger("Head6DOF_ServoController");
    Head6DOF_ServoController controller = null;
    private int w = 200, h = 200;

    /** Creates new form Head6DOF_GUI */
    public Head6DOF_GUI(Head6DOF_ServoController contrl) {
        controller = contrl;
        controller.getSupport().addPropertyChangeListener(this);
        initComponents();
        eyePanel.setPreferredSize(new Dimension(w, h));
        headPanel.setPreferredSize(new Dimension(w, h));
        pack();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        eyePanel = new javax.swing.JPanel();
        vergenceSlider = new javax.swing.JSlider();
        headPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        txtCommand = new javax.swing.JTextField();
        labelCommand = new javax.swing.JLabel();
        btnExecuteCommand = new javax.swing.JButton();
        labelCaution = new javax.swing.JLabel();
        txtareaPossibleCommands = new javax.swing.JTextArea();

        setTitle("HeadControl");

        eyePanel.setBackground(new java.awt.Color(255, 255, 255));
        eyePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("eye direction"));
        eyePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                eyePanelMouseClicked(evt);
            }
        });
        eyePanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                eyePanelMouseDragged(evt);
            }
        });

        javax.swing.GroupLayout eyePanelLayout = new javax.swing.GroupLayout(eyePanel);
        eyePanel.setLayout(eyePanelLayout);
        eyePanelLayout.setHorizontalGroup(
            eyePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        eyePanelLayout.setVerticalGroup(
            eyePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 161, Short.MAX_VALUE)
        );

        vergenceSlider.setToolTipText("vergence");
        vergenceSlider.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        vergenceSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                vergenceSliderStateChanged(evt);
            }
        });

        headPanel.setBackground(new java.awt.Color(255, 255, 255));
        headPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("head direction"));
        headPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                headPanelMouseDragged(evt);
            }
        });

        javax.swing.GroupLayout headPanelLayout = new javax.swing.GroupLayout(headPanel);
        headPanel.setLayout(headPanelLayout);
        headPanelLayout.setHorizontalGroup(
            headPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        headPanelLayout.setVerticalGroup(
            headPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 161, Short.MAX_VALUE)
        );

        txtCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCommandActionPerformed(evt);
            }
        });

        labelCommand.setText("Command:");

        btnExecuteCommand.setText("Execute Command");
        btnExecuteCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExecuteCommandActionPerformed(evt);
            }
        });

        labelCaution.setText("CAUTION: Not protected by pan / tilt limits");

        txtareaPossibleCommands.setEditable(false);
        txtareaPossibleCommands.setBackground(new java.awt.Color(240, 240, 240));
        txtareaPossibleCommands.setColumns(20);
        txtareaPossibleCommands.setFont(new java.awt.Font("Tahoma", 0, 11)); // NOI18N
        txtareaPossibleCommands.setRows(5);
        txtareaPossibleCommands.setText("Possible Commands:\n\n!Px,yyyy => sets servo x (0-5) to position yyyy\n\ns0 = Head pan: " + Integer.toString(controller.servoOffsets[0] - 450) + " - "  + Integer.toString(controller.servoOffsets[0] + 450) + "\ns1 = Head tilt: " + Integer.toString(controller.servoOffsets[1] - 450) + " - "  + Integer.toString(controller.servoOffsets[1] + 450) + "\ns2 = Left eye pan: " + Integer.toString(controller.servoOffsets[2] - 450) + " - "  + Integer.toString(controller.servoOffsets[2] + 450) + "\ns3 = Left eye tilt: " + Integer.toString(controller.servoOffsets[3] - 450) + " - "  + Integer.toString(controller.servoOffsets[3] + 450) + "\ns4 = Right eye pan: " + Integer.toString(controller.servoOffsets[4] - 450) + " - "  + Integer.toString(controller.servoOffsets[4] + 450) + "\ns5 = Right eye: " + Integer.toString(controller.servoOffsets[5] - 450) + " - "  + Integer.toString(controller.servoOffsets[5] + 450) + "\n\nwith vergence = 0");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(labelCommand)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCommand, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnExecuteCommand, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(labelCaution, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(24, 24, 24))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtareaPossibleCommands, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(55, 55, 55)
                .addComponent(labelCaution)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelCommand)
                    .addComponent(btnExecuteCommand))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtareaPossibleCommands, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(eyePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(vergenceSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 381, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(headPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(eyePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(vergenceSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(headPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void eyePanelMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_eyePanelMouseDragged
        float p = getPan(evt), t = getTilt(evt);
        try {
            controller.setEyeGazeDirection(p, t);
            repaint();
        } catch (Exception ex) {
            log.warning(ex.toString());
        }
    }//GEN-LAST:event_eyePanelMouseDragged

    private void headPanelMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_headPanelMouseDragged
       float p = getPan(evt), t = getTilt(evt);
        try {
            controller.setHeadDirection(p, t);
            repaint();
        } catch (Exception ex) {
            log.warning(ex.toString());
        }
    }//GEN-LAST:event_headPanelMouseDragged

    private float vergenceFromSlider(){
        int v=vergenceSlider.getValue();
        float vergence=controller.VERGENCE_LIMIT*(v-50f)/50f;
        return vergence;
    }
    private void vergenceSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_vergenceSliderStateChanged
        float vergence=vergenceFromSlider();
        try {
            controller.setVergence(vergence);
        } catch (HardwareInterfaceException | IOException ex) {
            log.warning(ex.toString());
        }
    }//GEN-LAST:event_vergenceSliderStateChanged

    private void eyePanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_eyePanelMouseClicked
        float p = getPan(evt), t = getTilt(evt);
        try {
            controller.setEyeGazeDirection(p, t);
            repaint();
        } catch (Exception ex) {
            log.warning(ex.toString());
        }
    }//GEN-LAST:event_eyePanelMouseClicked

    private void txtCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCommandActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCommandActionPerformed

    private void btnExecuteCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExecuteCommandActionPerformed
        if (!txtCommand.getText().trim().equals("")) {
            String cmd = this.txtCommand.getText();
            try {
                controller.serialPort.writeLn(cmd);
                log.info("sending " + this.txtCommand.getText() + " to robot head");
            } catch (IOException ex) {
                Logger.getLogger(Head6DOF_GUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            log.info("Command textfield is empty");
        }
    }//GEN-LAST:event_btnExecuteCommandActionPerformed

    private float getPan(MouseEvent evt) {
        int x = evt.getX();
        int w=((JPanel)evt.getSource()).getWidth();
        float pan = (float) x / w; // 0-1
        pan=2*pan-1;
//        log.info("computed x="+x+" w="+w+" pan="+pan);
        return pan;

    }

    private float getTilt(MouseEvent evt) {
        int y = evt.getY();
        int h=((JPanel)evt.getSource()).getHeight();
        float tilt = (float) (h - y) / h;
        tilt=tilt*2-1;
//        log.info("computed y="+y+" h="+h+" tilt="+tilt);
        return tilt;
    }
    private Point p2 = new Point();

    // converts from gaze to to panel coordinates
    private Point gaze2pix(JPanel pan, Point2D.Float gaze) {
        int h=pan.getHeight(), w=pan.getWidth();
        // (g+1)/2 ranges 0:1
        p2.setLocation((gaze.x + 1) / 2 * w, h-((gaze.y + 1) / 2 * h)); // y starts from top in AWT, so flip y here
        return p2;
    }
    
    public void resetVergenceSlider() {
        vergenceSlider.setValue((int) (vergenceSlider.getMaximum()-vergenceSlider.getMinimum())/2);
    }

    @Override
    public void paint(Graphics g) {
        final int r = 6;
        super.paint(g);
        Head6DOF_ServoController.GazeDirection gaze = controller.gazeDirection; // current state of all servos in -1:1 coordinates

        g= eyePanel.getGraphics();
        Point p = gaze2pix(eyePanel, gaze.eyeDirection);
        g.drawLine(p.x, p.y + r, p.x,  p.y - r);
        g.drawLine(p.x - r, p.y, p.x + r, p.y);

        g = headPanel.getGraphics();
        p = gaze2pix(headPanel, gaze.headDirection);
        g.drawLine(p.x, p.y + r, p.x, p.y - r);
        g.drawLine(p.x - r, p.y, p.x + r, p.y);

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnExecuteCommand;
    private javax.swing.JPanel eyePanel;
    private javax.swing.JPanel headPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel labelCaution;
    private javax.swing.JLabel labelCommand;
    private javax.swing.JTextField txtCommand;
    private javax.swing.JTextArea txtareaPossibleCommands;
    private javax.swing.JSlider vergenceSlider;
    // End of variables declaration//GEN-END:variables

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        log.info("got property change event "+evt.getPropertyName());
        repaint();
    }
}
