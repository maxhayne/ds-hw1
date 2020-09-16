package cs455.overlay.wireformats;
import java.nio.ByteBuffer;
import java.util.*;
import java.io.*;

public class OverlayNodeReportsTrafficSummary {

	public int nodeID;
	public int packetsSent;
	public int packetsRelayed;
	public int packetsReceived;
	public long sumSent;
	public long sumReceived;

	public OverlayNodeReportsTrafficSummary(int nodeID, int packetsSent, int packetsRelayed, int packetsReceived, long sumSent, long sumReceived) {
		this.nodeID = nodeID;
		this.packetsSent = packetsSent;
		this.packetsRelayed = packetsRelayed;
		this.packetsReceived = packetsReceived;
		this.sumSent = sumSent;
		this.sumReceived = sumReceived;
	}

	public OverlayNodeReportsTrafficSummary(byte[] message) {

		// Ugly, but made me more confident nothing fishy was going on...
		// Using bit shifting for the integers, for some reason, the same technique wasn't
		// working for longs, so opted to use byteBuffers instead

		byte first = message[1];
		byte second = message[2];
		byte third = message[3];
		byte fourth = message[4];

		this.nodeID = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		first = message[5];
		second = message[6];
		third = message[7];
		fourth = message[8];

		this.packetsSent = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		first = message[9];
		second = message[10];
		third = message[11];
		fourth = message[12];

		this.packetsRelayed = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		first = message[13];
		second = message[14];
		third = message[15];
		fourth = message[16];
		byte fifth = message[17];
		byte sixth = message[18];
		byte seventh = message[19];
		byte eighth = message[20];

		byte[] firstByteLong = new byte[8];
		for (int i = 0; i < 8; i++) {
			firstByteLong[i] = message[13+i];
		}
		ByteBuffer byteBuffer1 = ByteBuffer.allocate(Long.BYTES);
		byteBuffer1.put(firstByteLong);
		byteBuffer1.flip();
		this.sumSent = byteBuffer1.getLong();

		first = message[21];
		second = message[22];
		third = message[23];
		fourth = message[24];

		this.packetsReceived = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		first = message[25];
		second = message[26];
		third = message[27];
		fourth = message[28];
		fifth = message[29];
		sixth = message[30];
		seventh = message[31];
		eighth = message[32];

		byte[] secondByteLong = new byte[8];
		for (int i = 0; i < 8; i++) {
			secondByteLong[i] = message[25+i];
		}
		ByteBuffer byteBuffer2 = ByteBuffer.allocate(Long.BYTES);
		byteBuffer2.put(secondByteLong);
		byteBuffer2.flip();
		this.sumReceived = byteBuffer2.getLong();
	}

	public byte[] getBytes() throws IOException { // getBytes method for creating a byte array to send
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeByte(Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY);
		dout.writeInt(this.nodeID);
		dout.writeInt(this.packetsSent);
		dout.writeInt(this.packetsRelayed);
		dout.writeLong(this.sumSent);
		dout.writeInt(this.packetsReceived);
		dout.writeLong(this.sumReceived);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public byte getType() throws IOException {
		return Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY;
	}
}