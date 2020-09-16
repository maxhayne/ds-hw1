package cs455.overlay.wireformats;
import cs455.overlay.wireformats.Protocol;
import java.io.*;

public interface Event {
	byte getType() throws IOException;
	byte[] getBytes() throws IOException;
}