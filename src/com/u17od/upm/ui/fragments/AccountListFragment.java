package com.u17od.upm.ui.fragments;



import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
            setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, db.getAccountNames()));
        }
    }

    private void filterAccountsList(String textToFilterOn) {
        ArrayList<String> allAccountNames = UPMApplication.getInstance().getPasswordDatabase().getAccountNames();
        ArrayList<String> filteredAccountNames = new ArrayList<String>();
        String textToFilterOnLC = textToFilterOn.toLowerCase();

        // Loop through all the accounts and pick out those that match the search string
        for (String accountName : allAccountNames) {
            if (accountName.toLowerCase().indexOf(textToFilterOnLC) > -1) {
                filteredAccountNames.add(accountName);
            }
        }
        //TODO Move to fragments
        setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, filteredAccountNames));
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
        TextView itemSelected = (TextView) v;
        viewAccount(UPMApplication.getInstance().getPasswordDatabase().getAccount(itemSelected.getText().toString()));
    }
}
