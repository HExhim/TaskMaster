package com.twa.taskmaster.core.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class CSVExporter {

    public static Uri exportToCSV(Context context, String fileName, List<String> headers, List<List<String>> rows) {
        try {
            // Create export directory if not exists
            File exportDir = new File(context.getExternalFilesDir(null), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // Create CSV file
            File file = new File(exportDir, fileName + ".csv");
            FileWriter writer = new FileWriter(file);

            // Write headers
            for (int i = 0; i < headers.size(); i++) {
                writer.append(escapeCSV(headers.get(i)));
                if (i < headers.size() - 1) writer.append(",");
            }
            writer.append("\n");

            // Write rows
            for (List<String> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    writer.append(escapeCSV(row.get(i)));
                    if (i < row.size() - 1) writer.append(",");
                }
                writer.append("\n");
            }

            writer.flush();
            writer.close();

            // Create URI using FileProvider
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    file
            );

        } catch (Exception e) {
            Log.e("CSVExporter", "Export failed", e);
            return null;
        }
    }

    // Escapes fields containing commas, quotes or newlines
    private static String escapeCSV(String value) {
        if (value == null) return "";
        boolean hasSpecial = value.contains(",") || value.contains("\"") || value.contains("\n");
        if (hasSpecial) {
            value = value.replace("\"", "\"\""); // escape double quotes
            return "\"" + value + "\"";
        }
        return value;
    }
}
