package cs455.overlay.wireformats;
import cs455.overlay.routing.*;
import java.util.*;
import java.io.*;

public class RegistrySendsNodeManifest implements Event {

	public RoutingTable table;

	public RegistrySendsNodeManifest(RoutingTable rt) {
		this.table = rt;
	}

	public RegistrySendsNodeManifest(byte[] message) {
		this.table = new RoutingTable();

		int tableSize = message[1];
		//System.out.println("Table Size: " + tableSize);
		int entriesDecoded = 0;

		int placement = 2; 
		while (entriesDecoded < tableSize) {
			int hop = (int)Math.pow(2, entriesDecoded);
			//System.out.println("Hop: " + hop);

			byte first = message[placement];
			byte second = message[placement+1];
			byte third = message[placement+2];
			byte fourth = message[placement+3];

			int nodeID = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);
			//System.out.println("NodeID: " + nodeID);

			int ipLength = message[placement+4];
			byte[] byteIP = new byte[ipLength];
		
			for (int i = 0; i < ipLength; i++) { // Reading alleged IP of sender into its own byte array
				byteIP[i] = message[placement+5+i];
			}
			
			String ip = new String(byteIP); // Converting byte array to string
			//System.out.println(ip);
			//System.out.println(ipLength);

			first = message[placement+5+ipLength];
			second = message[placement+5+ipLength+1];
			third = message[placement+5+ipLength+2];
			fourth = message[placement+5+ipLength+3];

			int port = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);
			//System.out.println("Port: " + port);

			RoutingEntry entry = new RoutingEntry(ip, port, nodeID, hop);

			this.table.addEntry(entry);

			placement += 5 + ipLength + 4;
			entriesDecoded += 1;
		}

		//System.out.println("Starting to get nodeList entries.");
		//System.out.println("Decoded " + entriesDecoded + " entries");

		int idCount = message[placement];
		//int idCount = 2;
		//System.out.println("Placement to start reading nodeList: " + placement);
		//System.out.println("Number of id's in nodeList: " + idCount);
		placement += 1;
		for (int i = 0; i < idCount; i++) {
			byte first = message[placement];
			byte second = message[placement+1];
			byte third = message[placement+2];
			byte fourth = message[placement+3];

			int nodeID = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);
			//System.out.println("Entry " + i + " in the node list: " + nodeID);

			this.table.addNode(nodeID);

			placement += 4;
		}

	}

	public byte[] getBytes() throws IOException {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeByte(Protocol.REGISTRY_SENDS_NODE_MANIFEST);
		Integer tableSize = new Integer(this.table.getTableSize());
		byte tableLength = tableSize.byteValue();
		dout.writeByte(tableLength);

		Vector<RoutingEntry> tempEntries = this.table.getEntries();
		Vector<Integer> tempNodes = this.table.getNodes();


		for (int i = 0; i < tableSize; i++) { // finding the correct entries in the routing table to send, starting with 1, 2, 4, ...
			int hopCompare = (int)Math.pow(2,i);
			for (RoutingEntry entry : tempEntries) {
				if (entry.getHopsAway() == hopCompare) {
					int nodeID = entry.getNodeID();
					dout.writeInt(nodeID);

					byte[] byteIP = entry.getIP().getBytes();
					Integer length = new Integer(byteIP.length);
					byte byteLength = length.byteValue(); 
					dout.writeByte(byteLength);
					dout.write(byteIP);

					int port = entry.getPort();
					dout.writeInt(port);
					break;
				}
			}
		}

		byte nodeCount = new Integer(this.table.getNodeCount()).byteValue();
		dout.writeByte(nodeCount);
		for (Integer node : tempNodes) {
			dout.writeInt(node);
		}

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public byte getType() throws IOException {
		return Protocol.REGISTRY_SENDS_NODE_MANIFEST;
	}
}