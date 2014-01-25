package com.trevorshp.mserver;

public class RadioStation {
	public int number = -1;
	public String artRef = "";
	public String title = "";	
	
	@Override
	public String toString(){
		return this.title;
	}
}

