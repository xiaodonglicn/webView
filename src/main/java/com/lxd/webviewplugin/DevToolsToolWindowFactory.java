package com.lxd.webviewplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class DevToolsToolWindowFactory implements ToolWindowFactory {

    private static JBCefBrowser devToolsBrowser;
    private static Content devToolsContent;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 这里可以留空，因为我们会动态添加内容
    }

    // 静态方法用于在其他类中打开开发者工具
    public static void openDevTools(Project project, CefBrowser mainBrowser) {
        if (mainBrowser == null) {
            return;
        }

        // 获取或创建开发者工具窗口
        ToolWindow devToolsWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                .getToolWindow("WebView DevTools");

        if (devToolsWindow == null) {
            return;
        }

        // 如果开发者工具已经打开，先关闭
        closeDevTools(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // 创建开发者工具浏览器
                devToolsBrowser = new JBCefBrowser();

                // 获取开发者工具
                CefBrowser devTools = mainBrowser.getDevTools();
                if (devTools == null) {
                    Messages.showErrorDialog("Developer tools are not available", "Error");
                    return;
                }

                // 将开发者工具UI添加到我们的浏览器组件中
                Component devToolsUI = devTools.getUIComponent();
                if (devToolsUI != null) {
                    JPanel panel = new JPanel(new java.awt.BorderLayout());
                    panel.add(devToolsUI, java.awt.BorderLayout.CENTER);

                    // 创建内容
                    ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
                    devToolsContent = contentFactory.createContent(panel, "DevTools", false);

                    // 添加到工具窗口
                    ContentManager contentManager = devToolsWindow.getContentManager();
                    contentManager.addContent(devToolsContent);

                    // 显示工具窗口
                    devToolsWindow.show(() -> {
                        // 激活时的回调
                        contentManager.setSelectedContent(devToolsContent);
                    });
                }
            } catch (Exception e) {
                Messages.showInfoMessage("The current version does not support it for the time being", "Info");
            }
        });
    }

    // 关闭开发者工具
    public static void closeDevTools(Project project) {
        if (devToolsContent != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                ToolWindow devToolsWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                        .getToolWindow("WebView DevTools");

                if (devToolsWindow != null) {
                    ContentManager contentManager = devToolsWindow.getContentManager();
                    contentManager.removeContent(devToolsContent, true);
                }

                if (devToolsBrowser != null) {
                    devToolsBrowser.dispose();
                    devToolsBrowser = null;
                }

                devToolsContent = null;
            });
        }
    }
    
    // 检查开发者工具是否已打开
    public static boolean isDevToolsOpen(Project project) {
        if (devToolsContent == null) {
            return false;
        }
        
        ToolWindow devToolsWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                .getToolWindow("WebView DevTools");
        
        if (devToolsWindow == null) {
            return false;
        }
        
        ContentManager contentManager = devToolsWindow.getContentManager();
        return contentManager.getContents().length > 0;
    }
}