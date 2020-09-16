package cs455.overlay.transport;
import cs455.overlay.transport.TCPConnection;
import java.io.*;
import java.net.*; 

public class TCPSender {
	
	private Socket socket;
	private DataOutputStream dout;
	private TCPConnection connection;
	private byte[] data;

	public TCPSender(Socket socket) throws IOException {
		this.socket = socket;
		dout = new DataOutputStream(socket.getOutputStream());
	}

	public TCPSender(TCPConnection conn, byte[] data) throws IOException {
		this.connection = conn;
		this.data = data;
	}

	public void sendData(byte[] dataToSend) throws IOException {
		int dataLength = dataToSend.length;
		dout.writeInt(dataLength);
		dout.write(dataToSend, 0, dataLength);
		dout.flush();
	}

}
