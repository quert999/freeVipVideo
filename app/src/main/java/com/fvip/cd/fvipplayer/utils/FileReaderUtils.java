package com.fvip.cd.fvipplayer.utils;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileReaderUtils {
    public final static String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/webview/web.mht";

    private static String readFromInputStream(InputStream is) {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String str;
        try {
            while ((str = reader.readLine()) != null) {
                builder.append(str).append("\n");
            }
        } catch (IOException ignored) {
        }

        return builder.toString();
    }

    public static String readFromAsset(String assetName, Context context) {
        InputStream is = null;
        try {
            is = context.getAssets().open(assetName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (is == null) return "";
        return readFromInputStream(is);
    }

    public static String readFromFile(String filePath) {
        File file = new File(filePath);
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String str;
            while ((str = reader.readLine()) != null) {
                builder.append(str).append("\n");
            }
        } catch (IOException ignored) {
        }

        return builder.toString();
    }

    public static void checkFile() {
        File file = new File(FILE_PATH);
        try {
            System.out.println(file.createNewFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
