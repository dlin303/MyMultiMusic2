package com.example.android.wifidirect;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.BaseAdapter;

public class MyResultReceiver extends ResultReceiver {
	private Context context = null;
	private Receiver mReceiver;
	
	protected void setParentContext (Context context){
		this.context = context;
	}
	
	public MyResultReceiver(Handler handler){
		super(handler);
	}
	
	 public interface Receiver {
	        public void onReceiveResult(int resultCode, Bundle resultData);
	 }
	
	 public void setReceiver(Receiver receiver){
		 mReceiver = receiver;
	 }
	 
	 
	
	
	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData){
		
		if(mReceiver != null){
			mReceiver.onReceiveResult(resultCode, resultData);
		}
		
		//((BaseAdapter) ((ListActivity)context).getListAdapter()).notifyDataSetChanged();
	}
	
}
