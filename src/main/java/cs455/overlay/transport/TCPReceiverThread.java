package cs455.overlay.transport;
import cs455.overlay.transport.*;
import cs455.overlay.wireformats.*;
import cs455.overlay.routing.*;
import java.net.*;
import java.io.*;
import java.lang.Thread;
import java.util.*;

public class TCPReceiverThread extends Thread {

	private Socket socket = null;
	private TCPConnectionsCache cache = null;
	private DataInputStream din = null;
	private boolean registered;
	private boolean loop;

	public TCPReceiverThread(Socket sock, TCPConnectionsCache passed_cache) throws IOException {
		this.socket = sock;
		this.cache = passed_cache;
		this.din = new DataInputStream(new BufferedInputStream(sock.getInputStream(), 65536));
		//this.din = new DataInputStream(sock.getInputStream());
		this.registered = false;
		this.loop = true;
	}

	public TCPReceiverThread(Socket sock) throws IOException { // Constructor without TCPConnectionsCache passed
		this.socket = sock;
		this.din = new DataInputStream(new BufferedInputStream(sock.getInputStream(), 65536));
		//this.din = new DataInputStream(sock.getInputStream());
	}

	public synchronized void stopLoop() {
		this.loop = false;
	}

	public synchronized boolean getLoop() {
		return this.loop;
	} 

	@Override
	public void run() {

		// Grabbing some basic information about this socket, will be used to cross reference registration requests
		String thisIP = this.socket.getInetAddress().toString().substring(1);
		int thisPort = this.socket.getPort();
		int thisServerPort = -1;

		// call this.stopLoop() at the end of the case when the deregister

		while (this.getLoop()) { // looping until someone stops us 
			try {
				int dataLength = din.readInt();
				byte[] data = new byte[dataLength];
				din.readFully(data, 0, dataLength);

				switch (data[0]) { // switch statement for different message types that can be received by receiver
					case Protocol.OVERLAY_NODE_SENDS_REGISTRATION: {
						OverlayNodeSendsRegistration msg = new OverlayNodeSendsRegistration(data);
						int status = -1;
						String info = "";

						if (!msg.ip.equals(thisIP)) { // Is not who they say they are
							info = "Registration request unsuccessful. Node " + thisIP + " tried to register as " + msg.ip + ".";
						} else if (this.cache.check(msg.ip, msg.port)) { // Is already in the connections cache
							info = "Registration request unsuccessful. Node [" + msg.ip + ", " + msg.port + "] is already registered, but tried to register again.";
						} else if (this.registered) {
							info = "Registration request unsuccessful. This socket has already registered an overlay node.";
						} else { // Now must add the connection to TCPConnectionsCache
							status = cache.register(this.socket, this, msg.ip, msg.port);
						}

						if (info == "" && status == -1) {
							info = "Registration request unsuccessful. Cannot exceed 128 registered nodes.";
						} else if (status != -1) {
							info = "Registration was successful. The number of messaging nodes currently constituting the overlay is (" + String.valueOf(cache.numRegistered()) + ")";
							this.registered = true;
							thisServerPort = msg.port; // adding serverPort for deregistration in case of disconnect
						}

						RegistryReportsRegistrationStatus reportRegistration = new RegistryReportsRegistrationStatus(status, info);

						if (this.registered == true) { // created TCPConnection and added to cache, use this TCPConneciton to send data
							cache.findAndSend(this.socket, reportRegistration.getBytes());
						} else { // if registration was unsuccessful, use temporary TCPSender to send data along
							TCPSender sender = new TCPSender(this.socket);
							sender.sendData(reportRegistration.getBytes());
						}

						break;
					}

					case Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS : {
						RegistryReportsDeregistrationStatus status = new RegistryReportsDeregistrationStatus(data);
						System.out.println(status.info);
						System.out.println("Attempting to shutdown.");
						System.out.println("If you have already setup the overlay, this node will not shut down properly, and your routing tables will be out of date at all other nodes. Please issue another 'setup-overlay' command at the registry to both shutdown this node, and to bring routing tables up to date.");
						cache.removeAllConnections(); // removing all connections
						cache.registryDisconnect(); // disconnecting from the registry
						// this will not fully stop this node, as there will be some rogue TCPReceiver threads that will only shut down when a new setup-overlay is issued, and other nodes disconnect from them
						break;
					}

					case Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION: {
						OverlayNodeSendsDeregistration msg = new OverlayNodeSendsDeregistration(data);
						int status = -1;
						String info = "";

						if (!msg.ip.equals(thisIP)) { // Is not who they say they are
							info = "Deregistration request unsuccessful. Node " + thisIP + " tried to deregister [" + msg.ip + ", " + msg.port + "].";
						} else if (!this.cache.check(msg.ip, msg.port)) { // Isn't in the connections cache
							info = "Deregistration request unsuccessful. Node [" + msg.ip + ", " + msg.port + "] isn't registered, but tried to deregister anyway.";
						} else if (!this.registered) {
							info = "Deregistration request unsuccessful. This socket connection hasn't registered anything yet.";
						} else { // Now must remove the connection to TCPConnectionsCache
							status = cache.deregister(msg.ip, msg.port);
						}

						if (info == "" && status == -1) { // deregistration returned -1, but nothing else was wrong
							info = "Deregistration request unsuccessful. Something went wrong in the deregistration process.";
						} else if (status != -1) { // deregistration returned the node number, deregistration was successful
							info = "Deregistration was successful. The number of messaging nodes currently constituting the overlay is (" + String.valueOf(cache.numRegistered()) + ")";
							this.registered = false;
						}

						RegistryReportsDeregistrationStatus reportDeregistration = new RegistryReportsDeregistrationStatus(status, info);

						if (this.registered == true) { // was not deregistered just send out message from node that could not be deregistered
							cache.findAndSend(this.socket, reportDeregistration.getBytes());
						} else { // if deregistration was successful, use temporary TCPSender to send data along
							TCPSender sender = new TCPSender(this.socket);
							sender.sendData(reportDeregistration.getBytes());
							thisServerPort = -1; // removing from vector
						}

						this.stopLoop(); // Since this is a thread that is running for this node, can stop this loop unpon deregister
						break;
					}

					case Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS: {
						NodeReportsOverlaySetupStatus status = new NodeReportsOverlaySetupStatus(data);

						if (status.status == -1) {
							System.out.println(status.info);
							continue;
						} else {
							cache.setupNodeID(status.status); // status.status is the nodeID of the that has reported its setup status, setting flag 'setup' in that TCPConnection
						}
						break;
					}

					case Protocol.REGISTRY_SENDS_NODE_MANIFEST: {
						cache.removeAllConnections(); // removing all connections before creating more
						RegistrySendsNodeManifest manifest = new RegistrySendsNodeManifest(data);

						// use manifest.table to access table Routing table for this node
						Vector<RoutingEntry> entries = manifest.table.getEntries(); // grabbing routing entries
						boolean connectedToAll = true;

						for (RoutingEntry entry : entries) { // for every node, create socket and attempt to connect
							Socket messagingSocket;
							TCPConnection newNode;
							try {
								messagingSocket = new Socket(entry.getIP(),entry.getPort()); // create socket
								messagingSocket.setSendBufferSize(65536); // large send buffer size
								messagingSocket.setKeepAlive(true);
								newNode = new TCPConnection(messagingSocket); // create TCPConnection for socket
								newNode.setNodeID(entry.getNodeID()); // set TCPConnection's node id
								Thread senderThread = new Thread(newNode);
								senderThread.start(); // now there is a thread inside our TCPConnection that will constantly check for data to send
								cache.addMessagingConnection(newNode); // adding TCPConnection to the TCPConnectionsCache
							} catch (Exception e) {
								//System.err.println("There was a problem setting up a socket connection to node with id (" + entry.getNodeID() + ").");
								connectedToAll = false;
							}
						}

						// Setting the routing table for this messaging node to whatever we got from registry
						if (connectedToAll) { 
							manifest.table.setID(cache.getOurID());
							cache.setMessagingTable(manifest.table); // setting cache-wide Routing table to routing table we just decoded from the registry
							String info = "Overlay Node (" + cache.getOurID() + ") has connected to everyone in its routing table.";
							NodeReportsOverlaySetupStatus setup = new NodeReportsOverlaySetupStatus(cache.getOurID(), info);
							cache.sendToRegistry(setup.getBytes()); // sending status to registry
						} else {
							String info = "Overlay Node (" + cache.getOurID() + ") failed to connect to everyone in its routing table.";
							NodeReportsOverlaySetupStatus setup = new NodeReportsOverlaySetupStatus(-1, info);
							cache.sendToRegistry(setup.getBytes()); // sending status to registry
						}

						// May need to set the thisServerPort so that when cache.removeAllConnections() is run
						// the TCPReceiver thread on the other end

						// Actually, don't need to do that. Every messaging node connects to every other messaging node
						// in its routing table. When the node manifest is sent out a second time, all connections in
						// the connections cache are are removed, and sockets are closed. The receiving end of that 
						// socket exits the while loop because an exception has been raised, it tries to remove an entry
						// from the connections cache that is associated with it, but because the thisServerPort is still -1
						// it will not remove any entries, and the receiver thread will go offline gracefully

						break;

					}

					case Protocol.REGISTRY_REQUESTS_TASK_INITIATE: {

						cache.zeroStatistics(); // zeroing out our messaging node's counters
						RegistryRequestsTaskInitiate task = new RegistryRequestsTaskInitiate(data); // decoding
						OverlayNodeSendsData message = new OverlayNodeSendsData();
						cache.run(); // set 'isRunning' flag to true

						Random random = new Random();						
						Vector<Integer> hops = new Vector<Integer>();
						int source = cache.getOurID();
						int rounds = task.dataPackets;
						
						for (int i = 0 ; i < rounds; i++) { // send out messages to other nodes 
							cache.fullSend(); // calling a function that will send a 
						}

						String tempServerIP = cache.getServerIP();
						int tempServerPort = cache.getServerPort();
						int tempNodeID = cache.getOurID();
						OverlayNodeReportsTaskFinished finished = new OverlayNodeReportsTaskFinished(tempServerIP, tempServerPort, tempNodeID);
						cache.sendToRegistry(finished.getBytes()); // sending status to registry

						//System.out.println("Sent Task-Finished to Registry.");
						cache.unrun(); // set 'isRunning' flag to false
						break;
					}

					case Protocol.OVERLAY_NODE_SENDS_DATA : {
						OverlayNodeSendsData message = new OverlayNodeSendsData(data);

						if (message.sink == cache.getOurID()) { // if we are the sink
							cache.fullReceive(message.payload);
						} else { // we are not the sink
							cache.fullRelay(message);
							// Consolidating send, relay, and receive cases to one call, to minimize the exchange of locks
							// during the task. Should allow for receivers to read faster while their node is sending.
						}
						break;
					}

					case Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED: {
						OverlayNodeReportsTaskFinished finished = new OverlayNodeReportsTaskFinished(data);

						String nodeIP = finished.ip;
						int nodePort = finished.port;
						int nodeID = finished.nodeID;

						cache.completeNodeID(nodeID); // setting complete flag for this node

						break;
					}

					case Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY: {
						// Setting values of statistics I will construct on
						int nodeID = cache.getOurID();
						int packetsSent = cache.getSentPackets();
						int packetsRelayed = cache.getRelayedPackets();
						int packetsReceived = cache.getReceivedPackets();
						long sumSent = cache.getSumValuesSent();
						long sumReceived = cache.getSumValuesReceived();
						
						OverlayNodeReportsTrafficSummary summary = new OverlayNodeReportsTrafficSummary(nodeID, packetsSent, packetsRelayed, packetsReceived, sumSent, sumReceived);
						cache.sendToRegistry(summary.getBytes()); // sending out the traffic summary
						cache.zeroStatistics(); // zeroing out statistics

						break;
					}

					case Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY: {

						OverlayNodeReportsTrafficSummary summary = new OverlayNodeReportsTrafficSummary(data);
						int tempNodeID = summary.nodeID;
						cache.populateSummary(tempNodeID, summary); // populating cache-wide summary information 
						break;
					}

					default : {
						System.out.println("Unknown message received.");
						break;
					}
				}

			// Silencing these catches, this way, messaging nodes will continue to check for user input even when their receivers go offline
			} catch (SocketException se) {
				System.err.println("SocketException: " + se.getMessage());
				break;
			} catch (IOException ioe) {
				//System.err.println("IOException: " + ioe.getMessage());
				//System.err.println("Socket with IP " + thisIP + " and port number " + thisPort + " has disconnected; removing it from registration (if it successfully registered).");
				break;
			}
		}

		//We have exited the while-loop, need to clean up and automatically deregister, close datainputstream and socket
		if (this.din != null) { // close data input stream
			try {
				this.din.close();
			} catch (IOException ioe) {
				System.err.println("IOException attempting to close DataInputStream from disconnected node: " + ioe.getMessage());
			}
		}
		if (this.cache != null) {
			if (this.socket != null) {
				if (this.registered) {
					int closed = this.cache.deregister(thisIP, thisServerPort); // unregistering this socket from connections cache
				}
				try {
					this.socket.close(); // closing non-null socket
				} catch (IOException ioe) {
					System.err.println("IOException attempting to close socket from disconnected node: " + ioe.getMessage());
				}
			}
		}		
	}
}
