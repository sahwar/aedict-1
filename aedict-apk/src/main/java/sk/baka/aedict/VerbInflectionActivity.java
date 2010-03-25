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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.baka.aedict.dict.DictEntry;
import sk.baka.aedict.kanji.RomanizationEnum;
import sk.baka.aedict.kanji.VerbInflection;
import sk.baka.aedict.kanji.VerbInflection.Form;
import sk.baka.autils.AndroidUtils;
import sk.baka.autils.DialogUtils;
import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.SimpleExpandableListAdapter;

/**
 * Shows possible verb inflections, with examples.
 * 
 * @author Martin Vysny
 */
public class VerbInflectionActivity extends ExpandableListActivity {
	/**
	 * Expects {@link DictEntry} to be present in the Intent.
	 */
	public static final String INTENTKEY_ENTRY = "entry";
	private DictEntry entry;
	/**
	 * true if romaji is shown instead of katakana/hiragana.
	 */
	private boolean isShowingRomaji;
	/**
	 * true if we are showing only {@link Form#basic} forms.
	 */
	private boolean isShowingBasicOnly = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isShowingRomaji = AedictApp.getConfig().isUseRomaji();
		entry = (DictEntry) getIntent().getSerializableExtra(INTENTKEY_ENTRY);
		buildAndSetAdapter();
		getExpandableListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				final MenuItem miShowRomaji = menu.add(Menu.NONE, 0, 0, isShowingRomaji ? R.string.show_kana : R.string.show_romaji);
				miShowRomaji.setOnMenuItemClickListener(AndroidUtils.safe(VerbInflectionActivity.this, new MenuItem.OnMenuItemClickListener() {

					public boolean onMenuItemClick(MenuItem item) {
						isShowingRomaji = !isShowingRomaji;
						buildAndSetAdapter();
						return true;
					}

				}));
			}
		});
		new DialogUtils(this).showInfoOnce(getClass().getName(), R.string.info, R.string.inflectionWarning);
	}

	private String convertInflectionProduct(final String romaji) {
		final String kana = RomanizationEnum.NihonShiki.toHiragana(romaji);
		return isShowingRomaji ? AedictApp.getConfig().getRomanization().toRomaji(kana) : kana;
	}

	private static final String KEY_JP = "jp";
	private static final String KEY_EN = "en";

	private void buildAndSetAdapter() {
		final boolean isIchidan = entry.isIchidan();
		final RomanizationEnum romanization = !isShowingRomaji ? null : AedictApp.getConfig().getRomanization();
		final List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
		final List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
		// first, add all Base-x inflections
		for (final VerbInflection.AbstractBaseInflector inflector : VerbInflection.INFLECTORS) {
			final Map<String, String> data = new HashMap<String, String>(2);
			data.put(KEY_JP, convertInflectionProduct(inflector.inflect(entry.reading, isIchidan)));
			data.put(KEY_EN, inflector.getName());
			groupData.add(data);
			childData.add(Collections.<Map<String, String>> emptyList());
		}
		// now, add all possible inflections
		for (final VerbInflection.Form form : VerbInflection.Form.values()) {
			// filter out the form if we are showing basic forms only
			if (isShowingBasicOnly && !form.basic) {
				continue;
			}
			if (isIchidan && !form.appliesToIchidan()) {
				// filter out forms not applicable to ichidan verbs
				continue;
			}
			// okay, add it to the list
			Map<String, String> data = new HashMap<String, String>(2);
			data.put(KEY_JP, convertInflectionProduct(form.inflect(RomanizationEnum.NihonShiki.toRomaji(entry.reading), isIchidan)));
			data.put(KEY_EN, getString(form.explanationResId));
			groupData.add(data);
			// add example sentences as a sublist
			final String[][] examples = form.getExamples(this, romanization);
			final List<Map<String, String>> childDataItem = new ArrayList<Map<String, String>>();
			for (final String[] pair : examples) {
				data = new HashMap<String, String>(2);
				data.put(KEY_JP, pair[0]);
				data.put(KEY_EN, pair[1]);
				childDataItem.add(data);
			}
			childData.add(childDataItem);
		}
		// set the adapter
		setListAdapter(new SimpleExpandableListAdapter(this, groupData, R.layout.simple_expandable_list_item_2, new String[] { KEY_JP, KEY_EN }, new int[] { android.R.id.text1, android.R.id.text2 },
				childData, R.layout.simple_expandable_list_item_2, new String[] { KEY_JP, KEY_EN }, new int[] { android.R.id.text1, android.R.id.text2 }));
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.removeGroup(0);
		final MenuItem item2 = menu.add(0, 0, 0, isShowingBasicOnly ? R.string.showAdvanced : R.string.showBasic);
		item2.setIcon(android.R.drawable.ic_menu_add);
		item2.setOnMenuItemClickListener(AndroidUtils.safe(this, new MenuItem.OnMenuItemClickListener() {

			public boolean onMenuItemClick(MenuItem item) {
				isShowingBasicOnly = !isShowingBasicOnly;
				buildAndSetAdapter();
				return true;
			}
		}));
		return true;
	}
}