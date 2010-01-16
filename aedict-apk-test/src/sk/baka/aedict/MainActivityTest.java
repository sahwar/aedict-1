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

import sk.baka.aedict.dict.MatcherEnum;
import sk.baka.aedict.dict.SearchQuery;
import sk.baka.aedict.kanji.RomanizationEnum;

/**
 * Tests the main activity.
 * 
 * @author Martin Vysny
 */
public class MainActivityTest extends ActivityTestHelper<MainActivity> {

	public MainActivityTest() {
		super(MainActivity.class);
	}

	/**
	 * Tests that a search request is sent when japanese search is requested.
	 */
	public void testSimpleJapanSearch() {
		startActivity();
		setText(R.id.jpSearchEdit, "haha");
		click(R.id.jpSearch);
		assertStartedActivity(ResultActivity.class);
		final SearchQuery q = SearchQuery.fromIntent(getStartedActivityIntent());
		assertEquals(RomanizationEnum.Hepburn.toHiragana("haha"), q.query[1]);
		assertEquals(RomanizationEnum.Hepburn.toKatakana("haha"), q.query[0]);
		assertEquals(2, q.query.length);
		assertTrue(q.isJapanese);
		assertEquals(MatcherEnum.SubstringMatch, q.matcher);
	}

	/**
	 * Tests that a search request is sent when english search is requested.
	 */
	public void testSimpleEnglishSearch() {
		startActivity();
		setText(R.id.engSearchEdit, "mother");
		click(R.id.engExactMatch);
		click(R.id.engSearch);
		assertStartedActivity(ResultActivity.class);
		final SearchQuery q = SearchQuery.fromIntent(getStartedActivityIntent());
		assertEquals("mother", q.query[0]);
		assertEquals(1, q.query.length);
		assertFalse(q.isJapanese);
		assertEquals(MatcherEnum.ExactMatchEng, q.matcher);
	}

	public void testLaunchAboutActivity() {
		startActivity();
		click(R.id.btnAbout);
		assertStartedActivity(AboutActivity.class);
	}
}