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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Contains several useful methods which helps activity testing.
 * 
 * @author Martin Vysny
 */
public class ActivityTestHelper<T extends Activity> extends ActivityUnitTestCase<T> {

	private final Class<T> activityClass;

	public ActivityTestHelper(Class<T> activityClass) {
		super(activityClass);
		this.activityClass = activityClass;
		AedictApp.isInstrumentation = true;
	}

	/**
	 * Starts the activity and asserts that it is really started.
	 */
	protected void startActivity(final Intent intent) {
		final T activity = startActivity(intent, null, null);
		assertSame(activity, getActivity());
		assertTrue(activityClass.isInstance(activity));
	}

	/**
	 * Starts the activity and asserts that it is really started.
	 */
	protected void startActivity() {
		startActivity(new Intent(getInstrumentation().getContext(), activityClass));
	}

	/**
	 * Asserts that the current activity requested start of given activity.
	 * 
	 * @param activity
	 *            the new activity
	 */
	protected void assertStartedActivity(final Class<? extends Activity> activity) {
		final Intent i = getStartedActivityIntent();
		if (i == null) {
			throw new AssertionError("The activity did not requested a start of another activity yet");
		}
		assertEquals(activity.getName(), i.getComponent().getClassName());
	}

	/**
	 * Sets text of given {@link TextView}.
	 * 
	 * @param textViewId
	 *            the text view ID
	 * @param text
	 *            the text to set
	 */
	protected void setText(final int textViewId, final String text) {
		((TextView) getActivity().findViewById(textViewId)).setText(text);
	}

	/**
	 * Asserts that the text of given {@link TextView} is as expected.
	 * 
	 * @param textViewId
	 *            the text view ID
	 * @param text
	 *            the text to expect
	 */
	protected void assertText(final int textViewId, final String text) {
		assertEquals(text, ((TextView) getActivity().findViewById(textViewId)).getText());
	}

	/**
	 * Asserts that the text of given {@link TextView} is as expected.
	 * 
	 * @param textViewId
	 *            the text view ID
	 * @param stringId
	 *            the text to expect. References strings from the application,
	 *            not from the test module.
	 */
	protected void assertText(final int textViewId, final int stringId) {
		assertText(textViewId, getActivity().getString(stringId));
	}

	/**
	 * Clicks on given {@link Button}.
	 * 
	 * @param buttonId
	 *            the button id
	 */
	protected void click(final int buttonId) {
		((Button) getActivity().findViewById(buttonId)).performClick();
	}

	/**
	 * Checks whether given {@link CompoundButton} is checked.
	 * @param expected the expected check state
	 * @param checkboxId the button ID
	 */
	protected void assertChecked(final boolean expected, final int checkboxId) {
		assertEquals(expected, ((CompoundButton) getActivity().findViewById(checkboxId)).isChecked());
	}

	/**
	 * Activates an option menu.
	 * 
	 * @param itemId
	 *            the menu item ID.
	 */
	protected void activateOptionsMenu(final int itemId) {
		final MenuItem item = (MenuItem) Proxy.newProxyInstance(getActivity().getClassLoader(), new Class<?>[] { MenuItem.class }, new InvocationHandler() {

			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (!method.getName().equals("getItemId")) {
					throw new IllegalArgumentException("Unexpected method call: " + method);
				}
				return itemId;
			}

		});
		assertTrue("The handler did not handled the item. Nothing happened.", getActivity().onOptionsItemSelected(item));
	}

	/**
	 * Sets focus on given view.
	 * 
	 * @param view
	 *            the view to focus.
	 */
	protected void focus(final View view) {
		if (view.isFocused()) {
			return;
		}
		if (!view.isFocusable()) {
			throw new AssertionError("The view " + view.getId() + " is not focusable");
		}
		if (!view.requestFocus()) {
			throw new AssertionError("The view " + view.getId() + " did not took the focus");
		}
	}

	/**
	 * Opens and activates context menu item for given view. Fails if the view
	 * does not provide a context menu.
	 * 
	 * @param view
	 *            the view
	 * @param menuId
	 *            the menu item ID to activate.
	 */
	protected void contextMenu(final View view, final int menuId) {
		// view.performLongClick() does not work for ListView as the
		// ContextMenuInfo parameter is null
		if (view instanceof AbsListView) {
			throw new RuntimeException("Use the other contextMenu function for listview");
		}
		focus(view);
		if (!getInstrumentation().invokeContextMenuAction(getActivity(), menuId, 0)) {
			throw new AssertionError("Activation of menu item " + menuId + " failed for view " + view.getId() + " for unknown reasons. Check that there is menu item with such ID in the menu and it is enabled");
		}
	}

	/**
	 * Opens and activates context menu item for given ListView. Fails if the
	 * view does not provide a context menu.
	 * 
	 * @param view
	 *            the view
	 * @param menuId
	 *            the menu item ID which is to be activated. If the ID was not
	 *            assigned then the automatic ID generation is employed,
	 *            starting at 10000.
	 * @param item
	 *            the ListView item ordinal to click.
	 */
	protected void contextMenu(final AbsListView view, final int menuId, final int item) {
		try {
			final Field m = View.class.getDeclaredField("mOnCreateContextMenuListener");
			m.setAccessible(true);
			final OnCreateContextMenuListener listener = (OnCreateContextMenuListener) m.get(view);
			final ContextMenuHandler handler = new ContextMenuHandler();
			final ContextMenu menu = (ContextMenu) Proxy.newProxyInstance(getActivity().getClassLoader(), new Class<?>[] { ContextMenu.class }, handler);
			// the trick here is to force the ListView to create context menu on
			// our hacky ContextMenu, which then traces the listeners.
			listener.onCreateContextMenu(menu, null, new AdapterContextMenuInfo(null, item, 0));
			handler.click(menuId);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private class ContextMenuHandler implements InvocationHandler {
		private final Map<Integer, MenuItem.OnMenuItemClickListener> listeners = new HashMap<Integer, MenuItem.OnMenuItemClickListener>();
		private int autogeneratedId = 10000;

		public boolean click(final int id) {
			return listeners.get(id).onMenuItemClick(null);
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("add")) {
				final int id;
				if (method.getParameterTypes().length == 4) {
					id = (Integer) args[1];
				} else {
					id = autogeneratedId++;
				}
				return Proxy.newProxyInstance(getActivity().getClassLoader(), new Class<?>[] { MenuItem.class }, new InvocationHandler() {

					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if (method.getName().equals("setOnMenuItemClickListener")) {
							listeners.put(id, (OnMenuItemClickListener) args[0]);
						}
						return proxy;
					}

				});
			}
			if (ContextMenu.class.isAssignableFrom(method.getReturnType())) {
				return proxy;
			}
			return null;
		}
	}

	/**
	 * Returns intent set via invocation to the
	 * {@link Activity#setResult(int, Intent)} call. Fails if no such intent was
	 * set.
	 * 
	 * @return intent, never null.
	 */
	protected Intent getResultIntent() {
		try {
			final Field f = Activity.class.getDeclaredField("mResultData");
			f.setAccessible(true);
			final Intent result = (Intent) f.get(getActivity());
			if (result == null) {
				throw new AssertionError("result intent was not set via the setResult() call");
			}
			return result;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
