package br.com.jylabtech.pagseguro_payment_tech.printer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;

/** Busca do arquivo de printerFile no armazenamento do app (substitui o /Download público). */
public class PrintFileResolverTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void procuraNosDiretoriosDoAppNaOrdem() throws Exception {
    File downloads = tmp.newFolder("downloads");
    File external = tmp.newFolder("external");
    File files = tmp.newFolder("files");
    File inExternal = new File(external, "cupom.png");
    inExternal.createNewFile();
    File inFiles = new File(files, "cupom.png");
    inFiles.createNewFile();

    assertEquals(inExternal, PrintFileResolver.resolve("cupom.png", Arrays.asList(downloads, external, files)));

    File inDownloads = new File(downloads, "cupom.png");
    inDownloads.createNewFile();
    assertEquals(inDownloads, PrintFileResolver.resolve("cupom.png", Arrays.asList(downloads, external, files)));
  }

  @Test
  public void diretoriosNulosSaoIgnorados() throws Exception {
    File cache = tmp.newFolder("cache");
    File file = new File(cache, "a.txt");
    file.createNewFile();
    // getExternalFilesDir pode devolver null (armazenamento externo indisponível).
    assertEquals(file, PrintFileResolver.resolve("a.txt", Arrays.asList(null, null, cache)));
  }

  @Test
  public void caminhoAbsolutoExistenteEUsadoDireto() throws Exception {
    File file = tmp.newFile("absoluto.png");
    assertEquals(file, PrintFileResolver.resolve(file.getAbsolutePath(), Arrays.asList(tmp.newFolder("x"))));
    assertNull(PrintFileResolver.resolve(new File(tmp.getRoot(), "nao-existe.png").getAbsolutePath(), null));
  }

  @Test
  public void inexistenteVazioOuDiretorioDevolvemNull() throws Exception {
    File dir = tmp.newFolder("d");
    new File(dir, "pasta").mkdir();
    assertNull(PrintFileResolver.resolve("nada.png", Arrays.asList(dir)));
    assertNull(PrintFileResolver.resolve("pasta", Arrays.asList(dir)));
    assertNull(PrintFileResolver.resolve("", Arrays.asList(dir)));
    assertNull(PrintFileResolver.resolve(null, Arrays.asList(dir)));
    assertNull(PrintFileResolver.resolve("a.png", null));
  }

  @Test
  public void semContextNaoHaDiretorios() {
    assertEquals(0, PrintFileResolver.appDirectories(null).size());
  }
}
