package com.lxd.webviewplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WebViewToolWindow implements ToolWindowFactory {
    private JBCefBrowser browser;
    private JTextField urlField;
    private ContentFactory contentFactory;
    private JFrame devToolsFrame;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        // 创建主面板
        JPanel panel = new JPanel(new BorderLayout());

        // 创建地址栏和按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        urlField = new JTextField("Please enter the correct URL");

        JButton visitButton = new JButton("Go");
        JButton homeButton = new JButton("Home");
        JButton devToolsButton = new JButton("F12");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(visitButton);
        buttonPanel.add(homeButton);
        buttonPanel.add(devToolsButton);

        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        // 创建浏览器组件
        browser = new JBCefBrowser();
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(browser.getComponent(), BorderLayout.CENTER);

        // 加载欢迎语
        loadWelcomeMessage2();

        // 处理访问按钮点击事件
        visitButton.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                browser.loadURL(url);
            }
        });

        // 处理【返回首页】按钮点击事件
        homeButton.addActionListener(e -> loadWelcomeMessage2());

        // 处理【F12 DevTools】按钮点击事件
        devToolsButton.addActionListener(e -> toggleDevTools());

        // 将面板添加到 ToolWindow
        ContentManager contentManager = toolWindow.getContentManager();
        if (contentFactory == null) {
            contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        }
        Content content = contentFactory.createContent(panel, "", false);
        contentManager.addContent(content);
    }

    /**
     * 切换开发者工具窗口
     */
    private void toggleDevTools() {
        if (devToolsFrame == null || !devToolsFrame.isVisible()) {
            openDevTools();
        } else {
            devToolsFrame.setVisible(false);
            devToolsFrame.dispose();
            devToolsFrame = null;
        }
    }

    /**
     * 打开开发者工具窗口
     */
    private void openDevTools() {
        CefBrowser devTools = browser.getCefBrowser().getDevTools();

        devToolsFrame = new JFrame("WebView DevTools");
        devToolsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        devToolsFrame.setSize(800, 600);
        devToolsFrame.setLocationRelativeTo(null);

        devToolsFrame.add(devTools.getUIComponent());
        devToolsFrame.setVisible(true);

        // 监听窗口关闭事件
        devToolsFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                devToolsFrame = null;
            }
        });
    }

    /**
     * 从资源文件中加载 HTML 内容
     */
    private String loadHtmlFromResource(String fileName) {
        StringBuilder contentBuilder = new StringBuilder();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private void loadWelcomeMessage2() {
        String htmlContent = loadHtmlFromResource("welcome.html");
        if (htmlContent != null) {
            browser.loadHTML(htmlContent);
        } else {
            System.err.println("Failed to load welcome.html");
        }
    }
}