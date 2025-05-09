package com.lxd.webviewplugin;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.jcef.JBCefApp;

public class OpenWebViewAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Messages.showErrorDialog("No project found.", "Error");
            return;
        }
        if (!JBCefApp.isSupported()) {
            Messages.showErrorDialog("JCEF is not supported in this environment.", "Error");
            return;
        }
        // 打开右侧边栏1
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebView");
        if (toolWindow != null) {
            toolWindow.show(null);
        }
    }
}