package cs455.overlay.wireformats;
import java.util.*;
import java.io.*;

public class OverlayNodeSendsData {

	public int source;
	public int sink;
	public int payload;
	public int hops; // number entries in the hopTrace
	public Vector<Integer> hopTrace;

	public OverlayNodeSendsData(int source, int sink, int payload, Vector<Integer> hopTrace) {
		this.source = source;
		this.sink = sink;
		this.payload = payload;
		this.hops = hopTrace.size();
		this.hopTrace = hopTrace;
	}

	public OverlayNodeSendsData() { // empty constructor

	}

	public OverlayNodeSendsData(int source, int sink, int payload) { // empty constructor
		this.source = source;
		this.sink = sink;
		this.payload = payload;
		this.hops = 0;
	}

	public void newInput(int source, int sink, int payload, Vector<Integer> hopTrace) {
		this.source = source;
		this.sink = sink;
		this.payload = payload;
		this.hops = hopTrace.size();
		this.hopTrace = hopTrace;
	}

	public OverlayNodeSendsData(byte[] message) {
		hopTrace = new Vector<Integer>();

		byte first = message[1];
		byte second = message[2];
		byte third = message[3];
		byte fourth = message[4];

		this.sink = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		first = message[5];
		second = message[6];
		third = message[7];
		fourth = message[8];

		this.source = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		first = message[9];
		second = message[10];
		third = message[11];
		fourth = message[12];

		this.payload = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		first = message[13];
		second = message[14];
		third = message[15];
		fourth = message[16];

		this.hops = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		for (int i = 0; i < hops; i++) {
			first = message[17+(i*4)];
			second = message[18+(i*4)];
			third = message[19+(i*4)];
			fourth = message[20+(i*4)];

			int tempHop  = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

			this.hopTrace.add(tempHop);
		}
	}

	public byte[] getBytes() throws IOException { // getBytes method for creating a byte array to send
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeByte(Protocol.OVERLAY_NODE_SENDS_DATA);
		dout.writeInt(this.sink);
		dout.writeInt(this.source);
		dout.writeInt(this.payload);
		dout.writeInt(this.hops);

		for (int i = 0; i < hops; i++) {
			dout.writeInt(hopTrace.get(i));
		}

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}


	public byte getType() throws IOException {
		return Protocol.OVERLAY_NODE_SENDS_DATA;
	}
}