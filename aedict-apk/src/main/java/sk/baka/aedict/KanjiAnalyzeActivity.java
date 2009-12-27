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
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import sk.baka.aedict.AedictApp.Config;
import sk.baka.aedict.dict.DownloadDictTask;
import sk.baka.aedict.dict.EdictEntry;
import sk.baka.aedict.dict.LuceneSearch;
import sk.baka.aedict.dict.SearchQuery;
import sk.baka.aedict.kanji.KanjiUtils;
import sk.baka.aedict.kanji.Radicals;
import sk.baka.aedict.util.SearchUtils;
import sk.baka.autils.AndroidUtils;
import sk.baka.autils.MiscUtils;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
	/**
	 * A list of {@link EdictEntry} with all information filled (radical, stroke
	 * count, etc).
	 */
	public static final String INTENTKEY_ENTRYLIST = "entrylist";
	private List<EdictEntry> model = null;
	/**
	 * The word to analyze. If null then we were simply given a list of
	 * EdictEntry directly.
	 */
	private String word;
	/**
	 * True if we parsed given word on a per-character basis, or a per-word
	 * basis.
	 */
	private boolean isShowingPerCharacter = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		word = getIntent().getStringExtra(INTENTKEY_WORD);
		model = (List<EdictEntry>) getIntent().getSerializableExtra(INTENTKEY_ENTRYLIST);
		if (word == null && model == null) {
			throw new IllegalArgumentException("Both word and entrylist are null");
		}
		setTitle(AedictApp.format(R.string.kanjiAnalysisOf, word != null ? word : EdictEntry.getJapaneseWord(model)));
		if (model == null) {
			try {
				model = analyzeByCharacters(word);
			} catch (IOException e) {
				model = new ArrayList<EdictEntry>();
				model.add(EdictEntry.newErrorMsg("Analysis failed: " + e));
			}
		}
		setListAdapter(newAdapter());
		// check that the KANJIDIC dictionary file is available
		new SearchUtils(this).checkKanjiDic();
	}

	private ArrayAdapter<EdictEntry> newAdapter() {
		final Config cfg = AedictApp.loadConfig();
		return new ArrayAdapter<EdictEntry>(this, R.layout.kanjidetail, model) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					v = getLayoutInflater().inflate(R.layout.kanjidetail, getListView(), false);
				}
				final EdictEntry e = model.get(position);
				((TextView) v.findViewById(android.R.id.text1)).setText(cfg.useRomaji ? cfg.romanization.toRomaji(e.reading) : e.reading);
				final StringBuilder sb = new StringBuilder();
				if (e.radical != null) {
					// TODO mvy: show radicals as images when available?
					sb.append(' ').append(Radicals.getRadicals(e.kanji.charAt(0)));
				}
				if (e.strokes != null) {
					sb.append(" Strokes:").append(e.strokes);
				}
				if (e.skip != null) {
					sb.append(" SKIP:").append(e.skip);
				}
				if (e.grade != null) {
					sb.append(" Grade:").append(e.grade);
				}
				if (sb.length() > 0) {
					sb.replace(0, 1, "\n");
				}
				sb.insert(0, e.english);
				((TextView) v.findViewById(android.R.id.text2)).setText(sb.toString());
				final TextView tv = (TextView) v.findViewById(R.id.kanjiBig);
				tv.setText(e.getJapanese());
				return v;
			}
		};
	}

	private List<EdictEntry> analyzeByCharacters(final String word) throws IOException {
		final List<EdictEntry> result = new ArrayList<EdictEntry>(word.length());
		final LuceneSearch lsEdict = new LuceneSearch(false, AedictApp.getDictionaryLoc());
		try {
			LuceneSearch lsKanjidic = null;
			if (DownloadDictTask.isComplete(DownloadDictTask.LUCENE_INDEX_KANJIDIC)) {
				lsKanjidic = new LuceneSearch(true, null);
			}
			try {
				for (char c : MiscUtils.removeWhitespaces(word).toCharArray()) {
					final boolean isKanji = KanjiUtils.isKanji(c);
					if (!isKanji) {
						result.add(new EdictEntry(String.valueOf(c), String.valueOf(c), ""));
					} else {
						// it is a kanji. search for it in the dictionary.
						final SearchQuery q = SearchQuery.searchForJapanese(String.valueOf(c), true);
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

	private List<EdictEntry> analyzeByWords(final String word) throws IOException {
		final List<EdictEntry> result = new ArrayList<EdictEntry>();
		final LuceneSearch lsEdict = new LuceneSearch(false, AedictApp.getDictionaryLoc());
		try {
			for (final StringTokenizer t = new StringTokenizer(word); t.hasMoreTokens();) {
				String w = t.nextToken();
				while (w.length() > 0) {
					final EdictEntry entry = findLongestWord(w, lsEdict);
					result.add(entry);
					w = w.substring(entry.getJapanese().length());
				}
			}
			return result;
		} finally {
			MiscUtils.closeQuietly(lsEdict);
		}
	}

	/**
	 * Tries to find longest word which is present in the EDICT dictionary. The
	 * search starts with given word, then cuts the last character off, etc.
	 * 
	 * @param word
	 *            the word to analyze
	 * @return longest word found or an entry consisting of the first character
	 *         if we were unable to find nothing
	 * @throws IOException
	 */
	private EdictEntry findLongestWord(final String word, final LuceneSearch edict) throws IOException {
		String w = word;
		while (w.length() > 0) {
			final Collection<? extends EdictEntry> result = EdictEntry.removeInvalid(EdictEntry.tryParseEdict(edict.search(SearchQuery.searchForJapanese(w, true))));
			if (!result.isEmpty()) {
				return result.iterator().next();
			}
			w = w.substring(0, w.length() - 1);
		}
		return new EdictEntry(word.substring(0, 1), "", "");
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

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.removeGroup(0);
		if (word == null) {
			return false;
		}
		if (!isShowingPerCharacter) {
			final MenuItem item = menu.add(0, ANALYZE_CHARACTERS, Menu.NONE, R.string.analyzeCharacters);
			item.setIcon(android.R.drawable.ic_menu_zoom);
		} else {
			final MenuItem item = menu.add(0, ANALYZE_WORDS, Menu.NONE, R.string.analyzeWords);
			item.setIcon(android.R.drawable.ic_menu_search);
		}
		return true;
	}

	private static final int ANALYZE_CHARACTERS = 0;
	private static final int ANALYZE_WORDS = 1;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			switch (item.getItemId()) {
			case ANALYZE_CHARACTERS: {
				model = analyzeByCharacters(word);
				setListAdapter(newAdapter());
				isShowingPerCharacter = true;
			}
				break;
			case ANALYZE_WORDS: {
				model = analyzeByWords(word);
				setListAdapter(newAdapter());
				isShowingPerCharacter = false;
			}
				break;
			}
		} catch (Exception ex) {
			AndroidUtils.handleError(ex, this, this.getClass(), null);
		}
		return true;
	}
}
