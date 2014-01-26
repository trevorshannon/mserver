package com.trevorshp.mserver;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.trevorshp.mserver.ArtistsFragment.OnArtistSelectedListener;
import com.trevorshp.mserver.LibraryFragment.OnTrackSelectedListener;
import com.trevorshp.mserver.RadioFragment.OnStationSelectedListener;

import de.tavendo.autobahn.WebSocketHandler;

public class MainActivity extends FragmentActivity implements OnArtistSelectedListener, OnTrackSelectedListener, OnStationSelectedListener {
	
	private static final String TAG = "mserver"; 
	private static final int MODE_LIBRARY  = 0;
	private static final int MODE_RADIO = 1;
	private static final int MODE_INTERNET = 2;
	private static final String WS_SERVER_ADDRESS = "ws://10.1.10.12:8888/ws";
	private static final String STATE_TRACKS = "state_tracks";
	private static final String STATE_MODE = "state_mode";
	private static final int DEFAULT_PLAYLIST_LENGTH = 50;
	
	private Communicator comm;
	private WebSocketHandler handler;

	private CharSequence title;
	private ActionBarDrawerToggle DrawerToggle;
	private DrawerLayout DrawerLayout;
	private ListView DrawerList;
	private String[] topModes;
	private ArrayAdapter libraryAdapter;
	private ArrayAdapter radioAdapter;
	private ArrayAdapter librarySearchAdapter;
	private ArrayAdapter radioSearchAdapter;
	private ArrayAdapter internetAdapter;

	private boolean tracksInvalid;
	private boolean playing;
	private String oldText;
	private ArrayList<RadioStation> stations;
	private ArrayList<RadioStation> filteredStations;
	private ArrayList<Track> tracks;
	private ArrayList<Track> filteredTracks;
	private Map<String, ArrayList<Track>> byArtist;
	private ArrayList<String> artistNames;
	private ArrayList<Track> webResults;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		handler = new WebSocketHandler() {
			 
            @Override
            public void onOpen() {
               Log.d(TAG, "Status: Connected to " + WS_SERVER_ADDRESS);
               setConnected(true);
            }

			@Override
            public void onTextMessage(String payload) {
               Log.d(TAG, "Got new message from server");
               processMessage(payload);
            }
 
            @Override
            public void onClose(int code, String reason) {
               Log.d(TAG, "Connection closed");
               Log.d(TAG, code + " (" + reason + ")");
               setConnected(false);
            }
         };
         		
		comm = new Communicator(WS_SERVER_ADDRESS, handler);

		title = getTitle();
		topModes = getResources().getStringArray(R.array.top_modes);
		DrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		DrawerList = (ListView) findViewById(R.id.left_drawer);
		
		setConnected(false);
		
		playing = false;
		oldText = "";
		stations = new ArrayList<RadioStation>();
		filteredStations = new ArrayList<RadioStation>();
		filteredTracks = new ArrayList<Track>();
		artistNames = new ArrayList<String>();
		byArtist = new HashMap<String, ArrayList<Track>>();
		webResults = new ArrayList<Track>();
		if (savedInstanceState != null){
			tracks = savedInstanceState.getParcelableArrayList(STATE_TRACKS);
			generateArtistHash();
			//TODO: figure out why this line causes No Tracks and No Stations to be shown
			//selectItem(savedInstanceState.getInt(STATE_MODE));
			tracksInvalid = false;
		}
		else{
			
			tracks = new ArrayList<Track>();
			tracksInvalid = true;
			//selectItem(MODE_LIBRARY);
		}

		// Set the adapter for the draw list view
		DrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.top_mode_item, R.id.mode_name, topModes));
		// Set the list's click listener
		DrawerList.setOnItemClickListener(new DrawerItemClickListener());

		DrawerToggle = new ActionBarDrawerToggle(this, DrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getActionBar().setTitle(title);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getActionBar().setTitle(R.string.app_name);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};

		// Set the drawer toggle as the DrawerListener
		DrawerLayout.setDrawerListener(DrawerToggle);
		//DrawerLayout.setFocusableInTouchMode(false);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		
		libraryAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.station_item, R.id.station_name, artistNames);
		librarySearchAdapter = new ArrayAdapter<Track>(getApplicationContext(), R.layout.station_item, R.id.station_name, filteredTracks);
		radioAdapter = new ArrayAdapter<RadioStation>(getApplicationContext(), R.layout.station_item, R.id.station_name, stations);
		radioSearchAdapter = new ArrayAdapter<RadioStation>(getApplicationContext(), R.layout.station_item, R.id.station_name, filteredStations);
//		internetAdapter = new ArrayAdapter<Track>(getApplicationContext(), R.layout.station_item, R.id.station_name, );
		
		//initialize to library
		//TODO: initialize the mode based on saved instance state
		selectItem(MODE_LIBRARY);

		if (tracksInvalid){
			//get tracks from the server only if required
			new TrackDownloader().execute("http://10.1.10.12/getlibrary.php");
		}
	}
	
	@Override
	public void onPause(){
		super.onPause();	
		comm.end();
	}
	
	@Override
	public void onResume(){
		super.onResume();
	
		showPlayPause();
		comm.start();
	}
	
	@Override 
	public void onSaveInstanceState(Bundle savedInstanceState){
		//save list of tracks so a server request is not needed later
		savedInstanceState.putParcelableArrayList(STATE_TRACKS, tracks);
		//save current mode
		savedInstanceState.putInt(STATE_MODE, DrawerList.getCheckedItemPosition());
		super.onSaveInstanceState(savedInstanceState);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the options menu from XML
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    
	    //get the search view
	    final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
	    //set listeners
	    searchView.setOnCloseListener(new SearchView.OnCloseListener(){

			@Override
			public boolean onClose() {
				FragmentManager fragmentManager = getSupportFragmentManager();
				ListFragment fragment = (ListFragment) fragmentManager.findFragmentById(R.id.music_content);
				int currentMode = DrawerList.getCheckedItemPosition();
				switch (currentMode){
				case MODE_LIBRARY:
					//go back to the full list of tracks
					fragment.setListAdapter(libraryAdapter);
					break;
				case MODE_RADIO:
					//go back to the full list of stations
					fragment.setListAdapter(radioAdapter);
					break;
				case MODE_INTERNET: 
					break;
				}
				return false;
			}
	    	
	    }); 
	    
	    searchView.setOnSearchClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				searchView.setQuery("", false);
				FragmentManager fragmentManager = getSupportFragmentManager();
				ListFragment fragment = (ListFragment) fragmentManager.findFragmentById(R.id.music_content);
				
				int currentMode = DrawerList.getCheckedItemPosition();
				switch (currentMode){
				case MODE_LIBRARY:
					filteredTracks.clear();
					//switch fragments to show tracks
					ListFragment tracksFragment = new LibraryFragment();
					tracksFragment.setListAdapter(librarySearchAdapter);
					
					// Insert the fragment by replacing any existing fragment
					fragmentManager.beginTransaction()
					.replace(R.id.music_content, tracksFragment)
					//.addToBackStack(null)
					.commit();
					//switch to a filtered list of tracks
					//fragment.setListAdapter(librarySearchAdapter);
					break;
				case MODE_RADIO:
					filteredStations.clear();
					//switch to a filtered list of stations
					fragment.setListAdapter(radioSearchAdapter);
					break;
				case MODE_INTERNET: 
					break;
				}
			}
		});
	    
	    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				//close keyboard (http://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard)
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

				if (DrawerList.getCheckedItemPosition() == MODE_INTERNET){
					//no instant search for internet mode.  perform the search here.
					new WebSearch().execute(query);
				}
				return true;		
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {		
				//update search results
				int currentMode = DrawerList.getCheckedItemPosition();
				switch (currentMode){
				case MODE_LIBRARY:
					Iterator<Track> trackIt = tracks.iterator();
					ArrayList<Track> trackResults = new ArrayList<Track>();
					if (newText.length() > oldText.length() && !(oldText.isEmpty())){
						//user added to the query.  search within results
						trackIt = filteredTracks.iterator();
					}
					while(trackIt.hasNext()){
						Track track = trackIt.next();
						if (track.title != null){
							if (track.title.toLowerCase().contains(newText.toLowerCase())){
								trackResults.add(track);
								continue;
							}
						}
						if (track.artist != null){
							if (track.artist.toLowerCase().contains(newText.toLowerCase())){
								trackResults.add(track);
								continue;
							}
						}
						if (track.album != null){
							if (track.album.toLowerCase().contains(newText.toLowerCase())){
								trackResults.add(track);
								continue;
							}
						}
					}
					
					filteredTracks.clear();
					filteredTracks.addAll(trackResults);
					librarySearchAdapter.notifyDataSetChanged();
					break;
					
				case MODE_RADIO:
					Iterator<RadioStation> stationIt = stations.iterator();
					ArrayList<RadioStation> stationResults = new ArrayList<RadioStation>();
					if (newText.length() > oldText.length() && !(oldText.isEmpty())){
						//user added to the query.  search within results
						stationIt = filteredStations.iterator();
					}
					while(stationIt.hasNext()){
						RadioStation station = stationIt.next();
						if (station.title != null){
							if (station.title.toLowerCase().contains(newText.toLowerCase())){
								stationResults.add(station);
							}
						}
					}
					
					filteredStations.clear();
					filteredStations.addAll(stationResults);
					radioSearchAdapter.notifyDataSetChanged();
					break;
					
				case MODE_INTERNET:
					
					break;
				}
				
				oldText = newText;
				
				return true;
			}
		});
	    
	    return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (DrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		
        // Handle item selection
        switch (item.getItemId()) {
    
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	public void processMessage(String msg) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(msg);
			JsonNode typeNode = root.findValue("type");
			if (typeNode.getTextValue().equals("state")){
				//determine state and show the appropriate button
				String state = root.findValue("state").getTextValue();
				playing = state.equals("playing");
				showPlayPause();

				//get now playing information
				int active = root.findValue("active").getIntValue();
				JsonNode playlistNode = root.findValue("playlist");
				if (playlistNode.has(active)){
					JsonNode nowPlayingNode = playlistNode.get(active);
					String artist = nowPlayingNode.findValue("artist").getTextValue();
					String title = nowPlayingNode.findValue("title").getTextValue();
					TextView nowPlayingTitle = (TextView)findViewById(R.id.nowplaying_title);
					TextView nowPlayingArtist = (TextView)findViewById(R.id.nowplaying_artist);
					nowPlayingTitle.setText(title);
					nowPlayingArtist.setText(artist);
				}
			}
			else if (typeNode.getTextValue().equals("stationlist")){
				stations.clear();
				Iterator<JsonNode> stationsIterator = root.findValue("stations").getElements();
				while (stationsIterator.hasNext()){
					RadioStation station = mapper.readValue(stationsIterator.next(), RadioStation.class);
					stations.add(station);
				}
				radioAdapter.notifyDataSetChanged();
				Log.d(TAG, "got station list from server");
			}
		}
		catch (Exception e){
			Log.e(TAG, e.toString());
		}

	}
	
	public void setConnected(boolean connected){
		RelativeLayout connectedView = (RelativeLayout) findViewById(R.id.connection_status);
		TextView connectedText = (TextView) findViewById(R.id.connection_text);
		if (!connected){
			//show 'disconnected' notification
			connectedView.setVisibility(View.VISIBLE);
			connectedText.setText(R.string.disconnected);
		}
		else{
			//hide 'disconnected' notification
			connectedView.setVisibility(View.GONE);
			connectedText.setText(R.string.connected);
		}
	}
	
	@Override
	public void onArtistSelected(int position){
		String artistName = artistNames.get(position);
		//setTitle(artistName);
		//switch fragments to show tracks
		ListFragment tracksFragment = new LibraryFragment();
		ArrayList<Track> artistTracks = byArtist.get(artistName);
		tracksFragment.setListAdapter(new ArrayAdapter<Track>(this, R.layout.station_item, R.id.station_name, artistTracks));
		
		// Insert the fragment by replacing any existing fragment
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
		.replace(R.id.music_content, tracksFragment)
		.setBreadCrumbTitle(artistName)
		.addToBackStack(null)
		.commit();
	}

	@Override
	public void onStationSelected(RadioStation station){
		comm.send("station", String.valueOf(station.number));
	}
	
	@Override
	public void onTrackSelected(int position){
		
		ArrayList<Track> playlist = new ArrayList<Track>();
		FragmentManager fragmentManager = getSupportFragmentManager();
		ListFragment fragment = (ListFragment) fragmentManager.findFragmentById(R.id.music_content);
		ListAdapter adapter = fragment.getListAdapter();
		//generate playlist starting with selected track
		for (int i = 0; i < DEFAULT_PLAYLIST_LENGTH; i++){
			if ((position + i) < adapter.getCount()){
				playlist.add((Track)(adapter.getItem(position + i)));
			}
			else{
				break;
			}
		}
			
		
		ObjectMapper mapper = new ObjectMapper();
		StringWriter writer = new StringWriter();
		try{
			//create json
			mapper.writeValue(writer, playlist);
			writer.close();
		}
		catch(JsonParseException e){
			Log.e(TAG, "could not generate playlist json: " + e.getMessage());
		}
		catch(IOException e){
			Log.e(TAG, "could not close writer: " + e.getMessage());
		}		
		comm.send("playlist", writer.toString());
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = DrawerLayout.isDrawerOpen(DrawerList);
		menu.findItem(R.id.action_search).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		DrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		DrawerToggle.onConfigurationChanged(newConfig);
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			voldown(null);
		}
		else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			volup(null);
		}
		super.onKeyDown(keyCode, event);
		return true;
	}

	public void play(View view){
		comm.send("control", "play");
	}

	public void pause(View view){
		comm.send("control", "pause");
	}

	public void prev(View view){
		comm.send("control", "prev");
	}

	public void next(View view){
		comm.send("control", "next");
	}

	public void volup(View view){
		comm.send("control", "volup");
	}

	public void voldown(View view){
		comm.send("control", "voldown");
	}
	
	public void reconnect(View view){
		comm.start();
	}

	public void showPlayPause(){
		ImageButton playButton = (ImageButton) findViewById(R.id.play);
		ImageButton pauseButton = (ImageButton) findViewById(R.id.pause);
		//show the correct play/pause button based on current state
		if (playing){			
			playButton.setVisibility(View.GONE);
			pauseButton.setVisibility(View.VISIBLE);
		}
		else{
			playButton.setVisibility(View.VISIBLE);
			pauseButton.setVisibility(View.GONE);
		}
	}
	
	private class TrackDownloader extends AsyncTask<String, Integer, Boolean>{
		
		protected Boolean doInBackground(String... urls){
			//get tracks from the server
			try{
				ObjectMapper mapper = new ObjectMapper();
				URL url = new URL("http://10.1.10.12/getlibrary.php");
				JsonNode root = mapper.readTree(url);
				tracks.clear();
				//parse json into list of Tracks
				Iterator<JsonNode> tracksIterator = root.getElements();
				while (tracksIterator.hasNext()){
					Track track = mapper.readValue(tracksIterator.next(), Track.class);
					tracks.add(track);
				}
			}
			catch (Exception e){
				Log.e(TAG, "Error parsing track json: " + e.getMessage());
				return false;
			}
			generateArtistHash();
			return true;
		}
		
		protected void onPreExecute(){
			Log.d(TAG, "fetching library tracks...");
			//show loading notification
			LinearLayout pb = (LinearLayout) findViewById(R.id.loading_layout);
			pb.setVisibility(View.VISIBLE);
			
		}
		protected void onPostExecute(Boolean success){
			if (success){
				Log.d(TAG, "got library tracks from server");
				Toast.makeText(getApplicationContext(), "Retrieved library tracks", Toast.LENGTH_SHORT).show();
				libraryAdapter.notifyDataSetChanged();
			}
			else{
				Toast.makeText(getApplicationContext(), "Unable to retrieve library", Toast.LENGTH_SHORT).show();
				Log.e(TAG, "could not get library tracks from server");
			}
			//hide loading notification
			LinearLayout pb = (LinearLayout) findViewById(R.id.loading_layout);
			pb.setVisibility(View.GONE);
		}
	}
	
	private class WebSearch extends AsyncTask<String, Integer, Boolean>{
		protected Boolean doInBackground(String...query){
			webResults.clear();
			try{
				ObjectMapper mapper = new ObjectMapper();
				URL url = new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&q=" + URLEncoder.encode(query[0], "UTF-8") + "&type=video&maxResults=20&key=AIzaSyDkE1Q199bbUk5LNWI2PLY8mrlloJG-UUI");
				Map<String, Object> res = mapper.readValue(url, Map.class);
				//JsonNode root = mapper.readTree(url);
				//manually build the track object
				ArrayList<Object> items = (ArrayList<Object>)res.get("items");
				for (int i = 0; i < items.size(); i++){
					LinkedHashMap<String, Object> item = (LinkedHashMap<String, Object>)(items.get(i));
					LinkedHashMap<String, Object> id = (LinkedHashMap<String, Object>)(item.get("id"));
					LinkedHashMap<String, Object> snippet = (LinkedHashMap<String, Object>)(item.get("snippet"));
					Log.d(TAG, snippet.get("title").toString());
					Track track = new Track();
					track.id = id.get("videoId").toString();
					track.title = snippet.get("title").toString();
					webResults.add(track);
				}
			}
			catch(Exception e){
				Log.e(TAG, "unable to fetch web results: " + e.getMessage());
				return false;
			}
			return true;
		}
		
		protected void onPreExecute(){
			LinearLayout pb = (LinearLayout) findViewById(R.id.loading_layout);
			pb.setVisibility(View.VISIBLE);
		}
		
		protected void onPostExecute(Boolean success){
			if (!success){	
				Toast.makeText(getApplicationContext(), "Unable to perform search", Toast.LENGTH_SHORT).show();
			}
			else{
				FragmentManager fragmentManager = getSupportFragmentManager();
				ListFragment fragment = (ListFragment) fragmentManager.findFragmentById(R.id.music_content);
				fragment.setListAdapter(new ArrayAdapter<Track>(getApplicationContext(), R.layout.station_item, R.id.station_name, webResults));
			}
			LinearLayout pb = (LinearLayout) findViewById(R.id.loading_layout);
			pb.setVisibility(View.GONE);
		}
	}
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	/** Swaps fragments in the main content view */
	private void selectItem(int position) {	
		ListFragment fragment = new ListFragment();
		if (position == MODE_LIBRARY) {
			fragment = new ArtistsFragment();
			fragment.setListAdapter(libraryAdapter);
		}
		else if (position == MODE_RADIO){
			fragment = new RadioFragment();
			fragment.setListAdapter(radioAdapter);
		}
		else if (position == MODE_INTERNET){
			fragment = new LibraryFragment();
			//fragment.setListAdapter(internetAdapter);
		}

		// Insert the fragment by replacing any existing fragment
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
		.replace(R.id.music_content, fragment)
		.commit();

		// Highlight the selected item, update the title, and close the drawer
		DrawerList.setItemChecked(position, true);
		setTitle(topModes[position]);
		DrawerLayout.closeDrawer(DrawerList);
	}

	@Override
	public void setTitle(CharSequence title) {
		this.title = title;
		getActionBar().setTitle(title);
	}
	
	public void generateArtistHash(){
		//create Map from tracks list
		for (int i = 0; i < tracks.size(); i++){
			String artist = tracks.get(i).artist;
			if (artist == null || artist.isEmpty()){
				artist = "(no artist)";
			}
			if (byArtist.containsKey(artist)){
				byArtist.get(artist).add(tracks.get(i));
			}
			else{
				ArrayList<Track> trackList = new ArrayList<Track>();
				trackList.add(tracks.get(i));
				byArtist.put(artist, trackList);
			}
		}
		artistNames.clear();
		//get all artist names and put them in the adapter's data source
		String[] arr = byArtist.keySet().toArray(new String[0]);
		artistNames.addAll(Arrays.asList(arr));
		//be sure to organize them first!
		Collections.sort(artistNames);
	}
}
