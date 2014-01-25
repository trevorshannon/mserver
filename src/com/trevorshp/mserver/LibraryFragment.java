package com.trevorshp.mserver;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class LibraryFragment extends ListFragment {

	OnTrackSelectedListener callback;
	public LibraryFragment(){
	}
	
	public interface OnTrackSelectedListener{
		public void onTrackSelected(int position);
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		callback = (OnTrackSelectedListener) activity;
	}
	
	@Override
	public void onListItemClick (ListView l, View v, int position, long id){
		super.onListItemClick(l, v, position, id);
		callback.onTrackSelected(position);
		l.setItemChecked(position, true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.library_fragment, container, false);
		return view;
	}
}
