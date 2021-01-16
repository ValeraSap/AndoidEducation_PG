package com.example.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
	//ccb0056779a8e6dd223387a47a9c8449

	private static final String TAG = "PhotoGalleryFragment";

	private RecyclerView mPhotoRecyclerView;
	private List<GalleryItem> mItems = new ArrayList<>();

	public static PhotoGalleryFragment newInstance() {
		return new PhotoGalleryFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true); ///фрагмент не уничтожается вместе с его хостом и передается новому Activity в не измененном виде
		new FetchItemsTask().execute();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		View v=inflater.inflate(R.layout.fragment_photo_gallery, container,false);
		mPhotoRecyclerView=(RecyclerView)v.findViewById(R.id.photo_recycler_view);
		mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
		setupAdapter();

		return v;

	}

	private void setupAdapter() {
		if(isAdded()) { //?? Проверка подтверждает, что фрагмент был присоединен к активности, а следовательно, что результат getActivity() будет отличен от null.
			mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
		}
	}

	private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> { //https://www.techyourchance.com/asynctask-deprecated/
		@Override
		protected List<GalleryItem> doInBackground(Void... voids) {
			return new FlickrFetchr().fetchItems();

		}

		@Override
		protected void onPostExecute(List<GalleryItem> galleryItems) {
			mItems = galleryItems;
			setupAdapter();
		}

	}
	 private class PhotoHolder extends RecyclerView.ViewHolder {

		private TextView mTitileTextView;
		private GalleryItem mGalleryItem;

		public PhotoHolder (LayoutInflater inflater, ViewGroup parent){
			super (inflater.inflate(R.layout.list_item_photo,parent,false));

			mTitileTextView=(TextView)itemView.findViewById(R.id.photo_text_view);
		}

		public void bindGalleryItem(GalleryItem item)
		{
			mGalleryItem=item;
			mTitileTextView.setText(mGalleryItem.getCaption());
		}
	 }

	 private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

		private List<GalleryItem> mGalleryItems;

		public PhotoAdapter(List<GalleryItem> galleryItems) {
			mGalleryItems=galleryItems;
		}

		 @NonNull
		 @Override
		 public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = getLayoutInflater();

			 return new PhotoHolder(inflater,parent);
		 }

		 @Override
		 public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
			 GalleryItem galleryItem = mGalleryItems.get(position);
			 ((PhotoHolder)holder).bindGalleryItem(galleryItem);
		 }

		 @Override
		 public int getItemCount() {
			 return mGalleryItems.size();
		 }
	 }
}
