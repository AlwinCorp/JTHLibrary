package se.hj.doelibs.mobile.listadapter;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import se.hj.doelibs.mobile.R;
import se.hj.doelibs.mobile.asynctask.TaskCallback;
import se.hj.doelibs.mobile.listener.LoanableCheckInOnClickListener;
import se.hj.doelibs.model.Loan;
import se.hj.doelibs.model.Loanable;
import se.hj.doelibs.model.Reservation;

import java.util.List;

/**
 * Listadapter to handle the list with loans and reservations
 *
 * @author Alexander
 * @author Christoph
 */
public class LoansAndReservationsListAdapter extends BaseAdapter{

	private Activity activity;
	private List<Loan> loans;
	private List<Reservation> reservations;
	private TaskCallback<Boolean> checkInCallback;
	private Typeface novaLight;
	private Typeface novaRegItalic;
	private Typeface novaThin;

	public LoansAndReservationsListAdapter(Activity activity, List<Loan> loans, List<Reservation> reservations, TaskCallback<Boolean> checkInCallback) {
		this.activity = activity;
		this.loans = loans;
		this.reservations = reservations;
		this.checkInCallback = checkInCallback;
		this.novaLight = Typeface.createFromAsset(activity.getAssets(), "fonts/Proxima Nova Alt Condensed Light.otf");
		this.novaRegItalic = Typeface.createFromAsset(activity.getAssets(), "fonts/Proxima Nova Alt Condensed Regular Italic.otf");
		this.novaThin = Typeface.createFromAsset(activity.getAssets(), "fonts/Proxima Nova Thin.otf");
	}

	@Override
	public int getCount() {
		int size = 2; //min size is 2 because we need the header for the loan and reservation headings

		if(loans != null) {
			size += loans.size();
		}

		if(reservations != null) {
			size += reservations.size();
		}

		return size;
	}

	@Override
	public Object getItem(int position) {
		if(position == 0 || (loans != null && position == loans.size() + 1)) {
			//if it is one of the headers
			return null;
		} else {
			if(isPositionInLoanPartOfList(position)){
				return loans.get(position - 1); //-1 becuase the header is always there
			} else {
				if(loans != null) {
					return reservations.get(position - loans.size() - 2);
				} else {
					return reservations.get(position - 2);
				}
			}
		}
	}

	@Override
	public long getItemId(int position) {
		if(position == 0 || (loans != null && position == loans.size() + 1)) {
			//if it is one of the headers
			return 0;
		} else {
			//return the titleId as id

			if(isPositionInLoanPartOfList(position)){
				return ((Loan)loans.get(position)).getLoanable().getTitle().getTitleId();
			} else {
				if(loans != null) {
					return ((Reservation)reservations.get(position - loans.size() - 2)).getTitle().getTitleId();
				} else {
					return ((Reservation)reservations.get(position - 2)).getTitle().getTitleId();
				}
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//check if it is a header
		if(position == 0) {
			return getHeader(activity.getText(R.string.my_loans_MyLoans));
		} else if(loans != null && position == loans.size() + 1) {
			return getHeader(activity.getText(R.string.my_loans_MyReservations));
		} else {
			//it is a "real" item
			if(isPositionInLoanPartOfList(position)) {
				return getLoanRow(loans.get(position - 1));
			} else {
				if(loans != null) {
					return getReservationRow(reservations.get(position - loans.size() - 2));
				} else {
					return getReservationRow(reservations.get(position - 2));
				}
			}
		}
	}

	/**
	 * returns a row for a reservation
	 * @param reservation
	 * @return
	 */
	private View getReservationRow(Reservation reservation) {
		LayoutInflater inflater = activity.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.reservation_list_view_item,null,true);

		TextView header = (TextView)rowView.findViewById(R.id.tv_reservations_header);
		TextView subcontent1 = (TextView)rowView.findViewById(R.id.tv_reservations_info);

		header.setTypeface(novaLight);
		subcontent1.setTypeface(novaRegItalic);

		header.setText(reservation.getTitle().getBookTitle() + " (" + reservation.getTitle().getEditionYear() + ")");
		if(reservation.isLoanRecalled() && reservation.getAvailableDate() != null) {
			subcontent1.setText(activity.getText(R.string.prefix_status) + ": " + Loanable.Status.AVAILABLE.getText(activity));
		} else {
			subcontent1.setText(activity.getText(R.string.prefix_status) + ": " + activity.getText(R.string.reservation_waiting_for_loanable));
		}

		return rowView;
	}

	/**
	 * returns a row for a loan
	 * @param loan
	 * @return
	 */
	private View getLoanRow(Loan loan) {
		LayoutInflater inflater = activity.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.loans_list_view_item, null,true);

		TextView header = (TextView) rowView.findViewById(R.id.tv_loans_header);
		TextView subcontent1 = (TextView) rowView.findViewById(R.id.tv_loans_subcontent1);
		TextView subcontent1Header = (TextView) rowView.findViewById(R.id.tv_loans_subcontent1_header);
		TextView subcontent2 = (TextView) rowView.findViewById(R.id.tv_loans_subcontent2);
		TextView subcontent2Header = (TextView) rowView.findViewById(R.id.tv_loans_subcontent2_header);
		Button button = (Button)rowView.findViewById(R.id.btn_loans_checkIn);

		header.setTypeface(novaLight);
		subcontent1.setTypeface(novaRegItalic);
		subcontent2.setTypeface(novaRegItalic);
		subcontent1Header.setTypeface(novaRegItalic);
		subcontent2Header.setTypeface(novaRegItalic);
		if(button != null)
			button.setTypeface(novaLight);

		String timeGMT = loan.getToBeReturnedDate().toGMTString();

		header.setText(loan.getLoanable().getTitle().getBookTitle() +" ("+ loan.getLoanable().getTitle().getEditionYear()+") ");
		subcontent1.setText(": "+loan.getLoanable().getLocation()+" ("+ loan.getLoanable().getCategory().getName()+") ");
		subcontent2.setText(": "+timeGMT.substring(0, timeGMT.length()-12));

		//the checkIn button will be only on small devices be displayed. on tablets the checkin will be in the title details fragment
		if(button != null) {
			button.setOnClickListener(new LoanableCheckInOnClickListener(loan.getLoanable().getTitle().getTitleId(), loan.getLoanable().getLoanableId(), activity, checkInCallback));
		}

		return rowView;
	}

	/**
	 * creates the header for the list ("MyLoans or MyReservations")
	 * @param text
	 * @return
	 */
	private TextView getHeader(CharSequence text) {
		LayoutInflater inflater = activity.getLayoutInflater();
		TextView tv = (TextView)inflater.inflate(R.layout.my_loans_list_item_header, null,true);
		tv.setTypeface(novaThin);

		tv.setText(text);

		return tv;
	}

	/**
	 * returns if at the given position a loan is in the table or it there is a reservation
	 * @param position
	 * @return
	 */
	private boolean isPositionInLoanPartOfList(int position) {
		if(loans != null && position <= loans.size()) { //<= because we need one item for the header
			return true;
		} else {
			return false;
		}
	}
}
