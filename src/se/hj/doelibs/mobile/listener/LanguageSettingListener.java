package se.hj.doelibs.mobile.listener;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import se.hj.doelibs.LanguageManager;
import se.hj.doelibs.mobile.R;
import se.hj.doelibs.mobile.codes.PreferencesKeys;

/**
 * @author Adrien SAUNIER
 */
public class LanguageSettingListener implements OnItemSelectedListener{

	Context applicationContext;
	Activity parent;
	
	public LanguageSettingListener(Activity currentActivity) {
		this.applicationContext = currentActivity.getApplicationContext();
		this.parent = currentActivity;
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		
		Resources res = this.applicationContext.getResources();
		
		String languageCode = res.getStringArray(R.array.languages_code)[position];		
		SharedPreferences prefs = parent.getContext().getSharedPreferences(PreferencesKeys.NAME_MAIN_SETTINGS, this.parent.MODE_PRIVATE);
		
		/**
		 * Saving the application language
		 */
		if( ! prefs.getString(PreferencesKeys.KEY_APPLICATION_LANGUAGE, "").equals(languageCode)) {
			
			LanguageManager.setPreferedLanguage(this.applicationContext, languageCode);
			LanguageManager.setApplicationLanguage(this.applicationContext, languageCode);

			this.parent.recreate();
		}
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		
	}
	
}
