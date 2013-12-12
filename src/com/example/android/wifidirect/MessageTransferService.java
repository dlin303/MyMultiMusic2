package com.example.android.wifidirect;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
			try {
				
				Log.d("DL", "About to try to connect");
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
				Log.d("DL", "Client socket - " + socket.isConnected());
				
				//try to write to output
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				out.print("chubbers\r\n");
				out.flush();
				
				socket.close();
				Log.d("DL", "Socket closed");
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
	

}
