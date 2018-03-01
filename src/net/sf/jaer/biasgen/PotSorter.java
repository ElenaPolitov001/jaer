/*
 * PotSorter.java
 *
 * Created on November 4, 2008, 7:57 AM
 */

package net.sf.jaer.biasgen;

import java.util.ArrayList;

import javax.swing.JComponent;

/**
 *  Shows the header for a list of Pots and allows filtering of Pots.
 * 
 * @author  tobi
 */
public class PotSorter extends javax.swing.JPanel  {
ArrayList<JComponent> guiList;
ArrayList<Pot> pots;

    /** Creates new form PotSorter */
    public PotSorter(ArrayList<JComponent> guiList,ArrayList<Pot> pots) {
        initComponents();
        this.pots=pots;
        this.guiList=guiList;
    }

    private void filterBy(String s) {
        int i=0;
        for(Pot p:pots){
            String n=p.getName();
            n=n.toLowerCase();
            String t=p.getTooltipString().toLowerCase();
            if(s==null|| s.length()==0||n.contains(s)||(t!=null&&t.contains(s))){
                guiList.get(i).setVisible(true);
            }else{
                guiList.get(i).setVisible(false);
            }
            i++;
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

        filterPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        filterTextField = new javax.swing.JTextField();
        globalValuePanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        globalValueTextField = new javax.swing.JTextField();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        headerPanel = new javax.swing.JPanel();
        nameLabel = new javax.swing.JLabel();
        sexLabel = new javax.swing.JLabel();
        typeLabel = new javax.swing.JLabel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        sliderAndValuePanel = new javax.swing.JPanel();
        physicalValueTextField = new javax.swing.JTextField();
        bitValueTextField = new javax.swing.JTextField();
        bitPatternTextField = new javax.swing.JTextField();

        setMaximumSize(new java.awt.Dimension(2147483647, 50));
        setMinimumSize(new java.awt.Dimension(151, 50));
        setPreferredSize(new java.awt.Dimension(250, 50));
        setRequestFocusEnabled(false);
        setVerifyInputWhenFocusTarget(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

        filterPanel.setLayout(new javax.swing.BoxLayout(filterPanel, javax.swing.BoxLayout.X_AXIS));

        jLabel1.setLabelFor(filterTextField);
        jLabel1.setText("Filter");
        filterPanel.add(jLabel1);

        filterTextField.setColumns(20);
        filterTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        filterTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        filterTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterTextFieldActionPerformed(evt);
            }
        });
        filterTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                filterTextFieldKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                filterTextFieldKeyTyped(evt);
            }
        });
        filterPanel.add(filterTextField);

        globalValuePanel.setLayout(new javax.swing.BoxLayout(globalValuePanel, javax.swing.BoxLayout.X_AXIS));

        jLabel2.setText("Set global value");
        globalValuePanel.add(jLabel2);

        globalValueTextField.setColumns(20);
        globalValueTextField.setToolTipText("Enter a value here to set global bit value");
        globalValueTextField.setMaximumSize(new java.awt.Dimension(100, 30));
        globalValueTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                globalValueTextFieldActionPerformed(evt);
            }
        });
        globalValuePanel.add(globalValueTextField);
        globalValuePanel.add(filler1);

        filterPanel.add(globalValuePanel);

        add(filterPanel);

        headerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        headerPanel.setMinimumSize(new java.awt.Dimension(109, 25));
        headerPanel.setPreferredSize(new java.awt.Dimension(254, 25));
        headerPanel.setLayout(new javax.swing.BoxLayout(headerPanel, javax.swing.BoxLayout.X_AXIS));

        nameLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 12)); // NOI18N
        nameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        nameLabel.setText("name");
        nameLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        nameLabel.setMaximumSize(new java.awt.Dimension(100, 15));
        nameLabel.setMinimumSize(new java.awt.Dimension(17, 10));
        nameLabel.setPreferredSize(new java.awt.Dimension(85, 15));
        headerPanel.add(nameLabel);

        sexLabel.setText("sex");
        sexLabel.setToolTipText("Sex (N- or P-type)");
        sexLabel.setMaximumSize(new java.awt.Dimension(32000, 14));
        sexLabel.setMinimumSize(new java.awt.Dimension(17, 10));
        sexLabel.setPreferredSize(new java.awt.Dimension(30, 14));
        headerPanel.add(sexLabel);

        typeLabel.setText("type");
        typeLabel.setToolTipText("Type (Normal or Cascode)");
        typeLabel.setMaximumSize(new java.awt.Dimension(32000, 14));
        typeLabel.setMinimumSize(new java.awt.Dimension(17, 10));
        typeLabel.setPreferredSize(new java.awt.Dimension(30, 14));
        headerPanel.add(typeLabel);
        headerPanel.add(filler2);

        sliderAndValuePanel.setLayout(new javax.swing.BoxLayout(sliderAndValuePanel, javax.swing.BoxLayout.X_AXIS));
        headerPanel.add(sliderAndValuePanel);

        physicalValueTextField.setEditable(false);
        physicalValueTextField.setColumns(15);
        physicalValueTextField.setFont(new java.awt.Font("Courier New", 0, 10)); // NOI18N
        physicalValueTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        physicalValueTextField.setText("physical value");
        physicalValueTextField.setToolTipText("bit value as an int");
        physicalValueTextField.setFocusable(false);
        physicalValueTextField.setMaximumSize(new java.awt.Dimension(100, 2147483647));
        physicalValueTextField.setMinimumSize(new java.awt.Dimension(17, 10));
        physicalValueTextField.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                physicalValueTextFieldMouseWheelMoved(evt);
            }
        });
        physicalValueTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                physicalValueTextFieldActionPerformed(evt);
            }
        });
        physicalValueTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                physicalValueTextFieldKeyPressed(evt);
            }
        });
        headerPanel.add(physicalValueTextField);

        bitValueTextField.setEditable(false);
        bitValueTextField.setColumns(10);
        bitValueTextField.setFont(new java.awt.Font("Courier New", 0, 10)); // NOI18N
        bitValueTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        bitValueTextField.setText("bit value");
        bitValueTextField.setToolTipText("bit value as an int");
        bitValueTextField.setFocusable(false);
        bitValueTextField.setMaximumSize(new java.awt.Dimension(100, 2147483647));
        bitValueTextField.setMinimumSize(new java.awt.Dimension(17, 10));
        bitValueTextField.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                bitValueTextFieldMouseWheelMoved(evt);
            }
        });
        bitValueTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bitValueTextFieldActionPerformed(evt);
            }
        });
        bitValueTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                bitValueTextFieldKeyPressed(evt);
            }
        });
        headerPanel.add(bitValueTextField);

        bitPatternTextField.setEditable(false);
        bitPatternTextField.setColumns(15);
        bitPatternTextField.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        bitPatternTextField.setText("bit pattern");
        bitPatternTextField.setToolTipText("bit value as bits");
        bitPatternTextField.setFocusable(false);
        bitPatternTextField.setMaximumSize(new java.awt.Dimension(100, 2147483647));
        bitPatternTextField.setMinimumSize(new java.awt.Dimension(17, 10));
        headerPanel.add(bitPatternTextField);

        add(headerPanel);
    }// </editor-fold>//GEN-END:initComponents

private void bitValueTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_bitValueTextFieldKeyPressed

}//GEN-LAST:event_bitValueTextFieldKeyPressed

private void bitValueTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bitValueTextFieldActionPerformed

}//GEN-LAST:event_bitValueTextFieldActionPerformed

private void bitValueTextFieldMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_bitValueTextFieldMouseWheelMoved

}//GEN-LAST:event_bitValueTextFieldMouseWheelMoved

private void filterTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterTextFieldActionPerformed
    String s=filterTextField.getText().toLowerCase();
    filterBy(s);
}//GEN-LAST:event_filterTextFieldActionPerformed

private void filterTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_filterTextFieldKeyReleased
// TODO add your handling code here:
}//GEN-LAST:event_filterTextFieldKeyReleased

private void filterTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_filterTextFieldKeyTyped
    String s=filterTextField.getText().toLowerCase();
    filterBy(s);
}//GEN-LAST:event_filterTextFieldKeyTyped

private void globalValueTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_globalValueTextFieldActionPerformed
int v;
        try{
            v=Integer.parseInt(globalValueTextField.getText());
            for(Pot p:pots){
                p.setBitValue(v);
            }
        }catch(NumberFormatException e){
            globalValueTextField.selectAll();
        }
}//GEN-LAST:event_globalValueTextFieldActionPerformed

private void physicalValueTextFieldMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_physicalValueTextFieldMouseWheelMoved
// TODO add your handling code here:
}//GEN-LAST:event_physicalValueTextFieldMouseWheelMoved

private void physicalValueTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_physicalValueTextFieldActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_physicalValueTextFieldActionPerformed

private void physicalValueTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_physicalValueTextFieldKeyPressed
// TODO add your handling code here:
}//GEN-LAST:event_physicalValueTextFieldKeyPressed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField bitPatternTextField;
    private javax.swing.JTextField bitValueTextField;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JPanel filterPanel;
    private javax.swing.JTextField filterTextField;
    private javax.swing.JPanel globalValuePanel;
    private javax.swing.JTextField globalValueTextField;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField physicalValueTextField;
    private javax.swing.JLabel sexLabel;
    private javax.swing.JPanel sliderAndValuePanel;
    private javax.swing.JLabel typeLabel;
    // End of variables declaration//GEN-END:variables

    
}
