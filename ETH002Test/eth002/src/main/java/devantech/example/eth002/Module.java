package devantech.example.eth002;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author James Henderson
 */
public class Module extends Thread {
    
    public interface ErrorCallback {
        void moduleError(String e);
    }
    
    public class ByteData {
        public volatile byte data = 0;
    }
    
    public class IntData {
        public volatile int data = 0;
    }

    private static final byte GET_MODULE_INFO = 0x10;
    private static final byte GET_DIGI_OUTPUT = 0x24;
    private static final byte DIGITAL_OUTPUT_ACTIVE = 0x20;
    private static final byte DIGITAL_OUTPUT_INACTIVE = 0x21;
    private static final byte GET_SERIAL_NUMBER = 0x77;
    private static final byte GET_PSU = 0x78;
    private static final byte LOGOUT = 0x7B;
    private static final byte SET_PASSWORD = 0x79;
    private static final byte GET_UNLOCK = 0x7a;
    
    public final String ipAddress;  // The address of the module
    public final int port;  // The port number
    public final String password;   // The TCP/IP Password to access the module
    
    private final byte[] data = new byte[127];
    private Socket socket = null;
    private OutputStream output = null;
    private InputStream input = null;
    private boolean connected = false;
    
    public volatile int ID = 0; // The module ID
    public volatile int FIRMWARE = 0;   // The firmware version
    public volatile int HARDWARE = 0;   // The hardware version
    public volatile int PSU = 0;    // The supply voltage
    public volatile String SERIAL = ""; // The module serial number
    public final ByteData DIGITAL_OUTPUTS = new ByteData();  // The states of digital inputs
    
    List<byte[]> messages = new ArrayList<>();  // Holds commands to send to the module from the UI

    ErrorCallback err = null;
    
    Module(String ip, int p, String pass) {
        ipAddress = ip;
        port = p;
        password = pass;
        
    }
    
    @Override
    public void run() {

        while(connected) {
            
            // Send messages triggered from the UI here if there are any.
            if (!messages.isEmpty()) {
                
                for (byte[] m : messages) {
                    try {
                        output.write(m, 0, m.length);
                        input.read(m, 0, 1);
                    } catch (IOException ex) {
                        Logger.getLogger(Module.class.getName()).log(Level.SEVERE, null, ex);
                        if (err != null) err.moduleError("Error sending command to module.");
                        connected = false;
                        break;
                    }
                }
                
                messages.clear();
            }
            
            // Get information from the module.
            getPSU();
            getDigitalOutputStates();
            
        }
        
        logout();
        close();
    }
    
    public void subscribeForErrors(ErrorCallback e) {
        this.err = e;
    }
    
    /**
     * Try and connect to the module.
     * 
     * @throws IOException 
     */
    public void connect() throws IOException {
        socket = new Socket(ipAddress, port);
        output = socket.getOutputStream();
        input = socket.getInputStream();
        
        connected = true;
        
        if (getUnlock() == 0) {
            sendPassword();
            if (getUnlock() == 0) {
                if (err != null) err.moduleError("Wrong password.");
                return;
            }
        }
        
        getModuleData();
        getSerialNumber();
        
        this.start();
    }
    
    /**
     * Close the connection to the module
     */
    public void close() {
        if (socket != null) {
            try {
                socket.close();
                socket = null;
                input = null;
                output = null;
            } catch (IOException ex) {
                // Nothing to do here, the module was either already closed or was unable to close properly.
            }
            connected = false;
        }
    }
    
    /**
     * Get the unlock time of the module.
     * 
     * @return the unlock time.
     */
    public byte getUnlock() {
        
        if (!connected) return -1;
        
        data[0] = GET_UNLOCK;

        try {
            output.write(data, 0, 1);
            input.read(data, 0, 1);
        } catch (IOException ex) {
            Logger.getLogger(Module.class.getName()).log(Level.SEVERE, null, ex);
            if (err != null) err.moduleError("Error getting unlock time.");
            connected = false;
            return -1;
        }

        return data[0];
    }
    
    /**
     * Send the password to the module.
     * 
     * @return 1 for success, otherwise is a fail.
     */
    public final byte sendPassword() {
        
        if (!connected) return -1;
        
        data[0] = SET_PASSWORD;
        
        int index;
        for (index = 0; index < password.length(); index++) {
            data[1 + index] = (byte) (password.charAt(index) & 0xff);
        }

        try {
            output.write(data, 0, password.length() + 1);
            input.read(data, 0, 1);
        } catch (IOException ex) {
            Logger.getLogger(Module.class.getName()).log(Level.SEVERE, null, ex);
            if (err != null) err.moduleError("Error sending password.");
            connected = false;
            return -1;
        }

        return data[0];

    }
    
    /**
     * Get the module information such as the ID and the firmware version.
     */
    public final void getModuleData() {

        if (!connected) return;
        
        data[0] = GET_MODULE_INFO;

        try {
            output.write(data, 0, 1);
            input.read(data, 0, 3);
            ID = data[0];
            HARDWARE = data[1];
            FIRMWARE = data[2];
        } catch (IOException ex) {
            Logger.getLogger(Module.class.getName()).log(Level.SEVERE, null, ex);
            if (err != null) err.moduleError("Error getting module data.");
            connected = false;
            ID = 0;
            HARDWARE = 0;
            FIRMWARE = 0;
        }

    }
    
    /**
     * Get the unique 6 byte mac address from the module.
     *
     */
    public void getSerialNumber() {
        
        if (!connected) return;
        
        data[0] = GET_SERIAL_NUMBER;

        try {
            output.write(data, 0, 1);
            input.read(data, 0, 6);
        } catch (IOException ex) {
            Logger.getLogger(Module.class.getName()).log(Level.SEVERE, null, ex);
            if (err != null) err.moduleError("Error sgetting serial number.");
            connected = false;
            return;
        }

        SERIAL = String.format("%02X:%02X:%02X:%02X:%02X:%02X", data[0],data[1],data[2],data[3],data[4],data[5]);
      
    }

    /**
     * Logout from the module.
     *
     */
    public void logout() {
        
        if (!connected) return;
        
        data[0] = LOGOUT;

        try {
            output.write(data, 0, 1);
            input.read(data, 0, 1);
        } catch (IOException ex) {
            Logger.getLogger(Module.class.getName()).log(Level.SEVERE, null, ex);
            if (err != null) err.moduleError("Error logging out.");
            connected = false;
        }

    }
    
    /**
     * Get the power supply voltage from the module.
     *
     */
    public void getPSU() {
        
        if (!connected) return;
        
        data[0] = GET_PSU;

        try {
            output.write(data, 0, 1);
            input.read(data, 0, 1);
            PSU = data[0];
        } catch (IOException ex) {
            Logger.getLogger(Module.class.getName()).log(Level.SEVERE, null, ex);
            if (err != null) err.moduleError("Error getting PSU.");
            connected = false;
        }

    }    
    
    /**
     * Get the state of the outputs from the module as an array of bytes. 
     *
     */
    public void getDigitalOutputStates() {

        if (!connected) return;
        
        data[0] = GET_DIGI_OUTPUT;

        try {
            output.write(data, 0, 1);
            input.read(data, 0, 1);
        } catch (IOException ex) {
            Logger.getLogger(Module.class.getName()).log(Level.SEVERE, null, ex);
            if (err != null) err.moduleError("Error getting output states.");
            connected = false;
            return;
        }
        
        DIGITAL_OUTPUTS.data = data[0];

    }
    
    /**
     * Make a digital output on the module active.
     *
     * @param channel the output to set active
     * @param time the length of time to set the output active
     */
    public void digitalOutputActive(int channel, int time) {
        
        if (!connected) return;
        
        byte[] mData = new byte[3];
        mData[0] = DIGITAL_OUTPUT_ACTIVE;
        mData[1] = (byte) (channel & 0xff);
        mData[2] = (byte) (time & 0xff);

        messages.add(mData);
        
    }
    
    /**
     * Make a digital output on the module inactive.
     *
     * @param channel the output to set active
     * @param time the length of time to set the output active
     */
    public void digitalOutputInactive(int channel, int time) {
        
        if (!connected) return;
        
        byte[] mData = new byte[3];
        mData[0] = DIGITAL_OUTPUT_INACTIVE;
        mData[1] = (byte) (channel & 0xff);
        mData[2] = (byte) (time & 0xff);

        messages.add(mData);
        
    }
    
}
