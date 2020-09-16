package cs455.overlay.node;
import cs455.overlay.transport.*;
import cs455.overlay.wireformats.*;
import cs455.overlay.routing.*;
import cs455.overlay.util.*;
import java.net.*; 
import java.io.*;
import java.util.*;
import java.lang.Thread;

public class Registry {

	public static void main(String[] args) throws IOException {

		InetAddress inetAddress = InetAddress.getLocalHost(); // grabbing local address to pass in registration
		String localhost = inetAddress.getHostAddress().toString();
		
		TCPConnectionsCache connections = new TCPConnectionsCache(); // Creating connections cache, which will hold connections to messaging nodes
		ServerSocket server = null;
		Thread serverThread;
		Integer serverPortNum = -1;

		try {
			serverPortNum = Integer.valueOf(args[0]);
		} catch (NumberFormatException e) {
			System.err.println("Registry: Invalid port number (" + args[0] + ") provided.");
			return;
		}
		
		try {
			server = new ServerSocket(serverPortNum, 128, inetAddress); // creating server socket on the localhost address of the running machine
			serverThread = new Thread(new TCPServerThread(server, connections)); // creating new server thread, which will listen for connections from nodes
			serverThread.start(); // starting server thread
		} catch (IOException ioe) {
			System.err.println(ioe);
			if (server != null) {
				server.close();
			}
			return;
		}

		boolean loop = true;
		Scanner scanner = new Scanner(System.in);

		while (loop) { // looping for user input
			System.out.print("Command: ");
			String input = scanner.nextLine();
			input = input.toLowerCase();
			String[] split = input.split("\\s+");
			
			if (split.length > 2) { // no command should have more than two parts
				System.out.println("Invalid command.");
				continue;
			}
			
			String command = split[0]; // first string of the split should be the base command
			
			switch (command) {
				case "list-messaging-nodes": {
					System.out.print(connections.list());
					break;
				}

				case "setup-overlay" : {
					int rtEntries;
					try {
						rtEntries = Integer.valueOf(split[1]);
					} catch (NumberFormatException e) {
						System.err.println("Specified number of entries (" + split[1] + ") is invalid.");
						break;
					} catch (ArrayIndexOutOfBoundsException ob) { // if the size of each routing table is not specified, automatically set to three
						rtEntries = 3;
					}

					if (connections.isRunning()) {
						System.out.println("Task is already running, cannot setup the overlay while task is in progress.");
						break;
					}

					if (connections.getSize() < 3) {
						System.out.println("Overlay must contain 3 or more nodes. It currently contains (" + connections.getSize() + ") nodes.");
						break;
					} else if (rtEntries > connections.getSize()) { // this is WAY above unreasonable, any I don't add entries that wrap around to tables anyway
						System.out.println("Please choose a more reasonable value for your 'number-of-routing-table-entries'.");
						break;
					}

					connections.resetAllTables(); // resetting tables if the overlay has already been setup once
					connections.unsetupAll(); // changing status of all nodes to unsetup, which are booleans used to check when the setup is complete

					// Now need to construct routing tables at each TCPConnection, loop through them while sending
					Vector<Integer> overlayID = connections.vectorID(); // grab all IDs of overlay nodes
					Collections.sort(overlayID); // sort nodeID's of all overlay nodes

					int[] helperList = new int[2*overlayID.size()]; // place them twice into an array
					for (int i = 0; i < overlayID.size(); i++) {
						helperList[i] = overlayID.get(i);
						helperList[i+overlayID.size()] = overlayID.get(i);
					}

					int arraySize = overlayID.size();
					for (int i = 0; i < arraySize; i++) { // for every entry, find routing table entries
						int thisNode = helperList[i];
						int spaces = 1;
						Vector<Integer> perTable = new Vector<Integer>();
						for (int hop = 1; spaces < arraySize && hop <= rtEntries; hop++) {
							perTable.add(helperList[i+spaces]); // add nodes that should be accessible to this node
							spaces *= 2;
						}

						int added = 0;
						for (Integer friendID : perTable) { // create routing entries 
							int tempPort = connections.friendServerPort(friendID);
							String tempIP = connections.friendIP(friendID);
							RoutingEntry entry = new RoutingEntry(tempIP, tempPort, friendID, (int)Math.pow(2, added));
							// Place this RoutingEntry into thisNode's routing table
							connections.nodeRoutingEntry(entry, thisNode); // adding entry to the table for this particular node
							// Need to add nodeList to every node
							Vector<Integer> minusID = new Vector<Integer>(overlayID); // creating new list based on nodeList
							minusID.removeElement(thisNode); // removing entry of node that this will go to
							connections.addNodeSetList(minusID, thisNode);
							added += 1;
						}

					}

					System.out.println("Done creating Routing tables for every node");

					if (connections.sendManifests()) { // sending out the manifest
						System.out.println("Sent all node manifests.");
					} else {
						System.out.println("There was a problem sending out all of the manifests.");
					}

					// Now need to wait for all TCPConnections in the cache to set isSetup to true
					while (!connections.isEveryoneSetup()) { // don't need or want to check continuously (livelock?)
						try {
						    Thread.sleep(500); // sleeping between checks to see if every node successfully connected to every node in their routing table
						} catch(InterruptedException e) {
						     continue;
						}
					}

					System.out.println("Registry is now ready to initiate tasks.");

					break;

				}

				case "list-routing-tables" : {
					System.out.print(connections.listRoutingTables()); // listing the routing tables for all TCPConnections in the connection cache
					//System.out.print(connections.listNodeLists()); // ONLY FOR DEBUGGING AND CHECKING THAT EVERY NODE HAS THE CORRECT LIST
					break;
				}

				case "start" : {
					int messages;
					try {
						messages = Integer.valueOf(split[1]);
					} catch (NumberFormatException e) {
						System.err.println("Specified number of messages (" + split[1] + ") is invalid.");
						break;
					} catch (ArrayIndexOutOfBoundsException ob) {
						System.err.println("Must specify the number of messages (rounds) to as your argument.");
						break;
					}

					if (!connections.isEveryoneSetup()) {
						System.out.println("Not everyone has been setup with their routing tables. Issue a 'setup-overlay' command if you haven't yet.");
						break;
					}

					if (connections.isRunning()) {
						System.out.println("Task is already running, cannot start again.");
						break;
					}

					if (connections.getSize() < 3) {
						System.out.println("Overlay must contain 3 or more nodes. It currently contains (" + connections.getSize() + ") nodes.");
						break;
					} 

					if (messages <= 0) {
						System.out.println("Number of messages must be greater than 0.");
						break;
					}

					connections.zeroSummary(); // zeroing out the summary in each TCPConnection
					connections.run(); // setting the run flag, so that other critical commands can't take place while the overlay is running
					connections.uncompleteAll(); // changing status of all nodes to unsetup, which are booleans used to check when the setup is complete
					connections.sendTaskInitiate(messages); // sending out initiate message

					while (!connections.isEveryoneComplete()) { // don't need or want to check continuously (livelock?)
						//System.out.println("Checking if everyone is complete.");
						try {
						    Thread.sleep(500); // sleeping between checks to see if every node successfully connected to every node in their routing table
						} catch(InterruptedException e) {
						     continue;
						}
					}

					//System.out.println("All overlay nodes have sent completion messages. Waiting 20 seconds to request totals.");
					
					waitForSummaries wait = new waitForSummaries(connections);
					Thread waiter;
					waiter = new Thread(wait); // creating new waiting thread, which will print to the console after getting data from the nodes
					waiter.start(); // starting thread
					break;
				}

				default: {
					System.out.println("Invalid command issued, try again."); // if none of the commands, try again
					break;
				}
			}
		}
		scanner.close();
		return;
	}
}
