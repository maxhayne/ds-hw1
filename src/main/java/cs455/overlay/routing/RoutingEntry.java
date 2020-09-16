package cs455.overlay.routing;
import cs455.overlay.transport.*;
import cs455.overlay.wireformats.*;
import java.net.*; 
import java.io.*;
import java.util.*;

public class RoutingEntry {
	private int nodeID;
	private int port;
	private String ip;
	private int hopsAway;

	public RoutingEntry(String ip, int port, int nodeID, int hops) {
		this.ip = ip;
		this.nodeID = nodeID;
		this.port = port;
		this.hopsAway = hops;
	}

	public String getIP() {
		return this.ip;
	}

	public Integer getNodeID() {
		return this.nodeID;
	}

	public Integer getPort() {
		return this.port;
	}

	public Integer getHopsAway() {
		return this.hopsAway;
	}
}