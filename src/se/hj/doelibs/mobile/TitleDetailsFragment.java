package se.hj.doelibs.mobile;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import se.hj.doelibs.mobile.asynctask.LoadReservationStatusAsyncTask;
import se.hj.doelibs.mobile.asynctask.LoadTitleInformationAsynTaks;
import se.hj.doelibs.mobile.asynctask.ReserveTitleAsyncTask;
import se.hj.doelibs.mobile.asynctask.TaskCallback;
import se.hj.doelibs.mobile.codes.ExtraKeys;
import se.hj.doelibs.mobile.codes.RequestCodes;
import se.hj.doelibs.mobile.listadapter.LoanablesListAdapter;
import se.hj.doelibs.mobile.utils.CurrentUserUtils;
import se.hj.doelibs.mobile.utils.ListUtils;
import se.hj.doelibs.mobile.utils.ProgressDialogUtils;
import se.hj.doelibs.model.Author;
import se.hj.doelibs.model.Loan;
import se.hj.doelibs.model.Title;

import java.util.List;

/**
 * Fragment to show title details.
 *
 * @author Christoph
 */
public class TitleDetailsFragment extends Fragment {

	private int titleId;

	private TextView tv_title;
	private TextView tv_edition;

	private TextView tv_categories;
	private TextView tv_publisher;
	private TextView tv_authors;
	private TextView tv_noLoanablesAvailable;

	private Button btn_reserve;
	private ListView lv_loanables;

	private ProgressDialog reserveProgressDialog;
	private ProgressDialog loadReservationDetailsProgressDialog;
	private ProgressDialog loadTitleDetailProgressDialog;
	private ProgressDialog checkInAndOutProgressDialog;

	private TextView tv_categories_header;
	private TextView tv_publisher_header;
	private TextView tv_authors_header;
	private TextView tv_loanables;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_title_details, container, false);

		tv_title = (TextView)view.findViewById(R.id.tv_titledetails_titlename);
		tv_edition = (TextView)view.findViewById(R.id.tv_titledetails_edition);
		tv_categories = (TextView)view.findViewById(R.id.tv_titledetails_categories);
		tv_publisher = (TextView)view.findViewById(R.id.tv_titledetails_publisher);
		tv_authors = (TextView)view.findViewById(R.id.tv_titledetails_authors);
		btn_reserve = (Button)view.findViewById(R.id.btn_titledetails_reserve);
		lv_loanables = (ListView)view.findViewById(R.id.lv_titledetails_loanableslist);
		tv_noLoanablesAvailable = (TextView) view.findViewById(R.id.tv_no_loanables_available);


		tv_authors_header = (TextView) view.findViewById(R.id.tv_titledetails_authors_header);
		tv_publisher_header = (TextView) view.findViewById(R.id.tv_titledetails_publisher_header);
		tv_categories_header = (TextView) view.findViewById(R.id.tv_titledetails_categories_header);
		tv_loanables = (TextView) view.findViewById(R.id.tv_titledetails_loanables);

		Typeface novaThin = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Proxima Nova Alt Condensed Thin.otf");
		Typeface novaLight = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Proxima Nova Alt Condensed Light.otf");

		tv_title.setTypeface(novaThin);
		tv_edition.setTypeface(novaLight);
		tv_categories.setTypeface(novaLight);
		tv_publisher.setTypeface(novaLight);
		tv_authors.setTypeface(novaLight);
		tv_noLoanablesAvailable.setTypeface(novaLight);
		tv_categories_header.setTypeface(novaLight);
		tv_publisher_header.setTypeface(novaLight);
		tv_authors_header.setTypeface(novaLight);
		tv_loanables.setTypeface(novaThin);
		btn_reserve.setTypeface(novaLight);

		setHasOptionsMenu(true);

		return view;
	}

	public void setTitleId(int titleId) {
		this.titleId = titleId;
	}

	/**
	 * loads all data into the viewfields.
	 */
	public void setupView(){
		// by default the relative layout will be shown with the a error message that no title is selected.
		// But at this point a titleId was already given --> hide message
		RelativeLayout errorPannel = (RelativeLayout)getView().findViewById(R.id.title_details_no_title_selected);
		errorPannel.setVisibility(View.GONE);

		getActivity().invalidateOptionsMenu();
		setupReserveButton();
		setupData();
	}

	/**
	 * returns the callback after the checkIn loanable
	 * this will only make some UI changes, because it shouldn't be done in the onClick listener
	 * @return
	 */
	private TaskCallback<Boolean> getAfterCheckInTaskCallback() {
		return new TaskCallback<Boolean>() {

			@Override
			public void onTaskCompleted(Boolean checkInSuccess) {
				ProgressDialogUtils.dismissQuitely(checkInAndOutProgressDialog);
				if(checkInSuccess) {
					Toast.makeText(getActivity(), getResources().getText(R.string.loanable_checkin_successfull), Toast.LENGTH_SHORT).show();

					//reload title activity
					getActivity().finish();
					getActivity().startActivity(getActivity().getIntent());
				} else {
					Toast.makeText(getActivity(), getResources().getText(R.string.loanable_checkin_error), Toast.LENGTH_LONG).show();
				}
			}

			@Override
			public void beforeTaskRun() {
				checkInAndOutProgressDialog = new ProgressDialog(getActivity());
				checkInAndOutProgressDialog.setMessage(getResources().getText(R.string.dialog_progress_checkin_loanable));
				checkInAndOutProgressDialog.setCancelable(false);
				checkInAndOutProgressDialog.show();
			}
		};
	}

	/**
	 * returns the callback which will be called after a checkOut of a loanable
	 * this will only make some UI changes, because it shouldn't be done in the onClick listener
	 * @return
	 */
	private TaskCallback<Loan> getAfterCheckOutTaskCallback() {
		return new TaskCallback<Loan>() {
			@Override
			public void onTaskCompleted(Loan loan) {
				ProgressDialogUtils.dismissQuitely(checkInAndOutProgressDialog);

				if(loan != null) {
					Toast.makeText(getActivity(), getResources().getText(R.string.loanable_checkout_successfull), Toast.LENGTH_SHORT).show();
					//reload title activity
					getActivity().finish();
					getActivity().startActivity(getActivity().getIntent());
//					Intent titleActivity = new Intent(getActivity(), TitleDetailsActivity.class);
//					titleActivity.putExtra(ExtraKeys.TITLE_ID, titleId);
//					//finish();
//					startActivity(titleActivity);
				} else {
					Toast.makeText(getActivity(), getResources().getText(R.string.loanable_checkout_error), Toast.LENGTH_LONG).show();
				}
			}

			@Override
			public void beforeTaskRun() {
				checkInAndOutProgressDialog = new ProgressDialog(getActivity());

				checkInAndOutProgressDialog.setMessage(getResources().getText(R.string.dialog_progress_checkout_loanable));
				checkInAndOutProgressDialog.setCancelable(false);
				checkInAndOutProgressDialog.show();
			}
		};
	}

	/**
	 * loads all the data of the book and puts it into the view
	 */
	private void setupData() {
		new LoadTitleInformationAsynTaks(getActivity(), this.titleId, new TaskCallback<Title>() {
			@Override
			public void onTaskCompleted(Title title) {
				if(!TitleDetailsFragment.this.isAdded()) {
					//in case the screen gets rotated during the async load (http://stackoverflow.com/questions/10919240/fragment-myfragment-not-attached-to-activity)
					return;
				}

				//setup the views
				tv_title.setText(title.getBookTitle());
				tv_edition.setText("Edition " + title.getEditionNumber());
				tv_publisher.setText(title.getPublisher().getName() + " (" + title.getEditionYear() + ")");
				tv_categories.setText(ListUtils.implode(title.getTopics(), ", "));
				tv_authors.setText(createAuthorEditorString(title.getAuthors(), title.getEditors()));

				if(title.getLoanables() != null && title.getLoanables().size()>0) {
					lv_loanables.setAdapter(new LoanablesListAdapter(getActivity(), title.getLoanables(), getAfterCheckOutTaskCallback(), getAfterCheckInTaskCallback()));
					tv_noLoanablesAvailable.setVisibility(View.INVISIBLE);
					lv_loanables.setVisibility(View.VISIBLE);
				} else {
					tv_noLoanablesAvailable.setText(getResources().getText(R.string.no_loanables_available));
					tv_noLoanablesAvailable.setVisibility(View.VISIBLE);
					lv_loanables.setVisibility(View.INVISIBLE);
				}


				//hide progressbar
				ProgressDialogUtils.dismissQuitely(loadTitleDetailProgressDialog);
			}

			@Override
			public void beforeTaskRun() {
				//setup a progressdialog
				loadTitleDetailProgressDialog = new ProgressDialog(getActivity());
				loadTitleDetailProgressDialog.setMessage(getResources().getText(R.string.dialog_progress_load_titleinformation));
				loadTitleDetailProgressDialog.setCancelable(false);
				loadTitleDetailProgressDialog.show();
			}
		}).execute();
	}

	/**
	 * sets up the reserve button: depending if the user is logged in, has no current loans with this title and no reservation for this title it will display the button
	 */
	private void setupReserveButton() {
		if(CurrentUserUtils.getCredentials(getActivity()) != null) {
			new LoadReservationStatusAsyncTask(getActivity(), this.titleId, new TaskCallback<Boolean>() {
				@Override
				public void onTaskCompleted(Boolean showButton) {
					if(showButton) {
						btn_reserve.setVisibility(View.VISIBLE);

						//add onClickListener
						btn_reserve.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								reserveProgressDialog = new ProgressDialog(getActivity());
								final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
								dialogBuilder
										.setMessage(R.string.dialog_really_reserve_title)
										.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
												//reserve title

												new ReserveTitleAsyncTask(getActivity(), titleId, new TaskCallback<Boolean>() {
													@Override
													public void onTaskCompleted(Boolean success) {

														if (success) {
															btn_reserve.setVisibility(View.INVISIBLE);
															Toast.makeText(getActivity(), getResources().getText(R.string.title_reserve_successfull), Toast.LENGTH_SHORT).show();
														} else {
															Toast.makeText(getActivity(), getResources().getText(R.string.title_reserve_error), Toast.LENGTH_LONG).show();
														}
														ProgressDialogUtils.dismissQuitely(reserveProgressDialog);
													}

													@Override
													public void beforeTaskRun() {
														reserveProgressDialog.setMessage(getResources().getText(R.string.dialog_progress_reserve_title));
														reserveProgressDialog.setCancelable(false);
														reserveProgressDialog.show();
													}
												}).execute();

											}
										})
										.setNegativeButton(R.string.CANCEL, new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
												//nothing
											}
										});
								dialogBuilder.show();
							}
						});
					} else {
						btn_reserve.setVisibility(View.INVISIBLE);
					}
					ProgressDialogUtils.dismissQuitely(reserveProgressDialog);
				}

				@Override
				public void beforeTaskRun() {
					//setup a progressdialog
					reserveProgressDialog = new ProgressDialog(getActivity());
					reserveProgressDialog.setMessage(getResources().getText(R.string.dialog_progress_check_reservations));
					reserveProgressDialog.setCancelable(false);
					reserveProgressDialog.show();
				}
			}).execute();

		} else {
			//user is not logged in so hide button
			btn_reserve.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * concats all authors and editors in a string. adds behind each author the postfix "Editor"
	 * @param authors
	 * @param editors
	 * @return
	 */
	private String createAuthorEditorString(List<Author> authors, List<Author> editors) {
		String editorText = getResources().getText(R.string.titledetails_editor_prefix).toString();

		String authorString = ListUtils.implode(authors, ", ");
		String editorString = ListUtils.implode(editors, " ("+editorText+"), ");

		if(editors != null && editors.size() > 0) {
			editorString += " ("+editorText+")";
		}

		return authorString + ((authorString.length()>0 && editorString.length() > 0)?", ":"") + editorString;
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {

		if(getActivity().getIntent().getIntExtra(ExtraKeys.TITLE_ID, -1) > 0
				|| titleId > 0) {
			//show add loanable menu item if user is admin
			CurrentUserUtils.UserModel um = CurrentUserUtils.getCurrentUser(getActivity());
			if(um != null && um.isAdmin()) {
				MenuItem addLoanableMenuItem = (MenuItem)menu.findItem(R.id.action_add_loanable);
				addLoanableMenuItem.setVisible(true);
			}
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_add_loanable:
				Intent addLoanable = new Intent(getActivity(), AddLoanableActivity.class);
				addLoanable.putExtra(ExtraKeys.TITLE_ID, titleId);
				startActivityForResult(addLoanable, RequestCodes.CREATE_LOANABLE); //for result is needed to navigate back through finish()

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == RequestCodes.CREATE_LOANABLE) {
			//reload activity to display new loanables
			setupView();
		}
	}
}