package de.kaiserdragon.iconrequest;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.kaiserdragon.iconrequest.helper.CommonHelper;
import de.kaiserdragon.iconrequest.helper.DrawableHelper;
import de.kaiserdragon.iconrequest.helper.SettingsHelper;
import de.kaiserdragon.iconrequest.helper.ShareHelper;
import de.kaiserdragon.iconrequest.helper.XMLParserHelper;


public class RequestActivity extends AppCompatActivity {
    private static final String TAG = "RequestActivity";
    private static final boolean DEBUG = true;
    private static ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static boolean updateOnly = false;
    private static int mode;
    private static boolean OnlyNew;
    private static boolean SecondIcon;
    private static boolean Shortcut;
    private static boolean ActionMain;
    private static boolean firstrun;
    private final Context context = this;
    public static byte[] zipData = null;
    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private boolean IPackChoosen = false;
    private RecyclerView recyclerView;
    private AppAdapter adapter;

    public static void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            assert files != null;
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        path.delete();
    }

    private ArrayList<AppInfo> compare() {
        ArrayList<AppInfo> newList = new ArrayList<>();
        ArrayList<AppInfo> Listdopp = new ArrayList<>();

        if (mode == 2) {
            for (AppInfo appInfo : appListAll) {
                if (newList.contains(appInfo)) {
                    newList.remove(appInfo);
                } else newList.add(appInfo);
            }
            return sort(newList);
        }
        if (mode == 3) {
            for (AppInfo appInfo : appListAll) {
                if (!newList.contains(appInfo)) {
                    newList.add(appInfo);
                } else {
                    AppInfo geticon = newList.get(newList.indexOf(appInfo));  //get the element by passing the index of the element
                    appInfo.icon2 = geticon.icon;
                    Listdopp.add(appInfo);
                }
            }

            return sort(Listdopp);
        }
        if (mode == 4) {
            for (AppInfo appInfo : appListAll) {
                if (!newList.contains(appInfo)) {
                    newList.add(appInfo);
                } else {
                    AppInfo geticon = newList.get(newList.indexOf(appInfo));  //get the element by passing the index of the element
                    appInfo.icon2 = geticon.icon;
                    Listdopp.add(appInfo);
                }
            }

            return sort(Listdopp);
        }
        if (mode == 5) {
            for (AppInfo appInfo : appListAll) {
                if (appInfo.icon == null) newList.add(appInfo);
            }
            return sort(newList);
        }
        return null;
    }

    private ArrayList<AppInfo> sort(ArrayList<AppInfo> chaos) {
        Collections.sort(chaos, (object1, object2) -> {
            Locale locale = Locale.getDefault();
            Collator collator = Collator.getInstance(locale);
            collator.setStrength(Collator.TERTIARY);

            if (DEBUG)
                Log.v(TAG, "Comparing \"" + object1.label + "\" to \"" + object2.label + "\"");

            return collator.compare(object1.label, object2.label);
        });
        return chaos;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appListAll.clear();
        mode = getIntent().getIntExtra("update", 0);
        updateOnly = mode == 0;
        OnlyNew = SettingsHelper.loadDataBool("SettingOnlyNew",this);
        SecondIcon = SettingsHelper.loadDataBool("SettingRow",this);
        Shortcut = SettingsHelper.loadDataBool("Shortcut",this);
        ActionMain = SettingsHelper.loadDataBool("ActionMain",this);
        firstrun = false;

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Back is pressed... Finishing the activity
                if (DEBUG) Log.v(TAG, "onBackPressed");
                finish();
            }
        });

        setContentView(R.layout.activity_request);
        switcherLoad = findViewById(R.id.viewSwitcherLoadingMain);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            if (OnlyNew || SecondIcon || (mode >= 2 && mode <= 5)) {
                adapter = new AppAdapter(prepareData(true));
            } else adapter = new AppAdapter(prepareData(false)); //show all apps
            runOnUiThread(() -> {
                if (!OnlyNew && !SecondIcon && (mode < 2 || mode > 5))
                    findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                if(adapter.AdapterSize() < 1){
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();

            });
        });
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> ShareHelper.actionSaveExt(ShareHelper.actionSave(adapter,updateOnly,mode,context),zipData,result,context));
    }

    public void IPackSelect(String packageName) {
        switcherLoad.showNext();
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            try {
                XMLParserHelper.parseXML(packageName, SecondIcon || (mode >= 2 && mode <= 5), appListAll, context);
                if (DEBUG) Log.v(TAG, packageName);

                if (mode < 2 || mode > 5) {
                    adapter = new AppAdapter(prepareData(false));
                }
                if (!(mode <= 1) && (mode != 2 && mode != 3 || firstrun)) {
                    adapter = new AppAdapter(compare());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                // Show IPack chooser a second Time
                if (mode != 2 && mode != 3 || firstrun) {
                    findViewById(R.id.text_ipack_chooser).setVisibility(View.GONE);
                    IPackChoosen = true;
                    invalidateOptionsMenu();
                }
                firstrun = true;
                if(adapter.AdapterSize() < 1){
                    findViewById(R.id.Nothing).setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                switcherLoad.showNext();
            });
        });
    }


    public boolean onCreateOptionsMenu(Menu menu) {


        if ((OnlyNew || (mode >= 2 && mode <= 5)) && !IPackChoosen) {
            getMenuInflater().inflate(R.menu.menu_iconpack_chooser, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_request, menu);
            MenuItem save = menu.findItem(R.id.action_save);
            MenuItem share = menu.findItem(R.id.action_share);
            MenuItem share_text = menu.findItem(R.id.action_sharetext);
            MenuItem copy = menu.findItem(R.id.action_copy);
            MenuItem searchItem = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) searchItem.getActionView();



            if (updateOnly || (mode >= 2 && mode <= 5)) {
                save.setVisible(false);
                share.setVisible(false);
                share_text.setVisible(true);
                copy.setVisible(true);
            } else {
                share_text.setVisible(false);
                copy.setVisible(false);
                save.setVisible(true);
                share.setVisible(true);
            }
            // Set up search functionality
            assert searchView != null;

            searchView.setMaxWidth(700);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // Handle search query submission
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Handle search query text change
                    adapter.filter(newText);
                    return true;
                }
            });
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            ShareHelper.actionSend(ShareHelper.actionSave(adapter,updateOnly,mode,context),zipData,context);
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            ShareHelper.actionSendSave(activityResultLauncher);
            return true;
        } else if (item.getItemId() == R.id.action_sharetext) {
            ShareHelper.actionSendText(ShareHelper.actionSave(adapter,updateOnly,mode,context),context);
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            ShareHelper.actionCopy(ShareHelper.actionSave(adapter,updateOnly,mode,context),context);
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (item.getItemId() == R.id.selectall) {
            adapter.setAllSelected(!item.isChecked());
            item.setChecked(!item.isChecked());
            return true;
        } else {
            super.onOptionsItemSelected(item);
            return true;
        }
    }

    private void parseXML(String packageName) {
        // load appfilter.xml from the icon pack package
        Resources iconPackres;
        PackageManager pm = getPackageManager();

        try {
            iconPackres = pm.getResourcesForApplication(packageName);
            XmlPullParser xpp = null;
            int appfilterid = iconPackres.getIdentifier("appfilter", "xml", packageName);
            if (appfilterid > 0) {
                xpp = iconPackres.getXml(appfilterid);
            } else {
                try {
                    InputStream appfilterstream = iconPackres.getAssets().open("appfilter.xml");
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    xpp = factory.newPullParser();
                    xpp.setInput(appfilterstream, "utf-8");
                } catch (IOException e1) {
                    CommonHelper.makeToast(getString(R.string.appfilter_assets),context);
                    Log.v(TAG, "No appfilter.xml file");
                }
            }
            //write content of icon pack appfilter to the appListAll arraylist
            if (xpp != null) {
                int activity = xpp.getEventType();
                while (activity != XmlPullParser.END_DOCUMENT) {
                    String name = xpp.getName();
                    switch (activity) {
                        case XmlPullParser.END_TAG:
                            break;
                        case XmlPullParser.START_TAG:
                            if (name.equals("item")) {
                                try {
                                    String xmlLabel = xpp.getAttributeValue(null, "drawable");
                                    if (xmlLabel != null) {
                                        String xmlComponent = xpp.getAttributeValue(null, "component");
                                        if (xmlComponent != null) {

                                            String[] xmlCode = xmlComponent.split("/");
                                            if (xmlCode.length > 1) {
                                                String xmlPackage = xmlCode[0].substring(14);
                                                String xmlClass = xmlCode[1].substring(0, xmlCode[1].length() - 1);
                                                Drawable icon = null;
                                                if (SecondIcon || (mode >= 2 && mode <= 5)) {
                                                    icon = DrawableHelper.loadDrawable(xmlLabel, iconPackres, packageName);
                                                }
                                                appListAll.add(new AppInfo(icon, null, xmlLabel, xmlPackage, xmlClass, false));
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                    }
                    activity = xpp.next();
                }
            }
        } catch (Exception e) {
            CommonHelper.makeToast(getString(R.string.appfilter_assets),context);
            e.printStackTrace();
        }
    }

    private ArrayList<AppInfo> prepareData(boolean iPack) {
        // sort the apps
        ArrayList<AppInfo> arrayList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent;

        if (iPack) {
            intent = new Intent("org.adw.launcher.THEMES", null);
        } else if (Shortcut) {
            intent = new Intent("android.intent.action.CREATE_SHORTCUT", null);
            intent.addCategory("android.intent.category.DEFAULT");
        } else if (ActionMain) {
            intent = new Intent("android.intent.action.MAIN", null);
        } else {
            intent = new Intent("android.intent.action.MAIN", null);
            intent.addCategory("android.intent.category.LAUNCHER");
        }

        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);

        if (list.size() < 1 && iPack){
            OnlyNew =false;
            SecondIcon=false;
            iPack = false;
            if (Shortcut && mode <= 1) {
                intent = new Intent("android.intent.action.CREATE_SHORTCUT", null);
                intent.addCategory("android.intent.category.DEFAULT");
            }
            else if ( mode <= 1 && ActionMain){
                    intent = new Intent("android.intent.action.MAIN", null);
            }
            else if ( mode <= 1 ){
                intent = new Intent("android.intent.action.MAIN", null);
                intent.addCategory("android.intent.category.LAUNCHER");
            }
            list = pm.queryIntentActivities(intent, 0);

        }

        if (DEBUG) Log.v(TAG, "list size: " + list.size());

        for (ResolveInfo resolveInfo : list) {
            Drawable icon1 = DrawableHelper.getHighResIcon(pm, resolveInfo);
            AppInfo appInfo = new AppInfo(icon1, null, resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, false);

            if (SecondIcon && !iPack) {
                Drawable icon2 = null;
                if (appListAll.contains(appInfo)) {
                    AppInfo geticon = appListAll.get(appListAll.indexOf(appInfo));
                    icon2 = geticon.icon;
                }
                appInfo = new AppInfo(icon1, icon2, resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, false);
            }

            if (OnlyNew && !iPack) {
                if (!appListAll.contains(appInfo)) {
                    arrayList.add(appInfo);
                    if (DEBUG) Log.i(TAG, "Added app: " + resolveInfo.loadLabel(pm));
                } else {
                    if (DEBUG) Log.v(TAG, "Removed app: " + resolveInfo.loadLabel(pm));
                }
            } else {
                arrayList.add(appInfo);
            }
        }

        return sort(arrayList);
    }

    public class AppAdapter extends RecyclerView.Adapter<AppViewHolder> {
        private final List<AppInfo> appList;
        private List<AppInfo> filteredList;

        public AppAdapter(List<AppInfo> appList) {
            this.appList = appList;
            this.filteredList = new ArrayList<>(appList);
        }

        public void filter(String query) {
            filteredList.clear();
            if (query.isEmpty()) {
                filteredList.addAll(appList);
            } else {
                String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
                for (AppInfo app : appList) {
                    if (app.getLabel().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                        filteredList.add(app);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public int AdapterSize(){
            return this.appList.size();
        }

        public ArrayList<AppInfo> getAllSelected() {
            ArrayList<AppInfo> arrayList = new ArrayList<>();
            for (AppInfo app : appList) {
                if (app.selected) arrayList.add(app);
            }
            return arrayList;
        }

        public void setAllSelected(boolean selected) {
            for (AppInfo app : appList) {
                app.setSelected(selected);
            }
            notifyDataSetChanged();
        }


        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
            return new AppViewHolder(v, filteredList);
        }


        @Override
        public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
            AppInfo app = filteredList.get(position);
            holder.labelView.setText(app.getLabel());
            holder.packageNameView.setText(app.packageName);
            holder.classNameView.setText(app.className);
            holder.imageView.setImageDrawable(app.getIcon());
            if (app.selected) holder.checkBox.setDisplayedChild(1);
            else holder.checkBox.setDisplayedChild(0);
            if ((SecondIcon || mode == 3 || mode == 4) && IPackChoosen && !(mode == 2)) {
                holder.apkIconView.setVisibility(View.VISIBLE);
                holder.apkIconView.setImageDrawable(app.getIcon2());
            }

        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }
    }

    public  class AppViewHolder extends RecyclerView.ViewHolder {
        public TextView labelView;
        public TextView packageNameView;
        public TextView classNameView;
        public ImageView imageView;
        public ImageView apkIconView;
        public ViewSwitcher checkBox;

        public AppViewHolder(View v, List<AppInfo> appList) {
            super(v);
            labelView = v.findViewById(R.id.label_view);
            packageNameView = v.findViewById(R.id.packagename_view);
            classNameView = v.findViewById(R.id.classname_view);
            imageView = v.findViewById(R.id.icon_view);
            apkIconView = v.findViewById(R.id.apkicon_view);
            checkBox = v.findViewById(R.id.SwitcherChecked);

            v.setOnClickListener(v1 -> {
                int position = getAdapterPosition();
                AppInfo app = appList.get(position);
                app.setSelected(!app.isSelected());
                if (!IPackChoosen && (OnlyNew || SecondIcon || (mode >= 2 && mode <= 5)))
                    IPackSelect(app.packageName);
                Animation aniIn = AnimationUtils.loadAnimation(checkBox.getContext(), R.anim.request_flip_in_half_1);
                Animation aniOut = AnimationUtils.loadAnimation(checkBox.getContext(), R.anim.request_flip_in_half_2);
                checkBox.setInAnimation(aniIn);
                checkBox.setOutAnimation(aniOut);
                checkBox.showNext();

            });
        }
    }
}