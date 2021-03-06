/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PluginManager.java
 *
 * Created on Mar 5, 2010, 3:45:03 PM
 */
package org.geoimage.viewer.widget;

import java.util.List;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

import org.geoimage.viewer.core.Plugins;
import org.slf4j.LoggerFactory;

/**O
 *
 * @author thoorfr
 */
public class PluginManagerDialog extends javax.swing.JDialog {
	private static org.slf4j.Logger logger=LoggerFactory.getLogger(PluginManagerDialog.class);

    private final EntityManagerFactory emf;
    private final List<Plugins> plugins;
    private final DefaultListModel activemodel;
    private final DefaultListModel inactivemodel;

    /** Creates new form PluginManager */
    public PluginManagerDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        emf = Persistence.createEntityManagerFactory("GeoImageViewerPU");
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("select p from Plugins p");
        plugins = q.getResultList();
        em.close();
        activemodel = new DefaultListModel();
        for (Plugins p : plugins) {
            if(p==null){
                continue;
            }
            if (p.isActive()) {
                activemodel.addElement(p);
            }
        }

        activeList.setModel(activemodel);

        inactivemodel = new DefaultListModel();
        for (Plugins p : plugins) {
            if (!p.isActive()) {
                inactivemodel.addElement(p);
            }
        }

        inactiveList.setModel(inactivemodel);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        activeList = new javax.swing.JList();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        inactiveList = new javax.swing.JList();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        newPluginButton = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        activeList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        activeList.setName("activeList"); // NOI18N
        jScrollPane1.setViewportView(activeList);

        jLabel1.setText("Active");
        jLabel1.setName("jLabel1"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        inactiveList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        inactiveList.setName("inactiveList"); // NOI18N
        jScrollPane2.setViewportView(inactiveList);

        jLabel2.setText("Inactive");
        jLabel2.setName("jLabel2"); // NOI18N

        jButton1.setText("<<");
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText(">>");
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        newPluginButton.setText("Add New...");
        newPluginButton.setName("newPluginButton"); // NOI18N
        newPluginButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newPluginButtonActionPerformed(evt);
            }
        });

        jButton3.setText("Edit...");
        jButton3.setName("jButton3"); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 282, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                    .addComponent(newPluginButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 322, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 279, Short.MAX_VALUE)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(newPluginButton)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        Object[] selected = inactiveList.getSelectedValues();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (Object plugin : selected) {
            Plugins p = (Plugins) plugin;
            p.setActive(true);
            inactivemodel.removeElement(p);
            activemodel.addElement(p);
            p = em.merge(p);
            em.persist(p);
        }
        em.getTransaction().commit();
        em.close();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        Object[] selected = activeList.getSelectedValues();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (Object plugin : selected) {
            Plugins p = (Plugins) plugin;
            p.setActive(false);
            activemodel.removeElement(p);
            inactivemodel.addElement(p);
            p = em.merge(p);
            em.persist(p);
        }
        em.getTransaction().commit();
        em.close();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void newPluginButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newPluginButtonActionPerformed
        PluginEditor dialog = new PluginEditor(new javax.swing.JFrame(), true);
        dialog.setVisible(true);
        while (dialog.isVisible()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            	logger.error(ex.getMessage(),ex);
            }
        }
        if(dialog.getPlugin()==null) return;
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            em.persist(dialog.getPlugin());
        } catch (EntityExistsException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "The plugin is already registered!", "Warning", JOptionPane.WARNING_MESSAGE);
            em.getTransaction().rollback();
            em.close();
            return;
        }
        em.getTransaction().commit();
        em.close();
        inactivemodel.addElement(dialog.getPlugin());
    }//GEN-LAST:event_newPluginButtonActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        PluginEditor dialog = new PluginEditor(new javax.swing.JFrame(), true);
        dialog.setPlugin((Plugins) inactiveList.getSelectedValue());
        dialog.setVisible(true);
        while (dialog.isVisible()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            	logger.error(ex.getMessage(),ex);
            }
        }
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            em.persist(em.merge(dialog.getPlugin()));
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not save the new settings!", "Warning", JOptionPane.WARNING_MESSAGE);
            em.getTransaction().rollback();
            em.close();
            return;
        }
        em.getTransaction().commit();
        em.close();
    }//GEN-LAST:event_jButton3ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                PluginManagerDialog dialog = new PluginManagerDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList activeList;
    private javax.swing.JList inactiveList;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton newPluginButton;
    // End of variables declaration//GEN-END:variables
}
