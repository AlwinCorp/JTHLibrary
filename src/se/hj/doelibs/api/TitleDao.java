package se.hj.doelibs.api;

import android.util.Log;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import se.hj.doelibs.mobile.utils.ListUtils;
import se.hj.doelibs.model.Author;
import se.hj.doelibs.model.Loanable;
import se.hj.doelibs.model.Title;
import se.hj.doelibs.model.Topic;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Christoph
 */
public class TitleDao extends BaseDao<Title> {

    public TitleDao(UsernamePasswordCredentials credentials) {
        super(credentials);
    }

    @Override
    public Title getById(int titleId) throws HttpException {
        Title title = null;

        try {
            HttpResponse response = get("/Title/" + titleId);

            //check statuscode of request
            checkResponse(response);

            //get the result
            String responseString = getResponseAsString(response);

            //create object out of JSON result
            JSONObject titleModel = new JSONObject(responseString);

            //get basic titleinformation
            title = TitleDao.parseFromJson(titleModel.getJSONObject("Title"));

            //get loanables
            JSONArray loanableArray = titleModel.getJSONArray("Loanables");
            List<Loanable> loanables = new ArrayList<Loanable>();
            for(int i = 0;i<loanableArray.length();i++) {
                Loanable loanable = LoanableDao.parseFromJson(loanableArray.getJSONObject(i));
                loanables.add(loanable);
            }

            //get authors
            JSONArray authorsArray = titleModel.getJSONArray("Authors");
            List<Author> authors = new ArrayList<Author>();
            for(int i = 0;i<authorsArray.length();i++) {
                Author author = AuthorDao.parseFromJson(authorsArray.getJSONObject(i));
                authors.add(author);
            }

            //get Editors
            JSONArray editorsArray = titleModel.getJSONArray("Editors");
            List<Author> editors = new ArrayList<Author>();
            for(int i = 0;i<editorsArray.length();i++) {
                Author editor  = AuthorDao.parseFromJson(editorsArray.getJSONObject(i));
                editors.add(editor);
            }

            //get Topics
            JSONArray topicArray = titleModel.getJSONArray("Topics");
            List<Topic> topics = new ArrayList<Topic>();
            for(int i = 0;i<topicArray.length();i++) {
                Topic topic = TopicDao.parseFromJson(topicArray.getJSONObject(i));
                topics.add(topic);
            }

            //add everything
            title.setLoanables(loanables);
            title.setAuthors(authors);
            title.setEditors(editors);
            title.setTopics(topics);

        } catch (IOException e) {
            Log.e("AuthorDao", "Exception on GET request", e);
        } catch (JSONException e) {
            Log.e("AuthorDao", "could not parse JSON result", e);
        }

        return title;
    }

    /**
     * returns the title with the given ISBN number if it exists in DoeLibS
     *
     * @param isbn
     * @param isbnFormat the ISBN format (isbn10 or isbn13)
     * @return Title
     */
    public Title getByIsbn(String isbn, IsbnFormat isbnFormat) throws HttpException {
        Title title = null;

        try {
            String context = String.format("/Title/%s/%s", isbnFormat.getValue(), isbn);
            HttpResponse response = get(context);

            //check statuscode of request
            checkResponse(response);

            //get the result
            String responseString = getResponseAsString(response);

            //create object out of JSON result
            JSONObject titleModel = new JSONObject(responseString);

            //get basic titleinformation
            title = TitleDao.parseFromJson(titleModel);
        } catch (IOException e) {
            Log.e("TitleDao", "Exception on GET request", e);
        } catch (JSONException e) {
            Log.e("TitleDao", "could not parse JSON result", e);
        }

        return title;
    }

    /**
     * Search for titles. Returns a list.
     * @param term
     * @param topics
     * @return
     * @throws HttpException
     */
    public List<Title> searchTitle(String term, String topics) throws HttpException {
        List<Title> result = new ArrayList<Title>();

        try {
            boolean noTopic = false;
            boolean noTerm = false;
            if(topics.equals(""))
                noTopic = true;
            if(term.equals(""))
                noTerm = true;

            String context;
            HttpResponse response;
            if(!noTopic && !noTerm) {
                String encodedTopics = URLEncoder.encode(topics, "UTF-8");
                context = String.format("/Title?q=%s&topics=%s&onlyTopicMatches=true", term, encodedTopics);
                response = get(context);
                checkResponse(response);

                String responseString = getResponseAsString(response);
                JSONArray titleArr = new JSONArray(responseString);
                for (int i = 0; i < titleArr.length(); i++) {
                    JSONObject obj = new JSONObject(titleArr.get(i).toString());
                    result.add(TitleDao.parseFromJson(obj));
                }
            }
            else if (noTopic && !noTerm){
                String encodedTerm = URLEncoder.encode(term, "UTF-8");
                context = String.format("/Search/?searchTerm=%s&searchOption=Title", encodedTerm);
                response = get(context);

                String responseString = getResponseAsString(response);
                JSONObject titleObj = new JSONObject(responseString);
                JSONArray allTitles = titleObj.getJSONArray("Titles");
                for (int i = 0; i < allTitles.length(); i++) {
                    result.add(TitleDao.parseFromJson(allTitles.getJSONObject(i).getJSONObject("Title")));
                }
            }
            else{
                String encodedTopics = URLEncoder.encode(topics, "UTF-8");
                context = String.format("/Topic?=%s", encodedTopics);
                response = get(context);

                String responseString = getResponseAsString(response);
                JSONArray topicObj = new JSONArray(responseString);
                JSONArray titlesArr = topicObj.getJSONObject(0).getJSONArray("Titles");
                for (int i = 0; i < titlesArr.length(); i++) {
                    result.add(TitleDao.parseFromJson(titlesArr.getJSONObject(i)));
                }
            }


        } catch (IOException e) {
            Log.e("TitleDao", "Exception on GET request", e);
        } catch (JSONException e) {
            Log.e("TitleDao", "could not parse JSON result", e);
        }

        return result;
    }


    /**
     * returns the title for the given ISBN number from the google API
     * @param isbn
     * @return
     */
    public Title getFromGoogleApi(String isbn) {
        //https://www.googleapis.com/books/v1/volumes?q=isbn:9789144059679
        Title result = null;

        try {
            HttpGet httpGet = new HttpGet("https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn);
            HttpResponse response = httpClient.execute(httpGet);

            //check response
            checkResponse(response);

            //parse result
            String responseString = getResponseAsString(response);

            JSONObject root = new JSONObject(responseString);
            if(root.has("totalItems") && root.getInt("totalItems") >= 1) {
                JSONArray items = root.getJSONArray("items");

                if(items.length() > 0) {
                    JSONObject firstItem = items.getJSONObject(0);
                    JSONObject volumeInfo = firstItem.getJSONObject("volumeInfo");
                    
                    result = new Title();

                    //get title
                    if(volumeInfo.has("title")) {
                        result.setBookTitle(volumeInfo.getString("title"));
                    }

                    //get publishYear
                    if(volumeInfo.has("publishedDate")) {
                        result.setEditionYear(volumeInfo.getInt("publishedDate"));
                    }

                    //get ISBNs
                    if(volumeInfo.has("industryIdentifiers")) {
                        JSONArray isbns = volumeInfo.getJSONArray("industryIdentifiers");

                        for(int i = 0;i < isbns.length();i++) {
                            JSONObject entry = isbns.getJSONObject(i);

                            if(entry.has("type")) {
                                if(entry.get("type").equals("ISBN_10")) {
                                    result.setIsbn10(entry.getString("identifier"));
                                } else if(entry.get("type").equals("ISBN_13")) {
                                    result.setIsbn13(entry.getString("identifier"));
                                }
                            }
                        }
                    }

                    //get authors
                    if(volumeInfo.has("authors")) {
                        JSONArray authors = volumeInfo.getJSONArray("authors");
                        if(authors.length() > 0) {
                            ArrayList<Author> authorArrayList = new ArrayList<Author>();

                            for(int i = 0;i< authors.length(); i++) {
                                Author a = new Author();
                                a.setName(authors.getString(i));
                            }

                            result.setAuthors(authorArrayList);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e("TitleDao", "Exception at reading from GoogleAPI", e);
        } catch (HttpException e) {
            Log.e("TitleDao", "HttpException at reading from GoogleAPI", e);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * adds a title to DoeLibS
     * @param title
     */
    public Title add(Title title) throws HttpException {
        Title result = null;

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("Title", title.getBookTitle()));
        nameValuePairs.add(new BasicNameValuePair("ISBN10", title.getIsbn10()));
        nameValuePairs.add(new BasicNameValuePair("ISBN13", title.getIsbn13()));
        nameValuePairs.add(new BasicNameValuePair("Edition", String.valueOf(title.getEditionNumber())));
        nameValuePairs.add(new BasicNameValuePair("PublicationYear", String.valueOf(title.getEditionYear())));
        nameValuePairs.add(new BasicNameValuePair("FirstEditionYear", String.valueOf(title.getFirstEditionYear())));
        nameValuePairs.add(new BasicNameValuePair("Publisher", title.getPublisher().getName()));

        //create string list with author names so they can be imploded with ListUtils (commaseparated)
        List<String> authors = new ArrayList<String>();
        if(title.getAuthors() != null) {
            for(Author a : title.getAuthors()) {
                authors.add(a.getName());
            }
        }

        //create string list with editor names so they can be imploded with ListUtils (commaseparated)
        List<String> editors = new ArrayList<String>();
        if(title.getEditors() != null) {
            for(Author a : title.getEditors()) {
                editors.add(a.getName());
            }
        }

        //create string list with topic names so they can be imploded with ListUtils (commaseparated)
        List<String> topics = new ArrayList<String>();
        if(title.getTopics() != null) {
            for(Topic t : title.getTopics()) {
                topics.add(t.getName());
            }
        }

        nameValuePairs.add(new BasicNameValuePair("Authors", ListUtils.implode(authors, ", ")));
        nameValuePairs.add(new BasicNameValuePair("Editors", ListUtils.implode(editors, ", ")));
        nameValuePairs.add(new BasicNameValuePair("Topics", ListUtils.implode(topics, ", ")));

        try {
            HttpResponse response = post("/Title/", nameValuePairs);
            checkResponse(response);

            result = parseFromJson(new JSONObject(getResponseAsString(response)));
        } catch (IOException e) {
            Log.d("Add title", "IOException on POST request", e);
        } catch (JSONException e) {
            Log.d("Add title", "JSON parse exeption", e);
        }

        return result;
    }


    public static Title parseFromJson(JSONObject jsonObject) throws JSONException {
        Title title = new Title();

        title.setTitleId(jsonObject.getInt("TitleId"));
        title.setBookTitle(jsonObject.getString("BookTitle"));
        title.setIsbn10(jsonObject.getString("ISBN10"));
        title.setIsbn13(jsonObject.getString("ISBN13"));
        title.setEditionNumber(jsonObject.getInt("EditionNumber"));
        title.setEditionYear(jsonObject.getInt("EditionYear"));
        if(jsonObject.has("FirstEditionYear") && !jsonObject.isNull("FirstEditionYear")) {
            title.setFirstEditionYear(jsonObject.getInt("FirstEditionYear"));
        }

        //set publisher
        title.setPublisher(PublisherDao.parseFromJson(jsonObject.getJSONObject("Publisher")));

        return title;
    }

    /**
     * specifies the format of the isbn number
     */
    public enum IsbnFormat {
        ISBN10("isbn10"),
        ISBN13("isbn13");

        private String value;
        private IsbnFormat(String value) {
            this.value = value;
        }

        /**
         * returns the isbn string representation for this ISBN version
         * @return String
         */
        public String getValue() {
            return value;
        }
    }
}
