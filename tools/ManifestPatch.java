import com.wind.meditor.core.FileProcesser;
import com.wind.meditor.property.AttributeItem;
import com.wind.meditor.property.ModificationProperty;

import java.util.Arrays;

public final class ManifestPatch {
    private ManifestPatch() {}

    public static void main(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                "usage: ManifestPatch <input.apk> <output.apk> <package-name>");
        }

        String input = args[0];
        String output = args[1];
        String pkg = args[2];

        ModificationProperty property = new ModificationProperty();

        property.addApplicationAttribute(
            new AttributeItem("label", "FLY8's Light Cloner")
        );

        property.addProvider(Arrays.asList(
            new AttributeItem("name", "rikka.shizuku.ShizukuProvider"),
            new AttributeItem("authorities", pkg + ".shizuku"),
            new AttributeItem("exported", Boolean.TRUE),
            new AttributeItem("multiprocess", Boolean.FALSE),
            new AttributeItem(
                "permission",
                "android.permission.INTERACT_ACROSS_USERS_FULL"
            )
        ), null);

        property.addProvider(Arrays.asList(
            new AttributeItem("name", "com.fl8y.lightcloner.bridge.BridgeProvider"),
            new AttributeItem("authorities", pkg + ".fly8bridge"),
            new AttributeItem("exported", Boolean.FALSE)
        ), null);

        FileProcesser.processApkFile(input, output, property);

        if (!new java.io.File(output).isFile() ||
            new java.io.File(output).length() == 0) {
            throw new IllegalStateException("Manifest patch produced no APK");
        }
    }
}
