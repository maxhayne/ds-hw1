package cs455.overlay.wireformats;
import java.util.*;
import java.io.*;

public class RegistryRequestsTaskInitiate implements Event {
	public int dataPackets;

	public RegistryRequestsTaskInitiate(int dp) {
		this.dataPackets = dp;
	}

	public RegistryRequestsTaskInitiate(byte[] message) {
		byte first = message[1];
		byte second = message[2];
		byte third = message[3];
		byte fourth = message[4];

		int getDataPackets = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);
		this.dataPackets = getDataPackets;
	}

	public byte[] getBytes() throws IOException { // getBytes method for creating a byte array to send
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeByte(Protocol.REGISTRY_REQUESTS_TASK_INITIATE);
		dout.writeInt(this.dataPackets);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public byte getType() throws IOException {
		return Protocol.REGISTRY_REQUESTS_TASK_INITIATE;
	}

}