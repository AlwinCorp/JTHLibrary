package se.hj.doelibs.mobile;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpException;
import se.hj.doelibs.api.LoanableDao;
import se.hj.doelibs.api.exception.HttpBadRequestException;
import se.hj.doelibs.mobile.asynctask.TaskCallback;
import se.hj.doelibs.mobile.codes.ExtraKeys;
import se.hj.doelibs.mobile.utils.ProgressDialogUtils;

/**
 * Activity to add a new loanable
 *
 * @author Christoph
 */
public class AddLoanableActivity extends BaseActivity {

	private int titleId;

	private EditText txtDelibsId;
	private EditText txtLocationCategory;
	private EditText txtRoom;
	private Button btnSave;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View contentView = inflater.inflate(R.layout.activity_add_loanable, null, false);
		drawerLayout.addView(contentView, 0);

		//set titleId
		titleId = getIntent().getIntExtra(ExtraKeys.TITLE_ID, -1);

		if(titleId <= 0) {
			//no titleId given
			Toast.makeText(this, R.string.add_loanable_no_title, Toast.LENGTH_LONG);
			finish();
		}

		//setup elements
		txtDelibsId = (EditText)findViewById(R.id.txt_doelibs_identifier);
		txtLocationCategory = (EditText)findViewById(R.id.txt_location_category);
		txtRoom = (EditText)findViewById(R.id.txt_room);
		btnSave = (Button)findViewById(R.id.btn_add_loanable);
		TextView tv_doeid = (TextView)findViewById(R.id.add_loanable_identifier);
		TextView tv_doeCat = (TextView)findViewById(R.id.add_loanable_category);
		TextView tv_doeRoom = (TextView)findViewById(R.id.add_loanable_room);

		Typeface novaThin = Typeface.createFromAsset(getAssets(), "fonts/Proxima Nova Thin.otf");
		Typeface novaLight = Typeface.createFromAsset(getAssets(), "fonts/Proxima Nova Alt Condensed Light.otf");
		txtDelibsId.setTypeface(novaLight);
		txtLocationCategory.setTypeface(novaLight);
		txtRoom.setTypeface(novaLight);
		tv_doeCat.setTypeface(novaThin);
		tv_doeid.setTypeface(novaThin);
		tv_doeRoom.setTypeface(novaThin);
		btnSave.setTypeface(novaLight);

	}

	public void onSave(View view) {
		String doeLibsId = txtDelibsId.getText().toString();
		String locCat = txtLocationCategory.getText().toString();
		String room = txtRoom.getText().toString();

		if(doeLibsId.equals("") || locCat.equals("") || room.equals("")) {
			Toast.makeText(AddLoanableActivity.this, R.string.all_fields_required, Toast.LENGTH_LONG).show();
			return;
		}

		//add
		new AddLoanableAsyncTask(titleId, doeLibsId, locCat, room, new TaskCallback<HttpException>() {
			private ProgressDialog dialog;

			@Override
			public void onTaskCompleted(HttpException ex) {
				ProgressDialogUtils.dismissQuitely(dialog);

				if(ex == null) {
					//everything ok --> go back
					Toast.makeText(AddLoanableActivity.this, R.string.add_loanable_successfull, Toast.LENGTH_SHORT);
					finish();
				} else {
					if(ex instanceof HttpBadRequestException) {
						//invalid data
						Log.d("Add loanable", "BadRequest", ex);
						Toast.makeText(AddLoanableActivity.this, R.string.add_loanable_doelibsid_already_in_use, Toast.LENGTH_LONG).show();
					} else {
						//servererror, not authorized...
						Log.d("Add loanable", "response was: " + ex.getMessage());
						Toast.makeText(AddLoanableActivity.this, R.string.add_loanable_error, Toast.LENGTH_SHORT).show();
					}
				}
			}

			@Override
			public void beforeTaskRun() {
				dialog = new ProgressDialog(AddLoanableActivity.this);
				dialog.setMessage(getResources().getText(R.string.dialog_progress_add_loanable));
				dialog.setCancelable(false);
				dialog.show();
			}
		}).execute();


		//redirect to titleDetails (go back, because user could come from search or from my borrows in split screen mode)
	}


	/**
	 * Background task to add a new loanable for a title
	 */
	private class AddLoanableAsyncTask extends AsyncTask<Void, Void, HttpException> {

		private int titleId;
		private String doeLibSId;
		private String locationCategory;
		private String room;
		private TaskCallback<HttpException> callback;

		public AddLoanableAsyncTask(int titleId, String doeLibsId, String locationCategory, String room, TaskCallback<HttpException> callback) {
			this.titleId = titleId;
			this.doeLibSId = doeLibsId;
			this.locationCategory = locationCategory;
			this.room = room;
			this.callback = callback;
		}

		@Override
		protected HttpException doInBackground(Void... params) {
			LoanableDao loanableDao = new LoanableDao(getCredentials());

			try {
				loanableDao.addLoanableBasic(titleId, doeLibSId, locationCategory, room);
			} catch (HttpException e) {
				return e;
			}

			return null;
		}


		@Override
		protected void onPostExecute(HttpException ex) {
			callback.onTaskCompleted(ex);
		}

		@Override
		protected void onPreExecute() {
			callback.beforeTaskRun();
		}
	}
}