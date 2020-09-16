package cs455.overlay.wireformats;
import java.util.*;
import java.io.*;

public class OverlayNodeReportsTaskFinished {
	public String ip;
	public int port;
	public int nodeID;

	public OverlayNodeReportsTaskFinished(String ip, int port, int id) {
		this.ip = ip;
		this.port = port;
		this.nodeID = id;
	}

	public OverlayNodeReportsTaskFinished(byte[] message) {
		int length = message[1];
		byte[] byteIP = new byte[length];
		
		for (int i = 0; i < length; i++) { // Reading alleged IP of sender into its own byte array
			byteIP[i] = message[2+i];
		}
		this.ip = new String(byteIP); // Converting byte array to string

		byte first = message[2+length];
		byte second = message[2+length+1];
		byte third = message[2+length+2];
		byte fourth = message[2+length+3];

		int thisPort = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);
		this.port = thisPort;

		first = message[2+length+4];
		second = message[2+length+5];
		third = message[2+length+6];
		fourth = message[2+length+7];

		int thisID = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);
		this.nodeID = thisID;
	}

	public byte[] getBytes() throws IOException { // getBytes method for creating a byte array to send
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeByte(Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED);
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
		return Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED;
	}
}