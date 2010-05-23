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

import java.util.List;

import sk.baka.aedict.dict.KanjidicEntry;
import sk.baka.aedict.dict.TanakaSearchTask;
import sk.baka.aedict.kanji.KanjiUtils;
import sk.baka.aedict.kanji.RomanizationEnum;
import sk.baka.aedict.util.Check;
import sk.baka.aedict.util.Constants;
import sk.baka.aedict.util.SearchClickListener;
import sk.baka.aedict.util.SearchUtils;
import sk.baka.aedict.util.ShowRomaji;
import sk.baka.autils.DialogUtils;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 * Shows a detail of a single Kanji character.
 * 
 * @author Martin Vysny
 */
public class KanjiDetailActivity extends AbstractActivity {
	static final String INTENTKEY_KANJIDIC_ENTRY = "entry";

	/**
	 * Launches this activity.
	 * 
	 * @param activity
	 *            caller activity, not null.
	 * @param entry
	 *            show this entry, not null.
	 */
	public static void launch(final Context activity, final KanjidicEntry entry) {
		Check.checkNotNull("activity", activity);
		Check.checkNotNull("entry", entry);
		final Intent intent = new Intent(activity, KanjiDetailActivity.class);
		intent.putExtra(INTENTKEY_KANJIDIC_ENTRY, entry);
		activity.startActivity(intent);
	}

	private ShowRomaji showRomaji;
	private KanjidicEntry entry;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.kanji_detail);
		showRomaji = new ShowRomaji(this) {

			@Override
			protected void show(boolean romaji) {
				updateContent();
				tanakaSearchTask.updateModel();
			}
		};
		entry = (KanjidicEntry) getIntent().getSerializableExtra(INTENTKEY_KANJIDIC_ENTRY);
		final TextView kanji = (TextView) findViewById(R.id.kanji);
		kanji.setText(entry.kanji);
		new SearchClickListener(this, entry.kanji, true).registerTo(kanji);
		((TextView) findViewById(R.id.stroke)).setText(Integer.toString(entry.strokes));
		((TextView) findViewById(R.id.grade)).setText(entry.grade == null ? "-" : entry.grade.toString());
		((TextView) findViewById(R.id.skip)).setText(entry.skip);
		final Integer jlpt = entry.getJlpt();
		((TextView) findViewById(R.id.jlpt)).setText(jlpt == null ? "-" : jlpt.toString());
		final SearchUtils utils = new SearchUtils(this);
		utils.setupCopyButton(R.id.copy, R.id.kanji);
		((Button) findViewById(R.id.showStrokeOrder)).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				StrokeOrderActivity.launch(KanjiDetailActivity.this, entry.kanji);
			}
		});
		((Button) findViewById(R.id.addToNotepad)).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				NotepadActivity.addAndLaunch(KanjiDetailActivity.this, entry);
			}
		});
		addTextViews(R.id.english, entry.getEnglish(), false, 15);
		updateContent();
		// display hint
		if (!AedictApp.isInstrumentation) {
			new DialogUtils(this).showInfoOnce(Constants.INFOONCE_CLICKABLE_NOTE, R.string.note, R.string.clickableNote);
		}
	}

	private void updateContent() {
		// compute ONYOMI, KUNYOMI, NAMAE and ENGLISH
		addTextViews(R.id.onyomi, entry.getOnyomi(), true, 20);
		addTextViews(R.id.kunyomi, entry.getKunyomi(), true, 20);
		addTextViews(R.id.namae, entry.getNamae(), true, 20);
	}

	private void addTextViews(final int parent, final List<String> items, final boolean isJapanese, float textSize) {
		final ViewGroup p = (ViewGroup) findViewById(parent);
		p.removeAllViews();
		if (items.isEmpty()) {
			p.setVisibility(View.GONE);
			return;
		}
		for (int i = 0; i < items.size(); i++) {
			final String item = items.get(i);
			final String sitem = KanjidicEntry.removeSplits(item);
			final TextView tv = new TextView(p.getContext());
			String text = item + (i == items.size() - 1 ? "" : ", ");
			if (isJapanese) {
				text = showRomaji.romanize(text);
			}
			tv.setText(text);
			final String query = KanjiUtils.isKatakana(sitem.charAt(0)) ? RomanizationEnum.NihonShiki.toHiragana(RomanizationEnum.NihonShiki.toRomaji(sitem)) : sitem;
			new SearchClickListener(this, query, isJapanese).registerTo(tv);
			tv.setTextSize(textSize);
			p.addView(tv);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		showRomaji.register(menu);
		return true;
	}

	private TanakaSearchTask tanakaSearchTask;

	@Override
	protected void onResume() {
		super.onResume();
		showRomaji.onResume();
		if (tanakaSearchTask == null && entry.isValid()) {
			tanakaSearchTask = new TanakaSearchTask(this, (ViewGroup) findViewById(R.id.tanakaExamples), showRomaji, entry.getJapanese());
			tanakaSearchTask.execute(entry.getJapanese());
		}
	}

	@Override
	protected void onStop() {
		if (tanakaSearchTask.cancel(true)) {
			tanakaSearchTask = null;
		}
		super.onStop();
	}

}