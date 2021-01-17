package com.example.photogallery;

import android.graphics.Insets;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowMetrics;
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
	private static final int COLUMN_WIDTH=140;
	private static final int WIDTH_DIVIDER=460; //для регулирования размера ширины колонки в зависимости от ширины экрана
	//private String perPage="100";

	private RecyclerView mPhotoRecyclerView;
	private List<GalleryItem> mItems = new ArrayList<>();
	private String flickrPage="1";

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

		//делаем количество и размер столбцов зависимым от ширины экрана
		//	int spanCount=width/(COLUMN_WIDTH*(width>WIDTH_DIVIDER?width/WIDTH_DIVIDER:1));*/
		//mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),spanCount));

		setupAdapter();
		mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);

				if(!mPhotoRecyclerView.canScrollVertically(1) && newState==RecyclerView.SCROLL_STATE_IDLE)
				{
					flickrPage=""+(Integer.parseInt(flickrPage)+1);
					new FetchItemsTask().execute(); //скачет как козел
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


	private void setupAdapter() {
		if(isAdded()) { //?? Проверка подтверждает, что фрагмент был присоединен к активности, а следовательно, что результат getActivity() будет отличен от null.
			mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
		}
	}
	//private void saveRecyclerViewState

	private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> { //https://www.techyourchance.com/asynctask-deprecated/
		@Override
		protected List<GalleryItem> doInBackground(Void... voids) {
			return new FlickrFetchr().fetchItems(flickrPage);

		}

		@Override
		protected void onPostExecute(List<GalleryItem> galleryItems) {
			Parcelable recyclerViewState=null;
			if(mItems.size()==0) {
				mItems=galleryItems;
			}
			else
			{
				mItems.addAll(galleryItems);
				recyclerViewState = mPhotoRecyclerView.getLayoutManager().onSaveInstanceState(); //сохранение состояния ленты
			}

			setupAdapter();
			if(recyclerViewState != null)
				mPhotoRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState); //востановление состояния
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
