package com.u17od.upm.ui.fragments;



import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.u17od.upm.AddEditAccount;
import com.u17od.upm.EnterMasterPassword;
import com.u17od.upm.FullAccountList;
import com.u17od.upm.R;
import com.u17od.upm.SyncDatabaseActivity;
import com.u17od.upm.UIUtilities;
import com.u17od.upm.UPMApplication;
import com.u17od.upm.Utilities;
import com.u17od.upm.ViewAccountDetails;
import com.u17od.upm.database.AccountInformation;
import com.u17od.upm.database.PasswordDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by juan on 4/02/15.
 */
public class AccountListFragment extends ListFragment {

    private static final String TAG = AccountListFragment.class.getSimpleName();
    private static final String ARG_QUERY = "query";

    boolean isSearch = false;
    private String mSearchQuery;

    public static AccountListFragment newInstance() {
        AccountListFragment fragment= new AccountListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public static AccountListFragment newInstance(String query) {
        AccountListFragment fragment= new AccountListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if(getArguments() != null){
            mSearchQuery = getArguments().getString(ARG_QUERY, null);
            isSearch = !TextUtils.isEmpty(mSearchQuery);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(!isSearch) {
            populateAccountList();
        }else{
            filterAccountsList(mSearchQuery);
        }

        registerForContextMenu(getListView());
    }

    @Override
    public void onResume() {
        super.onResume();

        if(UPMApplication.getInstance().getPasswordDatabase() == null){
            getActivity().finish();
        }else if(isSearch){
            filterAccountsList(mSearchQuery);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionItemSelected " + item.getItemId());
        switch (item.getItemId()){
            case R.id.add: {
                if (Utilities.isSyncRequired(getActivity())) {
                    UIUtilities.showToast(getActivity(), R.string.sync_required);
                } else {
                    Intent i = new Intent(getActivity(), AddEditAccount.class);
                    i.putExtra(AddEditAccount.MODE, AddEditAccount.ADD_MODE);
                    startActivityForResult(i, AddEditAccount.EDIT_ACCOUNT_REQUEST_CODE);
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateAccountList() {
        PasswordDatabase db = UPMApplication.getInstance().getPasswordDatabase();
        if (db == null) {
            // If the UPM process was restarted since AppEntryActivity was last
            // run then databaseFileToDecrypt won't be set so set it here.
            EnterMasterPassword.databaseFileToDecrypt = Utilities.getDatabaseFile(getActivity());

            getActivity().setResult(FullAccountList.RESULT_ENTER_PW);
            getActivity().finish();
        } else {
            setListAdapter(new MyAdapter(getActivity(), R.layout.account_list_item, db.getAccounts()));
        }
    }

    private void filterAccountsList(String textToFilterOn) {
        List<AccountInformation> accounts = UPMApplication.getInstance().getPasswordDatabase().getAccounts();
        List<AccountInformation> filtered = new ArrayList<>();
        String textToFilterOnLC = textToFilterOn.toLowerCase();


        for(AccountInformation accountInformation: accounts){
            if(accountInformation.getAccountName().toLowerCase(Locale.getDefault()).contains(textToFilterOnLC)){
                filtered.add(accountInformation);
            }
        }
        setListAdapter(new MyAdapter(getActivity(), R.layout.account_list_item, filtered));
    }

    private void viewAccount(AccountInformation ai) {
        // Pass the AccountInformation object o the AccountDetails Activity by
        // way of a static variable on that class. I really don't like this but
        // it seems like the best way of doing it
        // @see http://developer.android.com/guide/appendix/faq/framework.html#3
        ViewAccountDetails.account = ai;

        Intent i = new Intent(getActivity(), ViewAccountDetails.class);
        startActivityForResult(i, ViewAccountDetails.VIEW_ACCOUNT_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult ");
        switch(requestCode) {
            case AddEditAccount.EDIT_ACCOUNT_REQUEST_CODE:
            case ViewAccountDetails.VIEW_ACCOUNT_REQUEST_CODE:
                if (resultCode == AddEditAccount.EDIT_ACCOUNT_RESULT_CODE_TRUE) {
                    populateAccountList();
                }
                break;
            case SyncDatabaseActivity.SYNC_DB_REQUEST_CODE:
                if (resultCode == SyncDatabaseActivity.RESULT_REFRESH) {
                    populateAccountList();
                }
                break;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        viewAccount((AccountInformation) l.getAdapter().getItem(position));
    }

    private class MyAdapter extends ArrayAdapter<AccountInformation> {
        private final int mResource;
        final Context mContext;

        private MyAdapter(Context context, int resource, List<AccountInformation> objects) {
            super(context, resource, objects);
            mResource = resource;
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            Placeholder placeholder;
            if(convertView == null){
                v = LayoutInflater.from(mContext).inflate(mResource, parent, false);
                placeholder = new Placeholder(v);

                v.setTag(placeholder);
            }else{
                v = convertView;
                placeholder = (Placeholder) v.getTag();
            }

            AccountInformation info = getItem(position);
            placeholder.accountName.setText(info.getAccountName());
            return v;
        }
    }

    private static class Placeholder{
        public TextView accountName;

        private Placeholder(@NonNull View rootView) {
            accountName = (TextView) rootView.findViewById(R.id.account_name);
        }
    }
}
