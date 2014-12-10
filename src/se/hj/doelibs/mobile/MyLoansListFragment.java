package se.hj.doelibs.mobile;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
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
import se.hj.doelibs.mobile.listadapter.LoansAndReservationsListAdapter;
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
	private ListView list_myLoansAndReservations;
	private OnTitleItemSelectedListener listener;
	private ProgressDialog loadLoansDialog;
	private ProgressDialog loadReservationsDialog;
	private Typeface novaLight;
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


		this.novaLight = Typeface.createFromAsset(view.getResources().getAssets(), "fonts/Proxima Nova Thin.otf");

		list_myLoansAndReservations = (ListView)view.findViewById(R.id.list_my_loans_and_reservations);
		list_myLoansAndReservations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (ConnectionUtils.isConnected(getActivity())) {
					Object itemAtPosition = list_myLoansAndReservations.getItemAtPosition(position);
					if(itemAtPosition == null) {
						//do nothing
					} else if (itemAtPosition instanceof Loan) {
						listener.onTitleItemSelected(((Loan) itemAtPosition).getLoanable().getTitle().getTitleId());
					} else if (itemAtPosition instanceof Reservation) {
						listener.onTitleItemSelected(((Reservation) itemAtPosition).getTitle().getTitleId());
					} else {
						Log.w("MyLoansList", "unknow item type: " + itemAtPosition.getClass());
					}
				} else {
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
			//load loans and reservations
			TaskCallback<LoansAndReservationsListAdapter> loadCallback = new TaskCallback<LoansAndReservationsListAdapter>() {
				ProgressDialog progressDialog;

				@Override
				public void onTaskCompleted(LoansAndReservationsListAdapter objectOnComplete) {
					ProgressDialogUtils.dismissQuitely(progressDialog);

					list_myLoansAndReservations.setAdapter(objectOnComplete);

					//on tablets in landscape mode load first title in title details fragment:
					if(getResources().getConfiguration().isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
							&& getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						if(objectOnComplete.getCount() > 2) {
							Object firstObject = objectOnComplete.getItem(1);
							if(firstObject instanceof Loan) {
								listener.onTitleItemSelected(((Loan)firstObject).getLoanable().getTitle().getTitleId());
							} else if(firstObject instanceof Reservation) {
								listener.onTitleItemSelected(((Reservation) firstObject).getTitle().getTitleId());
							} else {
								firstObject = objectOnComplete.getItem(2);
								if(firstObject instanceof Reservation) {
									listener.onTitleItemSelected(((Reservation)firstObject).getTitle().getTitleId());
								}
							}
						}
					}

				}

				@Override
				public void beforeTaskRun() {
					progressDialog = new ProgressDialog(activity);
					progressDialog.setMessage(getResources().getText(R.string.dialog_progress_load_reservations_and_data));
					progressDialog.setCancelable(false);
					progressDialog.show();
				}
			};

			TaskCallback<Boolean> checkInCallback = new TaskCallback<Boolean>() {
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
			};


			new LoadUsersLoansAndReservationsAsyncTask(getActivity(), loadCallback, checkInCallback).execute();
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
	 * task to load the users loans and reservations and create a listadapter
	 */
	private class LoadUsersLoansAndReservationsAsyncTask extends AsyncTask<Void, Void, LoansAndReservationsListAdapter> {

		private Activity activity;
		private TaskCallback<LoansAndReservationsListAdapter> duringRunCallback;
		private TaskCallback<Boolean> checkInCallback;

		public LoadUsersLoansAndReservationsAsyncTask(Activity activity, TaskCallback<LoansAndReservationsListAdapter> duringRunCallback, TaskCallback<Boolean> checkInCallback) {
			this.activity = activity;
			this.duringRunCallback = duringRunCallback;
			this.checkInCallback = checkInCallback;
		}

		@Override
		protected LoansAndReservationsListAdapter doInBackground(Void... params) {
			LoansAndReservationsListAdapter adapter;

			List<Loan> loans = null;
			List<Reservation> reservations = null;

			if (ConnectionUtils.isConnected(getActivity().getBaseContext())) {
				LoanDao loanDao = new LoanDao(CurrentUserUtils.getCredentials(activity));
				ReservationDao reservationDao = new ReservationDao(CurrentUserUtils.getCredentials(activity));

				loans = loanDao.getCurrentUsersLoans();
				reservations = reservationDao.getCurrentUsersReservations();

				fileUtils.writeReservationToFile(reservationFile, getActivity().getBaseContext(), reservations);
				fileUtils.writeLoansToFile(loansFile, getActivity().getBaseContext(), loans);
			} else {
				reservations = fileUtils.readReservationFromFile(reservationFile, getActivity().getBaseContext());
				loans = fileUtils.readLoansFromFile(loansFile, getActivity().getBaseContext());
			}

			adapter = new LoansAndReservationsListAdapter(activity, loans, reservations, checkInCallback);

			return adapter;
		}

		@Override
		protected void onPreExecute() {
			duringRunCallback.beforeTaskRun();
		}

		@Override
		protected void onPostExecute(LoansAndReservationsListAdapter result) {
			duringRunCallback.onTaskCompleted(result);
		}
	}

}