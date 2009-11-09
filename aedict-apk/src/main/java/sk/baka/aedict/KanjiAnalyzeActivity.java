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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Analyzes each kanji in given word.
 * 
 * @author Martin Vysny
 */
public class KanjiAnalyzeActivity extends ListActivity {
	/**
	 * The string word to analyze.
	 */
	public static final String INTENTKEY_WORD = "word";
	private List<EdictEntry> model = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String word = getIntent().getStringExtra(INTENTKEY_WORD);
		setTitle(AedictApp.format(R.string.kanjiAnalysisOf, word));
		try {
			model = analyze(word);
		} catch (IOException e) {
			model = new ArrayList<EdictEntry>();
			model.add(EdictEntry.newErrorMsg("Analysis failed: " + e));
		}
		setListAdapter(new ArrayAdapter<EdictEntry>(this, R.layout.kanjidetail, model) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					v = getLayoutInflater().inflate(R.layout.kanjidetail, getListView(), false);
				}
				final EdictEntry e = model.get(position);
				e.print((TextView) v.findViewById(android.R.id.text1), (TextView) v.findViewById(android.R.id.text2));
				final TextView tv = (TextView) v.findViewById(R.id.kanjiBig);
				tv.setText(e.getJapanese());
				return v;
			}

		});
	}

	/**
	 * A very simple check for kanji. Works only on a mixture of kanji, katakana
	 * and hiragana.
	 * 
	 * @param c
	 *            the character to analyze.
	 * @return true if it is a kanji, false otherwise.
	 */
	private boolean isKanji(char c) {
		return RomanizationEnum.Hepburn.toRomaji(String.valueOf(c)).charAt(0) == c;
	}

	private List<EdictEntry> analyze(final String word) throws IOException {
		final List<EdictEntry> result = new ArrayList<EdictEntry>(word.length());
		final LuceneSearch lsEdict = new LuceneSearch(false);
		try {
			LuceneSearch lsKanjidic = null;
			if (DownloadEdictTask.isComplete(DownloadEdictTask.LUCENE_INDEX_KANJIDIC)) {
				lsKanjidic = new LuceneSearch(true);
			}
			try {
				for (char c : word.toCharArray()) {
					final boolean isKanji = isKanji(c);
					if (!isKanji) {
						result.add(new EdictEntry(String.valueOf(c), String.valueOf(c), ""));
					} else {
						// it is a kanji. search for it in the dictionary.
						final SearchQuery q = new SearchQuery();
						q.isJapanese = true;
						q.matcher = MatcherEnum.ExactMatchEng;
						q.query = new String[] { String.valueOf(c) };
						List<String> matches = null;
						EdictEntry ee = null;
						if (lsKanjidic != null) {
							matches = lsKanjidic.search(q);
						}
						if (matches != null && !matches.isEmpty()) {
							ee = EdictEntry.tryParseKanjidic(matches.get(0));
						}
						if (ee == null) {
							matches = lsEdict.search(q);
							if (matches.size() > 0) {
								ee = EdictEntry.tryParseEdict(matches.get(0));
							}
						}
						if (ee == null) {
							// no luck. Just add the kanji
							ee = new EdictEntry(String.valueOf(c), "", "");
						}
						result.add(ee);
					}
				}
				return result;
			} finally {
				MiscUtils.closeQuietly(lsKanjidic);
			}
		} finally {
			MiscUtils.closeQuietly(lsEdict);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final EdictEntry e = model.get(position);
		if (!e.isValid()) {
			return;
		}
		final Intent intent = new Intent(this, EntryDetailActivity.class);
		intent.putExtra(EntryDetailActivity.INTENTKEY_ENTRY, e);
		startActivity(intent);
	}
}