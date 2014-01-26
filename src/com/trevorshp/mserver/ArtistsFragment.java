package com.trevorshp.mserver;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ArtistsFragment extends ListFragment {

	OnArtistSelectedListener callback;
	public ArtistsFragment(){
	}
	
	public interface OnArtistSelectedListener{
		public void onArtistSelected(int position);
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		callback = (OnArtistSelectedListener) activity;
	}
	
	@Override
	public void onListItemClick (ListView l, View v, int position, long id){
		super.onListItemClick(l, v, position, id);
		callback.onArtistSelected(position);
		l.setItemChecked(position, true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.library_fragment, container, false);
		return view;
	}
	
}
