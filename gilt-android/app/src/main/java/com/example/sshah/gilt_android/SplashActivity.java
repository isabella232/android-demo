package com.example.sshah.gilt_android;

import com.amplitude.api.Amplitude;
import com.amplitude.api.AmplitudeClient;
import com.example.sshah.gilt_android.util.SystemUiHider;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.localytics.android.AnalyticsListener;
import com.localytics.android.Localytics;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.optimizely.CodeBlocks.CodeBranch;
import com.optimizely.CodeBlocks.DefaultCodeBranch;
import com.optimizely.CodeBlocks.OptimizelyCodeBlock;
import com.optimizely.Optimizely;
import com.optimizely.Variable.LiveVariable;
import com.optimizely.integration.DefaultOptimizelyEventListener;
import com.optimizely.integration.OptimizelyEventListener;
import com.optimizely.integration.OptimizelyExperimentData;
import com.optimizely.integrations.localytics.OptimizelyLocalyticsIntegration;
import com.optimizely.integrations.universalanalytics.OptimizelyUniversalAnalyticsIntegration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class SplashActivity extends Activity {

    private static final int SPLASH_SCREEN_LENGTH = 2000;

    public static MixpanelAPI mixpanelAPI;
    public static AmplitudeClient amplitudeClient;

    // Hack to work around the fact that onCreate() gets called twice
    private boolean showSignUpFlowOnResume = false;

    long startTime;

    private static LiveVariable<Boolean> showAnimationManual = Optimizely.booleanForKey("share_group_show_animation_manual", false);


    /*  This method is used to pull in the correct Optimizely token and enables the mobile playground
    as well as the personalConstants file
     */
    private String getOptimizelyToken() {
        String projectToken = "fake_token";
        Intent launchIntent = getIntent();
        String appetizeToken = null;

        final Bundle extras = launchIntent.getExtras();
        if (extras != null) {
            appetizeToken = extras.getString("project");
        }

        // Check to see if a personal constants file/string is defined in the project
        int personalConstantsID = getResources().getIdentifier("personal_project_token", "string", getPackageName());

        if (appetizeToken != null) {
            projectToken = appetizeToken;
            GiltLog.d("Using appetize project token");
            Optimizely.enableEditor();
            String socketHostname = extras.getString("socketServerHostname");
            if (socketHostname != null) {
                try {
                    final Method setSocketHostMethod = Optimizely.class.getDeclaredMethod("setSocketHost", String.class);
                    setSocketHostMethod.setAccessible(true);
                    setSocketHostMethod.invoke(null, socketHostname);
                } catch (Exception ignored) {}
            }
        } else if (personalConstantsID != 0) {
            projectToken = getResources().getString(personalConstantsID);
            GiltLog.d("Using personal constants token");
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage("You haven't set an Optimizely project token. Please set one in the personal_constants.xml file in the res/values directory");
            builder.create().show();
            GiltLog.d("No project token found");
        }

        return projectToken;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);

        // The below is a hack to workaround the fact that when starting EditMode, the SplashActivity gets created twice-- and we only want to
        // show and create the SignUpActivity once. We also need to show the signupactivity on resume.
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                GiltLog.d("Main Activity is not the root.  Finishing Main Activity instead of launching.");
                finish();
                return;
            }
        }


        Amplitude.getInstance().initialize(this, "ee2f7032e05d3315e1a5f871de0225b4").enableForegroundTracking(getApplication());

        mixpanelAPI = MixpanelAPI.getInstance(this, "1c3bc346b8cf9d88cbe8f651f9fb4189");
        mixpanelAPI.identify("Testing 123");

        amplitudeClient = AmplitudeClient.getInstance();
        amplitudeClient.setUserId("Testing 123");

        JSONObject props = new JSONObject();
        try {
            props.put("accountType", "test");
            props.put("id", 123);
            mixpanelAPI.registerSuperProperties(props);
        } catch (JSONException e) {
            // Pass
        }
        mixpanelAPI.flush();

        Tracker tracker = GoogleAnalytics.getInstance(this).newTracker("UUID");
        OptimizelyUniversalAnalyticsIntegration.setTracker(tracker);

        Toast.makeText(this, "Hello", Toast.LENGTH_LONG).show();

//        Optimizely.enablePreview();
        Optimizely.setVerboseLogging(true);
        Optimizely.setDumpNetworkCalls(true);
        Optimizely.addOptimizelyEventListener(optimizelyListener);
        Optimizely.setEnableUncaughtExceptionHandler(true);

        // The api_key string resource should be set in a file called personal_constants.xml because
        // that file is git ignored and everyone has different project keys.
        // DO NOT SET THIS IS A STRINGS FILE THAT IS SOURCE CONTROLLED OR YOU WILL BREAK THE BUILD

        Optimizely.registerClassWithOptlyFields(AppRater.class, GiltProductsListAdapter.class, SplashActivity.class);
        Optimizely.forceVariation("5472683795", "5458030716");
        Optimizely.setEditGestureEnabled(true);
        Optimizely.enableEditor();
//        Optimizely.enablePreview();
        Optimizely.startOptimizelyWithAPIToken(getOptimizelyToken(), getApplication(), new DefaultOptimizelyEventListener());
        Optimizely.trackEvent("foo");
//        Optimizely.enableEditor();
//        Optimizely.startOptimizelyAsync(getOptimizelyToken(), getApplication(), new DefaultOptimizelyEventListener());
//        Optimizely.setValueForAttributeApiName("bar", "foo", this);


//        Optimizely.enableEditor();
//        Optimizely.startOptimizelyAsync(getOptimizelyToken(), getApplication(), null);

        showSignUpFlow();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Localytics.addAnalyticsListener(new AnalyticsListener() {
                    @Override
                    public void localyticsSessionWillOpen(boolean b, boolean b1, boolean b2) {
                        Log.d("GILT", "localyticsSessionWillOpen");
                    }

                    @Override
                    public void localyticsSessionDidOpen(boolean b, boolean b1, boolean b2) {
                        Log.d("GILT", "localyticsSessionDidOpen");
                    }

                    @Override
                    public void localyticsSessionWillClose() {
                        Log.d("GILT", "localyticsSessionWillClose");
                    }

                    @Override
                    public void localyticsDidTagEvent(String s, Map<String, String> map, long l) {
                        Log.d("GILT", "localyticsDidTagEvent");
                    }
                });
            }
        });

        showSignUpFlowOnResume = false;

        setContentView(R.layout.activity_splash);

        boolean showAnimationManualVal = showAnimationManual.get();
        if (showAnimationManualVal) {
            Toast.makeText(this, "New Preview!", Toast.LENGTH_LONG).show();
        }

    }

    private static OptimizelyEventListener optimizelyListener = new DefaultOptimizelyEventListener() {
        @Override
        public void onOptimizelyStarted() {
            GiltLog.d("OptimizelyStarted");
            GiltLog.printAllExperiments();
        }

        @Override
        public void onOptimizelyFailedToStart(String s) {
            GiltLog.d("Optimizely Failed to start");
        }

        @Override
        public void onOptimizelyEditorEnabled() {
            GiltLog.d("Optimizely editor enabled");
        }

        @Override
        public void onOptimizelyDataFileLoaded() {
            GiltLog.d("Optimizely datafile loaded");
        }

        @Override
        public void onGoalTriggered(String s, List<OptimizelyExperimentData> list) {
            GiltLog.d("Goal triggered: " + s);
            GiltLog.d("Triggered for experiments: ");

            for (int x = 0; x < list.size(); x++) {
                GiltLog.prettyPrintExperiment(list.get(x));
            }

            Optimizely.sendEvents();
        }

        @Override
        public void onMessage(String s, String s2, Bundle bundle) {
        }
    };



    private static OptimizelyCodeBlock signUpFlow = Optimizely.codeBlock("onboardingFlow").withBranchNames("showTutorial");

    private void showSignUpFlow() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                GiltLog.d("Starting sign up flow");


                signUpFlow.execute(new DefaultCodeBranch() {
                    @Override
                    public void execute() {
                        Intent signUpIntent = new Intent(SplashActivity.this, SignInActivity.class);
                        SplashActivity.this.startActivity(signUpIntent);
                    }
                }, new CodeBranch() {
                    @Override
                    public void execute() {
                        Intent tutorialIntent = new Intent(SplashActivity.this, TutorialFlowActivity.class);
                        SplashActivity.this.startActivity(tutorialIntent);
                    }
                });

            }
        }, SPLASH_SCREEN_LENGTH);

    }

    private void showProductListings() {
        Intent salesIntent = new Intent(SplashActivity.this, GiltSalesListActivity.class);
        SplashActivity.this.startActivity(salesIntent);
    }

    private static final String SIGNED_IN_KEY = "hasSignedIn";

    @Override
    protected void onResume() {
        super.onResume();
        if (showSignUpFlowOnResume) {
            showSignUpFlow();
        }

        Log.d("TIME", "Elapsed gilt" + (System.currentTimeMillis() - startTime));
    }

    @Override
    protected void onStop() {
        super.onStop();
        showSignUpFlowOnResume = true;
    }
}
