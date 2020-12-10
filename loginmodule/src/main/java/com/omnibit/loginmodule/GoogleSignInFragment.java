package com.omnibit.loginmodule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.renderscript.Element;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.unity3d.player.UnityPlayer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class GoogleSignInFragment extends android.app.Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String Tag = "GoogleSingInTag";
    private static final String DebugTAG = "unity";
    private static final int RC_SIGN_IN = 100;
    private static final String UnitySuccessCallbackName = "UnityGoogleSignInSuccessCallback";
    private static final String UnityErrorCallbackName = "UnityGoogleSignInErrorCallback";

    //region Public Api

    public static String WebClientId;
    public static String UnityGameObjectName;

    public static Activity thisActivity;
    public GoogleSignInAccount myAccount;

    public static void SignIn(Activity unityActivity, String webClientId)
    {
        // Creating an intent with the current activity and the activity we wish to start
        WebClientId = webClientId;
        thisActivity = unityActivity;
        SignIn(unityActivity);
    }

    public static void SignIn(Activity unityActivity) {
        if (WebClientId == null || WebClientId.length() == 0) {
            return;
        }

        android.app.FragmentTransaction trans;
        trans = unityActivity.getFragmentManager().beginTransaction();
        trans.add(new GoogleSignInFragment(), Tag);
        trans.commitAllowingStateLoss();
    }

    //endregion

    private GoogleApiClient _gApiClient;

    @Override
    public void onStart() {
        super.onStart();
        Log.i(DebugTAG, "Starting signing in from android plugin");

        Scope scope1 = new Scope("https://www.googleapis.com/auth/fitness.activity.read");
        Scope scope2 = new Scope("https://www.googleapis.com/auth/fitness.activity.write");
        Scope scope3 = new Scope("https://www.googleapis.com/auth/fitness.body.read");
        Scope scope4 = new Scope("https://www.googleapis.com/auth/fitness.heart_rate.read");
        Scope scope5 = new Scope("https://www.googleapis.com/auth/fitness.location.read");
        Scope scope6 = new Scope("https://www.googleapis.com/auth/fitness.sleep.read");

        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestId()
                .requestEmail()
                .requestProfile()
                .requestScopes(scope1, scope2, scope3, scope4, scope5, scope6);
        if (WebClientId != null && WebClientId.length() > 0) {
            builder.requestIdToken(WebClientId);
        }

        GoogleSignInOptions gso = builder.build();

        _gApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        _gApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.i(DebugTAG, "result: " + result.getSignInAccount().getGrantedScopes().toArray().length);
            if (result.isSuccess()) {
                GoogleSignInAccount acct = result.getSignInAccount();
                sendAccountToUnity(acct);
                myAccount = acct;
//                try {
//                    GetFitnessData();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }else {
                sendErrorToUnity("");
            }
        }
    }


    private void hideFragment() {
        getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentByTag(Tag)).commit();
    }

    //region Google Api Client
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Auth.GoogleSignInApi.signOut(_gApiClient);
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(_gApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onConnectionSuspended(int i) {
        sendErrorToUnity("");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        sendErrorToUnity("");
    }

    //endregion

    //region Unity Integration

    private void sendAccountToUnity(GoogleSignInAccount account) {
        UnityPlayer.UnitySendMessage(UnityGameObjectName, UnitySuccessCallbackName, serializeSignInAccount(account));
        hideFragment();
    }

    private void sendErrorToUnity(String error) {
        UnityPlayer.UnitySendMessage(UnityGameObjectName, UnityErrorCallbackName, error);
        hideFragment();
    }


    private static String serializeSignInAccount(GoogleSignInAccount account) {
        String result = "";
        if (account == null) return result;

        result += "{";

        result += "\"Id\": \"" + account.getId() + "\", ";
        result += "\"Token\": \"" + account.getIdToken() + "\", ";
        result += "\"DisplayName\": \"" + account.getDisplayName() + "\", ";
        result += "\"Email\": \"" + account.getEmail() + "\", ";
        result += "\"FamilyName\": \"" + account.getFamilyName() + "\", ";
        if (account.getPhotoUrl() != null) result += "\"PhotoUrl\": \"" +  account.getPhotoUrl().toString() + "\" ";
        if (account.getServerAuthCode() != null) result += "\"ServerAuthCode\": \"" +  account.getServerAuthCode() + "\" ";

        result += "}";

        return result;
    }

    /* endregion */

    //region Fitnedd Data
    public void GetFitnessData() throws ExecutionException, InterruptedException {
        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                        .build();

        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(thisActivity, fitnessOptions);

        Task<DataReadResponse> response = Fitness.getHistoryClient(thisActivity, myAccount)
                .readData(new DataReadRequest.Builder()
                        .read(DataType.TYPE_STEP_COUNT_DELTA)
//                        .setTimeRange(startTime.getMillis(), endTime.getMillis(), TimeUnit.MILLISECONDS)
                        .build());

        DataReadResponse readDataResult = Tasks.await(response);
        DataSet dataSet = readDataResult.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);
    }
    //End Region
}
