package se.hj.doelibs.mobile.listener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Toast;
import se.hj.doelibs.mobile.R;
import se.hj.doelibs.mobile.asynctask.CheckInAsyncTask;
import se.hj.doelibs.mobile.asynctask.TaskCallback;
import se.hj.doelibs.mobile.utils.ConnectionUtils;

/**
 * @author Christoph
 */
public class LoanableCheckInOnClickListener implements View.OnClickListener {

	private int titleId;
	private int loanableId;
	private Activity activity;
	private TaskCallback<Boolean> checkInCallback;

	public LoanableCheckInOnClickListener(int titleId, int loanableId, Activity activity, TaskCallback<Boolean> checkInCallback) {
		this.titleId = titleId;
		this.loanableId = loanableId;
		this.activity = activity;
		this.checkInCallback = checkInCallback;
	}

	@Override
	public void onClick(final View v) {
		if(ConnectionUtils.isConnected(activity.getBaseContext())) {
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
		else
		{
			Toast.makeText(activity, activity.getText(R.string.generic_not_connected_error), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * checks the loanable in again
	 */
	private void checkInLoanable() {
		new CheckInAsyncTask(activity.getBaseContext(), loanableId, checkInCallback).execute();
	}

}
