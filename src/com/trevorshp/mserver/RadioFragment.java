package com.trevorshp.mserver;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class RadioFragment extends ListFragment {
	
	OnStationSelectedListener callback;
	public RadioFragment(){;
	}
	
	public interface OnStationSelectedListener{
		public void onStationSelected(RadioStation station);
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		callback = (OnStationSelectedListener) activity;
	}
	
	@Override
	public void onListItemClick (ListView l, View v, int position, long id){
		super.onListItemClick(l, v, position, id);
		RadioStation selectedStation = (RadioStation) l.getItemAtPosition(position);
		callback.onStationSelected(selectedStation);
		l.setItemChecked(position, true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.radio_fragment, container, false);
		return view;
	}
}
