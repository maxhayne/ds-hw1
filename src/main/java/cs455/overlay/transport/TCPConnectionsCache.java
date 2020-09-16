package cs455.overlay.transport;
import cs455.overlay.routing.*;
import cs455.overlay.transport.*;
import cs455.overlay.wireformats.*;
import java.util.concurrent.atomic.*;
import java.util.*;
import java.net.*; 
import java.io.*;

public class TCPConnectionsCache {
	
	private Vector<TCPConnection> cache = new Vector<>();
	private Vector<Integer> idBank = new Vector<Integer>();
	private TCPConnection registry = null; // only for messaging nodes

	private AtomicInteger receivedPackets;
	private AtomicInteger sentPackets;
	private AtomicInteger relayedPackets;
	private AtomicLong sumValuesSent;
	private AtomicLong sumValuesReceived;

	private int ourNodeID;
	private RoutingTable messagingNodeRoutingTable = null;
	private boolean isRunning;

	private int ourID;
	private int ourServerPort;
	private String ourServerIP;

	public TCPConnectionsCache() {
		for (int i = 0; i < 128; i++) {
			idBank.add(i);
		}

		Collections.shuffle(idBank); // shuffling the order of all of the id's being given out

		this.receivedPackets = new AtomicInteger();
		this.sentPackets = new AtomicInteger();
		this.relayedPackets = new AtomicInteger();
		this.sumValuesSent = new AtomicLong();
		this.sumValuesReceived = new AtomicLong();
		this.ourNodeID = -1;
		this.isRunning = false;
	}

	public synchronized void zeroStatistics() {
		this.receivedPackets.set(0);
		this.sentPackets.set(0);
		this.relayedPackets.set(0);
		this.sumValuesSent.set(0);
		this.sumValuesReceived.set(0);
	}

	public synchronized void populateSummary(int nodeID, OverlayNodeReportsTrafficSummary summary) {
		for (TCPConnection conn : cache) {
			if (conn.getNodeID() == summary.nodeID) {
				conn.setSentPackets(summary.packetsSent);
				conn.setReceivedPackets(summary.packetsReceived);
				conn.setRelayedPackets(summary.packetsRelayed);
				conn.setSumValuesSent(summary.sumSent);
				conn.setSumValuesReceived(summary.sumReceived);
				conn.summarized();
				break;
			} 
		}
	}

	public synchronized void zeroSummary() {
		for (TCPConnection conn : cache) {
			conn.setSentPackets(0);
			conn.setReceivedPackets(0);
			conn.setRelayedPackets(0);
			conn.setSumValuesSent(0);
			conn.setSumValuesReceived(0);
			conn.unsummarized();
		}
	}

	public void finalSummary() {
		int packetsReceived = 0;
		int packetsRelayed = 0;
		int packetsSent = 0;
		long sumSent = 0;
		long sumReceived = 0;

		System.out.println("--------------------------------------------------------------------------------------------------------");
		System.out.printf("| %4s | %12s | %16s | %15s | %19s | %19s |\n", "NODE", "PACKETS_SENT", "PACKETS_RECEIVED" , "PACKETS_RELAYED", "SUM_VALUES_SENT", "SUM_VALUES_RECEIVED");
		//System.out.println();
		System.out.println("--------------------------------------------------------------------------------------------------------");
		for (TCPConnection conn : cache) {
			System.out.format("| %4d | %,12d | %,16d | %,15d | %,19d | %,19d |\n", conn.getNodeID(), conn.getSentPackets(), conn.getReceivedPackets(), conn.getRelayedPackets(), conn.getSumValuesSent(), conn.getSumValuesReceived());
			packetsSent += conn.getSentPackets();
			packetsRelayed += conn.getRelayedPackets();
			packetsReceived += conn.getReceivedPackets();
			sumSent += conn.getSumValuesSent();
			sumReceived += conn.getSumValuesReceived();
		}
		System.out.format("| %4s | %,12d | %,16d | %,15d | %,19d | %,19d |\n", "SUM", packetsSent, packetsReceived, packetsRelayed, sumSent, sumReceived);
		System.out.println("--------------------------------------------------------------------------------------------------------");
		return;
	}

	public String quickSummary() {
		int packetsReceived = 0;
		int packetsRelayed = 0;
		int packetsSent = 0;
		long sumSent = 0;
		long sumReceived = 0;
		String dataSummary = "";

		for (TCPConnection conn : cache) {
			packetsSent += conn.getSentPackets();
			packetsRelayed += conn.getRelayedPackets();
			packetsReceived += conn.getReceivedPackets();
			sumSent += conn.getSumValuesSent();
			sumReceived += conn.getSumValuesReceived();
		}

		dataSummary += "Packets Sent: " + packetsSent + "\n";
		dataSummary += "Packets Relayed: " + packetsRelayed + "\n";
		dataSummary += "Packets Received: " + packetsReceived + "\n";
		dataSummary += "Sent Sum: " + sumSent + "\n";
		dataSummary += "Received Sum: " + sumReceived + "\n";

		return dataSummary;
	}

	public String printCountersAndDiagnostics() {
		String dataSummary = "";
		dataSummary += "Packets Sent: " + this.getSentPackets() + "\n";
		dataSummary += "Packets Relayed: " + this.getRelayedPackets() + "\n";
		dataSummary += "Packets Received: " + this.getReceivedPackets() + "\n";
		dataSummary += "Sent Sum: " + this.getSumValuesSent() + "\n";
		dataSummary += "Received Sum: " + this.getSumValuesReceived() + "\n";
		return dataSummary;
	}

	public synchronized void fullSend() throws IOException {
		Random random = new Random();
		int payload = random.nextInt(); // generating random number
		int source = this.getOurID(); // getting this node's id
		int sink = this.randomSink(); // getting a random sink from the routing table
		OverlayNodeSendsData message = new OverlayNodeSendsData(source, sink, payload);
		int nextDestination = this.messageRoute(sink); // calculating the next destination based on the sink, and our unique routing table
		byte[] msg = message.getBytes();

		for (TCPConnection sendConnection : cache) { // find the correct TCPConnection, add msg to the sendQueue
			if (sendConnection.getNodeID() == nextDestination){
				try {
					sendConnection.addToSendQueue(msg); // adding to the send queue for this TCPConnection
				} catch(InterruptedException ie) {
					System.err.print("sendQueue could not be added to.");
					break;
				}
				break;
			} 
		}

		this.incrementSent(); // incrementing sent
		this.addSentSum(payload); // adding to the sentSum
	}

	public synchronized void fullRelay(OverlayNodeSendsData msgObj) throws IOException {
		msgObj.hopTrace.add(this.getOurID());
		msgObj.hops += 1;
		int nextDestination = this.messageRoute(msgObj.sink);
		byte[] msg = msgObj.getBytes();

		for (TCPConnection sendConnection : cache) { // find the correct TCPConnection, add msg to the sendQueue
			if (sendConnection.getNodeID() == nextDestination){
				try {
					sendConnection.addToSendQueue(msg); // adding to the send queue for this TCPConnection
				} catch(InterruptedException ie) {
					System.err.print("sendQueue could not be added to.");
					break;
				}
				break;
			} 
		}

		this.incrementRelayed(); // incrementing relayed
	}

	public synchronized void fullReceive(int payload) {
		this.incrementReceived(); // incrementing received
		this.addReceivedSum(payload); // incrementing received sum
	}

	public synchronized void run() {
		this.isRunning = true;
	}

	public synchronized boolean isRunning() {
		return this.isRunning;
	}

	public synchronized void unrun() {
		this.isRunning = false;;
	}

	public long getSumValuesSent() {
		return this.sumValuesSent.get();
	}

	public long getSumValuesReceived() {
		return this.sumValuesReceived.get();
	}

	public int getReceivedPackets() {
		return this.receivedPackets.get();
	}

	public int getRelayedPackets() {
		return this.relayedPackets.get();
	}

	public int getSentPackets() {
		return this.sentPackets.get();
	}

	public void setSumValuesSent(long sum) {
		this.sumValuesSent.getAndSet(sum);
	}

	public void setSumValuesReceived(long sum) {
		this.sumValuesReceived.getAndSet(sum);
	}

	public void setReceivedPackets(int received) {
		this.receivedPackets.getAndSet(received);
	}

	public void setRelayedPackets(int relayed) {
		this.relayedPackets.getAndSet(relayed);
	}

	public void setSentPackets(int sent) {
		this.sentPackets.getAndSet(sent);
	}

	public void incrementReceived() {
		this.receivedPackets.getAndIncrement();
	}

	public void incrementSent() {
		this.sentPackets.getAndIncrement();
	}

	public void incrementRelayed() {
		this.relayedPackets.getAndIncrement();
	}

	public void addReceivedSum(int payload) {
		this.sumValuesReceived.getAndAdd((long)payload);
	}

	public void addSentSum(int payload) {
		this.sumValuesSent.getAndAdd((long)payload);
	}

	public synchronized void addMessagingConnection(TCPConnection conn) {
		this.cache.add(conn);
	}

	public void sendToRegistry(byte[] message) throws IOException { // sending data out of the registry
		if (this.registry != null) {
			this.registry.sendData(message);
		}
	}

	public void setRegistry(TCPConnection registry) {
		this.registry = registry;
	}

	public void setOurID(int id) {
		this.ourID = id;
	}

	public int getOurID() {
		return this.ourID;
	}

	public void setServerPort(int port) {
		this.ourServerPort = port;
	}

	public int getServerPort() {
		return this.ourServerPort;
	}

	public void setServerIP(String ip) {
		this.ourServerIP = ip;
	}

	public String getServerIP() {
		return this.ourServerIP;
	}

	public void setMessagingTable(RoutingTable rt) {
		this.messagingNodeRoutingTable = rt;
	}

	public void unsetMessagingTable(RoutingTable rt) {
		this.messagingNodeRoutingTable = null;
	}

	public synchronized void setupNodeID(int id) {
		for (TCPConnection conn : cache) {
			if (conn.getNodeID() == id) {
				conn.setup();
				break;
			}
		}
	}

	public synchronized void completeNodeID(int id) {
		for (TCPConnection conn : cache) {
			if (conn.getNodeID() == id) {
				conn.complete();
				break;
			}
		}
	}

	public synchronized void unsetupAll() { // changing all setup to false again
		for (TCPConnection conn : cache) {
			conn.unsetup();
		}
	}

	public synchronized void uncompleteAll() { // changing all setup to false again
		for (TCPConnection conn : cache) {
			conn.uncomplete();
		}
	}

	public synchronized void resetAllTables() {
		for (TCPConnection conn : cache) {
			conn.resetRoutingTable();
		}
	}

	public synchronized boolean check(String ip, int port) {
		for (TCPConnection cacheConnection : cache) {
			if (cacheConnection.getIP().equals(ip) && cacheConnection.getServerPort() == port){
				return true;
			}
		}

		return false; // return false if the client isn't already in the connections cache
	}

	public synchronized Integer numRegistered() {
		return cache.size();
	}

	public synchronized Integer register(Socket sock, TCPReceiverThread receiver, String ip, int port) throws IOException {
		if (idBank.size() == 0) { // if no more id's to give out, return -1
			return -1;
		}

		Integer newID = idBank.firstElement();
		TCPConnection newConn = new TCPConnection(sock, newID, receiver, ip, port);
		cache.add(newConn);
		idBank.remove(0);

		return newID;
	}

	
	public synchronized Integer deregister(String ip, int port) {
		for (TCPConnection cacheConnection : cache) {
			if (cacheConnection.getServerPort() == port && cacheConnection.getIP().equals(ip)){
				int id = cacheConnection.getNodeID();
				cache.removeElement(cacheConnection);
				idBank.add(id); // adding id back to id bank space 
				return id;
			} 
		}
		return -1; // Could not deregister, the connection does not exist
	}
	
	public synchronized Integer findAndSend(Socket sock, byte[] message) throws IOException { // find socket in the connections cache and send data out of it
		for (TCPConnection cacheConnection : cache) {
			if (cacheConnection.getSocket().getInetAddress() == sock.getInetAddress() && cacheConnection.getSocket().getPort() == sock.getPort()){
				cacheConnection.sendData(message); // send data out
				return 1; // successful
			} 
		}
		return 0; // unsuccessful
	}

	public synchronized Integer sendToNode(int id, byte[] message) throws IOException { // find socket in the connections cache and send data out of it
		for (TCPConnection cacheConnection : cache) {
			if (cacheConnection.getNodeID() == id){
				cacheConnection.sendData(message); // send data out
				return 1; // successful
			} 
		}
		return 0; // unsuccessful
	}

	public synchronized String list() {
		String list = "";
		for (TCPConnection conn : cache) {
			list += conn.getIP() + ", " + conn.getServerPort() + ", " + conn.getNodeID() + "\n";
		}
		return list;
	}

	public synchronized String listRoutingTables() {
		System.out.println("---------------------------------------------------");
		System.out.println("Table Format:    IP    PORT    NODE_ID    HOPS_AWAY");
		System.out.println("---------------------------------------------------\n");
		String list = "";
		for (TCPConnection conn : cache) {
			list += "---------- Routing Table for Node " + conn.getNodeID() + " ----------\n";
			list += conn.getTable() + "\n\n";
		}
		if (list.length() != 0) {
			list = list.substring(0, list.length()-1);
		}
		return list;
	}

	public synchronized String listNodeLists() {
		String list = "";
		for (TCPConnection conn : cache) {
			list += "---------- Node List for Node " + conn.getNodeID() + " ----------\n";
			list += conn.getNodeList() + "\n\n";
		}
		return list;
	}

	public synchronized Integer getSize() {
		return cache.size();
	}

	public synchronized Vector<Integer> vectorID() { // creating fresh list of ID's to return to the registry main thread
		Vector<Integer> ids = new Vector<Integer>();
		for (TCPConnection conn : cache) {
			ids.add(conn.getNodeID());
		}
		return ids;
	}

	public synchronized String friendIP(int friendID) { // find IP of Registered node based on ID
		String tempIP = "";
		for (TCPConnection conn : cache) {
			if (conn.getNodeID() == friendID) {
				tempIP = conn.getIP();
				break;
			}
		}
		return tempIP;
	}

	public synchronized int friendServerPort(int friendID) { // find IP of Registered node based on ID
		int tempServerPort = -1;
		for (TCPConnection conn : cache) {
			if (conn.getNodeID() == friendID) {
				tempServerPort = conn.getServerPort();
				break;
			}
		}
		return tempServerPort;
	}

	public synchronized void nodeRoutingEntry(RoutingEntry entry, int nodeID) { // add routing entry to node with specific nodeID
		for (TCPConnection conn : cache) {
			if (conn.getNodeID() == nodeID) {
				conn.addRoutingEntry(entry);
				break;
			}
		}
		return;
	}

	public synchronized void addNodeSetList(Vector<Integer> setList, int nodeID) { // adding set list
		for (TCPConnection conn : cache) {
			if (conn.getNodeID() == nodeID) {
				conn.addSetList(setList);
				break;
			}
		}
		return;
	}

	public synchronized boolean sendManifests() {
		try {
			for (TCPConnection conn : cache) {
				RoutingTable tempTable = conn.rtReference();
				RegistrySendsNodeManifest manifest = new RegistrySendsNodeManifest(tempTable);
				byte[] message = manifest.getBytes();
				conn.sendData(message);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public synchronized boolean sendTaskInitiate(int messages) {
		try {
			RegistryRequestsTaskInitiate task = new RegistryRequestsTaskInitiate(messages);
			for (TCPConnection conn : cache) {
				byte[] message = task.getBytes();
				conn.sendData(message);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public synchronized boolean askTrafficSummary() {
		try {
			RegistryRequestsTrafficSummary summary = new RegistryRequestsTrafficSummary();
			for (TCPConnection conn : cache) {
				byte[] message = summary.getBytes();
				conn.sendData(message);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public synchronized boolean isEveryoneSetup() {
		for (TCPConnection conn : cache) {
			if (!conn.isSetup()) {
				return false;
			}
		}

		return true;
	}

	public synchronized boolean isEveryoneComplete() {
		for (TCPConnection conn : cache) {
			if (!conn.isComplete()) {
				return false;
			}
		}

		return true;
	}

	public synchronized boolean hasEveryoneSummarized() {
		for (TCPConnection conn : cache) {
			if (!conn.hasSummarized()) {
				return false;
			}
		}

		return true;
	}

	
	public synchronized void removeAllConnections() {
		for (TCPConnection conn : cache) {
			if (!conn.isRegistry()) {
				conn.setIsSending(false);
				conn.stopReceiver();
				conn.closeSocket();
			}
		}

		cache.removeIf(conn -> (conn.isRegistry() == false)); // removing all connections if they are not the registry
		return;
	}

	public synchronized void registryDisconnect() {
		registry.stopReceiver();
		registry.closeSocket();
	}

	public synchronized int randomSink() {
		return this.messagingNodeRoutingTable.randomSink();
	}

	public synchronized int messageRoute(int sink) {
		return this.messagingNodeRoutingTable.route(sink);
	}
}