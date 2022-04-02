# Privacy Space

[![Stars](https://img.shields.io/github/stars/GeekTR/PrivacySpace?label=Stars)](https://github.com/GeekTR/PrivacySpace)
[![Release](https://img.shields.io/github/v/release/Xposed-Modules-Repo/cn.geektang.privacyspace?label=Release)](https://github.com/Xposed-Modules-Repo/cn.geektang.privacyspace/releases/latest)
[![Download](https://img.shields.io/github/downloads/Xposed-Modules-Repo/cn.geektang.privacyspace/total)](https://github.com/Xposed-Modules-Repo/cn.geektang.privacyspace/releases/latest)
[![Channel](https://img.shields.io/badge/Follow-Telegram-blue.svg?logo=telegram)](https://t.me/PrivacySpaceAlpha)
[![GitHub license](https://img.shields.io/github/license/Xposed-Modules-Repo/cn.geektang.privacyspace)](https://github.com/Xposed-Modules-Repo/cn.geektang.privacyspace/blob/main/LICENSE)

[中文文档](https://github.com/Xposed-Modules-Repo/cn.geektang.privacyspace/blob/main/README_CN.md)

This is an **Xposed** module. The function of this module is to "hide" the apps, which can achieve the "Second space" function of MIUI.

## What cool things can it do?

1. In addition to detecting Root, some banking apps will also detect Xposed modules. This module can hide our Xposed modules and pass the detection of banking apps;

2. A certain version of an app is particularly useful, and we don't want it to be automatically updated by the app store;

3. When we are watching an advertisement, some apps detect the existence of another app and will open it directly, but we do not want to open that app;

4. Why should we tell software vendors something as personal as which apps are installed?

5. More cool things are waiting for you to discover...

## Notice

1. If random package names are enabled in your Magisk app, whitelist it. Otherwise, the apps moved to the "Privacy Space" cannot obtain the Root permission correctly.

2. Apps that move to the "Privacy Space" can be launched by clicking the APP icon on the "Privacy Space" home page.

3. If you don't want to hide an app (such as the desktop app), you can add it to the whitelist.

4. If you use this module to hide some system apps, the system may fail to boot after the restart. Therefore, exercise caution when hiding system apps.

5. If this module causes your system to fail to boot, you can restart the system again after connecting to the computer and executing "adb uninstall cn.geektang.privacyspace" on the system load page (with USB debugging enabled).

## Todo

They will coming soon in future.

- [x] Add the search function on the app list page.

- [x] Adapt to Android version 8-10.

- [x] Fixed the bug that some mobile phones crashed directly without root permission.

- [x] Remove the dependency on root permissions.

- [ ] Install Xposed module to automatically hide (user optional).

- [ ] When the Xposed module is hidden, the user can choose whether to add its recommended app as its "Connected App".

## Scope

1. System Framework (**required**)

2. Targeted hooks can be made when the non-Android system scope is checked (used when the hidden function cannot take effect after System Framework is checked)