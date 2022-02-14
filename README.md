# Privacy Space/隐秘空间

Similar to MIUI's "Second space", you can "move" some apps to the "Privacy Space" through some simple Settings, and then all non-system apps will not be able to detect the apps moved to the "Privacy Space". This feature will also take effect in some system apps, so after you move it, you will find that apps moved to the "Privacy Space" will "disappear" on the desktop (need to restart the desktop app) and some application management pages. You can use this feature to hide your installed Xposed modules (including this one) and other apps that you don't want to be detected by other apps (such as V2rayNG).

类似MIUI的“手机分身”，您可以通过一些简单的设置把部分APP“移动”到“隐秘空间”中，之后所有的非系统APP将无法检测到移动到“隐秘空间”的APP。此功能部分系统APP也将生效，所以移动之后您会发现在桌面（需重启桌面APP）以及一些应用管理页面，移动至“隐秘空间”的APP“消失”了。您可以通过此功能隐藏您已安装的Xposed模块（包括本模块）以及一些您不想让其他APP检测到的应用（比如：V2rayNG）。

## Notice / 注意事项

1. If random package names are enabled in your Magisk app, whitelist it. Otherwise, the apps moved to the "Privacy Space" cannot obtain the Root permission correctly.

  如果您的Magisk（面具）应用开启了随机包名，请将其加入白名单。否则移动至“隐秘空间”的APP将无法正确获取到Root权限。

2. Apps that move to the "Privacy Space" can be launched by clicking the APP icon on the "Privacy Space" home page.

  移动到“隐秘空间”的应用可以在“隐秘空间”主页面点击APP的图标启动。

3. Due to system limitations, this module requires Root permission to work properly.

  因为受限于系统，此APP需要Root权限才能正常工作。

4. If you don't want to hide apps that move to the "Privacy Space" on your desktop (or some other apps), you can whitelist your desktop (or apps that you don't want to hide from it).

  如果您不想在桌面（或一些其他APP内）隐藏移动到“隐秘空间”的APP，您可以将桌面（或您不想对其隐藏的APP）加入白名单。

5. If you use this module to hide some system apps, the system may fail to boot after the restart. Therefore, exercise caution when hiding system apps.

  如果用此模块隐藏了一些系统APP，将可能导致重启后系统无法开机，请对系统应用谨慎操作。

6. If this module causes your system to fail to boot, you can restart the system again after connecting to the computer and executing "adb uninstall cn.geektang.privacyspace" on the system load page (with USB debugging enabled).

  如果此模块导致您的系统无法启动，您可以在连接电脑后，在系统加载页面执行（开启usb调试的情况下）"adb uninstall cn.geektang.privacyspace"后再次重启。（或者您可以选择“搞机助手”的“自动神仙救砖”模块）


## Scope / 作用域

You should only choose the recommended apps.

您只应该选择推荐的应用。