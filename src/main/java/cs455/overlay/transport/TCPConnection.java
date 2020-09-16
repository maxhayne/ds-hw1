package cs455.overlay.transport;
import cs455.overlay.transport.*;
import cs455.overlay.routing.*;
import java.net.*; 
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class TCPConnection extends Thread {

	private TCPReceiverThread receiver = null; // reference to this TCPConnection's receiver 
	private Integer nodeID = null;
	private Socket socket = null;
	private DataOutputStream dout = null;
	private boolean isRegistry;
	
	private int serverPort;
	private String ip;

	private RoutingTable table = null; // routing table will be inside each TCPConnection for the registry

	private boolean isSetup;
	private boolean isComplete;
	private boolean hasSummarized;
	private boolean isSending;

	private AtomicInteger sentPackets;
	private AtomicInteger relayedPackets;
	private AtomicInteger receivedPackets;
	private AtomicLong sumSent;
	private AtomicLong sumReceived;

	private BlockingQueue<byte[]> sendQueue;

	public TCPConnection(Socket socket, Integer id, TCPReceiverThread receiver, String ip, int port) throws IOException { // constructor for registry to create TCPConnections
		this.nodeID = id;
		this.socket = socket;
		this.dout = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 65536));
		//this.dout = new DataOutputStream(socket.getOutputStream());
		this.ip = ip;
		this.serverPort = port;
		this.receiver = receiver;
		this.isRegistry = false;
		this.table = new RoutingTable();
		this.isSetup = false;
		this.isComplete = false;
		this.hasSummarized = false;
		this.isSending = false;

		this.sentPackets = new AtomicInteger(0);
		this.relayedPackets = new AtomicInteger(0);
		this.receivedPackets = new AtomicInteger(0);
		this.sumSent = new AtomicLong(0);
		this.sumReceived = new AtomicLong(0);
	}

	public TCPConnection(Socket socket) throws IOException { // constructor for messaging node adding other messaging nodes
		this.socket = socket;
		this.dout = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 65536));
		//this.dout = new DataOutputStream(socket.getOutputStream());
		this.isRegistry = false;
		this.sendQueue = new LinkedBlockingQueue<byte[]>();
		this.isSending = true;
	}

	public synchronized boolean getIsSending() {
		return this.isSending;
	}

	public synchronized void setIsSending(boolean var) {
		this.isSending = var;
	}

	public void addToSendQueue(byte[] msg) throws InterruptedException {
		this.sendQueue.put(msg);
	}

	public synchronized void addReceiver(TCPReceiverThread receiver) {
		this.receiver = receiver;
	}

	public long getSumValuesSent() {
		return this.sumSent.get();
	}

	public long getSumValuesReceived() {
		return this.sumReceived.get();
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
		this.sumSent.set(sum);
	}

	public void setSumValuesReceived(long sum) {
		this.sumReceived.set(sum);
	}

	public void setReceivedPackets(int received) {
		this.receivedPackets.set(received);
	}

	public void setRelayedPackets(int relayed) {
		this.relayedPackets.set(relayed);
	}

	public void setSentPackets(int sent) {
		this.sentPackets.set(sent);
	}

	public synchronized void unsetup() {
		this.isSetup = false;
	}

	public synchronized void setup() {
		this.isSetup = true;
	}

	public synchronized boolean isSetup() {
		return this.isSetup;
	}

	public synchronized void uncomplete() {
		this.isComplete = false;
	}

	public synchronized void complete() {
		this.isComplete = true;
	}

	public synchronized boolean isComplete() {
		return this.isComplete;
	}

	public synchronized void summarized() {
		this.hasSummarized = true;
	}

	public synchronized void unsummarized() {
		this.hasSummarized = false;
	}

	public synchronized boolean hasSummarized() {
		return this.hasSummarized;
	}

	public synchronized void resetRoutingTable() {
		this.table.reset();
	}

	public synchronized String getTable() {
		return this.table.printTable();
	}

	public synchronized String getNodeList() {
		return this.table.printNodeList();
	}

	public synchronized void sendData(byte[] dataToSend) throws IOException { // sending data must be synchronized
		int dataLength = dataToSend.length;
		dout.writeInt(dataLength);
		dout.write(dataToSend, 0, dataLength);
		dout.flush();
	}

	public Integer getNodeID() {
		return this.nodeID;
	}

	public Socket getSocket() {
		return this.socket;
	}

	public Integer getServerPort() {
		return this.serverPort;
	}

	public Integer getPort() {
		return this.socket.getPort();
	}

	public String getIP() {
		return this.ip;
	}

	public boolean isRegistry() {
		return this.isRegistry;
	}

	public void setRegistry() {
		this.isRegistry = true;
	}

	public synchronized void addRoutingEntry(RoutingEntry entry) { // add routing entry to this node's routing table
		this.table.addEntry(entry);
	}

	public synchronized void addSetList(Vector<Integer> setList) {
		this.table.setList(setList);
	}

	public synchronized RoutingTable rtReference() {
		return this.table;
	}

	public void stopReceiver() {
		if (this.receiver != null) {
			this.receiver.stopLoop();
		}
	}

	public void setNodeID(int id) { // pretty much only called once upon creation
		this.nodeID = id;
	}

	public synchronized void closeSocket() {
		try {
			this.dout.close();
			this.socket.close();
		} catch (IOException ioe) {
			System.err.println("There was a problem closing the socket or data output stream for this TCPConnection.");
			return;
		}
	}

	@Override
	public void run() { // if this run method is called, then there will be a thread in this TCPConnection that will continuously attempt to send the data in the sendQueue out of its socket
		while (this.getIsSending()) {
			byte[] msg = null;
			try {
				msg = this.sendQueue.poll(1, TimeUnit.SECONDS); // try to grab new message, exit blocking if waiting more than one second, allows for a check to the flag 'isSending'
			} catch (InterruptedException ie) {
				System.err.println("There was a problem grabbing a value from the sendQueue in a TCPConnection's run function.");
			}
			if (msg == null) {
				continue;
			} else {
				try {
					this.sendData(msg); // send the message along
				} catch (Exception e) {
					System.err.println("TCPConnection run() function could not send data.");
				}
			}
		}
	}
}
