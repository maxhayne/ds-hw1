package cs455.overlay.transport;
import cs455.overlay.transport.*;
import cs455.overlay.wireformats.*;
import java.net.*; 
import java.io.*;
import java.lang.Thread;
import java.util.*;

public class TCPServerThread extends Thread {
	
	private ServerSocket server = null;;
	private TCPConnectionsCache cache = null;

	public TCPServerThread(ServerSocket socket, TCPConnectionsCache passed_cache) {
		this.server = socket;
		this.cache = passed_cache;
	}

	@Override
	public void run() {
		try {
			while(true) {
				Socket incomingConnectionSocket = server.accept(); // accept new connections
				incomingConnectionSocket.setReceiveBufferSize(65536); // large receive buffer size
				incomingConnectionSocket.setKeepAlive(true);
				Thread receiverThread = new Thread(new TCPReceiverThread(incomingConnectionSocket, cache)); // create new TCPReceiver thread 
				receiverThread.start();
			}

		} catch (IOException e) {
			System.err.println("ServerSocket has closed.");
		} finally {
			try {
				server.close();
			} catch (IOException ioe) {
				System.err.println(ioe.getMessage());
			}
		}
	}
}