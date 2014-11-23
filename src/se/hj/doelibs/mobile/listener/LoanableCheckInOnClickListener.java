package se.hj.doelibs.mobile.listener;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import se.hj.doelibs.mobile.R;
import se.hj.doelibs.mobile.TitleDetailsActivity;
import se.hj.doelibs.mobile.asynctask.CheckInAsyncTask;
import se.hj.doelibs.mobile.asynctask.TaskCallback;
import se.hj.doelibs.mobile.codes.ExtraKeys;

/**
 * @author Christoph
 */
public class LoanableCheckInOnClickListener implements View.OnClickListener {

	private int titleId;
	private int loanableId;
	private Context context;

	public LoanableCheckInOnClickListener(int titleId, int loanableId, Context context) {
		this.titleId = titleId;
		this.loanableId = loanableId;
		this.context = context;
	}

	@Override
	public void onClick(final View v) {
		final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(v.getContext());
		dialogBuilder
				.setMessage(R.string.dialog_really_checkin_loanable)
				.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						checkInLoanable();
					}
				})
				.setNegativeButton(R.string.CANCEL, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//nothing
					}
				});
		dialogBuilder.show();
	}

	/**
	 * checks the loanable in again
	 */
	private void checkInLoanable() {
		final ProgressDialog progressDialog = new ProgressDialog(context);

		new CheckInAsyncTask(context, loanableId, new TaskCallback<Boolean>() {
			@Override
			public void onTaskCompleted(Boolean checkInSuccess) {
				progressDialog.hide();

				if(checkInSuccess) {
					Toast.makeText(context, context.getResources().getText(R.string.loanable_checkin_successfull), Toast.LENGTH_SHORT).show();
					//reload title activity
					Intent titleActivity = new Intent(context, TitleDetailsActivity.class);
					titleActivity.putExtra(ExtraKeys.TITLE_ID, titleId);
					context.startActivity(titleActivity);
				} else {
					Toast.makeText(context, context.getResources().getText(R.string.loanable_checkin_error), Toast.LENGTH_LONG).show();
				}
			}

			@Override
			public void beforeTaskRun() {
				progressDialog.setMessage(context.getResources().getText(R.string.dialog_progress_checkin_loanable));
				progressDialog.setCancelable(false);
				progressDialog.show();
			}
		}).execute();
	}

}
