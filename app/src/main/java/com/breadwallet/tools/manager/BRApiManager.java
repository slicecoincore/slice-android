package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Request;
import okhttp3.Response;

import static com.breadwallet.presenter.fragments.FragmentSend.isEconomyFee;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BRApiManager {
    private static final String TAG = BRApiManager.class.getName();

    private static BRApiManager instance;
    private Timer timer;

    private TimerTask timerTask;

    private Handler handler;

    private BRApiManager() {
        handler = new Handler();
    }

    public static BRApiManager getInstance() {
        if (instance == null) {
            instance = new BRApiManager();
        }
        return instance;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context) {
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONObject arr = fetchRates(context);
            updateFeePerKb(context);
            if (arr != null) {
                CurrencyEntity tmp = new CurrencyEntity();
                try {
                    tmp.name = "US Dollar";
                    tmp.code = "USD";
                    tmp.rate = (float) arr.getDouble("usd");
                    String selectedISO = BRSharedPrefs.getIso(context);
                    if (tmp.code.equalsIgnoreCase(selectedISO)) {
                        BRSharedPrefs.putIso(context, tmp.code);
                        BRSharedPrefs.putCurrencyListPosition(context, 0);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                set.add(tmp);
            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies, response string: " + arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List tempList = new ArrayList<>(set);
        Collections.reverse(tempList);
        return new LinkedHashSet<>(set);
    }


    private void initializeTimerTask(final Context context) {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (!BreadApp.isAppInBackground(context)) {
                                    Log.e(TAG, "doInBackground: Stopping timer, no activity on.");
                                    BRApiManager.getInstance().stopTimerTask();
                                }
                                Set<CurrencyEntity> tmp = getCurrencies((Activity) context);
                                CurrencyDataSource.getInstance(context).putCurrencies(tmp);
                            }
                        });
                    }
                });
            }
        };
    }

    public void startTimer(Context context) {
        //set a new Timer
        if (timer != null) return;
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask(context);

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 0, 60000); //
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    public static JSONObject fetchRates(Activity activity) {
        String jsonString = urlGET(activity, "https://api.coingecko.com/api/v3/simple/price/?ids=slice&vs_currencies=usd");
        JSONObject jsonObject = null;
        if (jsonString == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonString);
            jsonObject = obj.getJSONObject("slice");
        } catch (JSONException ignored) {
        }

        return jsonObject;
    }

    public static void updateFeePerKb(Context app) {
        long fee = 100000;
        long economyFee = 1000;
        BRSharedPrefs.putFeePerKb(app, fee);
        BRWalletManager.getInstance().setFeePerKb(fee, isEconomyFee); //todo improve that logic
        BRSharedPrefs.putFeeTime(app, System.currentTimeMillis()); //store the time of the last successful fee fetch
        BRSharedPrefs.putEconomyFeePerKb(app, economyFee);
    }

    private static String urlGET(Context app, String myURL) {
        Request request = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .get().build();
        String response = null;
        Response resp = APIClient.getInstance(app).sendRequest(request, false, 0);

        try {
            if (resp == null) {
                Log.e(TAG, "urlGET: " + myURL + ", resp is null");
                return null;
            }
            response = resp.body().string();
            String strDate = resp.header("date");
            if (strDate == null) {
                Log.e(TAG, "urlGET: strDate is null!");
                return response;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } finally {
            if (resp != null) resp.close();

        }
        return response;
    }


}
