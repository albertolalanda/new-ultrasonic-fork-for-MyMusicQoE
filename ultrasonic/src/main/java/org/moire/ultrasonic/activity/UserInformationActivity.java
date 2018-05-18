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

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
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
import org.moire.ultrasonic.util.AdapterGenres;
import org.moire.ultrasonic.util.BackgroundTask;
import org.moire.ultrasonic.util.GenreTitleCheckbox;
import org.moire.ultrasonic.util.TabActivityBackgroundTask;
import org.moire.ultrasonic.util.Util;
import org.moire.ultrasonic.view.GenreAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class UserInformationActivity extends SubsonicTabActivity {

	private static final String TAG = UserInformationActivity.class.getSimpleName();

	private ImageView imgMale;
	private ImageView imgFemale;
	private Spinner spinnerAge;
	private Spinner spinnerGenres;
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
		spinnerGenres = (Spinner) findViewById(R.id.spinner_genres);
		save = (Button) findViewById(R.id.button_save);
		save.setEnabled(false);

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

		//LALANDA SET generos favoritos empty
		GenreTitleCheckbox genreTitleCheckboxDefault = new GenreTitleCheckbox();
		genreTitleCheckboxDefault.setTitle("loading genres ...");
		genreTitleCheckboxDefault.setSelected(true);
		ArrayList<GenreTitleCheckbox> listGenres = new ArrayList<>();
		listGenres.add(genreTitleCheckboxDefault);
		AdapterGenres adapterGenres = new AdapterGenres(UserInformationActivity.this, 0, listGenres);
		spinnerGenres.setAdapter(adapterGenres);
		//

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
				ArrayList<GenreTitleCheckbox> listGenres = new ArrayList<>();

				Collection<String> sectionSet = new LinkedHashSet<String>(30);
				List<Integer> positionList = new ArrayList<Integer>(30);

				GenreTitleCheckbox genreTitleCheckboxDefault = new GenreTitleCheckbox();
				genreTitleCheckboxDefault.setTitle("generos favoritos");
				genreTitleCheckboxDefault.setSelected(true);
				listGenres.add(genreTitleCheckboxDefault);

				for (int i = 0; i < result.size(); i++)
				{
					Genre genre = result.get(i);
					String index = genre.getIndex();
					if (!sectionSet.contains(index))
					{
						sectionSet.add(index);
						positionList.add(i);
					}

					GenreTitleCheckbox genreTitleCheckbox = new GenreTitleCheckbox();
					genreTitleCheckbox.setTitle(genre.getName());
					genreTitleCheckbox.setSelected(false);
					listGenres.add(genreTitleCheckbox);

					AdapterGenres adapterGenres = new AdapterGenres(UserInformationActivity.this, 0, listGenres);
					spinnerGenres.setAdapter(adapterGenres);
					updateSendButton (sex, spinnerAge.getSelectedItemPosition(), spinnerGenres.getAdapter().getCount());

				}
			}
		};
		task.execute();

		spinnerAge.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				updateSendButton (sex, spinnerAge.getSelectedItemPosition(), spinnerGenres.getAdapter().getCount());
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
				updateSendButton (sex, spinnerAge.getSelectedItemPosition(), spinnerGenres.getAdapter().getCount());
			}
		});
		imgFemale.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				imgMale.setImageResource(R.drawable.ic_men_grey);
				imgFemale.setImageResource(R.drawable.ic_women_white);
				sex = "F";
				updateSendButton (sex, spinnerAge.getSelectedItemPosition(), spinnerGenres.getAdapter().getCount());
			}
		});

		save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateSendButton (sex, spinnerAge.getSelectedItemPosition(), spinnerGenres.getAdapter().getCount());
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
				}
			}
		});

	}

	private void updateSendButton (String sex, int spinnerAgeSelected, int spinnerGenresSize)
	{
		if (sex != "Undefined" && spinnerAgeSelected != 0 && spinnerGenresSize != 1) {
			save.setEnabled(true);
		}else{
			save.setEnabled(false);
		}
	}



}