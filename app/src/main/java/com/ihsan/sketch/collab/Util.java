package com.ihsan.sketch.collab;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;

import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Util {
    public static Hashtable<String, String> key2name = new Hashtable<>();
    private static final String TAG = "Util";

    public static String base64encrypt(String txt) {
        byte[] data = txt.getBytes();
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public static String base64decrypt(String base64) {
        byte[] data = Base64.decode(base64, Base64.DEFAULT);
        return new String(data);
    }

    public static ArrayList<SketchwareProject> getSketchwareProjects() {
        ArrayList<SketchwareProject> sketchwareProjects = new ArrayList<>();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/.sketchware/mysc/list/";
        Log.d(TAG, "getSketchwareProjects: " +path);
        for (String pat: listDir(path)) {
            try {
                Log.d(TAG, "getSketchwareProjects: " + pat + "/project");
                JSONObject project = new JSONObject(decrypt(pat + "/project"));
                SketchwareProject sw_proj = new SketchwareProject(
                        project.getString("my_app_name"),
                        project.getString("sc_ver_name"),
                        project.getString("my_sc_pkg_name"),
                        project.getString("my_ws_name"),
                        project.getString("sc_id"));
                sketchwareProjects.add(0, sw_proj);
            } catch (Exception e) {
                Log.e(TAG, "getSketchwareProjects: " + e.toString());
            }
        }
        return sketchwareProjects;
    }

    public static String decrypt(String path) {
        try {
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");;
            byte[] bytes = "sketchwaresecure".getBytes();
            instance.init(2, new SecretKeySpec(bytes, "AES"), new IvParameterSpec(bytes));
            RandomAccessFile randomAccessFile = new RandomAccessFile(path, "r");
            byte[] bArr = new byte[((int) randomAccessFile.length())];
            randomAccessFile.readFully(bArr);
            return new String(instance.doFinal(bArr));
        } catch (Exception ignored) {}
        return "ERROR WHILE DECRYPTING";
    }

    public static String encrypt(String str) {
        // The boolean is to determine if the process is successful or not
        try {
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] bytes = "sketchwaresecure".getBytes();
            instance.init(1, new SecretKeySpec(bytes, "AES"), new IvParameterSpec(bytes));
            byte[] doFinal = instance.doFinal(str.getBytes());
            return new String(doFinal);
        } catch (Exception ignored) { }
        return "ERROR WHILE ENCRYPTING";
    }

    public static ArrayList<String> listDir(String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        File file = new File(str);
        if (file.exists() && !file.isFile()) {
            File[] listFiles = file.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                arrayList.clear();
                for (File absolutePath : listFiles) {
                    arrayList.add(absolutePath.getAbsolutePath());
                }
            }
        }
        return arrayList;
    }
}
