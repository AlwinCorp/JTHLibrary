package se.hj.doelibs.mobile;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import se.hj.doelibs.api.LoanDao;
import se.hj.doelibs.api.ReservationDao;
import se.hj.doelibs.mobile.asynctask.TaskCallback;
import se.hj.doelibs.mobile.listadapter.LoanListAdapter;
import se.hj.doelibs.mobile.listadapter.ReservationListAdapter;
import se.hj.doelibs.mobile.listener.OnTitleItemSelectedListener;
import se.hj.doelibs.mobile.utils.ConnectionUtils;
import se.hj.doelibs.mobile.utils.CurrentUserUtils;
import se.hj.doelibs.mobile.utils.ProgressDialogUtils;
import se.hj.doelibs.model.Loan;
import se.hj.doelibs.model.Reservation;
import se.hj.doelibs.mobile.utils.fileUtils;
import java.util.List;

/**
 * Fragment to show the loans of the user
 *
 * @author Alexander
 */
public class MyLoansListFragment extends Fragment {

	private Activity activity;
	private ListView lv_myLoans;
	private ListView lv_myReservations;
	private OnTitleItemSelectedListener listener;
	private ProgressDialog loadLoansDialog;
	private ProgressDialog loadReservationsDialog;
	private String loansFile = "loans";
	private String reservationFile = "reservations";

	/**
	 * says if a title was already selected and loaded in the title details fragment
	 * this is only on tablets in landscape mode important
	 */
	private boolean titleSelected = false;

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_my_loans, container, false);

		lv_myLoans = (ListView) view.findViewById(R.id.loans_list);
		lv_myReservations = (ListView) view.findViewById(R.id.reservations_list);

		//add on click listener
			lv_myLoans.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if (ConnectionUtils.isConnected(getActivity()))
					{
						Loan clicked = (Loan) lv_myLoans.getItemAtPosition(position);
						listener.onTitleItemSelected(clicked.getLoanable().getTitle().getTitleId());
					}
					else
					{

						Toast.makeText(activity, getText(R.string.generic_not_connected_error), Toast.LENGTH_LONG).show();
					}
				}
			});
			lv_myReservations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if (ConnectionUtils.isConnected(getActivity())) {
						Reservation clicked = (Reservation) lv_myReservations.getItemAtPosition(position);
						listener.onTitleItemSelected(clicked.getTitle().getTitleId());
					}
					else
					{
						Toast.makeText(activity, getText(R.string.generic_not_connected_error), Toast.LENGTH_LONG).show();
					}
				}
			});

		//check if user is logged in
		if(CurrentUserUtils.getCredentials(view.getContext()) == null) {
			Log.d("MyLoans", "user is not logged in");
			Intent loginActivity = new Intent(view.getContext(), LoginActivity.class);
			startActivity(loginActivity);
		} else {
			//load loans
			new LoadUsersLoansAsyncTask(new TaskCallback<List<Loan>>() {
				@Override
				public void onTaskCompleted(List<Loan> loans) {
					ProgressDialogUtils.dismissQuitely(loadLoansDialog);
					lv_myLoans.setAdapter(new LoanListAdapter(activity, loans, new TaskCallback<Boolean>() {
						private ProgressDialog dialog;

						@Override
						public void onTaskCompleted(Boolean checkInSuccessfull) {
							ProgressDialogUtils.dismissQuitely(dialog);

							if (!checkInSuccessfull) {
								Toast.makeText(activity, getText(R.string.loanable_checkin_error), Toast.LENGTH_LONG).show();
							} else {
								activity.finish();
								activity.startActivity(activity.getIntent());
								Toast.makeText(activity, getText(R.string.loanable_checkin_successfull), Toast.LENGTH_SHORT).show();
							}
						}

						@Override
						public void beforeTaskRun() {
							dialog = new ProgressDialog(activity);
							dialog.setMessage(getResources().getText(R.string.dialog_progress_checkin_loanable));
							dialog.setCancelable(false);
							dialog.show();
						}
					}));

					//on tablets in landscape mode load first title in title details fragment:
					if(getResources().getConfiguration().isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
							&& getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						if(loans != null && loans.size() > 0 ) {
							listener.onTitleItemSelected(((Loan)lv_myLoans.getItemAtPosition(0)).getLoanable().getTitle().getTitleId());

							//set title selected
							titleSelected = true;
						}
					}

				}

				@Override
				public void beforeTaskRun() {
					loadLoansDialog = new ProgressDialog(activity);
					loadLoansDialog.setMessage(getResources().getText(R.string.dialog_progress_load_loans));
					loadLoansDialog.setCancelable(false);
					loadLoansDialog.show();
				}
			}).execute();

			//load reservations
			new LoadUsersReservationsAsyncTask(new TaskCallback<List<Reservation>>() {
				@Override
				public void onTaskCompleted(List<Reservation> reservations) {
					ProgressDialogUtils.dismissQuitely(loadReservationsDialog);

					lv_myReservations.setAdapter(new ReservationListAdapter(activity, reservations));

					//on tablets in landscape mode load first title in title details fragment if no title from a loan was loaded:
					if(!titleSelected
							&& getResources().getConfiguration().isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
							&& getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						if(reservations != null && reservations.size() > 0) {
							listener.onTitleItemSelected(((Reservation)lv_myReservations.getItemAtPosition(0)).getTitle().getTitleId());
						}
					}

				}

				@Override
				public void beforeTaskRun() {
					loadReservationsDialog = new ProgressDialog(activity);
					loadReservationsDialog.setMessage(getResources().getText(R.string.dialog_progress_load_reservations));
					loadReservationsDialog.setCancelable(false);
					loadReservationsDialog.show();
				}
			}).execute();

		}

		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;

		if (activity instanceof OnTitleItemSelectedListener) {
			listener = (OnTitleItemSelectedListener) activity;
		} else {
			throw new ClassCastException(activity.toString() + " must implement OnTitleItemSelectedListener");
		}
	}
	/**
	 * task to load the users reservations
	 */
	private class LoadUsersReservationsAsyncTask extends AsyncTask<Void, Void, List<Reservation>> {

		private TaskCallback<List<Reservation>> callback;

		public LoadUsersReservationsAsyncTask(TaskCallback<List<Reservation>> callback) {
			this.callback = callback;
		}

		@Override
		protected List<Reservation> doInBackground(Void... params) {

			if(ConnectionUtils.isConnected(getActivity().getBaseContext())) {
				ReservationDao reservationDao = new ReservationDao(CurrentUserUtils.getCredentials(activity));
				fileUtils.writeReservationToFile(reservationFile, getActivity().getBaseContext(), reservationDao.getCurrentUsersReservations());
				return reservationDao.getCurrentUsersReservations();
			}
			else {
				return fileUtils.readReservationFromFile(reservationFile,getActivity().getBaseContext());
			}

		}

		@Override
		protected void onPreExecute() {
			callback.beforeTaskRun();
		}

		@Override
		protected void onPostExecute(List<Reservation> reservations) {
			callback.onTaskCompleted(reservations);
		}
	}


	/**
	 * task to load the users loans
	 */
	private class LoadUsersLoansAsyncTask extends AsyncTask<Void, Void, List<Loan>> {

		private TaskCallback<List<Loan>> callback;

		public LoadUsersLoansAsyncTask(TaskCallback<List<Loan>> callback) {
			this.callback = callback;
		}

		@Override
		protected List<Loan> doInBackground(Void... params) {

			if(ConnectionUtils.isConnected(getActivity().getBaseContext()) == true) {
				LoanDao loanDao = new LoanDao(CurrentUserUtils.getCredentials(activity));
				//Saves to file
				fileUtils.writeLoansToFile(loansFile,getActivity().getBaseContext(),loanDao.getCurrentUsersLoans());
				return loanDao.getCurrentUsersLoans();
			}
			else
			{
				return fileUtils.readLoansFromFile(loansFile, getActivity().getBaseContext());
			}
		}

		@Override
		protected void onPreExecute() {
			callback.beforeTaskRun();
		}

		@Override
		protected void onPostExecute(List<Loan> loans) {
			callback.onTaskCompleted(loans);
		}
	}

}