/*
 * Copyright 2016 Sascha Peilicke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package saschpe.birthdays.helper;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import saschpe.birthdays.R;
import saschpe.birthdays.service.BirthdaysIntentService;

public class AccountHelper {
    private static final String TAG = AccountHelper.class.getSimpleName();

    public static Bundle addAccount(Context context) {
        Log.d(TAG, "AccountHelper.addAccount: Adding account...");

        final Account account = new Account(context.getString(R.string.app_name), context.getString(R.string.account_type));
        AccountManager manager = AccountManager.get(context);

        if (manager.addAccountExplicitly(account, null, null)) {
            // Enable automatic sync once per day
            ContentResolver.setSyncAutomatically(account, context.getString(R.string.content_authority), true);
            ContentResolver.setIsSyncable(account, context.getString(R.string.content_authority), 1);

            // Add periodic sync interval based on user preference
            final long freq = PreferencesHelper.getPeriodicSyncFrequency(context);
            ContentResolver.addPeriodicSync(account, context.getString(R.string.content_authority), new Bundle(), freq);

            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            Log.i(TAG, "Account added: " + account.name);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.notifyAccountAuthenticated(account);
            }
            return result;
        } else {
            Log.e(TAG, "Adding account explicitly failed!");
            return null;
        }
    }

    /**
     * Adds account and forces manual sync afterwards if adding was successful
     */
    public static Bundle addAccountAndSync(Context context, Handler backgroundStatusHandler) {
        final Bundle result = addAccount(context);
        if (result != null) {
            if (result.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
                BirthdaysIntentService.startActionSync(context, backgroundStatusHandler);
                return result;
            } else {
                Log.e(TAG, "Unable to add account. Result did not contain KEY_ACCOUNT_NAME");
            }
        } else {
            Log.e(TAG, "Unable to add account. Result was null.");
        }
        return null;
    }

    /**
     * Remove account from Android system
     */
    public static boolean removeAccount(Context context) {
        Log.d(TAG, "Removing account...");
        AccountManager manager = AccountManager.get(context);
        final Account account = new Account(context.getString(R.string.app_name), context.getString(R.string.account_type));
        AccountManagerFuture<Boolean> future = manager.removeAccount(account, null, null);
        if (future.isDone()) {
            try {
                future.getResult();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Problem while removing account!", e);
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Checks whether the account is enabled or not
     */
    public static boolean isAccountActivated(Context context) {
        AccountManager manager = AccountManager.get(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Lacking permission GET_ACCOUNTS to query existing accounts!");
            return false;
        }
        Account[] availableAccounts = manager.getAccountsByType(context.getString(R.string.account_type));
        for (Account currentAccount : availableAccounts) {
            if (currentAccount.name.equals(context.getString(R.string.app_name))) {
                Log.i(TAG, "Account already present");
                return true;
            }
        }
        return false;
    }
}
