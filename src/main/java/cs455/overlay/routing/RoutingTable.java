package cs455.overlay.routing;
import cs455.overlay.transport.*;
import cs455.overlay.wireformats.*;
import cs455.overlay.routing.RoutingEntry;
import java.net.*; 
import java.io.*;
import java.util.*;

public class RoutingTable {
	private Vector<RoutingEntry> table;
	private Vector<Integer> nodeList;
	private Integer ourID;

	public RoutingTable() {
		this.table = new Vector<RoutingEntry>();
		this.nodeList = new Vector<Integer>();
	}

	public int randomSink() {
		Random random = new Random();
		return nodeList.get(random.nextInt(nodeList.size())); // return random node from list
	}

	public synchronized Vector<RoutingEntry> getEntries() {
		Vector<RoutingEntry> tempTable = new Vector<RoutingEntry>();
		for (RoutingEntry entry : this.table) {
			RoutingEntry tempEntry = new RoutingEntry(entry.getIP(), entry.getPort(), entry.getNodeID(), entry.getHopsAway());
			tempTable.add(tempEntry);
		}
		return tempTable; // returning a copy of the routing table
	}

	public synchronized Vector<Integer> getNodes() {
		Vector<Integer> tempList = new Vector<Integer>();
		for (Integer node : this.nodeList) {
			int i = node;
			tempList.add(i);
		}
		return tempList; // returning a copy of the node list
	}

	public synchronized void reset() {
		this.table.clear();
		this.nodeList.clear();
		// Need to change this.ourID?
	}

	public synchronized String printTable() {
		String s = "";
		for (RoutingEntry entry : table) {
			s += entry.getIP() + "\t" + entry.getPort() + "\t" + entry.getNodeID() + "\t" + entry.getHopsAway() + "\n";
		}
		return s;
	}

	public synchronized String printNodeList() {
		String s = "";
		for (Integer entry : nodeList) {
			s += entry + ", ";
		}
		if (s.length() != 0) {
			s = s.substring(0, s.length()-2);
		}
		return s;
	}

	public synchronized void setID(int id) {
		this.ourID = id;
	}

	public synchronized void setList(Vector<Integer> list) {
		this.nodeList = list;
	}

	public synchronized void addEntry(RoutingEntry entry) {
		this.table.add(entry);
	}

	public synchronized void removeEntry(RoutingEntry entry) {
		this.table.removeElement(entry);
	}

	public synchronized void addNode(int id) {
		this.nodeList.add(id);
	}

	public synchronized void removeNode(int id) {
		this.nodeList.removeElement(id);
	}

	public synchronized Integer route(int sinkID) {
		int sinkDistance; // this will be the distance between ourID and sinkID
		if (sinkID > ourID) {
			sinkDistance = sinkID - ourID;
		} else {
			sinkDistance = 128 - (ourID - sinkID);
		}

		int furthestDistance = 0; // want to find hop that is furthest from ourID
		int bestID = ourID; // setting this equal to ourID to avoid errors
		for (RoutingEntry entry : table) { // for every entry in table, find hop that is furthest from ourID without overstepping the sink
			int tempDistance;
			int entryID = entry.getNodeID();

			if (sinkID == entryID) { // if the destination is in our routing table, return the sinkID
				return sinkID;
			}

			if (entryID > ourID) {
				tempDistance = entryID - ourID;
			} else {
				tempDistance = 128 - (ourID - entryID);
			}

			if (tempDistance > furthestDistance && tempDistance <= sinkDistance) {
				furthestDistance = tempDistance;
				bestID = entryID;
			}
		}

		return bestID;
	}

	public synchronized Integer getTableSize() {
		return table.size();
	}

	public synchronized Integer getNodeCount() {
		return nodeList.size();
	}
}