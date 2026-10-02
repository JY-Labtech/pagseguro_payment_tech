package br.com.jylabtech.pagseguro_payment_tech.printer;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Localiza o arquivo de {@code printerFile(fileName)} no armazenamento do próprio app.
 *
 * Antes o arquivo era procurado em {@code Environment.getExternalStorageDirectory()/Download}
 * (API obsoleta e sem acesso no Android 10+ por causa do scoped storage). Agora a busca é, nesta
 * ordem: caminho absoluto existente, {@code getExternalFilesDir(DIRECTORY_DOWNLOADS)},
 * {@code getExternalFilesDir(null)} (o mesmo diretório de {@code getExternalStorageDirectory()} do
 * path_provider no Dart), {@code getFilesDir()} e {@code getCacheDir()}.
 * A lógica de busca é pura ({@link #resolve(String, List)}) e testada na JVM.
 */
public final class PrintFileResolver {
  private PrintFileResolver() {
  }

  /** Diretórios do app onde o arquivo é procurado (pode chamar IO: use fora da main thread). */
  public static List<File> appDirectories(Context context) {
    if (context == null) {
      return Collections.emptyList();
    }
    List<File> dirs = new ArrayList<>();
    dirs.add(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));
    dirs.add(context.getExternalFilesDir(null));
    dirs.add(context.getFilesDir());
    dirs.add(context.getCacheDir());
    return dirs;
  }

  /** @return o primeiro arquivo existente para {@code fileName}, ou {@code null}. */
  public static File resolve(String fileName, List<File> baseDirs) {
    if (fileName == null || fileName.trim().isEmpty()) {
      return null;
    }
    File direct = new File(fileName);
    if (direct.isAbsolute()) {
      return direct.isFile() ? direct : null;
    }
    if (baseDirs != null) {
      for (File dir : baseDirs) {
        if (dir == null) {
          continue;
        }
        File candidate = new File(dir, fileName);
        if (candidate.isFile()) {
          return candidate;
        }
      }
    }
    return null;
  }
}
