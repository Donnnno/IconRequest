package de.kaiserdragon.iconrequest;

import de.kaiserdragon.iconrequest.SettingActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;




public class RequestActivity extends AppCompatActivity {
    private static final String TAG = "RequestActivity";
    private static final int BUFFER = 2048;
    private static final boolean DEBUG = true;
    private static final ArrayList<AppInfo> appListAll = new ArrayList<>();
    private static String xmlString;
    private static boolean updateOnly;
    private static ArrayList<AppInfo> appListFilter = new ArrayList<>();
    private String ImgLocation;
    private String ZipLocation;
    private ViewSwitcher switcherLoad;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private Context context;



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


    public static void createZipFile(final String path,
                                     final boolean keepDirectoryStructure,
                                     final String out_file) {
        final File f = new File(path);
        if (!f.canRead() || !f.canWrite()) {
            if (DEBUG) Log.d(TAG, path + " cannot be compressed due to file permissions.");
            return;
        }
        try {
            ZipOutputStream zip_out = new ZipOutputStream(new BufferedOutputStream(
                    new FileOutputStream(out_file), BUFFER));

            if (keepDirectoryStructure) {
                zipFile(path, zip_out, "");
            } else {
                final File[] files = f.listFiles();
                assert files != null;
                for (final File file : files) {
                    zip_folder(file, zip_out);
                }
            }
            zip_out.close();
        } catch (FileNotFoundException e) {
            if (DEBUG) Log.e("File not found", e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            if (DEBUG) Log.e("IOException", e.getMessage());
            e.printStackTrace();
        }

    }

    // keeps directory structure
    public static void zipFile(String path, ZipOutputStream out, String relPath) {
        final File file = new File(path);
        if (!file.exists()) {
            if (DEBUG) Log.d(TAG, file.getName() + " does not exist!");
            return;
        }

        final byte[] buf = new byte[1024];

        final String[] files = file.list();

        if (file.isFile()) {
            try (FileInputStream in = new FileInputStream(file.getAbsolutePath())) {
                out.putNextEntry(new ZipEntry(relPath + file.getName()));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
                out.closeEntry();
            } catch (Exception e) {
                if (DEBUG) Log.d(TAG, e.getMessage());
                e.printStackTrace();
            }
        } else if (files.length > 0) {
            // non-empty folder
            for (String file1 : files) {
                zipFile(path + "/" + file1, out, relPath + file.getName() + "/");
            }
        }
    }

    private static void zip_folder(File file, ZipOutputStream zout) throws IOException {
        byte[] data = new byte[BUFFER];
        int read;
        if (file.isFile()) {
            ZipEntry entry = new ZipEntry(file.getName());
            zout.putNextEntry(entry);
            BufferedInputStream instream = new BufferedInputStream(new FileInputStream(file));
            while ((read = instream.read(data, 0, BUFFER)) != -1)
                zout.write(data, 0, read);
            zout.closeEntry();
            instream.close();
        } else if (file.isDirectory()) {
            String[] list = file.list();
            //int len = list.length;
            for (String aList : list) zip_folder(new File(file.getPath() + "/" + aList), zout);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (DEBUG) Log.v(TAG, "onBackPressed");
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateOnly = getIntent().getBooleanExtra("update", false);

        setContentView(R.layout.activity_request);
        switcherLoad = findViewById(R.id.viewSwitcherLoadingMain);
        context = this;

        ImgLocation = context.getFilesDir() + "/Icons/IconRequest";
        ZipLocation = context.getFilesDir() + "/Icons";


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {

            ExecutorService executors = Executors.newSingleThreadExecutor();
            executors.execute(() -> {
                try {
                    // get included apps
                    parseXML();
                    // compare list to installed apps
                    prepareData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    populateView(appListFilter);
                    switcherLoad.showNext();
                });
            });

        } else {
            populateView(appListFilter);
            switcherLoad.showNext();
        }
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                actionSaveext(actionSave(), result);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (updateOnly) {
            getMenuInflater().inflate(R.menu.menu_request_update, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_request_new, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            actionSend(actionSave());
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            actionSendSave();
            return true;
        } else if (item.getItemId() == R.id.action_sharetext) {
            actionSendText(actionSave());
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            //IconPackManager iconPackManager = new IconPackManager();
            if (DEBUG) Log.v(TAG, String.valueOf(getAvailableIconPacks(true)));
            actionSave();
            actionCopy();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else {
            super.onOptionsItemSelected(item);
            return true;
        }
    }


    private boolean visible(Drawable one, Drawable two) {
        Bitmap bmp1 = getBitmapFromDrawable(one);
        Bitmap bmp2 = getBitmapFromDrawable(two);

        ByteBuffer buffer1 = ByteBuffer.allocate(bmp1.getHeight() * bmp1.getRowBytes());
        bmp1.copyPixelsToBuffer(buffer1);

        ByteBuffer buffer2 = ByteBuffer.allocate(bmp2.getHeight() * bmp2.getRowBytes());
        bmp2.copyPixelsToBuffer(buffer2);

        return Arrays.equals(buffer1.array(), buffer2.array());
    }

    public void makeToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private void actionCopy() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Icon Request", xmlString);
        clipboard.setPrimaryClip(clip);
        makeToast("Your icon request has been saved to the clipboard.");
    }

    private void actionSend(String[] array) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("application/zip");

        File file = new File(ZipLocation + "/" + array[0] + ".zip");

        Uri uri = FileProvider.getUriForFile(
                context, context.getApplicationContext().getPackageName() + ".provider", file);
        //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra("android.intent.extra.SUBJECT", getString(R.string.request_email_subject));
        intent.putExtra("android.intent.extra.TEXT", array[1]);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(Intent.createChooser(intent, null));
        } catch (Exception e) {
            makeToast(getString(R.string.no_email_clients));
            e.printStackTrace();
        }
    }

    private void actionSendText(String[] array) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, xmlString);
        try {
            startActivity(Intent.createChooser(intent, null));
        } catch (Exception e) {
            makeToast(getString(R.string.no_email_clients));
            e.printStackTrace();
        }
    }

    private void actionSaveext(String[] array, ActivityResult result) {

        if (DEBUG) Log.i(TAG, String.valueOf(result));
        File sourceFile = new File(ZipLocation + "/" + array[0] + ".zip");
        Intent data = result.getData();
        try (InputStream is = new FileInputStream(sourceFile); OutputStream os = getContentResolver().openOutputStream(data.getData())) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void actionSendSave() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        SimpleDateFormat date = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US);
        String zipName = date.format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE, "IconRequest_" + zipName);
        activityResultLauncher.launch(intent);

    }


    private String[] actionSave() {
        final File imgLocation = new File(ImgLocation);
        final File zipLocation = new File(ZipLocation);

        // delete old zips and recreate
        deleteDirectory(zipLocation);
        imgLocation.mkdirs();
        zipLocation.mkdirs();

        ArrayList<AppInfo> arrayList = appListFilter;
        StringBuilder stringBuilderEmail = new StringBuilder();
        StringBuilder stringBuilderXML = new StringBuilder();
        stringBuilderEmail.append(getString(R.string.request_email_text));
        int amount = 0;

        // process selected apps
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).selected) {
                String iconName = arrayList.get(i).label
                        .replaceAll("[^a-zA-Z0-9.\\-;]+", "")
                        .toLowerCase();
                if (DEBUG) Log.i(TAG, "iconName: " + iconName);

                stringBuilderEmail.append(arrayList.get(i).label).append("\n");
                stringBuilderXML.append("\t<!-- ")
                        .append(arrayList.get(i).label)
                        .append(" -->\n\t<item component=\"ComponentInfo{")
                        .append(arrayList.get(i).getCode())
                        .append("}\" drawable=\"")
                        .append(iconName)
                        .append("\"/>")
                        .append("\n\n");

                try {
                    Bitmap bitmap = getBitmapFromDrawable(arrayList.get(i).icon);
                    FileOutputStream fOut = new FileOutputStream(ImgLocation + "/" + iconName + ".png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.flush();
                    fOut.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                amount++;
            }
        }

        SimpleDateFormat date = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US);
        String zipName = date.format(new Date());
        xmlString = stringBuilderXML.toString();

        if (amount == 0) {
            // no apps are selected
            makeToast(getString(R.string.request_toast_no_apps_selected));
        } else {
            // write zip and start email intent
            try {
                FileWriter fstream = new FileWriter(ImgLocation + "/appfilter.xml");
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(stringBuilderXML.toString());
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            createZipFile(ImgLocation, true, ZipLocation + "/" + zipName + ".zip");

            // delete all generated files except the zip
            deleteDirectory(imgLocation);
            if (updateOnly) {
                deleteDirectory(zipLocation);
            }
        }
        return new String[]{zipName, stringBuilderEmail.toString()};
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    private void parseXML() {
        try {
            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser myparser = xmlFactoryObject.newPullParser();

            AssetManager am = context.getAssets();
            String xmlLocation = "empty.xml";
            InputStream inputStream = am.open(xmlLocation);
            myparser.setInput(inputStream, null);

            int activity = myparser.getEventType();
            while (activity != XmlPullParser.END_DOCUMENT) {
                String name = myparser.getName();
                switch (activity) {
                    case XmlPullParser.START_TAG:
                        break;
                    case XmlPullParser.END_TAG:
                        if (name.equals("item")) {
                            try {
                                String xmlLabel = myparser.getAttributeValue(null, "drawable");
                                String xmlComponent =
                                        myparser.getAttributeValue(null, "component");

                                String[] xmlCode = xmlComponent.split("/");
                                if (xmlCode.length > 1) {
                                    String xmlPackage = xmlCode[0].substring(14);
                                    String xmlClass = xmlCode[1].substring(0, xmlCode[1].length() - 1);
                                    appListAll.add(new AppInfo(null, null,
                                            xmlLabel, xmlPackage, xmlClass, false));
                                    if (DEBUG) Log.v(TAG, "XML APP: " + xmlLabel);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                }
                activity = myparser.next();
            }
        } catch (Exception e) {
            makeToast(getString(R.string.appfilter_assets));
            e.printStackTrace();
        }
    }


    private void prepareData() {
        // sort the apps
        ArrayList<AppInfo> arrayList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN", null);
        intent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        Iterator<ResolveInfo> localIterator = list.iterator();
        if (DEBUG) Log.v(TAG, "list size: " + list.size());
        boolean notVisible = loadDataBool("SettingOnlyNew");
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo resolveInfo = localIterator.next();
            Drawable icon1 = getHighResIcon(pm, resolveInfo);
            Drawable icon2 = resolveInfo.loadIcon(pm);
            if (DEBUG) Log.v(TAG, String.valueOf(icon2));
            AppInfo appInfo = new AppInfo(icon1,
                    icon2,
                    resolveInfo.loadLabel(pm).toString(),
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name,
                    false);
            if (notVisible) {
                if (DEBUG) Log.v(TAG, "Not Done");
                if (visible(icon1, icon2)) arrayList.add(appInfo);
            } else arrayList.add(appInfo);

        }

        //Custom comparator to ensure correct sorting for characters like and apps
        // starting with a small letter like iNex
        Collections.sort(arrayList, (object1, object2) -> {
            Locale locale = Locale.getDefault();
            Collator collator = Collator.getInstance(locale);
            collator.setStrength(Collator.TERTIARY);

            if (DEBUG)
                Log.v(TAG, "Comparing \"" + object1.label + "\" to \"" + object2.label + "\"");

            return collator.compare(object1.label, object2.label);
        });
        appListFilter = arrayList;
    }

    private Drawable getHighResIcon(PackageManager pm, ResolveInfo resolveInfo) {

        Drawable icon;

        try {
            ComponentName componentName = new ComponentName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            int iconId = resolveInfo.getIconResource();//Get the resource Id for the activity icon

            if (iconId != 0) {
                //Resources.Theme theme = context.getTheme();
                icon = ResourcesCompat.getDrawable(pm.getResourcesForActivity(componentName), iconId, null); //loads unthemed
                //icon = context.getPackageManager().getApplicationIcon(resolveInfo.activityInfo.packageName); //loads themed OnePlus
                //icon =pm.getDrawable(resolveInfo.activityInfo.packageName, iconId, null);               //loads unthemed
                //Drawable adaptiveDrawable = resolveInfo.loadIcon(pm);                                     //loads themed OnePlus
                //PackageManager packageManager = getPackageManager();
                //icon = resolveInfo.loadIcon(packageManager);                                             //loads themed OnePlus

                return icon;
            }
            return resolveInfo.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            //fails return the normal icon
            return resolveInfo.loadIcon(pm);
        } catch (Resources.NotFoundException e) {
            return resolveInfo.loadIcon(pm);
        }
    }

    private void populateView(ArrayList<AppInfo> arrayListFinal) {
        ArrayList<AppInfo> local_arrayList;
        local_arrayList = arrayListFinal;

        ListView grid = findViewById(R.id.app_list);
        grid.setFastScrollEnabled(true);
        grid.setFastScrollAlwaysVisible(true);
        grid.setAdapter(new AppAdapter(this, R.layout.item_request, local_arrayList));
        grid.setOnItemClickListener((AdapterView, view, position, row) -> {
            AppInfo appInfo = (AppInfo) AdapterView.getItemAtPosition(position);
            CheckBox checker = view.findViewById(R.id.CBappSelect);
            ViewSwitcher icon = view.findViewById(R.id.viewSwitcherChecked);
            LinearLayout localBackground = view.findViewById(R.id.card_bg);
            Animation aniIn = AnimationUtils.loadAnimation(context, R.anim.request_flip_in_half_1);
            Animation aniOut = AnimationUtils.loadAnimation(context, R.anim.request_flip_in_half_2);

            checker.toggle();
            appInfo.selected = checker.isChecked();

            icon.setInAnimation(aniIn);
            icon.setOutAnimation(aniOut);

            if (appInfo.selected) {
                if (DEBUG) Log.v(TAG, "Selected App: " + appInfo.label);
                localBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.request_card_pressed));
                if (icon.getDisplayedChild() == 0) {
                    icon.showNext();
                }
            } else {
                if (DEBUG) Log.v(TAG, "Deselected App: " + appInfo.label);
                localBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.request_card_unpressed));
                if (icon.getDisplayedChild() == 1) {
                    icon.showPrevious();
                }
            }
        });
    }

    public boolean loadDataBool(String setting) {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean(setting, false);
    }

    private class AppAdapter extends ArrayAdapter<AppInfo> {
        private final ArrayList<AppInfo> appList = new ArrayList<>();

        public AppAdapter(Context context, int position, ArrayList<AppInfo> adapterArrayList) {
            super(context, position, adapterArrayList);
            appList.addAll(adapterArrayList);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.item_request, null);
                holder = new ViewHolder();
                holder.apkIcon = convertView.findViewById(R.id.IVappIcon);
                holder.apkIconnow = convertView.findViewById(R.id.IVappIconnow);
                holder.apkName = convertView.findViewById(R.id.TVappName);
                holder.apkPackage = convertView.findViewById(R.id.TVappPackage);
                holder.apkClass = convertView.findViewById(R.id.TVappClass);
                holder.checker = convertView.findViewById(R.id.CBappSelect);
                holder.cardBack = convertView.findViewById(R.id.card_bg);
                holder.switcherChecked = convertView.findViewById(R.id.viewSwitcherChecked);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppInfo appInfo = appList.get(position);

            holder.apkPackage.setText(appInfo.packageName);
            holder.apkClass.setText(appInfo.className);
            holder.apkName.setText(appInfo.label);
            holder.apkIcon.setImageDrawable(appInfo.icon);
            holder.apkIconnow.setImageDrawable(appInfo.icon2);
            if (loadDataBool("SettingRow") == true) {
                holder.apkIconnow.setVisibility(View.VISIBLE);
            }


            holder.switcherChecked.setInAnimation(null);
            holder.switcherChecked.setOutAnimation(null);
            holder.checker.setChecked(appInfo.selected);
            if (appInfo.selected) {
                holder.cardBack.setBackgroundColor(ContextCompat.getColor(context, R.color.request_card_pressed));
                if (holder.switcherChecked.getDisplayedChild() == 0) {
                    holder.switcherChecked.showNext();
                }
            } else {
                holder.cardBack.setBackgroundColor(ContextCompat.getColor(context, R.color.request_card_unpressed));
                if (holder.switcherChecked.getDisplayedChild() == 1) {
                    holder.switcherChecked.showPrevious();
                }
            }
            return convertView;
        }

        private class ViewHolder {
            TextView apkName;
            TextView apkPackage;
            TextView apkClass;
            ImageView apkIcon;
            ImageView apkIconnow;
            CheckBox checker;
            LinearLayout cardBack;
            ViewSwitcher switcherChecked;

        }
    }


        private android.app.Application mContext;

        public class IconPack {
            public String packageName;
            public String name;

            private boolean mLoaded = false;
            private HashMap<String, String> mPackagesDrawables = new HashMap<String, String>();

            private List<Bitmap> mBackImages = new ArrayList<Bitmap>();
            private Bitmap mMaskImage = null;
            private Bitmap mFrontImage = null;
            private float mFactor = 1.0f;

            Resources iconPackres = null;

            public void load() {
                // load appfilter.xml from the icon pack package
                PackageManager pm = mContext.getPackageManager();
                try {
                    XmlPullParser xpp = null;

                    iconPackres = pm.getResourcesForApplication(packageName);
                    int appfilterid = iconPackres.getIdentifier("appfilter", "xml", packageName);
                    if (appfilterid > 0) {
                        xpp = iconPackres.getXml(appfilterid);
                    } else {
                        // no resource found, try to open it from assests folder
                        try {
                            InputStream appfilterstream = iconPackres.getAssets().open("appfilter.xml");

                            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                            factory.setNamespaceAware(true);
                            xpp = factory.newPullParser();
                            xpp.setInput(appfilterstream, "utf-8");
                        } catch (IOException e1) {
                            //Ln.d("No appfilter.xml file");
                        }
                    }

                    if (xpp != null) {
                        int eventType = xpp.getEventType();
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                if (xpp.getName().equals("iconback")) {
                                    for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                        if (xpp.getAttributeName(i).startsWith("img")) {
                                            String drawableName = xpp.getAttributeValue(i);
                                            Bitmap iconback = loadBitmap(drawableName);
                                            if (iconback != null)
                                                mBackImages.add(iconback);
                                        }
                                    }
                                } else if (xpp.getName().equals("iconmask")) {
                                    if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                        String drawableName = xpp.getAttributeValue(0);
                                        mMaskImage = loadBitmap(drawableName);
                                    }
                                } else if (xpp.getName().equals("iconupon")) {
                                    if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                        String drawableName = xpp.getAttributeValue(0);
                                        mFrontImage = loadBitmap(drawableName);
                                    }
                                } else if (xpp.getName().equals("scale")) {
                                    // mFactor
                                    if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor")) {
                                        mFactor = Float.valueOf(xpp.getAttributeValue(0));
                                    }
                                } else if (xpp.getName().equals("item")) {
                                    String componentName = null;
                                    String drawableName = null;

                                    for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                        if (xpp.getAttributeName(i).equals("component")) {
                                            componentName = xpp.getAttributeValue(i);
                                        } else if (xpp.getAttributeName(i).equals("drawable")) {
                                            drawableName = xpp.getAttributeValue(i);
                                        }
                                    }
                                    if (!mPackagesDrawables.containsKey(componentName))
                                        mPackagesDrawables.put(componentName, drawableName);
                                }
                            }
                            eventType = xpp.next();
                        }
                    }
                    mLoaded = true;
                } catch (PackageManager.NameNotFoundException e) {
                    //Ln.d("Cannot load icon pack");
                } catch (XmlPullParserException e) {
                    //Ln.d("Cannot parse icon pack appfilter.xml");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            private Bitmap loadBitmap(String drawableName) {
                int id = iconPackres.getIdentifier(drawableName, "drawable", packageName);
                if (id > 0) {
                    Drawable bitmap = iconPackres.getDrawable(id);
                    if (bitmap instanceof BitmapDrawable)
                        return ((BitmapDrawable) bitmap).getBitmap();
                }
                return null;
            }

            private Drawable loadDrawable(String drawableName) {
                int id = iconPackres.getIdentifier(drawableName, "drawable", packageName);
                if (id > 0) {
                    Drawable bitmap = iconPackres.getDrawable(id);
                    return bitmap;
                }
                return null;
            }

            public Drawable getDrawableIconForPackage(String appPackageName, Drawable defaultDrawable) {
                if (!mLoaded)
                    load();

                PackageManager pm = mContext.getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(appPackageName);
                String componentName = null;
                if (launchIntent != null)
                    componentName = pm.getLaunchIntentForPackage(appPackageName).getComponent().toString();
                String drawable = mPackagesDrawables.get(componentName);
                if (drawable != null) {
                    return loadDrawable(drawable);
                } else {
                    // try to get a resource with the component filename
                    if (componentName != null) {
                        int start = componentName.indexOf("{") + 1;
                        int end = componentName.indexOf("}", start);
                        if (end > start) {
                            drawable = componentName.substring(start, end).toLowerCase(Locale.getDefault()).replace(".", "_").replace("/", "_");
                            if (iconPackres.getIdentifier(drawable, "drawable", packageName) > 0)
                                return loadDrawable(drawable);
                        }
                    }
                }
                return defaultDrawable;
            }

            public Bitmap getIconForPackage(String appPackageName, Bitmap defaultBitmap) {
                if (!mLoaded)
                    load();

                PackageManager pm = mContext.getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(appPackageName);
                String componentName = null;
                if (launchIntent != null)
                    componentName = pm.getLaunchIntentForPackage(appPackageName).getComponent().toString();
                String drawable = mPackagesDrawables.get(componentName);
                if (drawable != null) {
                    return loadBitmap(drawable);
                } else {
                    // try to get a resource with the component filename
                    if (componentName != null) {
                        int start = componentName.indexOf("{") + 1;
                        int end = componentName.indexOf("}", start);
                        if (end > start) {
                            drawable = componentName.substring(start, end).toLowerCase(Locale.getDefault()).replace(".", "_").replace("/", "_");
                            if (iconPackres.getIdentifier(drawable, "drawable", packageName) > 0)
                                return loadBitmap(drawable);
                        }
                    }
                }
                return generateBitmap(appPackageName, defaultBitmap);
            }

            private Bitmap generateBitmap(String appPackageName, Bitmap defaultBitmap) {
                // the key for the cache is the icon pack package name and the app package name
                String key = packageName + ":" + appPackageName;

                // if generated bitmaps cache already contains the package name return it
//            Bitmap cachedBitmap = BitmapCache.getInstance(mContext).getBitmap(key);
//            if (cachedBitmap != null)
//                return cachedBitmap;

                // if no support images in the icon pack return the bitmap itself
                if (mBackImages.size() == 0)
                    return defaultBitmap;

                Random r = new Random();
                int backImageInd = r.nextInt(mBackImages.size());
                Bitmap backImage = mBackImages.get(backImageInd);
                int w = backImage.getWidth();
                int h = backImage.getHeight();

                // create a bitmap for the result
                Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas mCanvas = new Canvas(result);

                // draw the background first
                mCanvas.drawBitmap(backImage, 0, 0, null);

                // create a mutable mask bitmap with the same mask
                Bitmap scaledBitmap = defaultBitmap;
                if (defaultBitmap != null && (defaultBitmap.getWidth() > w || defaultBitmap.getHeight() > h))
                    Bitmap.createScaledBitmap(defaultBitmap, (int) (w * mFactor), (int) (h * mFactor), false);

                if (mMaskImage != null) {
                    // draw the scaled bitmap with mask
                    Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas maskCanvas = new Canvas(mutableMask);
                    maskCanvas.drawBitmap(mMaskImage, 0, 0, new Paint());

                    // paint the bitmap with mask into the result
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                    mCanvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
                    mCanvas.drawBitmap(mutableMask, 0, 0, paint);
                    paint.setXfermode(null);
                } else // draw the scaled bitmap without mask
                {
                    mCanvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
                }

                // paint the front
                if (mFrontImage != null) {
                    mCanvas.drawBitmap(mFrontImage, 0, 0, null);
                }

                // store the bitmap in cache
//            BitmapCache.getInstance(mContext).putBitmap(key, result);

                // return it
                return result;
            }
        }
            private HashMap<String, IconPack> iconPacks = null;

            public HashMap<String, IconPack> getAvailableIconPacks(boolean forceReload) {
                if (iconPacks == null || forceReload) {
                    iconPacks = new HashMap<String, IconPack>();

                    // find apps with intent-filter "com.gau.go.launcherex.theme" and return build the HashMap
                    PackageManager pm = getPackageManager();

                    List<ResolveInfo> adwlauncherthemes = pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA);
                    List<ResolveInfo> golauncherthemes = pm.queryIntentActivities(new Intent("com.gau.go.launcherex.theme"), PackageManager.GET_META_DATA);

                    // merge those lists
                    List<ResolveInfo> rinfo = new ArrayList<ResolveInfo>(adwlauncherthemes);
                    rinfo.addAll(golauncherthemes);

                    for (ResolveInfo ri : rinfo) {
                        IconPack ip = new IconPack();
                        ip.packageName = ri.activityInfo.packageName;

                        ApplicationInfo ai = null;
                        try {
                            ai = pm.getApplicationInfo(ip.packageName, PackageManager.GET_META_DATA);
                            ip.name = getPackageManager().getApplicationLabel(ai).toString();
                            iconPacks.put(ip.packageName, ip);
                        } catch (PackageManager.NameNotFoundException e) {
                            // shouldn't happen
                            e.printStackTrace();
                        }
                    }
                }
                return iconPacks;
            }
        }

