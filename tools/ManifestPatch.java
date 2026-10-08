import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import pxb.android.axml.AxmlWriter;
import com.wind.meditor.property.AttributeItem;
import com.wind.meditor.utils.NodeValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ManifestPatch {
    private static final String ANDROID = NodeValue.MANIFEST_NAMESPACE;
    private static final String LABEL = "FLY8's Light Cloner";

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "usage: ManifestPatch <input.apk> <output.apk> <package-name>");
        }

        File input = new File(args[0]);
        File output = new File(args[1]);
        final String pkg = args[2];

        try (ZipFile zip = new ZipFile(input);
             OutputStream fileOut = new FileOutputStream(output);
             ZipOutputStream out = new ZipOutputStream(fileOut)) {

            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("META-INF/") &&
                        (name.endsWith(".SF") || name.endsWith(".RSA") ||
                         name.endsWith(".DSA") || name.equals("MANIFEST.MF"))) {
                    continue;
                }

                ZipEntry copy = new ZipEntry(name);
                copy.setTime(entry.getTime());
                copy.setComment(entry.getComment());
                copy.setExtra(entry.getExtra());

                if (entry.getMethod() == ZipEntry.STORED) {
                    copy.setMethod(ZipEntry.STORED);
                    copy.setSize(entry.getSize());
                    copy.setCompressedSize(entry.getSize());
                    copy.setCrc(entry.getCrc());
                }

                out.putNextEntry(copy);

                if ("AndroidManifest.xml".equals(name)) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        out.write(patchManifest(in.readAllBytes(), pkg));
                    }
                } else if (!entry.isDirectory()) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        in.transferTo(out);
                    }
                }

                out.closeEntry();
            }
        }

        if (!output.isFile() || output.length() == 0) {
            throw new IllegalStateException("Manifest patch produced no APK");
        }
    }

    private static byte[] patchManifest(byte[] data, final String pkg) throws Exception {
        AxmlReader reader = new AxmlReader(data);
        AxmlWriter writer = new AxmlWriter();

        reader.accept(new AxmlVisitor(writer) {
            @Override
            public NodeVisitor child(String ns, String name) {
                NodeVisitor child = super.child(ns, name);

                if (!NodeValue.Application.TAG_NAME.equals(name)) {
                    return child;
                }

                return new NodeVisitor(child) {
                    @Override
                    public void attr(String ans, String aname, int resId, int type, Object value) {
                        if (ANDROID.equals(ans) &&
                                ("logo".equals(aname) || "banner".equals(aname))) {
                            return;
                        }

                        if (ANDROID.equals(ans) &&
                                NodeValue.Application.LABEL.equals(aname)) {
                            AttributeItem item = new AttributeItem("label", LABEL);
                            super.attr(ANDROID, "label", item.getResourceId(), item.getType(), LABEL);
                            return;
                        }

                        super.attr(ans, aname, resId, type, value);
                    }

                    @Override
                    public void end() {
                        addProvider(this, "rikka.shizuku.ShizukuProvider",
                                new AttributeItem("authorities", pkg + ".shizuku"),
                                new AttributeItem("exported", Boolean.TRUE),
                                new AttributeItem("multiprocess", Boolean.FALSE),
                                new AttributeItem("permission",
                                        "android.permission.INTERACT_ACROSS_USERS_FULL"));

                        addProvider(this, "com.fl8y.lightcloner.bridge.BridgeProvider",
                                new AttributeItem("authorities", pkg + ".fly8bridge"),
                                new AttributeItem("exported", Boolean.FALSE));

                        super.end();
                    }

                    private void addProvider(NodeVisitor app,
                                              String className,
                                              AttributeItem... attrs) {
                        NodeVisitor provider = app.child(null, "provider");

                        AttributeItem name = new AttributeItem("name", className);
                        provider.attr(ANDROID, "name",
                                name.getResourceId(), name.getType(), className);

                        for (AttributeItem attr : attrs) {
                            provider.attr(ANDROID, attr.getName(),
                                    attr.getResourceId(), attr.getType(), attr.getValue());
                        }

                        provider.end();
                    }
                };
            }
        });

        return writer.toByteArray();
    }
}
