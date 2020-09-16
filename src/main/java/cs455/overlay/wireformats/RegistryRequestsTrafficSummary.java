package cs455.overlay.wireformats;
import java.util.*;
import java.io.*;

public class RegistryRequestsTrafficSummary {

	public RegistryRequestsTrafficSummary(){

	}

	public byte[] getBytes() throws IOException { // getBytes method for creating a byte array to send
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeByte(Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public byte getType() throws IOException {
		return Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY;
	}
}