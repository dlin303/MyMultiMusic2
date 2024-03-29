package com.example.android.wifidirect;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
/**
 *DL  A service that process each message transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the message
 */
public class MessageTransferService extends IntentService{
	private static final int SOCKET_TIMEOUT = 5000;
	public static final String ACTION_SEND_MESSAGE = "com.example.android.wifidirect.SEND_MESSAGE";
	public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
	public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
	public static final String EXTRAS_MESSAGE = "message";
	
	public MessageTransferService (String name){
		super(name);
	}
	public MessageTransferService(){
		super("MessageTransferService");
	}
	
	@Override
	protected void onHandleIntent(Intent intent){
		Context context = getApplicationContext();
		Log.d("DL", "Entering handleIntent for MTS");
		
		if(intent.getAction().equals(ACTION_SEND_MESSAGE)){
			String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
			Socket socket = new Socket();
			int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
			String message = intent.getExtras().getString(EXTRAS_MESSAGE);
			try {
				
				Log.d("DL", "About to try to connect");
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
				Log.d("DL", "Client socket - " + socket.isConnected());
				
				//try to write to output
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				//test ping
				if(message.equals("sync")){
					long pingStart = System.currentTimeMillis();
					out.print(message + "\r\n");
					out.flush();
					Log.d("DL", "Ping sent. Awaiting response");
					String response = inputStream.readLine();
					long pingEnd = System.currentTimeMillis();
					long pingDelay = pingEnd-pingStart;
					
					if(response==null){
						Log.d("DL", "No response");
					}else{
						Log.d("DL", "Response received: " + response);
						long songTime = Long.parseLong(response);
						long seekTo = songTime + pingDelay/2;
						ResultReceiver rec = intent.getParcelableExtra("receiverTag");				
						Bundle b = new Bundle();
						b.putString("MessageTag", message);
						b.putLong("SeekTo", seekTo);
						//b.putLong("PlayTime", playTime);
						rec.send(0, b);
					}
				} 
				else {
					long playTime = System.currentTimeMillis()+800;
					
					out.print(message + " playTime:"+ playTime + "\r\n");
					//long startTime = System.currentTimeMillis();
					//notify activity that MTS has finished	
					ResultReceiver rec = intent.getParcelableExtra("receiverTag");				
					Bundle b = new Bundle();
					b.putString("MessageTag", message);
					//b.putLong("StartTime", startTime);
					//b.putLong("PlayTime", playTime);
					rec.send(0, b);
					out.flush();
						
					socket.close();									
					Log.d("DL", "Socket closed " + port);
	

				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
	

}
