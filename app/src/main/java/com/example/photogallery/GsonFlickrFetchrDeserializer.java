package com.example.photogallery;

import com.google.gson.Gson;

import java.util.List;

public class GsonFlickrFetchrDeserializer {

	private class Photo
	{
		String id;
				String owner;

		String secret;
				String server;
		String title;
		String url_s;
				int farm;
				int ispublic;
				int isfriend;
		int isfamily;
		int height_s;
		int width_s;
	}
	private class Photos
	{
		List<Photo> photo;
		int page;
		int pages;
		int perpage;
		int total;
	}
	private class Root {
		Photos photos;
	}


	public static void deserialize(List<GalleryItem> items, String jsonString) {

		Root root=new Gson().fromJson(jsonString,Root.class);
		Photos photos=root.photos;

		for (Photo p: photos.photo ) {
			GalleryItem galleryItem=new GalleryItem();
			galleryItem.setId(p.id);
			galleryItem.setCaption(p.title);
			if(p.url_s!=null)
				galleryItem.setUrl(p.url_s);


			items.add(galleryItem);
		}

	}
}
