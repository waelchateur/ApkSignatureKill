package com.signs.yowal.utils;

import android.content.Context;
import android.util.Base64;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.dexlib2.dexbacked.raw.ItemType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import bin.xml.decode.AXmlDecoder;
import bin.xml.decode.AXmlResourceParser;
import bin.xml.decode.XmlPullParser;

public class BinPlusSignatureTool {
    private boolean customApplication = false;
    private String customApplicationName;
    private Context mContext;
    private String outApk;
    private String packageName;
    private String signatures;
    private String srcApk;
    private String tempApk;

    public BinPlusSignatureTool(Context context) {
        mContext = context;
    }

    public void setPath(String input, String output) {
        srcApk = input;
        outApk = output;
        tempApk = new File(srcApk).getParentFile().toString() + "/.temp";
    }

    /*public void Kill() {
        new File(outApk).delete();
        System.out.println("Чтение подписи:" + srcApk);
        signatures = getApkSignInfo(srcApk);
        System.out.println("Чтение APK:" + srcApk);
        try (ZipFile zipFile = new ZipFile(srcApk);) {
            System.out.println("  -- Обработка AndroidManifest.xml");
            ZipEntry manifestEntry = zipFile.getEntry("AndroidManifest.xml");
            byte[] manifestData  = parseManifest(zipFile.getInputStream(manifestEntry));

            ZipEntry dexEntry = zipFile.getEntry("classes.dex");
            DexBackedDexFile dex  = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), new BufferedInputStream(zipFile.getInputStream(dexEntry)));
            System.out.println("  -- Обработка classes.dex");
            byte[] processDex = processDex(dex);

            InputStream fis_arm = mContext.getResources().openRawResource(R.raw.mt2_hook_arm);
            byte[] arm = StreamUtil.readBytes(fis_arm);

            InputStream fis_arm64 = mContext.getResources().openRawResource(R.raw.mt2_hook_arm64);
            byte[] arm64 = StreamUtil.readBytes(fis_arm64);

            InputStream fis_x86 = mContext.getResources().openRawResource(R.raw.mt2_hook_x86);
            byte[] x86 = StreamUtil.readBytes(fis_x86);

            InputStream fis_x86_64 = mContext.getResources().openRawResource(R.raw.mt2_hook_x86_64);
            byte[] x86_64 = StreamUtil.readBytes(fis_x86_64);

            System.out.println("\nОптимизация APK:" + outApk);
            ZipOutputStream zipOutputStream = new ZipOutputStream(new File(tempApk));
            zipOutputStream.setLevel(1);
            Enumeration<ZipEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                String name = ze.getName();
                if ((name.startsWith("classes") && name.endsWith("dex")) || name.startsWith("./")) {
                    zipOutputStream.copyZipEntry(ze, zipFile);
                }
            }
            zipOutputStream.close();

            System.out.println("\nЗапись в APK:" + outApk);
            try (ZipOutputStream zos = new ZipOutputStream(new File(outApk))) {
                zos.putNextEntry("AndroidManifest.xml");
                zos.write(manifestData);
                zos.closeEntry();

                zos.putNextEntry("classes.dex");
                zos.write(processDex);
                zos.closeEntry();

                zos.putNextEntry("armeabi-v7a/libmthook.so");
                zos.write(arm);
                zos.closeEntry();

                zos.putNextEntry("arm64-v8a/libmthook.so");
                zos.write(arm64);
                zos.closeEntry();

                zos.putNextEntry("x86/libmthook.so");
                zos.write(x86);
                zos.closeEntry();

                zos.putNextEntry("x86_64/libmthook.so");
                zos.write(x86_64);
                zos.closeEntry();

                Enumeration<ZipEntry> enumeration = zipFile.getEntries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry ze = enumeration.nextElement();
                    if (ze.getName().equals("AndroidManifest.xml")
                            || ze.getName().equals("classes.dex")
                            || ze.getName().startsWith("META-INF/"))
                        continue;
                    zos.copyZipEntry(ze, zipFile);
                }
                new File(tempApk).delete();
                zipFile.close();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        } catch (Throwable th2) {
            th2.printStackTrace();
        }
    }*/

    private Certificate @Nullable [] loadCertificates(JarFile jarFile, JarEntry jarEntry,
                                                      byte[] bArr) {
        try {
            InputStream inputStream = jarFile.getInputStream(jarEntry);
            while (inputStream.read(bArr, 0, bArr.length) != -1) {
                inputStream.close();
                if (jarEntry != null) {
                    return jarEntry.getCertificates();
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private @NotNull String getApkSignInfo(String str) {
        byte[] bArr = new byte[ItemType.CLASS_DATA_ITEM];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            JarFile jarFile = new JarFile(str);
            Certificate[] loadCertificates = loadCertificates(jarFile, jarFile.getJarEntry("AndroidManifest.xml"), bArr);
            dataOutputStream.write(loadCertificates.length);
            for (Certificate certificate : loadCertificates) {
                byte[] encoded = certificate.getEncoded();
                dataOutputStream.writeInt(encoded.length);
                dataOutputStream.write(encoded);
            }
            jarFile.close();
            return Base64.encodeToString(byteArrayOutputStream.toByteArray(), 0).replace(IOUtils.LINE_SEPARATOR_UNIX, "\\n");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /*private byte @NotNull [] processDex(DexBackedDexFile dex) throws Exception {
        DexBuilder dexBuilder = new DexBuilder(Opcodes.getDefault());
        try (InputStream fis = mContext.getResources().openRawResource(R.raw.mt2_hook)) {
            String src = new String(StreamUtil.readBytes(fis), StandardCharsets.UTF_8);
            if (customApplication) {
                if (customApplicationName.startsWith(".")) {
                    if (packageName == null)
                        throw new NullPointerException("Package name is null.");
                    customApplicationName = packageName + customApplicationName;
                }
                customApplicationName = "L" + customApplicationName.replace('.', '/') + ";";
                src = src.replace("Landroid/app/Application;", customApplicationName);
            }
            if (signatures == null)
                throw new NullPointerException("Signatures is null");
            src = src.replace("### Signatures Data ###", signatures);
            ClassDef classDef = Smali.assembleSmaliFile(src, dexBuilder, new SmaliOptions());
            if (classDef == null)
                throw new Exception("Parse smali failed");
            for (DexBackedClassDef dexBackedClassDef : dex.getClasses()) {
                dexBuilder.internClassDef(dexBackedClassDef);
            }
        }
        MemoryDataStore store = new MemoryDataStore();
        dexBuilder.writeTo(store);
        return Arrays.copyOf(store.getBufferData(), store.getSize());
    }*/

    private byte @NotNull [] parseManifest(InputStream is) throws IOException {
        AXmlDecoder axml = AXmlDecoder.decode(is);
        AXmlResourceParser parser = new AXmlResourceParser();
        parser.open(new ByteArrayInputStream(axml.getData()), axml.mTableStrings);
        boolean success = false;

        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG)
                continue;
            if (parser.getName().equals("manifest")) {
                int size = parser.getAttributeCount();
                for (int i = 0; i < size; ++i) {
                    if (parser.getAttributeName(i).equals("package")) {
                        packageName = parser.getAttributeValue(i);
                    }
                }
            } else if (parser.getName().equals("application")) {
                int size = parser.getAttributeCount();
                for (int i = 0; i < size; ++i) {
                    if (parser.getAttributeNameResource(i) == 0x01010003) {
                        customApplication = true;
                        customApplicationName = parser.getAttributeValue(i);
                        int index = axml.mTableStrings.getSize();
                        byte[] data = axml.getData();
                        int off = parser.currentAttributeStart + 20 * i;
                        off += 8;
                        FileUtils.writeInt(data, off, index);
                        off += 8;
                        FileUtils.writeInt(data, off, index);
                    }
                }
                if (!customApplication) {
                    int off = parser.currentAttributeStart;
                    byte[] data = axml.getData();
                    byte[] newData = new byte[data.length + 20];
                    System.arraycopy(data, 0, newData, 0, off);
                    System.arraycopy(data, off, newData, off + 20, data.length - off);

                    // chunkSize
                    int chunkSize = FileUtils.readInt(newData, off - 32);
                    FileUtils.writeInt(newData, off - 32, chunkSize + 20);
                    // attributeCount
                    FileUtils.writeInt(newData, off - 8, size + 1);

                    int idIndex = parser.findResourceID(0x01010003);
                    if (idIndex == -1)
                        throw new IOException("idIndex == -1");

                    boolean isMax = true;
                    for (int i = 0; i < size; ++i) {
                        int id = parser.getAttributeNameResource(i);
                        if (id > 0x01010003) {
                            isMax = false;
                            if (i != 0) {
                                System.arraycopy(newData, off + 20, newData, off, 20 * i);
                                off += 20 * i;
                            }
                            break;
                        }
                    }
                    if (isMax) {
                        System.arraycopy(newData, off + 20, newData, off, 20 * size);
                        off += 20 * size;
                    }

                    FileUtils.writeInt(newData, off, axml.mTableStrings.find("http://schemas.android.com/apk/res/android"));
                    FileUtils.writeInt(newData, off + 4, idIndex);
                    FileUtils.writeInt(newData, off + 8, axml.mTableStrings.getSize());
                    FileUtils.writeInt(newData, off + 12, 0x03000008);
                    FileUtils.writeInt(newData, off + 16, axml.mTableStrings.getSize());
                    axml.setData(newData);
                }
                success = true;
                break;
            }
        }
        if (!success)
            throw new IOException();
        ArrayList<String> list = new ArrayList<>(axml.mTableStrings.getSize());
        axml.mTableStrings.getStrings(list);
        list.add("bin.mt.apksignaturekillerplus.HookApplication");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        axml.write(list, baos);
        return baos.toByteArray();
    }
}
