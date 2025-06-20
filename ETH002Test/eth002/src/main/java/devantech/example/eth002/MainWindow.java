package devantech.example.eth002;

import devantech.example.eth002.ETHScan.ETHScanDelegate;
import devantech.example.eth002.ETHScan.ScanResult;
import devantech.example.eth002.Module.ErrorCallback;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


/**
 *
 * @author James Henderson
 */
public class MainWindow extends javax.swing.JFrame implements ETHScanDelegate, ErrorCallback {

    List<ScanResult> modules = new ArrayList<>();
    
    ETHScan scanner;
    
    java.util.Timer timer = new java.util.Timer();
    
    Module module = null;
    
    /**
     * Creates new form MainWindow
     */
    public MainWindow() {
        
        initComponents();
        this.setLocationRelativeTo(null);
        setButtonActions();
        
        scanner = new ETHScan();
        
        addWindowListener(new WindowAdapter() {         // Log out of module and close connection when the window is closed.
            
            @Override
            public void windowClosing(WindowEvent e) {
                timer.cancel();
                if (module != null) module.close();
            }
            
            @Override
            public void windowOpened(WindowEvent e) {
                if (e.getSource().getClass() == MainWindow.class) {
                    scanner.addDelegate((ETHScanDelegate)(MainWindow)e.getSource());   
                    startScanning();
                    setUIState(false);
                }
            }
            
        });
        
    }
    
    /**
     * Set the state of the UI elements.
     * 
     * @param st 
     */
    void setUIState(boolean st) {
        
        this.moduleSelectionComboBox.setSelectedIndex(0);
        
        this.relay_1_button.setEnabled(st);
        this.relay_2_button.setEnabled(st);
        
    }
    

    /**
     * Set the actions to trigger on button presses.
     */
    private void setButtonActions() {
        relay_1_button.addActionListener((e) -> {
            toggleOutput(1);
        });
        relay_2_button.addActionListener((e) -> {
            toggleOutput(2);
        });
                
    }
    
    /**
     * Start scanning for modules on the network.
     */
    final void startScanning() {
        clearModuleSelection();
        modules.clear();
        scanner.udpAction();
    }

    /**
     * Add a module to the module selection drop down menu.
     * 
     * @param module the module to add.
     */
    public void addModuleToSelection(String module) {
        for (int index = 0; index < moduleSelectionComboBox.getItemCount(); index++) {  // check to make sure we are not entering the same module in the menu twice.
            if (moduleSelectionComboBox.getItemAt(index).compareTo(module) == 0) return;
        }
        moduleSelectionComboBox.addItem(module);
    }

    /**
     * Clear all items from the module selection dialog.
     */
    public void clearModuleSelection() {
        moduleSelectionComboBox.removeAllItems();
        moduleSelectionComboBox.addItem("Select module.");
    }
    
    @Override
    public void moduleFound(ETHScan.ScanResult sr) {
        modules.add(sr);
        addModuleToSelection(sr.host_name + " (" + sr.ip + ")" );
    }
    
    /**
     * Connect to the selected module.
     * 
     * @param mod the module to connect to.
     */
    void connectToModule(ScanResult mod) {
        scanner.close_action();

        int port = Integer.parseInt(this.portNumber.getText());
        String pass = this.password.getText();

        module = new Module(mod.ip, port, pass);
        module.subscribeForErrors(this);

        setUIState(true);

        try {
            module.connect();
            String firmware = "Firmware: "+ module.FIRMWARE;
            firmwareLabel.setText(firmware);
        } catch (IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }


        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int v = module.PSU;
                String psu = "PSU volts: " + (v / 10) + "." +(v % 10) ;
                psuLabel.setText(psu);

                updateUI();
            }
        }, 0, 100);

    }
    
    /**
     * Update the UI elements
     * 
     */
    void updateUI() {
        SwingUtilities.invokeLater(() -> {
            int relays = module.DIGITAL_OUTPUTS.data;
            relay_1_button.setBackground(getStateColour(relays & 0x01));
            relay_2_button.setBackground(getStateColour(relays & 0x02));
        });
    }
    
    @Override
    public void moduleError(String e) {
        JOptionPane.showMessageDialog(this, e);
        module.close();
        timer.cancel();
        setUIState(false);
    }
    
    /**
     * Get the colour for a relay button state.
     * 
     * @param state the state of the relay.
     * @return the colour to set the button.
     */
    Color getStateColour(int state) {
        if (state == 0) {
            return Color.white;
        }
        return Color.red;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        firmwareLabel = new javax.swing.JLabel();
        psuLabel = new javax.swing.JLabel();
        relay_1_button = new javax.swing.JButton();
        relay_2_button = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        portNumber = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        password = new javax.swing.JTextField();
        moduleSelectionComboBox = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        firmwareLabel.setText("Firmware:      ");

        psuLabel.setText("PSU volts:           ");

        relay_1_button.setText("Relay 1");

        relay_2_button.setText("Relay 2");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Network Settings"));

        jLabel1.setText("Port:");

        portNumber.setText("17494");

        jLabel2.setText("Password:");

        moduleSelectionComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select module." }));
        moduleSelectionComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
                moduleSelected(evt);
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(moduleSelectionComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(portNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 81, Short.MAX_VALUE))
                            .addComponent(password))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(portNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(moduleSelectionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(psuLabel)
                            .addComponent(firmwareLabel))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(relay_1_button, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(relay_2_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relay_1_button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(relay_2_button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(psuLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(firmwareLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void moduleSelected(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_moduleSelected
        if (moduleSelectionComboBox.getSelectedIndex() > 0) {
            System.out.println("Selected -> " + moduleSelectionComboBox.getSelectedItem());
            connectToModule(modules.get(moduleSelectionComboBox.getSelectedIndex() - 1));
        }
        
    }//GEN-LAST:event_moduleSelected

    void toggleOutput(int relay) {
        int state;
        switch (relay) {
            default:
                return; // This output does not exist, do nothing.
            case 1:
                state = module.DIGITAL_OUTPUTS.data & 0x01;
                break;
            case 2:
                state = module.DIGITAL_OUTPUTS.data & 0x02;
                break;
        }
        
        if (state == 0) {
            module.digitalOutputActive(relay, 0);
        } else {
            module.digitalOutputInactive(relay, 0);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel firmwareLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JComboBox<String> moduleSelectionComboBox;
    private javax.swing.JTextField password;
    private javax.swing.JTextField portNumber;
    private javax.swing.JLabel psuLabel;
    private javax.swing.JButton relay_1_button;
    private javax.swing.JButton relay_2_button;
    // End of variables declaration//GEN-END:variables

   
}
