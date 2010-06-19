/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.jaer.aemonitor;

import java.net.InetSocketAddress;
import java.util.ArrayList;

/**
 * A raw data packet with network source / destination information. Currently used to store the source addresses and starting event indices for data from UDP clients.
 *
 * @author tobi
 */
public class AENetworkRawPacket extends AEPacketRaw {

        /** List of clients that contributed to this packet. */
   public class ClientList extends ArrayList<ClientInfo>{}

    private ClientList clientList=new ClientList();

    /** Maps data from startingIndex to client address. Each item in client list will tell the EventExtractor2D which client the succeeding
     * data came from.
     */
    public class ClientInfo{
        private InetSocketAddress client;
        private int startingIndex;

        public ClientInfo(InetSocketAddress client, int index) {
            this.client = client;
            this.startingIndex = index;
        }

        /**
         * @return the client
         */
        public InetSocketAddress getClient() {
            return client;
        }

        /**
         * @return the startingIndex
         */
        public int getStartingIndex() {
            return startingIndex;
        }

    }


    /** Clears the packet and list of clients. */
    @Override
    public void clear() {
        super.clear();
        clientList.clear();
    }

    /** Add a client address and starting event index to the list of clients included in this raw packet.
     *
     * @param client
     * @param startingIndex
     */
    synchronized public void addClientAddress(InetSocketAddress client, int startingIndex){
        clientList.add(new ClientInfo(client,startingIndex));
    }

    /** Returns the list of clients included in this packet.
     *
     * @return the list of clients.
     */
   public ClientList getClientList(){
       return clientList;
   }
}
