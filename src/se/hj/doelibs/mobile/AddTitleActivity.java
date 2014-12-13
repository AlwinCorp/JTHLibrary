package se.hj.doelibs.mobile;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpException;
import se.hj.doelibs.api.TitleDao;
import se.hj.doelibs.mobile.codes.ExtraKeys;
import se.hj.doelibs.mobile.utils.ListUtils;
import se.hj.doelibs.mobile.utils.ProgressDialogUtils;
import se.hj.doelibs.model.Author;
import se.hj.doelibs.model.Publisher;
import se.hj.doelibs.model.Title;
import se.hj.doelibs.model.Topic;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christoph
 */
public class AddTitleActivity extends BaseActivity {

	private TextView tv_titleName;
	private TextView tv_isbn10;
	private TextView tv_isbn13;
	private TextView tv_edition;
	private TextView tv_year;
	private TextView tv_firstEditionYear;
	private TextView tv_publisher;
	private TextView tv_authors;
	private TextView tv_editors;
	private TextView tv_topics;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View contentView = inflater.inflate(R.layout.activity_add_title, null, false);
		drawerLayout.addView(contentView, 0);

		//setup fields
		tv_titleName = (TextView)findViewById(R.id.txt_add_title_name);
		tv_isbn10 = (TextView)findViewById(R.id.txt_add_title_isbn10);
		tv_isbn13 = (TextView)findViewById(R.id.txt_add_title_isbn13);
		tv_edition = (TextView)findViewById(R.id.txt_add_title_edition);
		tv_year = (TextView)findViewById(R.id.txt_add_title_year);
		tv_firstEditionYear = (TextView)findViewById(R.id.txt_add_title_first_edition_year);
		tv_publisher = (TextView)findViewById(R.id.txt_add_title_publisher);
		tv_authors = (TextView)findViewById(R.id.txt_add_title_authors);
		tv_editors = (TextView)findViewById(R.id.txt_add_title_editors);
		tv_topics = (TextView)findViewById(R.id.txt_add_title_topics);

		//load data
		String isbn = getIntent().getStringExtra(ExtraKeys.TITLE_ISBN);
		String format = getIntent().getStringExtra(ExtraKeys.TITLE_ISBN_FORMAT);
		if(isbn != null && !isbn.equals("") && format != null && !format.equals("")) {
			loadData(isbn, format);
		}
	}

	/**
	 * saves the title in the doelibs system
	 * @param view
	 */
	public void onSave(View view) {
		//check fields
		if(!tv_titleName.getText().toString().equals("")
				&& !(tv_isbn10.getText().toString().equals("") && tv_isbn13.getText().equals(""))
				&& !tv_authors.getText().toString().equals("")
				&& !tv_year.getText().toString().equals("")
				&& !tv_edition.getText().toString().equals("")
				&& !tv_publisher.getText().toString().equals("")
				&& !tv_topics.getText().toString().equals("")) {

			Title title = new Title();

			title.setBookTitle(tv_titleName.getText().toString());
			title.setIsbn10(tv_isbn10.getText().toString());
			title.setIsbn13(tv_isbn13.getText().toString());
			title.setEditionNumber(Integer.valueOf(tv_edition.getText().toString()));
			title.setEditionYear(Integer.valueOf(tv_year.getText().toString()));

			if(!tv_firstEditionYear.getText().toString().equals("")) {
				title.setFirstEditionYear(Integer.valueOf(tv_firstEditionYear.getText().toString()));
			}

			//get publisher
			Publisher p = new Publisher();
			p.setName(tv_publisher.getText().toString());
			title.setPublisher(p);

			//get authors
			List<Author> authors = new ArrayList<Author>();
			if(tv_authors.getText().toString().contains(",")) {

				for(String name : tv_authors.getText().toString().split(",")) {
					if(!name.trim().equals("")) {
						Author a = new Author();
						a.setName(name.trim());
						authors.add(a);
					}
				}
			} else {
				Author a = new Author();
				a.setName(tv_authors.getText().toString());
				authors.add(a);
			}
			title.setAuthors(authors);

			//get editors
			if(!tv_editors.getText().toString().equals("")) {
				List<Author> editors = new ArrayList<Author>();
				if(tv_editors.getText().toString().contains(",")) {

					for(String name : tv_editors.getText().toString().split(",")) {
						if(!name.trim().equals("")) {
							Author a = new Author();
							a.setName(name.trim());
							editors.add(a);
						}
					}
				} else {
					Author a = new Author();
					a.setName(tv_editors.getText().toString());
					editors.add(a);
				}
				title.setEditors(editors);
			}

			//get topics
			List<Topic> topics = new ArrayList<Topic>();
			if(tv_topics.getText().toString().contains(",")) {

				for(String name : tv_topics.getText().toString().split(",")) {
					if(!name.trim().equals("")) {
						Topic t = new Topic();
						t.setName(name.trim());
						topics.add(t);
					}
				}
			} else {
				Topic t = new Topic();
				t.setName(tv_topics.getText().toString());
				topics.add(t);
			}
			title.setTopics(topics);

			saveData(title);
		} else {
			Toast.makeText(this, R.string.add_title_required_fields, Toast.LENGTH_LONG).show();
		}
	}

	private void saveData(final Title title) {
		new AsyncTask<Void, Void, Title>() {
			ProgressDialog dialog;

			@Override
			protected Title doInBackground(Void... params) {
				TitleDao titleDao = new TitleDao(getCredentials());
				Title result = null;
				try {
					result = titleDao.add(title);
				} catch (HttpException e) {

				}
				return result;
			}


			@Override
			protected void onPostExecute(Title title) {
				ProgressDialogUtils.dismissQuitely(dialog);

				if(title == null) {
					Toast.makeText(AddTitleActivity.this, R.string.invalid_data, Toast.LENGTH_LONG).show();
				} else {
					//title was added successfully --> redirect to title page
					Intent titleDetails = new Intent(AddTitleActivity.this, TitleDetailsActivity.class);
					titleDetails.putExtra(ExtraKeys.TITLE_ID, title.getTitleId());
					startActivity(titleDetails);
					finish();
				}
			}

			@Override
			protected void onPreExecute() {
				dialog = new ProgressDialog(AddTitleActivity.this);
				dialog.setMessage(getResources().getText(R.string.dialog_progress_add_title_save));
				dialog.setCancelable(false);
				dialog.show();
			}
		}.execute();
	}

	/**
	 * loads the data and inserts it into the view
	 * @param isbn
	 * @param format
	 */
	private void loadData(final String isbn, final String format) {
		new AsyncTask<Void, Void, Title>() {

			ProgressDialog dialog;

			@Override
			protected Title doInBackground(Void... params) {
				TitleDao titleDao = new TitleDao(getCredentials());
				return titleDao.getFromGoogleApi(isbn);
			}


			@Override
			protected void onPostExecute(Title title) {
				ProgressDialogUtils.dismissQuitely(dialog);

				//check if title was found on google API
				if(title == null) {
					Toast.makeText(AddTitleActivity.this, R.string.add_title_no_title_found, Toast.LENGTH_LONG).show();

					if(format.equals("EAN_13")) {
						tv_isbn13.setText(isbn);
					} else {
						tv_isbn10.setText(isbn);
					}
				} else {
					tv_titleName.setText(title.getBookTitle());
					tv_year.setText(String.valueOf(title.getEditionYear()));
					tv_isbn10.setText(title.getIsbn10());
					tv_isbn13.setText(title.getIsbn13());

					if(title.getAuthors() != null && title.getAuthors().size() > 0) {
						List<String> authors = new ArrayList<String>();
						for(Author a : title.getAuthors()) {
							authors.add(a.getName());
						}

						tv_authors.setText(ListUtils.implode(authors, ", "));
					}
				}
			}

			@Override
			protected void onPreExecute() {
				dialog = new ProgressDialog(AddTitleActivity.this);
				dialog.setMessage(getResources().getText(R.string.dialog_progress_load_titleinformation));
				dialog.setCancelable(false);
				dialog.show();
			}
		}.execute();
	}
}