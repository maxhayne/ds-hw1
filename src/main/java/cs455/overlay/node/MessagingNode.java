package cs455.overlay.node;
import cs455.overlay.transport.*;
import cs455.overlay.wireformats.*;
import cs455.overlay.routing.RoutingTable;
import java.net.*; 
import java.io.*;
import java.util.*;

public class MessagingNode {

	public static void main(String[] args) {

		try {

			InetAddress inetAddress = InetAddress.getLocalHost(); // grabbing local address to pass in registration
			TCPConnectionsCache connections = new TCPConnectionsCache(); // Creating connections cache
			DataInputStream inputStream;		
			ServerSocket server; // server socket
			Thread serverThread;
			Integer serverPortNum = -1;
			String serverIP = inetAddress.getHostAddress().toString();
			String registryHostname = "";
			Integer registryPort = -1;

			try {
				registryHostname = String.valueOf(args[0]);
				registryPort = Integer.valueOf(args[1]);
			} catch (Exception e) {
				System.err.println(e);
				System.err.println("There was a problem with your command line arguments. args[0]: RegistryIP, args[1]: RegistryPort");
				return;
			}
			
			try {
				server = new ServerSocket(0, 128, inetAddress); // on any open port
				serverPortNum = server.getLocalPort(); // grabbing the port number of the server
				serverThread = new Thread(new TCPServerThread(server, connections));
				serverThread.start(); // starting server thread
			} catch (IOException e) {
				System.err.println(e);
				return;
			}

			Socket registrySocket;
			try {
				registrySocket = new Socket(registryHostname,registryPort); // setting up a socket connection to the registry
				inputStream = new DataInputStream(registrySocket.getInputStream()); // grabbing the input stream
			} catch (Exception e) {
				System.err.println("There was a problem setting up a socket connection to the registry.");
				server.close(); // 
				return;
			}

			// At this point, serverSocket running, connected to registry
			// Now to need to attempt to register self

			connections.setServerPort(serverPortNum); // setting the server port in the connections cache
			connections.setServerIP(serverIP); // setting the server ip in the connections cache
			OverlayNodeSendsRegistration register = new OverlayNodeSendsRegistration(serverIP, serverPortNum);
			TCPSender sender = new TCPSender(registrySocket);
			sender.sendData(register.getBytes()); // sending registration request to the registry

			Integer recvLength = inputStream.readInt(); // registry sends back registration status
			byte[] recv = new byte[recvLength];
			inputStream.readFully(recv, 0, recvLength);
			RegistryReportsRegistrationStatus status = new RegistryReportsRegistrationStatus(recv); // decoding response from the registry

			if (status.status == -1) { // if the registration request was unsuccessful, close the server socket, print the error, and return
				System.out.println(status.info); // print reason for not being able to register, return
				server.close();
				return;
			} else {
				System.out.println(status.info); // ;rint successful registration as well
			}

			// No longer need that temporary sender
			sender = null;

			TCPConnection tcpRegistry = new TCPConnection(registrySocket); // creating TCPConnection specifically for the registry
			tcpRegistry.setRegistry();
			connections.setOurID(status.status); // setting ID of this messaging node
			connections.setRegistry(tcpRegistry); // setting the registry socket of the connectionsCache
			TCPReceiverThread registryReceiver = new TCPReceiverThread(registrySocket, connections); // creating a receiver thread for the registry
			tcpRegistry.addReceiver(registryReceiver); // adding a reference to the registry receiver in the TCPConnection 

			Thread receiverThread = new Thread(registryReceiver);
			receiverThread.start(); // starting the receiver thread

			boolean loop = true;
			Scanner scanner = new Scanner(System.in);

			while (loop) { // take user input
				System.out.print("Command: ");
				String input = scanner.nextLine();
				input = input.toLowerCase();
				String[] split = input.split("\\s+");
				
				if (split.length > 2) { // no command has more than two arguments
					System.out.println("Invalid command.");
					continue;
				}

				String command = split[0];

				switch (command) {
					case "print-counters-and-diagnostics": { // rinting out counters and diagnostics
						System.out.print(connections.printCountersAndDiagnostics());
						break;
					}

					case "exit-overlay" : {
						int tempNodeID = connections.getOurID();
						OverlayNodeSendsDeregistration deregister = new OverlayNodeSendsDeregistration(serverIP, serverPortNum, tempNodeID); // sending a deregistration request

						if (connections.isRunning()) {
							System.out.println("Cannot exit the overlay until the overlay nodes finish sending messages.");
							break;
						} else {
							connections.sendToRegistry(deregister.getBytes());
						}

						loop = false;
						server.close(); // closing the server
						connections.removeAllConnections(); // removing all connections in the tcp connectino cache (but not the receiver for the registry)
						break;
					}

					default : {
						System.out.println("Invalid command issued, try again.");
						break;
					}
				}
			}

		} catch(Exception e) {
			System.out.println(e);
		}
		
	}
}
