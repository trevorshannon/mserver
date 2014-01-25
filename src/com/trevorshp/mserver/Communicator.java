package com.trevorshp.mserver;

import android.util.Log;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketHandler;

public class Communicator {
	
	private final String TAG = "mserver.Communicator";
	private final WebSocketConnection mConnection = new WebSocketConnection();
	private String wsuri;
	
	private WebSocketHandler handler;
	
	public Communicator(String wsuri, WebSocketHandler handler){
		this.wsuri = wsuri;
		this.handler = handler;
	}
	
	public void start() {
	      try {
	         mConnection.connect(wsuri, handler);
	      }
	      catch(Exception e){
	    	  Log.e(TAG, e.getMessage());
	      }
	} 

	public void send(String type, String content){
		//TODO: use jackson to pack this json
		if (mConnection.isConnected()){
			if (content.substring(0, 1).equals("{") || content.substring(0, 1).equals("[")){
				mConnection.sendTextMessage("{\"type\":\"" + type + "\", \"content\":" + content + "}");
			}
			else{
				mConnection.sendTextMessage("{\"type\":\"" + type + "\", \"content\":\"" + content + "\"}");
			}
		}
	}
	
	public void end(){
		mConnection.disconnect();
	}
}
