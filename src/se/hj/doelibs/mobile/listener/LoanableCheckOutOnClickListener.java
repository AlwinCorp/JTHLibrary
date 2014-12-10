package se.hj.doelibs.mobile.listener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Toast;
import se.hj.doelibs.mobile.R;
import se.hj.doelibs.mobile.asynctask.CheckOutAsyncTask;
import se.hj.doelibs.mobile.asynctask.TaskCallback;
import se.hj.doelibs.mobile.utils.ConnectionUtils;
import se.hj.doelibs.model.Loan;

/**
 * onclick listener for a checkout button. Checks a loanable out and changes the text of the button again to checkIn
 *
 * @author Christoph
 */
public class LoanableCheckOutOnClickListener implements View.OnClickListener {

	private int titleId;
	private int loanableId;
	private Activity activity;
	private TaskCallback<Loan> checkOutCallback;

	public LoanableCheckOutOnClickListener(int titleId, int loanableId, Activity activity, TaskCallback<Loan> checkOutCallback) {
		this.loanableId = loanableId;
		this.activity = activity;
		this.titleId = titleId;
		this.checkOutCallback = checkOutCallback;
	}

	@Override
	public void onClick(final View v) {
		if(ConnectionUtils.isConnected(activity.getBaseContext())) {
			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(v.getContext());
			dialogBuilder
					.setMessage(R.string.dialog_really_checkout_loanable)
					.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							checkOutLoanable();
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
	 * checks a loanable out
	 */
	private void checkOutLoanable() {
		new CheckOutAsyncTask(activity.getBaseContext(), this.loanableId, checkOutCallback).execute();
	}
}
