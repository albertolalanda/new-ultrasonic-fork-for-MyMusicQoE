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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

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

	ImageView imgMale;
	ImageView imgFemale;
	Spinner spinnerAge;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_information);

		imgMale = (ImageView) findViewById(R.id.image_view_male);
		imgFemale = (ImageView) findViewById(R.id.image_view_female);
		spinnerAge = (Spinner) findViewById(R.id.spinner_age);

		String UserSex = Util.getUserSex(this);

		switch (UserSex) {
			case "M":
				imgMale.setImageResource(R.drawable.ic_men_white);
				imgFemale.setImageResource(R.drawable.ic_women_grey);
				break;
			case "F":
				imgMale.setImageResource(R.drawable.ic_men_grey);
				imgFemale.setImageResource(R.drawable.ic_women_white);
				break;
			default:
				imgMale.setImageResource(R.drawable.ic_men_grey);
				imgFemale.setImageResource(R.drawable.ic_women_grey);
				break;
		}

		imgMale.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				imgMale.setImageResource(R.drawable.ic_men_white);
				imgFemale.setImageResource(R.drawable.ic_women_grey);
				Util.setUserSex(UserInformationActivity.this, 1);
			}
		});
		imgFemale.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				imgMale.setImageResource(R.drawable.ic_men_grey);
				imgFemale.setImageResource(R.drawable.ic_women_white);
				Util.setUserSex(UserInformationActivity.this, 0);
			}
		});

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

		final Spinner spinnerGenres = (Spinner) findViewById(R.id.spinner_genres);

		BackgroundTask<List<Genre>> task = new TabActivityBackgroundTask<List<Genre>>(this, true)
		{
			@Override
			protected List<Genre> doInBackground() throws Throwable
			{
				MusicService musicService = MusicServiceFactory.getMusicService(UserInformationActivity.this);

				List<Genre> genres = new ArrayList<Genre>();

				try
				{
					genres = musicService.getGenres(UserInformationActivity.this, this);
				}
				catch (Exception x)
				{
					Log.e(TAG, "Failed to load genres", x);
					//TODO LALANDA TENTAR DE NOVO OU MANDAR AVISO DE ERRO E FECHAR ???
				}

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
				}
			}
		};
		task.execute();

	}
}