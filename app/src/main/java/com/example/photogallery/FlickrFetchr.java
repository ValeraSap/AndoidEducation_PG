package com.example.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
	private static final String TAG = "FlickrFetchr";
	private static final String API_KEY = "ccb0056779a8e6dd223387a47a9c8449";

	public byte[] getUrlBytes (String urlSpec) throws IOException {


		URL url =new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		try{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = connection.getInputStream();
			if( connection.getResponseCode()!=HttpURLConnection.HTTP_OK) {
				throw new IOException(connection.getResponseMessage() +
						": with " +
						urlSpec);
			}

			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			while ((bytesRead=in.read(buffer))>0) { //???
				out.write(buffer,0,bytesRead);
			}
			out.close();
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}

	}

	public String getUrlString (String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}

	//https://www.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=ccb0056779a8e6dd223387a47a9c8449&format=json&nojsoncallback=1

	public List<GalleryItem> fetchItems() {
		List<GalleryItem> items = new ArrayList<>();
		try {
			String url = Uri.parse("https://api.flickr.com/services/rest/")
					.buildUpon()
					.appendQueryParameter("method","flickr.photos.getRecent")
					.appendQueryParameter("api_key",API_KEY)
					.appendQueryParameter("format","json")
					.appendQueryParameter("nojsoncallback", "1")
					.appendQueryParameter("extras", "url_s")
					.build().toString();
			String jsonString = getUrlString(url);
			Log.i(TAG, "Received JSON: " + jsonString);
			JSONObject jsonBody = new JSONObject(jsonString);
			parseItems(items, jsonBody);
			//parseItemsGson(items,jsonString);

		}catch (IOException ioe) {
			Log.e(TAG, "Failed to fetch items", ioe);
		}catch (JSONException je) {
			Log.e(TAG, "Failed to parse JSON", je);
		}
		return items;
	}

	public void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException,JSONException {
		JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
		JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

		for(int i=0;i<photoJsonArray.length();i++) {
			JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

			GalleryItem item = new GalleryItem();
			item.setId(photoJsonObject.getString("id"));
			item.setCaption(photoJsonObject.getString("title"));

			if(!photoJsonObject.has("url_s")){
				continue;
			}
			item.setUrl(photoJsonObject.getString("url_s"));
			items.add(item);
		}
	}
	///////////////////////////////////////////////////////////////////////////////////////
	private static class Item
	{
		String id;
		String title;
		String url_s;
		String secret;
		String server;
		int farm;
		int ispublic;
		int isfriend;
		int isfamily;
		int height_s;
		int width_s;
	}
	private static class photos
	{
		List <Item> photo;
		int page;
		int pages;
		int perpage;
		int total;
	}

	public void parseItemsGson(List<GalleryItem> items, String jsonString) {

		photos phs=new Gson().fromJson(jsonString,photos.class);

		for (Item p: phs.photo) {
			GalleryItem item = new GalleryItem();
			item.setId(p.id);
			item.setCaption(p.title);
			if(p.url_s!=null)
				item.setUrl(p.url_s);
			items.add(item);
		}

	}

}
