/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    protected static final int SEND_MESSAGE_RESULT_CODE = 21; //DL
    
    /*
     * DL Group Owner always listens on port one, client always listens on port two
     */
    protected static final int PORT_NUMBER_ONE = 8988;//DL
    protected static final int PORT_NUMBER_TWO = 8989;//DL
    protected static final int PORT_NUMBER_ONE_FILE = 8990; 
    protected static final int PORT_NUMBER_TWO_FILE = 8991;
    
    
    protected HandShakeAsyncTask handShaker; //DL
    protected ArrayList<String> ipList = new ArrayList<String>();
    
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {           
                        ((DeviceActionListener) getActivity()).disconnect();

                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        //intent.setType("image/*");
                        intent.setType("audio/mpeg3"); //send mp3 file TODO: make server accept mp3 files and not image
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });
        mContentView.findViewById(R.id.btn_send_message).setOnClickListener(
        		new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Log.d("DL", "test send message");
						
						/* Probably don't need this just yet since we're not starting another activity. We just want to send a generic message
						Intent intent = new Intent();
						startActivityForResult(intent, SEND_MESSAGE_RESULT_CODE);
						*/
						
						//if group owner, send message to everyone in the ipList
						if(info.isGroupOwner){
							for(int i=0; i<ipList.size(); i++){
								Log.d("DL", "Sending message from GO to " + ipList.get(i));
								sendTextMessage("Message sent from group owner", PORT_NUMBER_TWO, ipList.get(i));
							}
						}else{
							sendTextMessage("Message sent from client", PORT_NUMBER_ONE);
						}
					}
					
        		});

        return mContentView;
    }

    /*
     * DL - This will perform the same actions as onActivityResult except that this is called
     * directly, and not on the return/result of some other action
     */
    public void sendTextMessage(String message, int portNumber){
    	Log.d("DL", "Entering sendTextMessage for client");
    	Intent serviceIntent = new Intent(getActivity(), MessageTransferService.class);
    	serviceIntent.setAction(MessageTransferService.ACTION_SEND_MESSAGE);
    	serviceIntent.putExtra(MessageTransferService.EXTRAS_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());
    	serviceIntent.putExtra(MessageTransferService.EXTRAS_GROUP_OWNER_PORT, portNumber);
    	serviceIntent.putExtra(MessageTransferService.EXTRAS_MESSAGE, message);
    	getActivity().startService(serviceIntent);
    }
    
    public void sendTextMessage(String message, int portNumber, String host){
    	Log.d("DL", "Entering sendTextMessage for host");
    	Intent serviceIntent = new Intent(getActivity(), MessageTransferService.class);
    	serviceIntent.setAction(MessageTransferService.ACTION_SEND_MESSAGE);
    	serviceIntent.putExtra(MessageTransferService.EXTRAS_GROUP_OWNER_ADDRESS, host);
    	serviceIntent.putExtra(MessageTransferService.EXTRAS_GROUP_OWNER_PORT, portNumber);
    	serviceIntent.putExtra(MessageTransferService.EXTRAS_MESSAGE, message);
    	getActivity().startService(serviceIntent);
    }
    /**
     * wrapper to init handshake
     */
    public void clientHandShake(int portNumber){
    	sendTextMessage("", portNumber);
    }
    

    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        
        //if group owner, send to everyone in ipList
        if(info.isGroupOwner){
	        for(int i=0; i<ipList.size(); i++){
	        	String host = ipList.get(i);
		        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
		        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
		        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
		        serviceIntent.putExtra(FileTransferService.EXTRAS_DESTINATION_ADDRESS,
		                host);
		        serviceIntent.putExtra(FileTransferService.EXTRAS_DESTINATION_PORT, PORT_NUMBER_TWO_FILE);
		        getActivity().startService(serviceIntent);
	        }
        }else{
        	//send to group owner
	        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
	        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
	        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
	        serviceIntent.putExtra(FileTransferService.EXTRAS_DESTINATION_ADDRESS,
	                info.groupOwnerAddress.getHostAddress());
	        serviceIntent.putExtra(FileTransferService.EXTRAS_DESTINATION_PORT, PORT_NUMBER_ONE_FILE);
	        getActivity().startService(serviceIntent);
        	
        }
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            //new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
            //        .execute();
        	
        	//init handshaking. Server instantiated in postExecute of handshake
        	handShaker = new HandShakeAsyncTask(PORT_NUMBER_ONE, info.groupOwnerAddress.getHostAddress());
        	handShaker.execute();
            //new ServerAsyncTask(getActivity(), PORT_NUMBER_ONE, info.groupOwnerAddress.getHostAddress()).execute();
            mContentView.findViewById(R.id.btn_send_message).setVisibility(View.VISIBLE);
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
                                          
             
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
           
            //DL make the send_message button show
            mContentView.findViewById(R.id.btn_send_message).setVisibility(View.VISIBLE);
            
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
            
            clientHandShake(PORT_NUMBER_ONE);
            new ServerAsyncTask(getActivity(), PORT_NUMBER_TWO, info.groupOwnerAddress.getHostAddress()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text), PORT_NUMBER_TWO_FILE)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        }
        
        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * DL A simple server socket that accepts incoming messages and makes a toast with them
     */
    public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {
    	private Context context; //not sure what this is for, but it's the activity that gets passed it
    	int portNumber; //port number to open connection on
    	String go_address; //group owner address for reference
    	public ServerAsyncTask(Context c, int portNumber, String go_address){
    		context = c;
    		this.portNumber = portNumber;
    		this.go_address=go_address;
    	}
    	
    	protected String doInBackground(Void... params){
    		try{
    			//infinite loop to keep socket open for recieving messages
    			
    			while(true){
    				Log.d("DL", "About to open socket on port " + portNumber);
	    			ServerSocket serverSocket = new ServerSocket(portNumber);
	    			Log.d("DL", "My Server Socket Opened");
	    			Socket clientSocket = serverSocket.accept();
	    			Log.d("DL", "connection established");	
	    			
	    			BufferedReader inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

				    String line;
			        line = inputStream.readLine();
			        
			        if(line==null)
			        	Log.d("DL", "message not received");
			        else
			        	Log.d("DL", "message received: " + line);
			        
			        inputStream.close();
			        clientSocket.close();
			        serverSocket.close();
			        
			        Log.d("DL", "Sockets closed");
    			}
		        //return "success";
    		}
    		catch(IOException e){
    			Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
    		}
    	}
    }
    
    
    /**
     * Handshaking protocol: This async method will be called whenever a connection 
     * is established. The purpose is to allow the group owner to get the ip address
     * of the client
     */
    public class HandShakeAsyncTask extends AsyncTask<Void, Void, String> {
    	int portNumber;
    	String address;
    	
    	public HandShakeAsyncTask(int portNumber, String go_address){
    		this.portNumber = portNumber;
    		this.address=go_address;
    	}
    	
    	protected String doInBackground(Void... params){
    		try{
				Log.d("DL", "About to open socket for handshake on port " + portNumber);
				
				ServerSocket serverSocket = new ServerSocket(portNumber);
				Log.d("DL", "My Server Socket Opened for handshake");
				
				Socket clientSocket = serverSocket.accept();
				Log.d("DL", "connection established for handshake");	
				
				String clientIP = clientSocket.getInetAddress().getHostAddress();
				Log.d("DL", "ClientIP: " + clientIP);
				
				ipList.add(clientIP);

		        clientSocket.close();
		        serverSocket.close();
		        
		        Log.d("DL", "Sockets closed after handshake");
		        return "success";
    		}catch(IOException e){
    			Log.d("DL", "Handshaking failed due to IO Exception");
    			e.printStackTrace();
    			return null;
    		} 
    	}
    	
    	//start the server up
    	protected void onPostExecute(String results){
    		if(results != null){
    			new ServerAsyncTask(getActivity(), PORT_NUMBER_ONE, info.groupOwnerAddress.getHostAddress()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    			new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text), PORT_NUMBER_ONE_FILE)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    		}
    	}
    }
    
    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;
        int portNumber;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText, int port) {
            this.context = context;
            this.statusText = (TextView) statusText;
            portNumber = port;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Log.d("DL", "About to open file socket for reading: " + portNumber);
                ServerSocket serverSocket = new ServerSocket(portNumber);
                Log.d("DL", "Server: Socket for file reading opened");
                Socket client = serverSocket.accept();
                Log.d("DL", "Server: connection done for file reading");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + "sdcard0/Music" + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".mp3");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d("DL", "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                
                Log.d("DL", "filepath: " + f.getAbsolutePath());
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e("DL", e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "audio/mpeg3");
                context.startActivity(intent);
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket for file reading");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

}
