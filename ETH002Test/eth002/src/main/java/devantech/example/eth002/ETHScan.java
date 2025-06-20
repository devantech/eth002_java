package devantech.example.eth002;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class performs a UDP scan of the network and notifies a delegate of any 
 * ETHx modules that are on it.
 * @author James Henderson
 */
public class ETHScan {
    
    private final List<ETHScanDelegate> delegates = new ArrayList<>();
    
    String udp_string = "Discovery: Who is out there?\0\n";
    
    ReentrantLock lock = new ReentrantLock();

    private Thread receive_thread = null;
    private DatagramSocket socket;
    private DatagramPacket packet;
    
    /**
     * Holds the details of a module that has been found.
     */
    public class ScanResult {
        
        /**
         * The IP address of the module
         */
        public final String ip;

        /**
         *The modules host name
         */
        public final String host_name;

        /**
         *  The module ID
         */
        public final int id;

        /**
         *  The modules Mac address
         */
        public final String mac;
        
        /**
         * Constructor
         * @param i the ip address
         * @param h the host name
         * @param d the module id
         * @param m the mac address
         */
        public ScanResult(String i, String h, int d, String m) {
            ip = i;
            host_name = h;
            id = d;
            mac = m;
        }
        
    }
    
    /**
     * Should be implemented by classes that wish to be notified when a module
     * has been found.
     */
    public interface ETHScanDelegate {
        
        /**
         * Called when a module is found on the network.
         * @param module the module that was found
         */
        public void moduleFound(ScanResult module);
    
    }
    
    /**
     * Add a delegate to listen for discovered modules.
     * @param d the delegate to add
     */
    public void addDelegate(ETHScanDelegate d) {
        if (!delegates.contains(d)) {
            delegates.add(d);
        }
    }
    
    /**
     * Remove a delegate from this object.
     * @param d the delegate to remove
     */
    public void removeDelegate(ETHScanDelegate d) {
        if (delegates.contains(d)) {
            delegates.remove(d);
        }
    }
    
    /**
     * Update all of the delegates that are listening for modules.
     * @param module the module found.
     */
    void updateDelegates(ScanResult module) {
        for (ETHScanDelegate d : delegates) {
            d.moduleFound(module);
        }
    }
    
    /**
     * Perform the UDP search for modules on the network.
     */
    public void udpAction() {
        try {
            receive_thread = new Thread() {
                @Override
                public void run() {
                    receiveUDPPacket();
                }
            };

            socket = new DatagramSocket(30303);
            socket.setBroadcast(true);
            InetAddress address = InetAddress.getByName("255.255.255.255");
            packet = new DatagramPacket(udp_string.getBytes(),
                    udp_string.length(), address, 30303);

            socket.send(packet);
            receive_thread.start();
        } catch (IOException e) {
            close_action();
        }
    }
    
    /**
     * Find the data we need in the UDP packet.
     */
    private void receiveUDPPacket() {
        try {
            byte[] buf = new byte[1500 - 28];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = ' ';
            }
            packet = new DatagramPacket(buf, buf.length);

            //ignore first packet which is a loopback one
            socket.receive(packet);
            
            for (;;) {
                for (int i = 0; i < buf.length; i++) {
                    buf[i] = ' ';
                }
                packet = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {

                }

                if (new String(packet.getData()).trim().length() == 0) {
                    continue;
                }
                if (packet.getLength() < 35) {
                    continue;
                }

                lock.lock();
                try {
                    addDiscoveryEntry(packet);
                } finally {
                    lock.unlock();
                }
            }
        } catch (IOException e) {
            close_action();
        }
    }
    
    /**
     * Notify the delegates of a discover.
     * @param receivedPacket 
     */
    private void addDiscoveryEntry(DatagramPacket receivedPacket) {
        byte[] packet_buffer = receivedPacket.getData();
        int packet_length = receivedPacket.getLength();
        int data_count;
        String mac_address = "";
        String host_name = "";
        String ip_addr = "";

        byte[] temp = new byte[6];
        int index;

        String byteToStr;
        for (data_count = 0; data_count < packet_length;) {
            switch (packet_buffer[data_count]) {
                case 0x01:
                    return;
                case 0x02: // MAC address
                    data_count++;
                    for (index = 0; index < 6; index++) {
                        temp[index] = packet_buffer[data_count++];
                    }

                    if (temp.length == 0) {
                        break;
                    }
                    // MAC address string construction
                    StringBuilder sb = new StringBuilder(22);
                    for (byte b : temp) {
                        if (sb.length() > 0) {
                            sb.append(':');
                        }
                        sb.append(String.format("%02x", b));
                    }
                    mac_address = mac_address.concat(sb.toString());
                    break;
                case 0x03: // MAC Type
                    data_count++;
                    temp = new byte[16];
                    index = 0;
                    while ((packet_buffer[data_count] != 0x0d) && (packet_buffer[data_count + 1] != 0x0a)) {
                        temp[index++] = packet_buffer[data_count++];
                    }
                    break;
                case 0x04: // Host Name
                    data_count++;
                    temp = new byte[16];
                    index = 0;
                    while ((packet_buffer[data_count] != 0x0d) && (packet_buffer[data_count + 1] != 0x0a)) {
                        temp[index++] = packet_buffer[data_count++];
                    }
                    byteToStr = new String(temp);
                    byteToStr = byteToStr.substring(0, byteToStr.indexOf(" "));

                    host_name = host_name.concat(byteToStr);
                    break;
                case 0x05: // IPv4 Address
                    temp = new byte[4];
                    data_count++;
                    index = 0;
                    while (index < 4) {
                        temp[index++] = packet_buffer[data_count++];
                    }

                    if (temp.length == 0) {
                        break;
                    }
                    sb = new StringBuilder(22);
                    for (byte b : temp) {
                        if (sb.length() > 0) {
                            sb.append('.');
                        }
                        sb.append(String.format("%d", (short) (b & 0xFF)));
                    }
                    ip_addr = ip_addr.concat(sb.toString());
                    break;
                case 0x06: // IPv6U address
                    break;
                case 0x07: // IPv6M Address
                    break;
                case 0x08: // IPv6Deafult Router address
                    break;
                case 0x09: // IPv6Deafult Gateway address
                    break;
                case 0x40:
                    
                    data_count++;
                    temp = new byte[16];
                    index = 0;
                    while ((packet_buffer[data_count] != 0x0d) && (packet_buffer[data_count + 1] != 0x0a)) {
                        temp[index++] = packet_buffer[data_count++];
                    }

                    switch ((int)temp[0]  & 0xFF) {
                        case 30:
                        case 31:
                        case 34:
                        case 35:
                            // Ignoring dS modules for this application.
                            return;
                        case 18:    // ETH002 32 bit
                        case 19:    // ETH008 32 bit
                        case 20:    // ETH484 32 bit
                        case 21:    // ETH8020 32 bit
                        case 51:    // ETH1620
                        case 52:    // ETH1610
                        case 54:    // ETH24V008
                        case 200:   // ETH-UPLOADER
                            ScanResult r = new ScanResult(ip_addr, host_name, (int)temp[0]  & 0xFF, mac_address);
                            updateDelegates(r);
                            break;
                        default:
                            return;
                    }
                    
                    break;
                case 0x41:
                    return;
                    
                default:
                    return;
            }
            if (packet_buffer[data_count] == 0x0d) {
                data_count++;
            }
            if (packet_buffer[data_count] == 0x0a) {
                data_count++;
            }
        }
    }
    
    /**
     * Close the UDP port.
     */
    public void close_action() {
        if (socket != null) {
            if (socket.isConnected() == true) {
                socket.close();
            }
        }
        if (receive_thread != null) {
            if (receive_thread.isAlive() == true) {
                receive_thread = null;
            }
        }

    }
    
}
