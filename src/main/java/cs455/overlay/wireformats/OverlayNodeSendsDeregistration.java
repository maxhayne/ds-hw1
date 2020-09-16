package cs455.overlay.wireformats;
import java.util.*;
import java.io.*;

public class OverlayNodeSendsDeregistration implements Event { 
	public int port;
	public String ip;
	public int nodeID;

	public OverlayNodeSendsDeregistration(String ip, int port, int nodeID) { // constructor for instantiating variables
		this.port = port;
		this.ip = ip;
		this.nodeID = nodeID;
	}

	public OverlayNodeSendsDeregistration(byte[] message) {
		int length = message[1]; // length of IP address
		byte[] byteIP = new byte[length];
		
		for (int i = 0; i < length; i++) { // Reading alleged IP of sender into its own byte array
			byteIP[i] = message[2+i];
		}
		this.ip = new String(byteIP); // Converting byte array to string

		byte first = message[2+length]; // grabbing bytes that make up the port number
		byte second = message[2+length+1];
		byte third = message[2+length+2];
		byte fourth = message[2+length+3];

		this.port = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		first = message[2+length+4]; // grabbing bytes that make up the nodeID number
		second = message[2+length+5];
		third = message[2+length+6];
		fourth = message[2+length+7];

		this.nodeID = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);
	}

	public byte[] getBytes() throws IOException { // getBytes method for creating a byte array to send
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeByte(Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION);
		byte[] ipToByte = this.ip.getBytes();
		Integer length = new Integer(ipToByte.length);
		byte byteLength = length.byteValue();
		dout.writeByte(byteLength);
		dout.write(ipToByte);
		dout.writeInt(this.port);
		dout.writeInt(this.nodeID);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public byte getType() throws IOException {
		return Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION;
	}
}