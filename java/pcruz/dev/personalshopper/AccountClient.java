package pcruz.dev.personalshopper;

import android.app.Application;

import pcruz.dev.personalshopper.models.Account;

public class AccountClient extends Application {
    private Account mAccount = null;

    public Account getAccount() {
        return mAccount;
    }

    public void setAccount(Account account) {
        mAccount = account;
    }
}
