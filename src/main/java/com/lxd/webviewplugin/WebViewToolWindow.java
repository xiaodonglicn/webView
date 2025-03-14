package com.lxd.webviewplugin;

import com.intellij.openapi.application.ApplicationManager;
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
    private ContentFactory contentFactory;

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
        if (contentFactory == null) {
            contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        }
        Content content = contentFactory.createContent(panel, "", false); // 保留旧方法作为过渡
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
//        String htmlContent = loadHtmlFromResource("welcome.html");
        String htmlContent = getHtml();
        if (htmlContent != null) {
            browser.loadHTML(htmlContent); // 设置到 browser 中
        } else {
            System.err.println("Failed to load welcome.html");
        }
    }

    private String getHtml() {
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <link href=\"https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@900&display=swap\" rel=\"stylesheet\">\n" +
                "    <style>\n" +
                "        body {\n" +
                "            position: relative;\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            height: 100vh;\n" +
                "            margin: 0;\n" +
                "            background: radial-gradient(circle, #dcdcdc, #8c8c8c);\n" +
                "            color: rgba(0, 0, 0, 0.8);\n" +
                "            font-family: 'STKaiti', serif;\n" +
                "            font-size: 24px;\n" +
                "            line-height: 1.5;\n" +
                "            text-align: center;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        /* 上方内容框样式 */\n" +
                "        .content-box {\n" +
                "            position: relative;\n" +
                "            max-width: 80%;\n" +
                "            writing-mode: vertical-rl;\n" +
                "            text-orientation: upright;\n" +
                "            text-align: right;\n" +
                "        }\n" +
                "        .content-box::before {\n" +
                "            content: \"\";\n" +
                "            position: absolute;\n" +
                "            top: -20px;\n" +
                "            left: -20px;\n" +
                "            right: -20px;\n" +
                "            bottom: -20px;\n" +
                "            border: 2px solid rgba(0, 0, 0, 0.2);\n" +
                "            border-radius: 10px;\n" +
                "        }\n" +
                "        .seal {\n" +
                "            position: absolute;\n" +
                "            bottom: -30px;\n" +
                "            left: -30px;\n" +
                "            font-family: 'Noto Serif SC', serif;\n" +
                "            font-size: 36px;\n" +
                "            color: red;\n" +
                "            font-weight: bold;\n" +
                "            transform: rotate(0deg);\n" +
                "        }\n" +
                "        /* 优化：链接框样式 */\n" +
                "        .link-box {\n" +
                "            position: absolute;\n" +
                "            bottom: 50px;\n" +
                "            left: 20px;\n" +
                "            padding: 10px 20px;\n" +
                "            max-width: 300px;\n" +
                "            max-height: 200px;\n" +
                "            overflow-y: auto;\n" +
                "            border: 2px solid rgba(0, 0, 0, 0.2);\n" +
                "            border-radius: 10px;\n" +
                "            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\n" +
                "            font-family: 'Noto Serif SC', serif; /* 更改字体为书法字体 */\n" +
                "            font-size: 16px; /* 减小字体大小 */\n" +
                "            line-height: 1.4; /* 调整行高 */\n" +
                "        }\n" +
                "        .link-box a {\n" +
                "            display: block;\n" +
                "            margin: 5px 0;\n" +
                "            font-weight: 500; /* 增加字体粗细 */\n" +
                "            color: rgba(0, 0, 0, 0.6); /* 浅灰色文字 */\n" +
                "            text-decoration: none;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .link-box a:hover {\n" +
                "            color: rgba(0, 0, 0, 0.8); /* 鼠标悬停时加深颜色 */\n" +
                "        }\n" +
                "        /* 新增：右上角框样式 */\n" +
                "        .quote-box {\n" +
                "            position: absolute;\n" +
                "            top: 20px;\n" +
                "            right: 20px;\n" +
                "            padding: 20px;\n" +
                "            max-width: 300px;\n" +
                "            border: 2px solid rgba(0, 0, 0, 0.2);\n" +
                "            border-radius: 10px;\n" +
                "            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\n" +
                "            writing-mode: vertical-rl;\n" +
                "            text-orientation: upright;\n" +
                "            text-align: right;\n" +
                "            font-size: 24px;\n" +
                "            line-height: 1.5;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<!-- 上方内容框 -->\n" +
                "<div class=\"content-box\">\n" +
                "    晓逐风云意未央<br>\n" +
                "    东凌绝顶瞰八荒<br>\n" +
                "    制胜长空凭羽翼<br>\n" +
                "    作赋星辰耀穹苍\n" +
                "    <span class=\"seal\">東</span> <!-- 印章内容 -->\n" +
                "</div>\n" +
                "<!-- 优化：链接框 -->\n" +
                "<div class=\"link-box\">\n" +
                "    <a href=\"#\">Art is long, life is short.</a>\n" +
                "</div>\n" +
                "<!-- 新增：右上角框 -->\n" +
                "<div class=\"quote-box\">\n" +
                "    若有诗书藏于心<br>\n" +
                "    岁月从不败少年\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>";
        return html;
    }

}