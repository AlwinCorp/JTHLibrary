package se.hj.doelibs.mobile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import se.hj.doelibs.mobile.codes.ExtraKeys;
import se.hj.doelibs.mobile.listener.OnTitleItemSelectedListener;
import se.hj.doelibs.mobile.utils.ConnectionUtils;

/**
 * @author Alexander
 * @author Christoph
 */
public class MyLoansActivity extends BaseActivity implements OnTitleItemSelectedListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View contentView = inflater.inflate(R.layout.activity_my_loans, null, false);
		drawerLayout.addView(contentView, 0);
	}

	@Override
	public void onTitleItemSelected(int titleId) {
		TitleDetailsFragment fragment = (TitleDetailsFragment) getFragmentManager().findFragmentById(R.id.detailFragment);

		if (fragment != null && fragment.isInLayout()) {
			if(ConnectionUtils.isConnected(getBaseContext())) {
				fragment.setTitleId(titleId);
				fragment.setupView();
			}
			else {
				TextView BSMOBILE = (TextView) findViewById(R.id.textView);
				BSMOBILE.setText(getText(R.string.generic_not_connected_error));
			}
		} else {
			//fragment was not found in current layout --> it is not on a large device --> start activity with title details
			Intent titleDetailsActivity = new Intent(this, TitleDetailsActivity.class);
			titleDetailsActivity.putExtra(ExtraKeys.TITLE_ID, titleId);
			startActivity(titleDetailsActivity);
		}
	}
}