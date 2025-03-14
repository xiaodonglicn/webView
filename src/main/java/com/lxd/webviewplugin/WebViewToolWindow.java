package com.lxd.webviewplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.jcef.JBCefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WebViewToolWindow implements ToolWindowFactory {
    private JBCefBrowser browser;
    private JTextField urlField;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        // 创建主面板
        JPanel panel = new JPanel(new BorderLayout());
        // 创建地址栏和访问按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        urlField = new JTextField("请输入正确的网址"); // 默认网址
//        urlField = new JTextField("https://www.zhihu.com?theme=dark"); // 默认网址
        JButton visitButton = new JButton("Go");
        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(visitButton, BorderLayout.EAST);

        JButton homeButton = new JButton("Home"); // 新增【返回首页】按钮
        topPanel.add(urlField, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); // 按钮容器
        buttonPanel.add(visitButton);
        buttonPanel.add(homeButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        // 创建浏览器组件
        browser = new JBCefBrowser();
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(browser.getComponent(), BorderLayout.CENTER);
        // 加载欢迎语
        loadWelcomeMessage2();
        // 处理访问按钮点击事件
        visitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url = urlField.getText().trim();
                if (!url.isEmpty()) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url; // 默认使用 HTTPS
                    }
                    browser.loadURL(url);
                }
            }
        });
        // 处理【返回首页】按钮点击事件
        homeButton.addActionListener(e -> loadWelcomeMessage2());
        // 将面板添加到 ToolWindow
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = ContentFactory.SERVICE.getInstance().createContent(panel, "", false); // 保留旧方法作为过渡
        contentManager.addContent(content);
    }


    /**
     * 从资源文件中加载 HTML 内容
     *
     * @param fileName 文件名
     * @return HTML 内容字符串
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
            browser.loadHTML(htmlContent); // 设置到 browser 中
        } else {
            System.err.println("Failed to load welcome.html");
        }
    }

}