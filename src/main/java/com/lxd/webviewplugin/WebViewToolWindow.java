package com.lxd.webviewplugin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WebViewToolWindow implements ToolWindowFactory {

    private static final String URL_MSG = "Please enter the correct URL and press Enter";
    private JBCefBrowser browser;
    private JTextField urlField;
    private ContentFactory contentFactory;
    // 缩放控制变量
    private double zoomFactor = 1.0;
    private static final double ZOOM_STEP = 0.1;
    // 在类的字段部分添加以下新字段
    private JButton backButton;
    private JButton forwardButton;
    private boolean isDarkMode = false;
    private JMenuItem themeToggleMenuItem;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // 创建主面板
            JPanel panel = new JPanel(new BorderLayout());
            // 构建顶部工具栏
            JPanel topPanel = this.createTopToolbar();
            // 创建浏览器组件
            browser = new JBCefBrowser();
            // 添加生命周期处理器，防止打开新窗口
            browser.getJBCefClient().addLifeSpanHandler(new org.cef.handler.CefLifeSpanHandlerAdapter() {
                @Override
                public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
                    // 阻止弹出新窗口，在当前浏览器中加载URL
                    WebViewToolWindow.this.browser.loadURL(target_url);
//                    WebViewToolWindow.this.urlField.setText(target_url);
                    return true; // 返回true表示取消弹窗
                }
            }, browser.getCefBrowser());
            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(browser.getComponent(), BorderLayout.CENTER);
            // 加载欢迎页面
            this.loadWelcomeMessage2();
            // 将面板添加到 ToolWindow
            ContentManager contentManager = toolWindow.getContentManager();
            if (contentFactory == null) {
                contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
            }
            Content content = contentFactory.createContent(panel, "", false);
            contentManager.addContent(content);
        });
    }

    private JPanel createTopToolbar() {
        JPanel topPanel = new JPanel(new BorderLayout());
        urlField = new JTextField(URL_MSG);
        urlField.setForeground(Color.GRAY);
        addPlaceholder();

//        JButton visitButton = this.createGoButton();
        JButton moreButton = this.createMoreDropdownButton();

        // 布局地址栏和按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
//        buttonPanel.add(visitButton);
        buttonPanel.add(moreButton);

        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        return topPanel;
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        // 更新菜单项文本
        if (themeToggleMenuItem != null) {
            themeToggleMenuItem.setText(isDarkMode ? "Light Mode" : "Dark Mode");
        }
        applyTheme();
    }

    // 添加更新导航按钮状态的方法
    private void updateNavigationButtons() {
        if (browser != null) {
            // 注意：JBCefBrowser目前没有直接提供canGoBack和canGoForward方法
            // 这里我们简单地启用按钮，实际项目中可能需要更复杂的实现
            if (backButton != null) {
                // 可以通过其他方式检查是否可以后退
                backButton.setEnabled(true);
            }
            if (forwardButton != null) {
                forwardButton.setEnabled(true);
            }
        }
    }

    // 在 loadURL 之后调用此方法更新按钮状态
    private void onUrlLoaded() {
        ApplicationManager.getApplication().invokeLater(this::updateNavigationButtons);
    }

    private JButton createGoButton() {
        JButton visitButton = new JButton("Go");
        visitButton.addActionListener(e -> navigateToUrl());
        return visitButton;
    }

    private JButton createMoreDropdownButton() {
        JButton moreButton = new JButton("Tools", AllIcons.General.ExternalTools);
        moreButton.setHorizontalTextPosition(SwingConstants.LEFT); // 图标在左，文字在右
        moreButton.setIconTextGap(8); // 文字与图标间距
        JPopupMenu moreMenu = new JPopupMenu();

        // 添加导航菜单项
        moreMenu.add(createBackMenuItem());
        moreMenu.add(createForwardMenuItem());
        moreMenu.addSeparator();

        // 添加原有菜单项
        moreMenu.add(createHomeMenuItem());
        moreMenu.addSeparator();
//        moreMenu.add(createThemeToggleMenuItem());
//        moreMenu.addSeparator();
        moreMenu.add(createZoomInMenuItem());
        moreMenu.add(createZoomOutMenuItem());
        moreMenu.add(createResetZoomMenuItem());

        // 绑定弹出菜单
        moreButton.addActionListener(e -> moreMenu.show(moreButton, 0, moreButton.getHeight()));
        return moreButton;
    }

    private JMenuItem createBackMenuItem() {
        JMenuItem item = new JMenuItem("Back");
        item.setIcon(AllIcons.Actions.Back);
        item.addActionListener(e -> {
            if (browser != null) {
                browser.getCefBrowser().goBack();
                updateNavigationButtons();
            }
        });
        return item;
    }

    private JMenuItem createForwardMenuItem() {
        JMenuItem item = new JMenuItem("Forward");
        item.setIcon(AllIcons.Actions.Forward);
        item.addActionListener(e -> {
            if (browser != null) {
                browser.getCefBrowser().goForward();
                updateNavigationButtons();
            }
        });
        return item;
    }

    private JMenuItem createThemeToggleMenuItem() {
        themeToggleMenuItem = new JMenuItem(isDarkMode ? "Light Mode" : "Dark Mode");
        themeToggleMenuItem.setIcon(AllIcons.General.InspectionsEye);
        themeToggleMenuItem.addActionListener(e -> toggleTheme());
        return themeToggleMenuItem;
    }

    private JMenuItem createHomeMenuItem() {
        JMenuItem item = new JMenuItem("Home");
        item.setIcon(AllIcons.Nodes.HomeFolder);
        item.addActionListener(e -> loadWelcomeMessage2());
        return item;
    }

    private JMenuItem createZoomInMenuItem() {
        JMenuItem item = new JMenuItem("Zoom In");
        item.setIcon(AllIcons.General.ZoomIn);
        item.addActionListener(e -> {
            if (browser != null) {
                zoomFactor += ZOOM_STEP;
                browser.setZoomLevel(zoomFactor);
            }
        });
        return item;
    }

    private JMenuItem createZoomOutMenuItem() {
        JMenuItem item = new JMenuItem("Zoom Out");
        item.setIcon(AllIcons.General.ZoomOut);
        item.addActionListener(e -> {
            if (browser != null && zoomFactor > 0.1) {
                zoomFactor -= ZOOM_STEP;
                browser.setZoomLevel(zoomFactor);
            }
        });
        return item;
    }

    private JMenuItem createResetZoomMenuItem() {
        JMenuItem item = new JMenuItem("Zoom Reset");
        item.setIcon(AllIcons.General.Reset);
        item.addActionListener(e -> {
            if (browser != null) {
                zoomFactor = 1.0;
                browser.setZoomLevel(zoomFactor);
            }
        });
        return item;
    }

    /**
     * URL输入框添加Placeholder效果和回车监听
     */
    private void addPlaceholder() {
        if (urlField == null) {
            return;
        }
        urlField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (urlField.getText().equals(URL_MSG)) {
                    urlField.setText("");
                    urlField.setForeground(Color.LIGHT_GRAY);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (urlField.getText().isEmpty()) {
                    urlField.setForeground(Color.GRAY);
                    urlField.setText(URL_MSG);
                }
            }
        });

        urlField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (urlField.getText().equals(URL_MSG)) {
                    urlField.setText("");
                    urlField.setForeground(Color.LIGHT_GRAY);
                }
            }

            // 添加回车键监听
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigateToUrl();
                }
            }
        });
    }

    /**
     * 导航到URL字段中指定的网址
     */
    private void navigateToUrl() {
        ApplicationManager.getApplication().invokeLater(() -> {
            String url = urlField.getText().trim();
            if (!url.isEmpty() && browser != null) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                browser.loadURL(url);
                onUrlLoaded(); // 更新导航按钮状态
            }
        });
    }

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
        ApplicationManager.getApplication().invokeLater(() -> {
            if (browser == null) {
                return;
            }
            String htmlContent = loadHtmlFromResource("welcome.html");
            if (htmlContent != null) {
                browser.loadHTML(htmlContent);
            } else {
                System.err.println("Failed to load welcome.html");
            }
        });
    }

    private void applyTheme() {
        if (browser == null) return;

        String css = isDarkMode ?
                "body, html { " +
                        "  background-color: #1e1e1e !important; " +
                        "  color: #ffffff !important; " +
                        "} " +
                        "* { " +
                        "  background-color: #1e1e1e !important; " +
                        "  color: #ffffff !important; " +
                        "  border-color: #444444 !important; " +
                        "} " +
                        "a { " +
                        "  color: #4fc1ff !important; " +
                        "} " +
                        "a:hover { " +
                        "  color: #7dd3ff !important; " +
                        "} " +
                        "button, input, select, textarea { " +
                        "  background-color: #2d2d2d !important; " +
                        "  color: #ffffff !important; " +
                        "  border: 1px solid #555555 !important; " +
                        "} " +
                        "::placeholder { " +
                        "  color: #aaaaaa !important; " +
                        "}"
                :
                "body, html { " +
                        "  background-color: #ffffff !important; " +
                        "  color: #000000 !important; " +
                        "} " +
                        "* { " +
                        "  background-color: initial !important; " +
                        "  color: inherit !important; " +
                        "  border-color: initial !important; " +
                        "}";

        // 注入CSS到当前页面
        String script = "var existingStyle = document.getElementById('intellij-theme-style'); " +
                "if (existingStyle) { " +
                "  existingStyle.innerHTML = '" + css.replace("'", "\\'") + "'; " +
                "} else { " +
                "  var style = document.createElement('style'); " +
                "  style.id = 'intellij-theme-style'; " +
                "  style.type = 'text/css'; " +
                "  style.innerHTML = '" + css.replace("'", "\\'") + "'; " +
                "  document.getElementsByTagName('head')[0].appendChild(style); " +
                "}";

        browser.getCefBrowser().executeJavaScript(script, browser.getCefBrowser().getURL(), 0);
    }
}
