package cs455.overlay.util;
import cs455.overlay.transport.*;

public class waitForSummaries extends Thread {
	private TCPConnectionsCache connections;
	
	public waitForSummaries(TCPConnectionsCache cache) {
		this.connections = cache;
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(20000); // sleeping between checks to see if every node successfully connected to every node in their routing table
		} catch(InterruptedException e) {
			 // Nothing to catch, really
		}

		connections.askTrafficSummary(); // asking for traffic summary

		while (!connections.hasEveryoneSummarized()) { // waiting until all TCPConnections have set their 'summarized' flag, indicating successful reception of summary data for that node
			//System.out.println("Waiting for summary data.");
			try {
				Thread.sleep(500); // sleeping between checks to see if every node successfully connected to every node in their routing table
			} catch(InterruptedException e) {
				 continue;
			}
		}

		//System.out.println("All overlay nodes have sent their summary data.");

		connections.unrun(); // set the running variable to false
		
		System.out.println();
		connections.finalSummary(); // printing out the final summary for all nodes
		System.out.println("continue your command-in-progress:");
	}
}
