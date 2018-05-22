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

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import org.moire.ultrasonic.R;
import org.moire.ultrasonic.domain.Genre;
import org.moire.ultrasonic.service.MusicService;
import org.moire.ultrasonic.service.MusicServiceFactory;
import org.moire.ultrasonic.util.BackgroundTask;
import org.moire.ultrasonic.util.TabActivityBackgroundTask;
import org.moire.ultrasonic.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class UserInformationActivity extends SubsonicTabActivity {

	private static final String TAG = UserInformationActivity.class.getSimpleName();

	private ImageView imgMale;
	private ImageView imgFemale;
	private Spinner spinnerAge;

	private Button buttonGenres;
	private boolean taskIsFinished;
	private String[] listGenres;
	private boolean[] checkedGenres;
	private ArrayList<Integer> userGenres = new ArrayList<>();
	private int numberOfFavoriteGenres;
	private ArrayList<String> listPreferenceFavoriteGenres = new ArrayList<>();

	private Button save;
	private String sex;
	private int age;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_information);

		imgMale = (ImageView) findViewById(R.id.image_view_male);
		imgFemale = (ImageView) findViewById(R.id.image_view_female);
		spinnerAge = (Spinner) findViewById(R.id.spinner_age);
		buttonGenres = (Button) findViewById(R.id.button_genres);
		save = (Button) findViewById(R.id.button_save);
		save.setEnabled(false);
		//to check if background task is finished. might exist a better way
		taskIsFinished = false;

		//get sex from preferences
		String UserSex = Util.getUserSex(this);

		switch (UserSex) {
			case "M":
				imgMale.setImageResource(R.drawable.ic_men_white);
				imgFemale.setImageResource(R.drawable.ic_women_grey);
				sex = "M";
				break;
			case "F":
				imgMale.setImageResource(R.drawable.ic_men_grey);
				imgFemale.setImageResource(R.drawable.ic_women_white);
				sex = "F";
				break;
			default:
				imgMale.setImageResource(R.drawable.ic_men_grey);
				imgFemale.setImageResource(R.drawable.ic_women_grey);
				sex = "Undefined";
				break;
		}

		//get age from preferences
		age = Util.getUserAge(this);
		String[] ageArray = new String[] {
				String.valueOf(getText(R.string.user_information_age_default)),
				String.valueOf(getText(R.string.user_information_age_20)),
				String.valueOf(getText(R.string.user_information_age_20_30)),
				String.valueOf(getText(R.string.user_information_age_30_40)),
				String.valueOf(getText(R.string.user_information_age_40_50)),
				String.valueOf(getText(R.string.user_information_age_50_60)),
				String.valueOf(getText(R.string.user_information_age_60))
		};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ageArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAge.setAdapter(adapter);
		spinnerAge.setSelection(age);

		//genres
		BackgroundTask<List<Genre>> task = new TabActivityBackgroundTask<List<Genre>>(this, true)
		{
			@Override
			protected List<Genre> doInBackground() throws Throwable
			{
				MusicService musicService = MusicServiceFactory.getMusicService(UserInformationActivity.this);

				List<Genre> genres = new ArrayList<Genre>();

				do {
					try
					{
						genres = musicService.getGenres(UserInformationActivity.this, this);
						break;
					}
					catch (Exception x)
					{
						Log.e(TAG, "Failed to load genres ", x);
					}
				}while(genres.size() <= 0);

				return genres;
			}

			@Override
			protected void done(List<Genre> result)
			{
				listGenres = new String[result.size()];
				checkedGenres = new boolean[result.size()];

				Collection<String> sectionSet = new LinkedHashSet<String>(30);
				List<Integer> positionList = new ArrayList<Integer>(30);

				//fill shared preference genres list
				String item = "";
				numberOfFavoriteGenres = Util.getNumberOfFavoriteGenres(UserInformationActivity.this);
				for (int i = 0; i < numberOfFavoriteGenres; i++){
					System.out.println(Util.getNumberOfFavoriteGenres(UserInformationActivity.this));
					System.out.println("FAVORITE GENRES: "+Util.getFavoriteGenre(UserInformationActivity.this, i));
					listPreferenceFavoriteGenres.add(Util.getFavoriteGenre(UserInformationActivity.this, i));
					item = item + Util.getFavoriteGenre(UserInformationActivity.this, i);
					if (i != numberOfFavoriteGenres - 1) {
						item = item + ", ";
					}
				}
				if (!item.isEmpty()){
					buttonGenres.setText(item);
				}else{
					buttonGenres.setText("select favorite genres");
				}

				for (int i = 0; i < result.size(); i++)
				{
					Genre genre = result.get(i);
					String index = genre.getIndex();
					if (!sectionSet.contains(index))
					{
						sectionSet.add(index);
						positionList.add(i);
					}

					listGenres[i] = genre.getName();
					if (listPreferenceFavoriteGenres.contains(genre.getName())){
						checkedGenres[i] = true;
					}else{
						checkedGenres[i] = false;
					}
				}

				buttonGenres.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						AlertDialog.Builder mBuilder = new AlertDialog.Builder(UserInformationActivity.this);
						mBuilder.setTitle("Select Favorite Genres");
						mBuilder.setMultiChoiceItems(listGenres, checkedGenres, new DialogInterface.OnMultiChoiceClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
								if(isChecked){
									userGenres.add(position);
								}else{
									userGenres.remove((Integer.valueOf(position)));
								}
							}
						});

						mBuilder.setCancelable(false);
						mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								String item = "";
								for (int i = 0; i < userGenres.size(); i++) {
									item = item + listGenres[userGenres.get(i)];
									if (i != userGenres.size() - 1) {
										item = item + ", ";
									}
								}
								if (!item.isEmpty()){
									buttonGenres.setText(item);
								}else{
									buttonGenres.setText("select favorite genres");
								}

							}
						});

						mBuilder.setNeutralButton("Clear All", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								for (int i = 0; i < checkedGenres.length; i++) {
									checkedGenres[i] = false;
									userGenres.clear();
									buttonGenres.setText("select favorite genres");
								}
							}
						});

						AlertDialog mDialog = mBuilder.create();
						mDialog.show();
					}
				});

				taskIsFinished = true;
				updateSendButton (sex, spinnerAge.getSelectedItemPosition());
			}
		};
		task.execute();

		spinnerAge.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				updateSendButton (sex, spinnerAge.getSelectedItemPosition());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				// your code here
			}
		});

		imgMale.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				imgMale.setImageResource(R.drawable.ic_men_white);
				imgFemale.setImageResource(R.drawable.ic_women_grey);
				sex = "M";
				updateSendButton (sex, spinnerAge.getSelectedItemPosition());
			}
		});
		imgFemale.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				imgMale.setImageResource(R.drawable.ic_men_grey);
				imgFemale.setImageResource(R.drawable.ic_women_white);
				sex = "F";
				updateSendButton (sex, spinnerAge.getSelectedItemPosition());
			}
		});

		save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateSendButton (sex, spinnerAge.getSelectedItemPosition());
				if(save.isEnabled()){
					//Save user sex
					switch (sex) {
						case "M":
								Util.setUserSex(UserInformationActivity.this, 1);
							break;
						case "F":
								Util.setUserSex(UserInformationActivity.this, 0);
							break;
					}
					//Save user age
					Util.setUserAge(UserInformationActivity.this, spinnerAge.getSelectedItemPosition());
					//Save favorite genres
					Util.setNumberOfFavoriteGenres(UserInformationActivity.this, userGenres.size());
					System.out.println("NUMBER OF NEW FAVORITE GENRES: " + userGenres.size());
					for (int i = 0; i < userGenres.size(); i++){
						System.out.println("SET FAVORITE GENRES: "+ listGenres[userGenres.get(i)]);
						Util.setFavoriteGenre(UserInformationActivity.this, listGenres[userGenres.get(i)], i);
					}
					finish();
				}
			}
		});

	}

	private void updateSendButton (String sex, int spinnerAgeSelected)
	{
		if (sex != "Undefined" && spinnerAgeSelected != 0 && taskIsFinished) {
			save.setEnabled(true);
		}else{
			save.setEnabled(false);
		}
	}

	@Override
	public void onBackPressed() {
		// super.onBackPressed(); commented this line in order to disable back press
	}

}