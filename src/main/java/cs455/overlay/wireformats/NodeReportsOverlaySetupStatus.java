package cs455.overlay.wireformats;
import java.util.*;
import java.io.*;

public class NodeReportsOverlaySetupStatus implements Event {

	public int status;
	public String info;

	public NodeReportsOverlaySetupStatus(int status, String info) {
		this.status = status;
		this.info = info;
	}

	public NodeReportsOverlaySetupStatus(byte[] message) { // constructor for decoding byte message
		byte first = message[1];
		byte second = message[2];
		byte third = message[3];
		byte fourth = message[4];

		int getStatus = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);
		this.status = getStatus;

		int length = message[5];
		byte[] byteInfo = new byte[length];
		
		for (int i = 0; i < length; i++) { // Reading alleged IP of sender into its own byte array
			byteInfo[i] = message[6+i];
		}
		this.info = new String(byteInfo); // Converting byte array to string
	}

	public byte[] getBytes() throws IOException { // getBytes method for creating a byte array to send
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeByte(Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS);
		dout.writeInt(this.status);
		byte[] infoToByte = this.info.getBytes();
		Integer length = new Integer(infoToByte.length);
		byte byteLength = length.byteValue();
		dout.writeByte(byteLength);
		dout.write(infoToByte);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public byte getType() throws IOException {
		return Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS;
	}
}