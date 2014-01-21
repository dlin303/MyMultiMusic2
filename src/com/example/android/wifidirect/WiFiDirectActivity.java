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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;
import com.example.android.wifidirect.MyResultReceiver.Receiver;


//class to filter by mp3 file
class Mp3Filter implements FilenameFilter{
	public boolean accept(File dir, String name){
		return (name.endsWith(".mp3"));
	}
	
}


/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends Activity implements ChannelListener, DeviceActionListener, Receiver {

    public static final String TAG = "wifidirectdemo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    
    /**
     * Music player related attributes
     */
	private static final String SD_PATH = new String("/storage/sdcard0/Music/");
	private List<String> songs = new ArrayList<String>();
	private MediaPlayer mp = new MediaPlayer();
	private ListView lv;
	
	/*
	 * My Receiver
	 */
	public MyResultReceiver myReceiver;

	
	
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //create result receiver to control music player
        myReceiver = new MyResultReceiver(new Handler());
        myReceiver.setReceiver(this);

        // add necessary intent values to be matched.

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        
        //Music player related actions
        updatePlayList();
        
        Button stopPlay = (Button) findViewById(R.id.stopBtn);
       
		stopPlay.setOnClickListener(new OnClickListener(){
			
			@Override
			public void onClick(View v){
        		//get the device detail fragment and call it's send message function
        		DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
        	                .findFragmentById(R.id.frag_detail);
        		
        		fragmentDetails.sendMusicInstruction("stop");
				mp.stop();
			}
		});        
    }
    
    public MyResultReceiver getReceiver(){
    	return myReceiver;
    }
    
/**
 * In order to cut down latency between two devices, we only execute the media player 
 * commands when the MessageTransferService has finished sending.
 */
    public void onReceiveResult(int resultCode, Bundle resultData) {
        // TODO Auto-generated method stub
         		long startTime = resultData.getLong("StartTime");   
         		
         		long playTime = resultData.getLong("PlayTime");
                String message = resultData.getString("MessageTag");
                
		        if(message.contains("playSelected")){
		        	String firstNumber = message.replaceFirst(".*?(\\d+).*", "$1");
		        	int position = Integer.parseInt(firstNumber);
		        	
		        	long endTime = System.currentTimeMillis();
	         		long delay = endTime - startTime;
	                Log.d("DL","MTS finished! Message:"+resultData.getString("MessageTag")+ " Delay=" + delay + " playTime: " + playTime);
		        	
	                //wait until system time is playtime
	                while(System.currentTimeMillis() < playTime)
	                	;
	                
	                playSelected(position);
		        	
		        }else if (message.contains("stop")){
		        	mediaStop();
		        }
    }
    
    //used to populate song list
	private void updatePlayList(){
		File home = new File(SD_PATH);
		lv = (ListView) findViewById(R.id.mylist);
		
		//if there are 1 or more mp3 files
		if(home.listFiles(new Mp3Filter()).length>0){
			for(File file : home.listFiles(new Mp3Filter())){
				Log.d("DL", "Found song");
				songs.add(file.getName());
			}
			
			ArrayAdapter<String> songList = new ArrayAdapter<String>(getApplicationContext(), R.layout.song_item, songs);
			lv.setAdapter(songList);
		}
		
		//set click listener to play song
	    lv.setOnItemClickListener(new OnItemClickListener(){
	        public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	        	//try{
	        		//get the device detail fragment and call it's send message function
	        		DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
	        	                .findFragmentById(R.id.frag_detail);
	        		
	        		fragmentDetails.sendMusicInstruction("playSelected " + position);
	        		
	        		/*
	    			mp.reset();
	    			mp.setDataSource(SD_PATH + songs.get(position));
	    			mp.prepare();
	    			//mp.start();
	    			
	    			//sleep for 5 ms before starting audio
	    			new Thread(new Runnable(){
	    				public void run() {
	    					
	    					try{
	    						Thread.sleep(100);
	    					}catch(InterruptedException e){
	    						e.printStackTrace();
	    					}
	    					
	    					runOnUiThread(new Runnable(){
	    						public void run(){
	    							mp.start();
	    						}
	    					});
	    				}
	    			}).start();
	    			*/
	    			
	    		/*}catch(IOException e){
	    			Log.d("DL", "IOException in onListItemClick");
	    		}*/
	        }

	    });
		
	}
	
	/** Methods to control the media player **/
	public void mediaStop(){
		mp.stop();
	}
	
	public void mediaPause(){
		mp.pause();
	}
	
	public void mediaStart(){
		mp.start();
	}
	
	public void playSelected(int position){
		try{
			mp.reset();
			mp.setDataSource(SD_PATH + songs.get(position));
			mp.prepare();
			mp.start();
		}catch(IOException e){
			Log.d("DL", "IOException in onListItemClick");
		}
	}
	
	

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.

                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

            case R.id.atn_direct_discover:
                if (!isWifiP2pEnabled) {
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }
}
