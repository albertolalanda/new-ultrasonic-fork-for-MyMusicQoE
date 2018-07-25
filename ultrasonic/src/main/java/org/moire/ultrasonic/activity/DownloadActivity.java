/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package org.moire.ultrasonic.activity;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.mobeta.android.dslv.DragSortListView;

import org.moire.ultrasonic.R;
import org.moire.ultrasonic.domain.MusicDirectory;
import org.moire.ultrasonic.domain.MusicDirectory.Entry;
import org.moire.ultrasonic.domain.PlayerState;
import org.moire.ultrasonic.domain.RepeatMode;
import org.moire.ultrasonic.service.DownloadFile;
import org.moire.ultrasonic.service.DownloadService;
import org.moire.ultrasonic.service.MusicService;
import org.moire.ultrasonic.service.MusicServiceFactory;
import org.moire.ultrasonic.util.Constants;
import org.moire.ultrasonic.util.SilentBackgroundTask;
import org.moire.ultrasonic.util.Util;
import org.moire.ultrasonic.view.AutoRepeatButton;
import org.moire.ultrasonic.view.SongListAdapter;
import org.moire.ultrasonic.view.VisualizerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;

import static org.moire.ultrasonic.domain.PlayerState.COMPLETED;
import static org.moire.ultrasonic.domain.PlayerState.IDLE;
import static org.moire.ultrasonic.domain.PlayerState.PAUSED;
import static org.moire.ultrasonic.domain.PlayerState.STOPPED;

public class DownloadActivity extends SubsonicTabActivity implements OnGestureListener
{
	private static final String TAG = DownloadActivity.class.getSimpleName();
	private static final int DIALOG_SAVE_PLAYLIST = 100;
	private static final int PERCENTAGE_OF_SCREEN_FOR_SWIPE = 5;

	private ViewFlipper playlistFlipper;
	private TextView emptyTextView;
	private TextView songTitleTextView;
	private TextView albumTextView;
	private TextView artistTextView;
	private ImageView albumArtImageView;
	private DragSortListView playlistView;
	private TextView positionTextView;
	private TextView downloadTrackTextView;
	private TextView downloadTotalDurationTextView;
	private TextView durationTextView;
	private static SeekBar progressBar;
	private View pauseButton;
	private View stopButton;
	private View startButton;
	private ImageView repeatButton;
	private ScheduledExecutorService executorService;
	private DownloadFile currentPlaying;
	private Entry currentSong;
	private long currentRevision;
	private EditText playlistNameView;
	private GestureDetector gestureScanner;
	private int swipeDistance;
	private int swipeVelocity;
	private VisualizerView visualizerView;
	private boolean visualizerAvailable;
	private boolean equalizerAvailable;
	private boolean jukeboxAvailable;
	private SilentBackgroundTask<Void> onProgressChangedTask;
	LinearLayout visualizerViewLayout;
	private MenuItem starMenuItem;
	private View starButtonView;

	private static SeekBar verticalSeekBar;
	private LinearLayout verticalSeekBarParent;
	private TextView seekbarRatingText;
	private TextView separatorRatingExcellentText;
	private TextView separatorRatingGoodText;
	private TextView separatorRatingFairText;
	private TextView separatorRatingPoorText;
	private TextView separatorRatingBadText;

	private Vibrator vibrator;

	// variables for the user rating
	private boolean canRate = false; // timer is over and user can rate
	private static boolean hasRated = false; //has rated in the activity, star goes full
	private boolean changeStar = false; //true when we want to change star

	//use for timer
	private int secondsLeftForRate = 10;
	private boolean songUnrated = true; // song unrated on saved array of the service
	private CountDownTimer countDownTimer;
	private boolean isRunning = false; // countdowntimer is running

	private Drawable starDrawable = Util.getDrawableFromAttribute(SubsonicTabActivity.getInstance(), R.attr.star_disabled);

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download);

		final WindowManager windowManager = getWindowManager();
		final Display display = windowManager.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;

		swipeDistance = (width + height) * PERCENTAGE_OF_SCREEN_FOR_SWIPE / 100;
		swipeVelocity = swipeDistance;
		gestureScanner = new GestureDetector(this, this);

		playlistFlipper = (ViewFlipper) findViewById(R.id.download_playlist_flipper);
		emptyTextView = (TextView) findViewById(R.id.download_empty);
		songTitleTextView = (TextView) findViewById(R.id.download_song_title);
		albumTextView = (TextView) findViewById(R.id.download_album);
		artistTextView = (TextView) findViewById(R.id.download_artist);
		albumArtImageView = (ImageView) findViewById(R.id.download_album_art_image);
		positionTextView = (TextView) findViewById(R.id.download_position);
		downloadTrackTextView = (TextView) findViewById(R.id.download_track);
		downloadTotalDurationTextView = (TextView) findViewById(R.id.download_total_duration);
		durationTextView = (TextView) findViewById(R.id.download_duration);
		progressBar = (SeekBar) findViewById(R.id.download_progress_bar);
		playlistView = (DragSortListView) findViewById(R.id.download_list);
		final AutoRepeatButton previousButton = (AutoRepeatButton) findViewById(R.id.download_previous);
		final AutoRepeatButton nextButton = (AutoRepeatButton) findViewById(R.id.download_next);
		pauseButton = findViewById(R.id.download_pause);
		stopButton = findViewById(R.id.download_stop);
		startButton = findViewById(R.id.download_start);
		final View shuffleButton = findViewById(R.id.download_shuffle);
		repeatButton = (ImageView) findViewById(R.id.download_repeat);

		visualizerViewLayout = (LinearLayout) findViewById(R.id.download_visualizer_view_layout);

		verticalSeekBar = findViewById(R.id.tickSeekBar);
		verticalSeekBarParent = findViewById(R.id.tickSeekBarParent);
		seekbarRatingText = findViewById(R.id.seekbar_current_rating);
		separatorRatingExcellentText = findViewById(R.id.rating_separator_excellent_text);
		separatorRatingGoodText = findViewById(R.id.rating_separator_good_text);
		separatorRatingFairText = findViewById(R.id.rating_separator_fair_text);
		separatorRatingPoorText = findViewById(R.id.rating_separator_poor_text);
		separatorRatingBadText = findViewById(R.id.rating_separator_bad_text);
		vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

		View.OnTouchListener touchListener = new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View view, MotionEvent me)
			{
				return gestureScanner.onTouchEvent(me);
			}
		};

		albumArtImageView.setOnTouchListener(touchListener);

		albumArtImageView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{
				toggleFullScreenAlbumArtRating(1);
			}
		});

		previousButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{
				warnIfNetworkOrStorageUnavailable();

				new SilentBackgroundTask<Void>(DownloadActivity.this)
				{
					@Override
					protected Void doInBackground() throws Throwable
					{
						//LALANDA SEND ON PREVIEOUS
						//getDownloadService().sendRatingMyMusicQoE(getDownloadService().getCurrentPlaying());
						getDownloadService().previous();
						return null;
					}

					@Override
					protected void done(final Void result)
					{
						onCurrentChanged();
						onSliderProgressChanged();
					}
				}.execute();
			}
		});

		previousButton.setOnRepeatListener(new Runnable()
		{
			@Override
			public void run()
			{
				int incrementTime = Util.getIncrementTime(DownloadActivity.this);
				changeProgress(-incrementTime);
			}
		});

		nextButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{
				warnIfNetworkOrStorageUnavailable();

				new SilentBackgroundTask<Boolean>(DownloadActivity.this)
				{
					@Override
					protected Boolean doInBackground() throws Throwable
					{

						if (getDownloadService().getCurrentPlayingIndex() < getDownloadService().size() - 1)
						{
							//getDownloadService().sendRatingMyMusicQoE(getDownloadService().getCurrentPlaying());
							getDownloadService().next();
							return true;
						}
						else
						{
							return false;
						}
					}

					@Override
					protected void done(final Boolean result)
					{
						if (result)
						{
							onCurrentChanged();
							onSliderProgressChanged();
						}
					}
				}.execute();
			}
		});

		nextButton.setOnRepeatListener(new Runnable()
		{
			@Override
			public void run()
			{
				int incrementTime = Util.getIncrementTime(DownloadActivity.this);
				changeProgress(incrementTime);
			}
		});



		pauseButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{

				new SilentBackgroundTask<Void>(DownloadActivity.this)
				{
					@Override
					protected Void doInBackground() throws Throwable
					{
						getDownloadService().pause();
						return null;
					}

					@Override
					protected void done(final Void result)
					{
						onCurrentChanged();
						onSliderProgressChanged();
					}
				}.execute();
			}
		});

		stopButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{
				new SilentBackgroundTask<Void>(DownloadActivity.this)
				{
					@Override
					protected Void doInBackground() throws Throwable
					{
						getDownloadService().reset();
						return null;
					}

					@Override
					protected void done(final Void result)
					{
						onCurrentChanged();
						onSliderProgressChanged();
					}
				}.execute();
			}
		});

		startButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{
				warnIfNetworkOrStorageUnavailable();

				new SilentBackgroundTask<Void>(DownloadActivity.this)
				{
					@Override
					protected Void doInBackground() throws Throwable
					{
						start();
						return null;
					}

					@Override
					protected void done(final Void result)
					{
						onCurrentChanged();
						onSliderProgressChanged();
					}
				}.execute();
			}
		});

		//LALANDA TODO TESTES PARA VER O QUÃO BEM ISTO FOI IMPLEMENTADO
		// maybe this is the answer https://stackoverflow.com/questions/13532919/how-do-i-shuffle-two-arrays-in-same-order-in-java
		shuffleButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{
				getDownloadService().shuffle();
				Util.toast(DownloadActivity.this, R.string.download_menu_shuffle_notification);
			}
		});


		repeatButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{
				final RepeatMode repeatMode = getDownloadService().getRepeatMode().next();

				getDownloadService().setRepeatMode(repeatMode);
				onDownloadListChanged();

				switch (repeatMode)
				{
					case OFF:
						Util.toast(DownloadActivity.this, R.string.download_repeat_off);
						break;
					case ALL:
						Util.toast(DownloadActivity.this, R.string.download_repeat_all);
						break;
					case SINGLE:
						Util.toast(DownloadActivity.this, R.string.download_repeat_single);
						break;
					default:
						break;
				}
			}
		});

		//LALANDA PROGRESS BAR CHANGED
		progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onStopTrackingTouch(final SeekBar seekBar)
			{
				new SilentBackgroundTask<Void>(DownloadActivity.this)
				{
					@Override
					protected Void doInBackground() throws Throwable
					{
						getDownloadService().seekTo(getProgressBar().getProgress());
						return null;
					}

					@Override
					protected void done(final Void result)
					{
						onSliderProgressChanged();
					}
				}.execute();
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar)
			{
			}

			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser)
			{
			}
		});

		playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
			{
				warnIfNetworkOrStorageUnavailable();

				new SilentBackgroundTask<Void>(DownloadActivity.this)
				{
					@Override
					protected Void doInBackground() throws Throwable
					{
						getDownloadService().play(position);
						return null;
					}

					@Override
					protected void done(final Void result)
					{
						onCurrentChanged();
						onSliderProgressChanged();
					}
				}.execute();
			}
		});

		registerForContextMenu(playlistView);

		final DownloadService downloadService = getDownloadService();
		if (downloadService != null && getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, false))
		{
			warnIfNetworkOrStorageUnavailable();
			downloadService.setShufflePlayEnabled(true);
		}

		//LALANDA populate lists NOT NEEDED ANYMORE
		/*if (downloadService != null && !downloadService.getSongs().isEmpty()){
			List<DownloadFile> list = downloadService.getSongs();

			/*List<Boolean> songsRated =new ArrayList<Boolean>(Arrays.asList(new Boolean[list.size()]));
			Collections.fill(songsRated, Boolean.FALSE);
		}*/

		visualizerAvailable = (downloadService != null) && (downloadService.getVisualizerController() != null);
		equalizerAvailable = (downloadService != null) && (downloadService.getEqualizerController() != null);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					DownloadService downloadService = getDownloadService();
					jukeboxAvailable = (downloadService != null) && (downloadService.isJukeboxAvailable());
				}
				catch (Exception e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}).start();

		final View nowPlayingMenuItem = findViewById(R.id.menu_now_playing);
		menuDrawer.setActiveView(nowPlayingMenuItem);

		if (visualizerAvailable)
		{
			visualizerView = new VisualizerView(this);
			visualizerViewLayout.addView(visualizerView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

			if (!visualizerView.isActive())
			{
				visualizerViewLayout.setVisibility(View.GONE);
			}
			else
			{
				visualizerViewLayout.setVisibility(View.VISIBLE);
			}

			visualizerView.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(final View view, final MotionEvent motionEvent)
				{
					visualizerView.setActive(!visualizerView.isActive());
					getDownloadService().setShowVisualization(visualizerView.isActive());
					return true;
				}
			});
		}
		else
		{
			visualizerViewLayout.setVisibility(View.GONE);
		}

		verticalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				seekbarRatingText.setText((""+i));
				if (!hasRated){
					hasRated = true;
					changeStar = true;
				}
				verticalSeekBarChangeText(i);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				Toast.makeText(DownloadActivity.this, "Seekbar touch started", Toast.LENGTH_SHORT).show();
				vibrator.vibrate(15);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Toast.makeText(DownloadActivity.this, "Seekbar touch stopped", Toast.LENGTH_SHORT).show();
				vibrator.vibrate(15);
			}
		});



//LALANDA TOUCH ON THE RIGHT SEEKBAR DOENST WORK TO INVESTIGATE LATER
//		final View child = verticalSeekBar;
//		verticalSeekBarParent.post(new Runnable() {
//			public void run() {
//				// Post in the parent's message queue to make sure the
//				// parent
//				// lays out its children before we call getHitRect()
//				Rect delegateArea = new Rect();
//				View delegate = child;
//				delegate.getHitRect(delegateArea);
//				delegateArea.right += 3000;
//				TouchDelegate expandedArea = new TouchDelegate(delegateArea, delegate);
//				// give the delegate to an ancestor of the view we're
//				// delegating the
//				// area to
//				if (View.class.isInstance(delegate.getParent())) {
//					((View) delegate.getParent()).setTouchDelegate(expandedArea);
//				}
//			}
//		});
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		final DownloadService downloadService = getDownloadService();

		if (downloadService == null || downloadService.getCurrentPlaying() == null)
		{
			playlistFlipper.setDisplayedChild(1);
		}

		final Handler handler = new Handler();
		final Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						update();
					}
				});
			}
		};

		if (downloadService != null || downloadService.getCurrentPlaying() != null) {
			//RESUME LALANDA
			System.out.println("LALANDA : " + getDownloadService().getPlayerPosition());
			if (downloadService.getPlayerPosition() > 10000) {
				canRate = true;
				changeStar = true;
				songUnrated = false;
			} else {
				secondsLeftForRate = (int) Math.round(getDownloadService().getPlayerPosition() * 0.001);
				songUnrated = false;
				if (isRunning){
					countDownTimer.cancel();
				}
				countDownTimer = new CountDownTimer(secondsLeftForRate * 1000, 1000) {
					public void onTick(long millisUntilFinished) {
						isRunning = true;
						secondsLeftForRate = (int) Math.round(millisUntilFinished * 0.001);
						System.out.println("COUNTDOWN seconds" + secondsLeftForRate);
					}

					public void onFinish() {
						isRunning = false;
						canRate = true;
						changeStar = true;
						songUnrated = false;
					}
				}.start();
			}
		}

		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleWithFixedDelay(runnable, 0L, 250L, TimeUnit.MILLISECONDS);

		if (downloadService != null && downloadService.getKeepScreenOn())
		{
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		else
		{
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		if (visualizerView != null)
		{
			visualizerView.setActive(downloadService != null && downloadService.getShowVisualization());
		}

		invalidateOptionsMenu();

	}

	// Scroll to current playing/downloading.
	private void scrollToCurrent()
	{
		if (getDownloadService() == null)
		{
			return;
		}

		ListAdapter adapter = playlistView.getAdapter();

		if (adapter != null)
		{
			int count = adapter.getCount();

			for (int i = 0; i < count; i++)
			{
				if (currentPlaying == playlistView.getItemAtPosition(i))
				{
					playlistView.smoothScrollToPositionFromTop(i, 40);
					return;
				}
			}

			final DownloadFile currentDownloading = getDownloadService().getCurrentDownloading();
			for (int i = 0; i < count; i++)
			{
				if (currentDownloading == playlistView.getItemAtPosition(i))
				{
					playlistView.smoothScrollToPositionFromTop(i, 40);
					return;
				}
			}
		}
	}

	//Util.toast(this, R.string.download_menu_shuffle_notification);
	//timer pause
	@Override
	protected void onPause()
	{
		super.onPause();
		executorService.shutdown();

		if (visualizerView != null)
		{
			visualizerView.setActive(false);
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id)
	{
		if (id == DIALOG_SAVE_PLAYLIST)
		{
			final AlertDialog.Builder builder;

			final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			final View layout = layoutInflater.inflate(R.layout.save_playlist, (ViewGroup) findViewById(R.id.save_playlist_root));

			if (layout != null)
			{
				playlistNameView = (EditText) layout.findViewById(R.id.save_playlist_name);
			}

			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.download_playlist_title);
			builder.setMessage(R.string.download_playlist_name);
			builder.setPositiveButton(R.string.common_save, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(final DialogInterface dialog, final int clickId)
				{
					savePlaylistInBackground(String.valueOf(playlistNameView.getText()));
				}
			});
			builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(final DialogInterface dialog, final int clickId)
				{
					dialog.cancel();
				}
			});
			builder.setView(layout);
			builder.setCancelable(true);

			return builder.create();
		}
		else
		{
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog)
	{
		if (id == DIALOG_SAVE_PLAYLIST)
		{
			final String playlistName = (getDownloadService() != null) ? getDownloadService().getSuggestedPlaylistName() : null;
			if (playlistName != null)
			{
				playlistNameView.setText(playlistName);
			}
			else
			{
				final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				playlistNameView.setText(dateFormat.format(new Date()));
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.nowplaying, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		final MenuItem screenOption = menu.findItem(R.id.menu_item_screen_on_off);
		//final MenuItem jukeboxOption = menu.findItem(R.id.menu_item_jukebox);
		final MenuItem equalizerMenuItem = menu.findItem(R.id.menu_item_equalizer);
		final MenuItem visualizerMenuItem = menu.findItem(R.id.menu_item_visualizer);
		//final MenuItem shareMenuItem = menu.findItem(R.id.menu_item_share);
		starMenuItem = menu.findItem(R.id.menu_item_star);
		//MenuItem bookmarkMenuItem = menu.findItem(R.id.menu_item_bookmark_set);
		//MenuItem bookmarkRemoveMenuItem = menu.findItem(R.id.menu_item_bookmark_delete);

		//Lalanda star button view
		starButtonView = findViewById(R.id.menu_item_star);

		if (Util.isOffline(this))
		{
//			if (shareMenuItem != null)
//			{
//				shareMenuItem.setVisible(false);
//			}

			if (starMenuItem != null)
			{
				starMenuItem.setVisible(false);
			}

//			if (bookmarkMenuItem != null)
//			{
//				bookmarkMenuItem.setVisible(false);
//			}
//
//			if (bookmarkRemoveMenuItem != null)
//			{
//				bookmarkRemoveMenuItem.setVisible(false);
//			}
		}

		if (equalizerMenuItem != null)
		{
			equalizerMenuItem.setEnabled(equalizerAvailable);
			equalizerMenuItem.setVisible(equalizerAvailable);
		}

		if (visualizerMenuItem != null)
		{
			visualizerMenuItem.setEnabled(visualizerAvailable);
			visualizerMenuItem.setVisible(visualizerAvailable);
		}

		final DownloadService downloadService = getDownloadService();

		if (downloadService != null)
		{
			DownloadFile downloadFile = downloadService.getCurrentPlaying();

			if (downloadFile != null)
			{
				currentSong = downloadFile.getSong();
			}

			if (currentSong != null)
			{
				if (changeStar) {
					if (!canRate){
						starDrawable = Util.getDrawableFromAttribute(SubsonicTabActivity.getInstance(), R.attr.star_disabled);
					}else if(!hasRated) {
						starDrawable = Util.getDrawableFromAttribute(SubsonicTabActivity.getInstance(), R.attr.star_hollow);
					}else {
						starDrawable = Util.getDrawableFromAttribute(SubsonicTabActivity.getInstance(), R.attr.star_full);
					}
					changeStar = false;
				}

				if (starMenuItem != null)
				{
					starMenuItem.setIcon(starDrawable);
				}
			}
			else
			{
				Drawable starDrawable = Util.getDrawableFromAttribute(SubsonicTabActivity.getInstance(), R.attr.star_hollow);

				if (starMenuItem != null)
				{
					starMenuItem.setIcon(starDrawable);
				}
			}


			if (downloadService.getKeepScreenOn())
			{
				if (screenOption != null)
				{
					screenOption.setTitle(R.string.download_menu_screen_off);
				}
			}
			else
			{
				if (screenOption != null)
				{
					screenOption.setTitle(R.string.download_menu_screen_on);
				}
			}

//			if (jukeboxOption != null)
//			{
//				jukeboxOption.setEnabled(jukeboxAvailable);
//				jukeboxOption.setVisible(jukeboxAvailable);
//
//				if (downloadService.isJukeboxEnabled())
//				{
//					jukeboxOption.setTitle(R.string.download_menu_jukebox_off);
//				}
//				else
//				{
//					jukeboxOption.setTitle(R.string.download_menu_jukebox_on);
//				}
//			}
		}

		return true;
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		if (view == playlistView)
		{
			final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			final DownloadFile downloadFile = (DownloadFile) playlistView.getItemAtPosition(info.position);

			final MenuInflater menuInflater = getMenuInflater();
			menuInflater.inflate(R.menu.nowplaying_context, menu);

			Entry song = null;

			if (downloadFile != null)
			{
				song = downloadFile.getSong();
			}

			if (song != null && song.getParent() == null)
			{
				MenuItem menuItem = menu.findItem(R.id.menu_show_album);

				if (menuItem != null)
				{
					menuItem.setVisible(false);
				}
			}

			if (Util.isOffline(this) || !Util.getShouldUseId3Tags(this))
			{
				MenuItem menuItem = menu.findItem(R.id.menu_show_artist);

				if (menuItem != null)
				{
					menuItem.setVisible(false);
				}
			}

			if (Util.isOffline(this))
			{
				MenuItem menuItem = menu.findItem(R.id.menu_lyrics);

				if (menuItem != null)
				{
					menuItem.setVisible(false);
				}
			}
		}
	}

	@Override
	public boolean onContextItemSelected(final MenuItem menuItem)
	{
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();

		DownloadFile downloadFile = null;

		if (info != null)
		{
			downloadFile = (DownloadFile) playlistView.getItemAtPosition(info.position);
		}

		return menuItemSelected(menuItem.getItemId(), downloadFile) || super.onContextItemSelected(menuItem);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem menuItem)
	{
		return menuItemSelected(menuItem.getItemId(), null) || super.onOptionsItemSelected(menuItem);
	}

	private boolean menuItemSelected(final int menuItemId, final DownloadFile song)
	{
		Entry entry = null;

		if (song != null)
		{
			entry = song.getSong();
		}

		switch (menuItemId)
		{
			case R.id.menu_show_artist:
				if (entry == null)
				{
					return false;
				}

				if (Util.getShouldUseId3Tags(DownloadActivity.this))
				{
					Intent intent = new Intent(DownloadActivity.this, SelectAlbumActivity.class);
					intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, entry.getArtistId());
					intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, entry.getArtist());
					intent.putExtra(Constants.INTENT_EXTRA_NAME_PARENT_ID, entry.getArtistId());
					intent.putExtra(Constants.INTENT_EXTRA_NAME_ARTIST, true);
					startActivityForResultWithoutTransition(DownloadActivity.this, intent);
				}

				return true;
			case R.id.menu_show_album:
				if (entry == null)
				{
					return false;
				}

				Intent intent = new Intent(this, SelectAlbumActivity.class);
				intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, entry.getParent());
				intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, entry.getAlbum());
				startActivityForResultWithoutTransition(this, intent);
				return true;
			case R.id.menu_lyrics:
				if (entry == null)
				{
					return false;
				}

				intent = new Intent(this, LyricsActivity.class);
				intent.putExtra(Constants.INTENT_EXTRA_NAME_ARTIST, entry.getArtist());
				intent.putExtra(Constants.INTENT_EXTRA_NAME_TITLE, entry.getTitle());
				startActivityForResultWithoutTransition(this, intent);
				return true;
			case R.id.menu_remove:
				getDownloadService().remove(song);
				//lalanda delete song !!!!!!!!!!!!!!!!!!!!
				//deleteFromPlaylist(song);
				onDownloadListChanged();
				return true;
			case R.id.menu_item_screen_on_off:
				if (getDownloadService().getKeepScreenOn())
				{
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					getDownloadService().setKeepScreenOn(false);
				}
				else
				{
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					getDownloadService().setKeepScreenOn(true);
				}
				return true;
			case R.id.menu_shuffle:
				getDownloadService().shuffle();
				Util.toast(this, R.string.download_menu_shuffle_notification);
				return true;
			case R.id.menu_item_equalizer:
				startActivity(new Intent(DownloadActivity.this, EqualizerActivity.class));
				return true;
			case R.id.menu_item_visualizer:
				final boolean active = !visualizerView.isActive();
				visualizerView.setActive(active);

				if (!visualizerView.isActive())
				{
					visualizerViewLayout.setVisibility(View.GONE);
				}
				else
				{
					visualizerViewLayout.setVisibility(View.VISIBLE);
				}

				getDownloadService().setShowVisualization(visualizerView.isActive());
				Util.toast(DownloadActivity.this, active ? R.string.download_visualizer_on : R.string.download_visualizer_off);
				return true;
				//LALANDA TOGGLE PLAYLIST BUTTON
			case R.id.menu_item_toggle_list:
				toggleFullScreenAlbumArtRating(1);
				return true;
			case R.id.menu_item_clear_playlist:
				getDownloadService().setShufflePlayEnabled(false);
				//lalanda playlist
				deletePlaylist();

				getDownloadService().clear();
				onDownloadListChanged();
				return true;
			case R.id.menu_item_star:
				if (currentSong == null)
				{
					return true;
				}
				if (canRate) {
					toggleFullScreenAlbumArtRating(2);
				}else{
					if (secondsLeftForRate!=10){
						Util.toast(DownloadActivity.this, "please listen for another " + secondsLeftForRate + " seconds.", false);
					}else{
						Util.toast(DownloadActivity.this, "please listen at least for " + secondsLeftForRate + " seconds.", false);
					}
				}
				return true;
			default:
				return false;
		}
	}

	private void update()
	{
		if (getDownloadService() == null)
		{
			return;
		}

		if (currentRevision != getDownloadService().getDownloadListUpdateRevision())
		{
			onDownloadListChanged();
		}

		if (currentPlaying != getDownloadService().getCurrentPlaying())
		{
			onCurrentChanged();
		}

		onSliderProgressChanged();
		invalidateOptionsMenu();
	}

	private void savePlaylistInBackground(final String playlistName)
	{
		Util.toast(DownloadActivity.this, getResources().getString(R.string.download_playlist_saving, playlistName));
		getDownloadService().setSuggestedPlaylistName(playlistName);
		new SilentBackgroundTask<Void>(this)
		{
			@Override
			protected Void doInBackground() throws Throwable
			{
				final List<MusicDirectory.Entry> entries = new LinkedList<MusicDirectory.Entry>();
				for (final DownloadFile downloadFile : getDownloadService().getSongs())
				{
					entries.add(downloadFile.getSong());
				}
				final MusicService musicService = MusicServiceFactory.getMusicService(DownloadActivity.this);
				musicService.createPlaylist(null, playlistName, entries, DownloadActivity.this, null);
				return null;
			}

			@Override
			protected void done(final Void result)
			{
				Util.toast(DownloadActivity.this, R.string.download_playlist_done);
			}

			@Override
			protected void error(final Throwable error)
			{
				final String msg = String.format("%s %s", getResources().getString(R.string.download_playlist_error), getErrorMessage(error));
				Util.toast(DownloadActivity.this, msg);
			}
		}.execute();
	}

	//TOGGLE LIST LALANDA BETWEEN PLAYLIST AND ALBUM IMAGE AND RATING BAR
	private void toggleFullScreenAlbumArtRating(int index)
	{
		if (playlistFlipper.getDisplayedChild() == 2 && index == 2)
		{
			playlistFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_in));
			playlistFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_out));
			playlistFlipper.setDisplayedChild(0);
		}
		else if(playlistFlipper.getDisplayedChild() == 0 && index == 2)
		{
			playlistFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_in));
			playlistFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_out));
			playlistFlipper.setDisplayedChild(2);
		}else if(playlistFlipper.getDisplayedChild() == 1 && index == 1){
			playlistFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_in));
			playlistFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_out));
			playlistFlipper.setDisplayedChild(0);
		}else if (playlistFlipper.getDisplayedChild() == 1 && index == 2){
			playlistFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_in));
			playlistFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_out));
			playlistFlipper.setDisplayedChild(2);
		}else{
			playlistFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_in));
			playlistFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_out));
			playlistFlipper.setDisplayedChild(1);
		}

		scrollToCurrent();
	}

	private void start()
	{
		final DownloadService service = getDownloadService();
		final PlayerState state = service.getPlayerState();

		if (state == PAUSED || state == COMPLETED || state == STOPPED)
		{
			service.start();
		}
		else if (state == IDLE)
		{
			warnIfNetworkOrStorageUnavailable();

			final int current = service.getCurrentPlayingIndex();

			if (current == -1)
			{
				service.play(0);
			}
			else
			{
				service.play(current);
			}
		}
	}

	//LALANDA when this happens might need to delete the cache
	//UPDATE. THIS CAN BE ON

	//REPEAT
	//REMOVE A SONG FROM PLAYLIST
	//CLEAR PLAYLIST
	//
	//REMOVE FROM PLAYLIST
	private void onDownloadListChanged()
	{
		final DownloadService downloadService = getDownloadService();

		// ALBERTO LALANDA METHOD WHEN DOWNLOAD LIST IS CHANGED IN THE ORDER
		if (downloadService == null)
		{
			return;
		}

		final List<DownloadFile> list = downloadService.getSongs();

		emptyTextView.setText(R.string.download_empty);
		final SongListAdapter adapter = new SongListAdapter(this, list);
		playlistView.setAdapter(adapter);

		playlistView.setDragSortListener(new DragSortListView.DragSortListener()
		{
			@Override
			public void drop(int from, int to)
			{
				if (from != to)
				{
					//myMusicQoE drag change rating list
					downloadService.songsRatingInfoDragNDrop(from, to);

					DownloadFile item = adapter.getItem(from);
					adapter.remove(item);
					adapter.notifyDataSetChanged();
					adapter.insert(item, to);
					adapter.notifyDataSetChanged();
				}
			}

			@Override
			public void drag(int from, int to)
			{

			}

			@Override
			public void remove(int which)
			{
				DownloadFile item = adapter.getItem(which);
				DownloadService downloadService = getDownloadService();

				if (item == null || downloadService == null)
				{
					return;
				}

				DownloadFile currentPlaying = downloadService.getCurrentPlaying();

				if (currentPlaying == item)
				{
					getDownloadService().next();
				}

				//myMusicQoE remove change rating list
				downloadService.songsRatingInfoDelete(which);

				adapter.remove(item);
				adapter.notifyDataSetChanged();

				String songRemoved = String.format(getResources().getString(R.string.download_song_removed), item.getSong().getTitle());

				Util.toast(DownloadActivity.this, songRemoved);

				onDownloadListChanged();
				onCurrentChanged();
			}
		});

		emptyTextView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
		currentRevision = downloadService.getDownloadListUpdateRevision();

		switch (downloadService.getRepeatMode())
		{
			case OFF:
				repeatButton.setImageDrawable(Util.getDrawableFromAttribute(this, R.attr.media_repeat_off));
				break;
			case ALL:
				repeatButton.setImageDrawable(Util.getDrawableFromAttribute(this, R.attr.media_repeat_all));
				break;
			case SINGLE:
				repeatButton.setImageDrawable(Util.getDrawableFromAttribute(this, R.attr.media_repeat_single));
				break;
			default:
				break;
		}
	}

	//WHEN MUSIC IS CHANGED LALANDA OR PAUSED OR WHATEVER
	private void onCurrentChanged()
	{
		DownloadService downloadService = getDownloadService();
		if (playlistFlipper.getDisplayedChild()==2){
			toggleFullScreenAlbumArtRating(0);
		}

		if (downloadService == null)
		{
			return;
		}

		currentPlaying = downloadService.getCurrentPlaying();

		scrollToCurrent();

		//total duration of list
		long totalDuration = downloadService.getDownloadListDuration();
		//number of songs of list
		long totalSongs = downloadService.getSongs().size();
		//index of current song
		//ATENÇÃO O SEGUINTE VALOR DE INDEX IRÁ COMERÇAR NO 1
		int currentSongIndex = downloadService.getCurrentPlayingIndex() + 1;
		System.out.println("LALANDA onCurrentChanged: current song index - " + currentSongIndex);

		//LALANDA WHEN CURRENT MUSIC IS CHANGED MODIFICATIONS
		//this will get information of the music the user changed to and change rating button and rating bar

		///LALANDA THIS NEEDS TO BE RESOLVED
		if (downloadService.isNewSong()){
			downloadService.setNewSong(false);
			if (downloadService.getSongsRatingInfo(currentSongIndex-1, 0) == 0){
				verticalSeekBar.setProgress(0);
				seekbarRatingText.setText("");
				verticalSeekBarChangeText(0);
				songUnrated = true;
				canRate = false;
				hasRated = false;
				changeStar = true;
				secondsLeftForRate = 10;
			}else{
				int progress = downloadService.getSongsRatingInfo(currentSongIndex-1, 1);
				verticalSeekBar.setProgress(progress);
				seekbarRatingText.setText(""+progress);
				verticalSeekBarChangeText(progress);
				songUnrated = false;
				canRate = true;
				hasRated = true;
				changeStar = true;
				secondsLeftForRate = 10;
			}
		}

		String duration = Util.formatTotalDuration(totalDuration);

		String trackFormat = String.format(Locale.getDefault(), "%d / %d", currentSongIndex, totalSongs);

		if (currentPlaying != null)
		{
			currentSong = currentPlaying.getSong();
			songTitleTextView.setText(currentSong.getTitle());
			albumTextView.setText(currentSong.getAlbum());
			artistTextView.setText(currentSong.getArtist());
			downloadTrackTextView.setText(trackFormat);
			downloadTotalDurationTextView.setText(duration);
			getImageLoader().loadImage(albumArtImageView, currentSong, true, 0, false, true);
		}
		else
		{
			currentSong = null;
			songTitleTextView.setText(null);
			albumTextView.setText(null);
			artistTextView.setText(null);
			downloadTrackTextView.setText(null);
			downloadTotalDurationTextView.setText(null);
			getImageLoader().loadImage(albumArtImageView, null, true, 0, false, true);
		}
	}

	private void onSliderProgressChanged()
	{
		DownloadService downloadService = getDownloadService();

		if (downloadService == null || onProgressChangedTask != null)
		{
			return;
		}

		onProgressChangedTask = new SilentBackgroundTask<Void>(this)
		{
			DownloadService downloadService;
			boolean isJukeboxEnabled;
			int millisPlayed;
			Integer duration;
			PlayerState playerState;

			@Override
			protected Void doInBackground() throws Throwable
			{
				downloadService = getDownloadService();
				isJukeboxEnabled = downloadService.isJukeboxEnabled();
				millisPlayed = Math.max(0, downloadService.getPlayerPosition());
				duration = downloadService.getPlayerDuration();
				playerState = getDownloadService().getPlayerState();

				return null;
			}

			@Override
			protected void done(final Void result)
			{
				if (currentPlaying != null)
				{
					final int millisTotal = duration == null ? 0 : duration;

					positionTextView.setText(Util.formatTotalDuration(millisPlayed, true));
					durationTextView.setText(Util.formatTotalDuration(millisTotal, true));
					progressBar.setMax(millisTotal == 0 ? 100 : millisTotal); // Work-around for apparent bug.
					progressBar.setProgress(millisPlayed);
					progressBar.setEnabled(currentPlaying.isWorkDone() || isJukeboxEnabled);


				}
				else
				{
					positionTextView.setText(R.string.util_zero_time);
					durationTextView.setText(R.string.util_no_time);
					progressBar.setProgress(0);
					progressBar.setMax(0);
					progressBar.setEnabled(false);
				}

				switch (playerState)
				{
					case DOWNLOADING:
						final long bytes = currentPlaying != null ? currentPlaying.getPartialFile().length() : 0;
						String downloadStatus = getResources().getString(R.string.download_playerstate_downloading, Util.formatLocalizedBytes(bytes, DownloadActivity.this));
						setActionBarSubtitle(downloadStatus);
						break;
					case PREPARING:
						setActionBarSubtitle(R.string.download_playerstate_buffering);
						break;
					case STARTED:
						final DownloadService downloadService = getDownloadService();

						if (songUnrated){
							songUnrated = false;
							if (isRunning){
							    countDownTimer.cancel();
                            }
							countDownTimer = new CountDownTimer(secondsLeftForRate*1000, 1000) {
								public void onTick(long millisUntilFinished) {
									secondsLeftForRate = (int) Math.round(millisUntilFinished * 0.001);
									isRunning = true;
								}

								public void onFinish() {
									canRate = true;
									changeStar = true;
									songUnrated = false;
									isRunning = false;
									new SimpleTooltip.Builder(DownloadActivity.this)
											.anchorView(starButtonView)
											.text(R.string.mymusicqoe_rating_tooltip)
											.gravity(Gravity.BOTTOM)
											.animated(false)
											.transparentOverlay(true)
											.backgroundColor(Color.parseColor("#31698a"))
											.arrowColor(Color.parseColor("#31698a"))
											.textColor(Color.WHITE)
											.build()
											.show();
								}
							}.start();
						}



						if (downloadService != null && downloadService.isShufflePlayEnabled())
						{
							setActionBarSubtitle(R.string.download_playerstate_playing_shuffle);
						}
						else
						{
							setActionBarSubtitle(null);
						}
						break;
					default:
						setActionBarSubtitle(null);
						break;
					case IDLE:
						break;
					case PREPARED:
						break;
					case STOPPED:
						break;
					case PAUSED:
						if (!canRate && !songUnrated){
							songUnrated = true;
							countDownTimer.cancel();
						}
						break;
					case COMPLETED:
						break;
				}

				switch (playerState)
				{
					case STARTED:
						pauseButton.setVisibility(View.VISIBLE);
						stopButton.setVisibility(View.GONE);
						startButton.setVisibility(View.GONE);
						break;
					case DOWNLOADING:
					case PREPARING:
						pauseButton.setVisibility(View.GONE);
						stopButton.setVisibility(View.VISIBLE);
						startButton.setVisibility(View.GONE);
						break;
					default:
						pauseButton.setVisibility(View.GONE);
						stopButton.setVisibility(View.GONE);
						startButton.setVisibility(View.VISIBLE);
						break;
				}

				onProgressChangedTask = null;
			}
		};
		onProgressChangedTask.execute();
	}

	private void changeProgress(final int ms)
	{
		//setOffset((int) (ms*0.001));
		final DownloadService downloadService = getDownloadService();
		if (downloadService == null)
		{
			return;
		}

		new SilentBackgroundTask<Void>(this)
		{
			int msPlayed;
			Integer duration;
			int seekTo;

			@Override
			protected Void doInBackground() throws Throwable
			{
				msPlayed = Math.max(0, downloadService.getPlayerPosition());
				duration = downloadService.getPlayerDuration();

				final int msTotal = duration;
				seekTo = msPlayed + ms > msTotal ? msTotal : msPlayed + ms;
				downloadService.seekTo(seekTo);

				return null;
			}

			@Override
			protected void done(final Void result)
			{
				progressBar.setProgress(seekTo);
			}
		}.execute();
	}

	@Override
	public boolean onTouchEvent(final MotionEvent me)
	{
		return gestureScanner.onTouchEvent(me);
	}

	@Override
	public boolean onDown(final MotionEvent me)
	{
		return false;
	}

	@Override
	public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY)
	{

		final DownloadService downloadService = getDownloadService();

		if (downloadService == null || e1 == null || e2 == null)
		{
			return false;
		}

		float e1X = e1.getX();
		float e2X = e2.getX();
		float e1Y = e1.getY();
		float e2Y = e2.getY();
		float absX = Math.abs(velocityX);
		float absY = Math.abs(velocityY);

		// Right to Left swipe
		if (e1X - e2X > swipeDistance && absX > swipeVelocity)
		{
			warnIfNetworkOrStorageUnavailable();
			if (downloadService.getCurrentPlayingIndex() < downloadService.size() - 1)
			{
				downloadService.next();
				onCurrentChanged();
				onSliderProgressChanged();
			}
			return true;
		}

		// Left to Right swipe
		if (e2X - e1X > swipeDistance && absX > swipeVelocity)
		{
			warnIfNetworkOrStorageUnavailable();
			downloadService.previous();
			onCurrentChanged();
			onSliderProgressChanged();
			return true;
		}

		// Top to Bottom swipe
		if (e2Y - e1Y > swipeDistance && absY > swipeVelocity)
		{
			warnIfNetworkOrStorageUnavailable();
			downloadService.seekTo(downloadService.getPlayerPosition() + 30000);
			onSliderProgressChanged();
			return true;
		}

		// Bottom to Top swipe
		if (e1Y - e2Y > swipeDistance && absY > swipeVelocity)
		{
			warnIfNetworkOrStorageUnavailable();
			downloadService.seekTo(downloadService.getPlayerPosition() - 8000);
			onSliderProgressChanged();
			return true;
		}

		return false;
	}

	@Override
	public void onLongPress(final MotionEvent e)
	{
	}

	@Override
	public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY)
	{
		return false;
	}

	@Override
	public void onShowPress(final MotionEvent e)
	{
	}

	@Override
	public boolean onSingleTapUp(final MotionEvent e)
	{
		return false;
	}

	public static SeekBar getProgressBar()
	{
		return progressBar;
	}

	public static SeekBar getVerticaSeekBar()
	{
		return verticalSeekBar;
	}

	/*private void setSecondsPassed(int newValue, int oldValue)
	{
		newValue = newValue - offset;
		secondsPassed = newValue - oldValue;
	}

	private int getSecondsPassed(){
        //System.out.println("LALANDA seconds passed " + secondsPassed);
		return secondsPassed;
	}



	private void setOffset(int newOffset){
        //System.out.println("LALANDA offset " + newOffset);
		offset = newOffset;
	}*/

	//LALANDA DELETE CACHE
	private void deletePlaylist()
	{
		final List<MusicDirectory.Entry> songs = new LinkedList<MusicDirectory.Entry>();
		for (final DownloadFile downloadFile : getDownloadService().getSongs())
		{
			songs.add(downloadFile.getSong());
		}

		if (songs.isEmpty())
		{
			return;
		}

		if (getDownloadService() != null)
		{
			getDownloadService().delete(songs);
		}
	}

	private void deleteFromPlaylist(DownloadFile song)
	{
		final List<MusicDirectory.Entry> songs = new LinkedList<MusicDirectory.Entry>();
		songs.add(song.getSong());

		if (songs.isEmpty())
		{
			return;
		}

		if (getDownloadService() != null)
		{
			getDownloadService().delete(songs);
		}
	}

	public void excellentButton(View view){


		animateProgression(100);
		//verticalSeekBar.setProgress(100);
		vibrator.vibrate(15);
	}
	public void goodButton(View view){
		animateProgression(80);
		//verticalSeekBar.setProgress(80);
		vibrator.vibrate(15);
	}
	public void fairButton(View view){
		animateProgression(60);
		//verticalSeekBar.setProgress(60);
		vibrator.vibrate(15);
	}
	public void poorButton(View view){
		animateProgression(40);
		//verticalSeekBar.setProgress(40);
		vibrator.vibrate(15);
	}
	public void badButton(View view){
		animateProgression(20);
		//verticalSeekBar.setProgress(20);
		vibrator.vibrate(15);
	}

	private void verticalSeekBarChangeText(int i){
		if (i > 80){
			separatorRatingExcellentText.setTextColor(Color.CYAN);
			separatorRatingExcellentText.setTypeface(Typeface.DEFAULT_BOLD);
			separatorRatingGoodText.setTypeface(Typeface.DEFAULT);
			separatorRatingGoodText.setTextColor(Color.parseColor("#ffffff"));
			separatorRatingFairText.setTextColor(Color.parseColor("#ffffff"));
			separatorRatingPoorText.setTextColor(Color.parseColor("#ffffff"));
			separatorRatingBadText.setTextColor(Color.parseColor("#ffffff"));
		}else{
			if (i > 60){
				separatorRatingExcellentText.setTextColor(Color.parseColor("#ffffff"));
				separatorRatingExcellentText.setTypeface(Typeface.DEFAULT);
				separatorRatingGoodText.setTextColor(Color.CYAN);
				separatorRatingGoodText.setTypeface(Typeface.DEFAULT_BOLD);
				separatorRatingFairText.setTypeface(Typeface.DEFAULT);
				separatorRatingFairText.setTextColor(Color.parseColor("#ffffff"));
				separatorRatingPoorText.setTextColor(Color.parseColor("#ffffff"));
				separatorRatingBadText.setTextColor(Color.parseColor("#ffffff"));
			}else{
				if (i > 40){
					separatorRatingExcellentText.setTextColor(Color.parseColor("#ffffff"));
					separatorRatingGoodText.setTextColor(Color.parseColor("#ffffff"));
					separatorRatingGoodText.setTypeface(Typeface.DEFAULT);
					separatorRatingFairText.setTextColor(Color.CYAN);
					separatorRatingFairText.setTypeface(Typeface.DEFAULT_BOLD);
					separatorRatingPoorText.setTypeface(Typeface.DEFAULT);
					separatorRatingPoorText.setTextColor(Color.parseColor("#ffffff"));
					separatorRatingBadText.setTextColor(Color.parseColor("#ffffff"));
				}else{
					if (i > 20){
						separatorRatingExcellentText.setTextColor(Color.parseColor("#ffffff"));
						separatorRatingGoodText.setTextColor(Color.parseColor("#ffffff"));
						separatorRatingFairText.setTextColor(Color.parseColor("#ffffff"));
						separatorRatingFairText.setTypeface(Typeface.DEFAULT);
						separatorRatingPoorText.setTextColor(Color.CYAN);
						separatorRatingPoorText.setTypeface(Typeface.DEFAULT_BOLD);
						separatorRatingBadText.setTypeface(Typeface.DEFAULT);
						separatorRatingBadText.setTextColor(Color.parseColor("#ffffff"));
					}else{
						if (i > 0){
							separatorRatingExcellentText.setTextColor(Color.parseColor("#ffffff"));
							separatorRatingGoodText.setTextColor(Color.parseColor("#ffffff"));
							separatorRatingFairText.setTextColor(Color.parseColor("#ffffff"));
							separatorRatingPoorText.setTextColor(Color.parseColor("#ffffff"));
							separatorRatingPoorText.setTypeface(Typeface.DEFAULT);
							separatorRatingBadText.setTextColor(Color.CYAN);
							separatorRatingBadText.setTypeface(Typeface.DEFAULT_BOLD);
						}
					}
				}
			}
		}
	}

	private void animateProgression(int progress) {
		ObjectAnimator animation = ObjectAnimator.ofInt(verticalSeekBar, "progress", verticalSeekBar.getProgress(), progress);
		animation.setDuration(500);
		animation.setInterpolator(new DecelerateInterpolator());
		animation.start();
		verticalSeekBar.clearAnimation();
	}

	//FUNCTIONS FOR MyMusicQoE

	public int getVerticalSeekBarProgress() {
		return verticalSeekBar.getProgress();
	}

	public static boolean isRated() {
		return hasRated;
	}

}