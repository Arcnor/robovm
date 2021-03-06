/*
 * Copyright (C) 2013 Trillian AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package org.robovm.compiler.target.ios;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.exec.util.StringUtils;
import org.robovm.compiler.log.Logger;
import org.robovm.compiler.util.Executor;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;

/**
 * Contains info on an SDK installed on the system.
 */
public class SDK implements Comparable<SDK> {
    private String displayName;
    private String minimalDisplayName;
    private String canonicalName;
    private String version;
    private File root;
    private NSDictionary defaultProperties;
    private int major;
    private int minor;
    private int revision;
    private String build;
    private String platformBuild;
    private String platformVersion;
    private String platformName;
    
    public static SDK create(File root) throws Exception {
        File sdkSettingsFile = new File(root, "SDKSettings.plist");
        File sdkSysVersionFile = new File(root, "System/Library/CoreServices/SystemVersion.plist");
        File platformVersionFile = new File(root, "../../../version.plist");
        File platformInfoFile = new File(root, "../../../Info.plist");
        if (sdkSettingsFile.exists() && platformVersionFile.exists() && platformInfoFile.exists()) {
            NSDictionary sdkSettingsDict = (NSDictionary) PropertyListParser.parse(sdkSettingsFile);
            NSDictionary sdkSysVersionDict = (NSDictionary) PropertyListParser.parse(sdkSysVersionFile);
            NSDictionary platformVersionDict = (NSDictionary) PropertyListParser.parse(platformVersionFile);
            NSDictionary platformInfoDict = (NSDictionary) PropertyListParser.parse(platformInfoFile);
            
            SDK sdk = new SDK();
            
            sdk.root = root;
            
            sdk.displayName = toString(sdkSettingsDict.objectForKey("DisplayName"));
            sdk.minimalDisplayName = toString(sdkSettingsDict.objectForKey("MinimalDisplayName"));
            sdk.canonicalName = toString(sdkSettingsDict.objectForKey("CanonicalName"));
            sdk.version = toString(sdkSettingsDict.objectForKey("Version"));
            sdk.defaultProperties = (NSDictionary) sdkSettingsDict.objectForKey("DefaultProperties");
            
            sdk.build = toString(sdkSysVersionDict.objectForKey("ProductBuildVersion"));

            sdk.platformBuild = toString(platformVersionDict.objectForKey("ProductBuildVersion"));
            
            NSDictionary additionalInfo = (NSDictionary) platformInfoDict.objectForKey("AdditionalInfo");
            sdk.platformVersion = toString(additionalInfo.objectForKey("DTPlatformVersion"));
            sdk.platformName = toString(additionalInfo.objectForKey("DTPlatformName"));

            String[] parts = StringUtils.split(sdk.version, ".");
            sdk.major = Integer.parseInt(parts[0]);
            sdk.minor = parts.length >= 2 ? Integer.parseInt(parts[1]) : 0;
            sdk.revision = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
            
            return sdk;
        }
        throw new IllegalArgumentException(root.getAbsolutePath() + " is not an SDK root path");
    }
    
    private static String toString(NSObject o) {
        return o != null ? o.toString() : null;
    }
    
    private static List<SDK> listSDKs(String platform) {
        try {
            List<SDK> sdks = new ArrayList<SDK>();
            List<File> sdksDirs = Arrays.asList(
                    new File(new Executor(Logger.NULL_LOGGER, "xcode-select").args("--print-path").execCapture() + "/Platforms/" + platform + ".platform/Developer/SDKs"),
                    new File("/Applications/Xcode.app/Contents/Developer/Platforms/" + platform + ".platform/Developer/SDKs"),
                    new File("/Developer/Platforms/" + platform + ".platform/Developer/SDKs"));
            for (File sdksDir : sdksDirs) {
                if (sdksDir.exists() && sdksDir.isDirectory()) {
                    for (File root : sdksDir.listFiles()) {
                        try {
                            sdks.add(SDK.create(root));
                        } catch (Exception e) {
                        }
                    }
                    if (!sdks.isEmpty()) {
                        return sdks;
                    }
                }
            }
            
            return sdks;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    public static List<SDK> listDeviceSDKs() {
        return listSDKs("iPhoneOS");
    }
    
    public static List<SDK> listSimulatorSDKs() {
        return listSDKs("iPhoneSimulator");
    }
    
    public String getDisplayName() {
        return displayName;
    }

    public String getMinimalDisplayName() {
        return minimalDisplayName;
    }
    
    public String getCanonicalName() {
        return canonicalName;
    }
    
    public String getVersion() {
        return version;
    }

    public File getRoot() {
        return root;
    }

    public NSDictionary getDefaultProperties() {
        return defaultProperties;
    }
    
    public NSObject getDefaultProperty(String key) {
        return defaultProperties.objectForKey(key);
    }
    
    public String getBuild() {
        return build;
    }

    public String getPlatformBuild() {
        return platformBuild;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public String getPlatformName() {
        return platformName;
    }

    public int getMajor() {
        return major;
    }
    
    public int getMinor() {
        return minor;
    }
    
    public int getRevision() {
        return revision;
    }
    
    @Override
    public int compareTo(SDK o) {
        int c = major < o.major ? -1 : (major > o.major ? 1 : 0);
        if (c == 0) {
            c = minor < o.minor ? -1 : (minor > o.minor ? 1 : 0);
            if (c == 0) {
                c = revision < o.revision ? -1 : (revision > o.revision ? 1 : 0);                    
            }
        }
        return c;
    }
    
    @Override
    public String toString() {
        return "SDK [displayName=" + displayName + ", minimalDisplayName="
                + minimalDisplayName + ", canonicalName=" + canonicalName
                + ", version=" + version + ", root=" + root + ", major="
                + major + ", minor=" + minor + ", revision=" + revision
                + ", build=" + build + ", platformBuild=" + platformBuild
                + ", platformVersion=" + platformVersion + ", platformName="
                + platformName + "]";
    }
}
