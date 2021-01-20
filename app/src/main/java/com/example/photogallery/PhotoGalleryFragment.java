package com.example.photogallery;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Insets;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SearchView;
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
	private static final int COLUMN_WIDTH=140;
	private static final int WIDTH_DIVIDER=460; //для регулирования размера ширины колонки в зависимости от ширины экрана
	private static String flickrPage="1"; //возможно это воспрепятствует поиску на др страницах


	private RecyclerView mPhotoRecyclerView;
	private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
	private List<GalleryItem> mItems = new ArrayList<>();


	public static PhotoGalleryFragment newInstance() {
		return new PhotoGalleryFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true); ///фрагмент не уничтожается вместе с его хостом и передается новому Activity в не измененном виде
		setHasOptionsMenu(true);

		updateItems();

		Handler responseHandler=new Handler();
		mThumbnailDownloader=new ThumbnailDownloader<>(responseHandler);
		mThumbnailDownloader.setThumbnailDownloadListener(
				new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
					@Override
					public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
						Drawable drawable=new BitmapDrawable(getResources(),bitmap);
						photoHolder.bindDrawable(drawable);
					}
				}
		);
		mThumbnailDownloader.start();
		mThumbnailDownloader.getLooper();
		Log.i(TAG, "Background thread started");
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		View v=inflater.inflate(R.layout.fragment_photo_gallery, container,false);
		mPhotoRecyclerView=(RecyclerView)v.findViewById(R.id.photo_recycler_view);

		setupAdapter();
		mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);

				if(!mPhotoRecyclerView.canScrollVertically(1) && newState==RecyclerView.SCROLL_STATE_IDLE)
				{
					flickrPage=""+(Integer.parseInt(flickrPage)+1);
					updateItems(); //скачет как козел
				}
			}
		});

		mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
		mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				int dpi = getResources().getDisplayMetrics().densityDpi;
				int width= (int) (mPhotoRecyclerView.getWidth() / (dpi / 160)); //px / (dpi / 160); //width in dp
				//dynamic column width and span count change
				float addition = (float)(width/WIDTH_DIVIDER-1)/2; 									//(2 -> 0.5) (3 - > 1) (4 -> 1.5)
				float multiplier = width>=WIDTH_DIVIDER?(1 + addition):1;//*2 -> *1.5
				float column_width = COLUMN_WIDTH*multiplier;
				int spanCount= (int) (width/column_width);
				((GridLayoutManager)mPhotoRecyclerView.getLayoutManager()).setSpanCount(spanCount);
			}
		});

		return v;

	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_photo_gallery,menu);

		MenuItem searchItem=menu.findItem(R.id.menu_item_search);
		final SearchView searchView=(SearchView) searchItem.getActionView();

		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.d(TAG, "QueryTextSubmit: " + query);
				QueryPreferences.setStoredQuery(getActivity(), query);
				updateItems();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				//Log.d(TAG, "QueryTextChange: " + newText);
				return false;
			}
		});
		searchView.setOnSearchClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String query = QueryPreferences.getStoredQuery(getActivity());
				searchView.setQuery(query, false);
			}
		});

		MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
		if(PollService.isServiceAlarmOn(getActivity())) {
			toggleItem.setTitle(R.string.stop_polling);
		}else {
			toggleItem.setTitle(R.string.start_polling);
		}
	}

	@SuppressLint("NonConstantResourceId") //todo find out what does it mean
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_clear:
				QueryPreferences.setStoredQuery(getActivity(),null);
				updateItems();
				return true;
			case R.id.menu_item_toggle_polling:
				boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
				PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
				getActivity().invalidateOptionsMenu();
				return true;
			default: return super.onOptionsItemSelected(item);
		}
	}

	private void updateItems() {
		String query=QueryPreferences.getStoredQuery(getActivity());
		new FetchItemsTask(query).execute();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailDownloader.clearQueue();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailDownloader.quit();
		Log.i(TAG, "Background thread destroyed");

	}

	private void setupAdapter() {
		if(isAdded()) { //?? Проверка подтверждает, что фрагмент был присоединен к активности, а следовательно, что результат getActivity() будет отличен от null.
			mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
		}
	}
	//private void saveRecyclerViewState

	private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> { //https://www.techyourchance.com/asynctask-deprecated/
		private String mQuery;

		public FetchItemsTask(String query) {
			mQuery = query;
		}

		@Override
		protected List<GalleryItem> doInBackground(Void... voids) {

			 if(mQuery==null)
				return new FlickrFetchr().fetchRecentPhotos(flickrPage);
			else
				return new FlickrFetchr().searchPhotos(mQuery,flickrPage);

		}

		@Override
		protected void onPostExecute(List<GalleryItem> galleryItems) {
			Parcelable recyclerViewState=null;
			//todo подрузка страниц с фото из запроса
			//mItems=galleryItems;

			if(mItems.size()==0 || mQuery!=null) { //будет трудно листать дальше запросные фото
				mItems=galleryItems;
			}
			else
			{
				mItems.addAll(galleryItems);
				//todo узнать с какой именно позиции продолжается листание
				recyclerViewState = mPhotoRecyclerView.getLayoutManager().onSaveInstanceState(); //сохранение состояния ленты
			}

			setupAdapter();
			if(recyclerViewState != null)
				mPhotoRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState); //востановление состояния


		}

	}
	 private class PhotoHolder extends RecyclerView.ViewHolder {

		//private TextView mTitileTextView;
		private GalleryItem mGalleryItem;
		private ImageView mItemImageView;

		 public PhotoHolder(@NonNull View itemView) {
			 super(itemView);
			 mItemImageView=(ImageView) itemView.findViewById(R.id.item_image_view);
		 }


		 public void bindDrawable(Drawable drawable) {
			 mItemImageView.setImageDrawable(drawable);
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
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			 View view = inflater.inflate(R.layout.gallery_item, parent, false);
			 return new PhotoHolder(view);

		 }

		 @Override
		 public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
			 GalleryItem galleryItem = mGalleryItems.get(position);
			 Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
			 holder.bindDrawable(placeholder);
			 mThumbnailDownloader.queueThumbnail(holder,galleryItem.getUrl());
		 }

		 @Override
		 public int getItemCount() {
			 return mGalleryItems.size();
		 }
	 }
}
