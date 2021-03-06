package project.bookrental.activity.admin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import project.bookrental.R;
import project.bookrental.activity.common.LoginActivity;
import project.bookrental.models.BookModel;
import project.bookrental.models.BorrowedBookModel;
import project.bookrental.models.UserModel;
import project.bookrental.utils.DataStoreUtils;

/**
 * @author Mateusz Wieczorek
 */
public class ReturnBookActivity extends AppCompatActivity {

    public static final int MILLIS_IN_SECOND = 1000;
    public static final int SECONDS_IN_MINUTE = 60;
    public static final int MINUTES_IN_HOUR = 60;
    public static final int HOURS_IN_DAY = 24;
    private FirebaseAuth auth;
    private EditText adminReturnBookFilter;
    private ListView listView;
    private final List<BookModel> books = new ArrayList<>();
    private final List<UserModel> users = new ArrayList<>();
    private final List<BorrowedBookModel> listOfBorrowedBooks = new ArrayList<>();

    private boolean isBooksStored = false;
    private boolean isBorrowedBooksStored = false;
    private boolean isUsersStored = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent( ReturnBookActivity.this, LoginActivity.class));
            finish();
        }

        setContentView(R.layout.activity_admin_return_books);
        adminReturnBookFilter = (EditText) findViewById(R.id.AdminReturnBookFilter);
        setAdminReturnBookListener();
        listView = (ListView) findViewById(R.id.AdminReturnBookListView);
        setListViewItemClickListener();
        getAllBooksFromDatabase();
    }

    private void setListViewItemClickListener() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("Hello!", "Y u no see me?");
            }
        });
    }

    private void setAdminReturnBookListener() {
        adminReturnBookFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterList(String email) {
        if (!isBooksStored || !isBorrowedBooksStored || !isUsersStored) {
            return;
        }
        List<ReturnBookView> returnBookViews = new ArrayList<>();

        for (UserModel userModel : users) {
            if (userModel.getEmail().contains(email)) {
                for (BorrowedBookModel bookModel : listOfBorrowedBooks) {
                    if (bookModel.getUserId().equalsIgnoreCase(userModel.getUId())) {
                        for (BookModel book : books) {
                            if (Objects.equals(bookModel.getBookId(), book.getId())) {
                                returnBookViews.add(new ReturnBookView(book, userModel));
                                break;
                            }
                        }
                    }
                }
            }
        }

        ReturnBookAdapter borrowBookAdapter = new ReturnBookAdapter(ReturnBookActivity.this, returnBookViews);
        listView.setAdapter(borrowBookAdapter);
    }

    void getAllBooksFromDatabase() {
        final DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("books");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    return;
                }
                books.clear();
                books.addAll(DataStoreUtils.readBooks(dataSnapshot.getValue()));
                isBooksStored = true;
                filterList(adminReturnBookFilter.getText().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Err:listofbooks:", databaseError.toException());
            }
        });

        final DatabaseReference confirmations = FirebaseDatabase.getInstance().getReference("borrowed_books");
        confirmations.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    return;
                }
                listOfBorrowedBooks.clear();
                listOfBorrowedBooks.addAll(DataStoreUtils.readBorrowedBooks(dataSnapshot.getValue()));
                isBorrowedBooksStored = true;
                filterList(adminReturnBookFilter.getText().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Err:listofbooks:", databaseError.toException());
            }
        });

        final DatabaseReference usersdRef = FirebaseDatabase.getInstance().getReference().child("users");
        usersdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    return;
                }
                users.clear();
                users.addAll(DataStoreUtils.readUsers(dataSnapshot.getValue()));
                isUsersStored = true;
                filterList(adminReturnBookFilter.getText().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Err:listofbooks:", databaseError.toException());
            }
        });
    }

    private class ReturnBookAdapter extends ArrayAdapter<ReturnBookView> {

        ReturnBookAdapter(Context context, List<ReturnBookView> books) {
            super(context, 0, books);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ReturnBookView book = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_admin_return_books_layout, parent, false);
            }
            TextView textView = convertView.findViewById(R.id.adminReturnBookListText);
            Button button = convertView.findViewById(R.id.adminReturnBookButton);
            textView.setText(book.getBookModel() != null ? book.getBookModel().toString() : "");
            textView.setText(textView.getText() + "\n" + "User: " + book.getUser().getEmail());
            if (!isBooksStored || !isBorrowedBooksStored || !isUsersStored) {
                return convertView;
            }
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("borrowed_books");
                    myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            myRef.removeEventListener(this);
                            final DatabaseReference bookRef = myRef.child(book.getBookModel().getId().toString());
                            bookRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    bookRef.removeEventListener(this);
                                    HashMap<String, Object> map = (HashMap<String,Object>) dataSnapshot.getValue();
                                    HashMap<String,Object> borrowDateMap = (HashMap <String,Object>) map.get("borrowDate");
                                    Long time = (Long) borrowDateMap.get("time");
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTime(new Date(time));
                                    calendar.add(Calendar.DAY_OF_YEAR, 14);
                                    calendar.getTime();
                                    if(new Date().getTime() > calendar.getTime().getTime()) {
                                        long latenessInMillis = Math.abs(new Date().getTime() - calendar.getTime().getTime());
                                        int latenessInDays = (int)Math.ceil((float)latenessInMillis/ MILLIS_IN_SECOND / SECONDS_IN_MINUTE / MINUTES_IN_HOUR / HOURS_IN_DAY);
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setTitle("Lateness!")
                                                .setMessage("Lateness in returning book: "+latenessInDays+" DAYS!\nPenalty to pay: "+latenessInDays*5+"zl")
                                                .setNeutralButton("OK!", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        bookRef.removeValue();
                                                    }
                                                }).show();
                                    } else {
                                        bookRef.removeValue();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.w("Err:returnBook:", databaseError.toException());
                                }
                            });
                            Toast.makeText(getApplicationContext(), "Book returned", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d("err:BorrowBook:183", databaseError.getMessage());
                        }
                    });
                }
            });
            return convertView;
        }
    }

    private class ReturnBookView {

        ReturnBookView(BookModel bookModel, UserModel user) {
            this.bookModel = bookModel;
            this.user = user;
        }

        BookModel bookModel;
        UserModel user;

        BookModel getBookModel() {
            return bookModel;
        }

        void setBookModel(BookModel bookModel) {
            this.bookModel = bookModel;
        }

        UserModel getUser() {
            return user;
        }

        void setUser(UserModel user) {
            this.user = user;
        }
    }

}
