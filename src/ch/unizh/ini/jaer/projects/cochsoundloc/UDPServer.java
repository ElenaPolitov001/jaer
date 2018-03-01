/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * UDPServer.java
 *
 * Created on 30.06.2009, 13:33:28
 */
package ch.unizh.ini.jaer.projects.cochsoundloc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thiss little app can receive the filter outputs from the panTiltThread.
 * 
 * @author Holger
 */
public class UDPServer extends javax.swing.JFrame {

    /** Creates new form UDPServer */
    public UDPServer() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        txtPort = new javax.swing.JTextField();
        btnOpen = new javax.swing.JButton();
        FilterOutput = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtRetinaPanOffset = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtCochleaPanOffset = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        txtRetinaTiltOffset = new javax.swing.JTextField();
        txtCochleaTiltOffset = new javax.swing.JTextField();
        txtCochleaConfidence = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtRetinaConfidence = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Port:");

        txtPort.setText("7778");

        btnOpen.setText("Open");
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });

        FilterOutput.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Outputs"));

        jLabel2.setText("Retina:");

        jLabel3.setText("Cochlea:");

        txtRetinaPanOffset.setEditable(false);
        txtRetinaPanOffset.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtRetinaPanOffset.setText("1");

        jLabel5.setText("Pan-Offset");

        txtCochleaPanOffset.setEditable(false);
        txtCochleaPanOffset.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtCochleaPanOffset.setText("1");

        jLabel8.setText("Tilt-Offset");

        txtRetinaTiltOffset.setEditable(false);
        txtRetinaTiltOffset.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtRetinaTiltOffset.setText("1");

        txtCochleaTiltOffset.setEditable(false);
        txtCochleaTiltOffset.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtCochleaTiltOffset.setText("1");

        txtCochleaConfidence.setEditable(false);
        txtCochleaConfidence.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtCochleaConfidence.setText("1");

        jLabel6.setText("Confidence");

        txtRetinaConfidence.setEditable(false);
        txtRetinaConfidence.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtRetinaConfidence.setText("1");

        javax.swing.GroupLayout FilterOutputLayout = new javax.swing.GroupLayout(FilterOutput);
        FilterOutput.setLayout(FilterOutputLayout);
        FilterOutputLayout.setHorizontalGroup(
            FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FilterOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtRetinaPanOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(txtCochleaPanOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(FilterOutputLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addGap(19, 19, 19))
                    .addGroup(FilterOutputLayout.createSequentialGroup()
                        .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtRetinaTiltOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtCochleaTiltOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)))
                .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtCochleaConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(txtRetinaConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        FilterOutputLayout.setVerticalGroup(
            FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FilterOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, FilterOutputLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txtCochleaPanOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(txtRetinaPanOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, FilterOutputLayout.createSequentialGroup()
                        .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jLabel8))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtCochleaConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtCochleaTiltOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(FilterOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtRetinaConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRetinaTiltOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtPort, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnOpen, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE))
                    .addComponent(FilterOutput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnOpen))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(FilterOutput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        DatagramSocket ds;
        try {
            ds = new DatagramSocket(Integer.parseInt(txtPort.getText()));
            //while (true)
            {
                DatagramPacket pack = new DatagramPacket(new byte[16], 16);
                ds.receive(pack);
                byte[] data = pack.getData();
                CommObjForPanTilt test = new CommObjForPanTilt();
                test.setBytes(data);
                if (test.isFromCochlea()) {
                    txtCochleaPanOffset.setText(String.valueOf(test.getPanOffset()));
                    txtCochleaTiltOffset.setText(String.valueOf(test.getTiltOffset()));
                    txtCochleaConfidence.setText(String.valueOf(test.getConfidence()));
                } else if (test.isFromRetina()) {
                    txtRetinaPanOffset.setText(String.valueOf(test.getPanOffset()));
                    txtRetinaTiltOffset.setText(String.valueOf(test.getTiltOffset()));
                    txtRetinaConfidence.setText(String.valueOf(test.getConfidence()));
                }
            }
        } catch (SocketException ex) {
            Logger.getLogger(UDPServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UDPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
}//GEN-LAST:event_btnOpenActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new UDPServer().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel FilterOutput;
    private javax.swing.JButton btnOpen;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JTextField txtCochleaConfidence;
    private javax.swing.JTextField txtCochleaPanOffset;
    private javax.swing.JTextField txtCochleaTiltOffset;
    private javax.swing.JTextField txtPort;
    private javax.swing.JTextField txtRetinaConfidence;
    private javax.swing.JTextField txtRetinaPanOffset;
    private javax.swing.JTextField txtRetinaTiltOffset;
    // End of variables declaration//GEN-END:variables
}
