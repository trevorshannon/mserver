package com.trevorshp.mserver;

import android.os.Parcel;
import android.os.Parcelable;

public class Track implements Parcelable{
	public String title = "";
	public String artist = "";
	public String album = "";
	public String id = "";
	
	public Track(){
		
	}
	
	public String toString(){
		return this.title + " | " + this.artist;
	}


	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.title);
		out.writeString(this.artist);
		out.writeString(this.album);
		out.writeString(this.id);
	}

	public static final Parcelable.Creator<Track> CREATOR = 
	new Parcelable.Creator<Track>() {
		public Track createFromParcel(Parcel in) {
			return new Track(in);
		}

		public Track[] newArray(int size) {
			return new Track[size];
		}
	};

	private Track(Parcel in) {
		this.title = in.readString();
		this.artist = in.readString();
		this.album = in.readString();
		this.id = in.readString();
	}
}


