package cs455.overlay.wireformats;
import java.util.*;
import java.io.*;

public class RegistryReportsRegistrationStatus implements Event {
	public int status;
	public String info;

	public RegistryReportsRegistrationStatus(byte[] message) { // constructor for decoding byte message
		byte first = message[1];
		byte second = message[2];
		byte third = message[3];
		byte fourth = message[4];

		this.status = (first & 0xff) << 24 | (second & 0xff) << 16 | (third & 0xff) << 8 | (fourth & 0xff);

		int length = message[5];
		byte[] byteInfo = new byte[length];
		
		for (int i = 0; i < length; i++) { // Reading alleged IP of sender into its own byte array
			byteInfo[i] = message[i+6];
		}
		this.info = new String(byteInfo); // Converting ascii byte array to string
	}

	public RegistryReportsRegistrationStatus(int status, String info) {
		this.status = status;
		this.info = info;
	}

	public byte[] getBytes() throws IOException { // getBytes method for creating a byte array to send
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeByte(Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS);
		dout.writeInt(status);

		byte[] infoToByte = this.info.getBytes("US-ASCII"); // Encoding using ascii
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
		return Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS;
	}
}