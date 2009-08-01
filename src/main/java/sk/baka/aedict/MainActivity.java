/**
 *     Aedict - an EDICT browser for Android
 Copyright (C) 2009 Martin Vysny
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package sk.baka.aedict;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Provides means to search the edict dictionary file.
 * 
 * @author Martin Vysny
 */
public class MainActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			JpUtils.initialize(getClassLoader());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		setContentView(R.layout.main);
		final Button jpSearch = (Button) findViewById(R.id.jpSearch);
		final EditText jpSearchEdit = (EditText) findViewById(R.id.jpSearchEdit);
		jpSearch.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				final SearchQuery q = new SearchQuery();
				q.isJapanese = true;
				q.query = jpSearchEdit.getText().toString();
				performSearch(q);
			}

		});
		final Button engSearch = (Button) findViewById(R.id.engSearch);
		final EditText engSearchEdit = (EditText) findViewById(R.id.engSearchEdit);
		engSearch.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				final SearchQuery q = new SearchQuery();
				q.isJapanese = false;
				q.query = engSearchEdit.getText().toString();
				performSearch(q);
			}

		});
	}

	private void performSearch(final SearchQuery query) {
		final Intent intent = new Intent(this, ResultActivity.class);
		query.putTo(intent);
		startActivity(intent);
	}
}