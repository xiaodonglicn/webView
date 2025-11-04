package com.lxd.webviewplugin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WebViewToolWindow implements ToolWindowFactory {

    private static final String URL_MSG = "Please enter the correct URL and press Enter";
    private JBCefBrowser browser;
    private JTextField urlField;
    private ContentFactory contentFactory;
    // 缩放控制变量
    private double zoomFactor = 1.0;
    private static final double ZOOM_STEP = 0.1;
    // 在类的字段部分添加以下新字段
    private JMenuItem backButton;
    private JMenuItem devToolsButton;
    private JMenuItem forwardButton;

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
            this.doNoOpenNewWindow();
            // 添加加载监听器来处理URL更新和按钮状态
            this.doMonitorUpdateURL();
            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(browser.getComponent(), BorderLayout.CENTER);
            // 加载欢迎页面
            this.loadWelcomeMessage2();
            // 初始化导航按钮状态
            if (backButton != null) {
                backButton.setEnabled(false);
            }
            if (forwardButton != null) {
                forwardButton.setEnabled(false);
            }
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
        JButton moreButton = this.createMoreDropdownButton();

        // 布局地址栏和按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(moreButton);

        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        return topPanel;
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

    private JButton createMoreDropdownButton() {
        JButton moreButton = new JButton("Tools", AllIcons.General.ExternalTools);
        moreButton.setHorizontalTextPosition(SwingConstants.LEFT); // 图标在左，文字在右
        moreButton.setIconTextGap(8); // 文字与图标间距
        JPopupMenu moreMenu = new JPopupMenu();

        // 添加导航菜单项
        moreMenu.add(createBackMenuItem());
        moreMenu.add(createForwardMenuItem());
        moreMenu.addSeparator();
        // 调试功能
        moreMenu.add(createDevToolMenuItem());
        moreMenu.addSeparator();
        // 添加原有菜单项
        moreMenu.add(createHomeMenuItem());
        moreMenu.addSeparator();
        moreMenu.add(createZoomInMenuItem());
        moreMenu.add(createZoomOutMenuItem());
        moreMenu.add(createResetZoomMenuItem());

        // 绑定弹出菜单
        moreButton.addActionListener(e -> moreMenu.show(moreButton, 0, moreButton.getHeight()));
        return moreButton;
    }

    private JMenuItem createDevToolMenuItem() {
        devToolsButton = new JMenuItem("F12");
        devToolsButton.setIcon(AllIcons.Toolwindows.ToolWindowDebugger);
        devToolsButton.addActionListener(e -> toggleDevTools());
        return devToolsButton;
    }

    private JMenuItem createBackMenuItem() {
        backButton = new JMenuItem("Back");
        backButton.setIcon(AllIcons.Actions.Back);
        backButton.addActionListener(e -> {
            if (browser != null) {
                browser.getCefBrowser().goBack();
            }
        });
        return backButton;
    }

    private JMenuItem createForwardMenuItem() {
        forwardButton = new JMenuItem("Forward");
        forwardButton.setIcon(AllIcons.Actions.Forward);
        forwardButton.addActionListener(e -> {
            if (browser != null) {
                browser.getCefBrowser().goForward();
            }
        });
        return forwardButton;
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

    /**
     * 添加加载监听器来处理URL更新和按钮状态
     */
    private void doMonitorUpdateURL() {
        browser.getJBCefClient().addLoadHandler(new org.cef.handler.CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 更新导航按钮状态
                    if (WebViewToolWindow.this.backButton != null) {
                        WebViewToolWindow.this.backButton.setEnabled(canGoBack);
                    }
                    if (WebViewToolWindow.this.forwardButton != null) {
                        WebViewToolWindow.this.forwardButton.setEnabled(canGoForward);
                    }
                    // 当页面加载完成时更新地址栏
                    if (!isLoading) {
                        String currentUrl = browser.getURL();
                        if (urlField != null && currentUrl != null) {
                            // 如果是内部生成的页面或about:blank，显示占位符
                            if (currentUrl.equals("about:blank") || currentUrl.startsWith("file:///jbcefbrowser/")) {
                                urlField.setText(URL_MSG);
                                urlField.setForeground(Color.GRAY);
                            } else {
                                urlField.setText(currentUrl);
                                urlField.setForeground(Color.LIGHT_GRAY);
                            }
                        }
                    }
                });
            }
        }, browser.getCefBrowser());
    }

    /**
     * 添加生命周期处理器，防止打开新窗口
     */
    private void doNoOpenNewWindow() {
        browser.getJBCefClient().addLifeSpanHandler(new org.cef.handler.CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
                // 阻止弹出新窗口，在当前浏览器中加载URL
                WebViewToolWindow.this.browser.loadURL(target_url);
                return true; // 返回true表示取消弹窗
            }
        }, browser.getCefBrowser());
    }

    private void toggleDevTools() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!JBCefApp.isSupported()) {
                Messages.showErrorDialog("JCEF is disabled in IDE settings", "Error");
                return;
            }

            if (browser == null || browser.getCefBrowser() == null) {
                Messages.showErrorDialog("Browser is not properly initialized", "Error");
                return;
            }

            // 检查当前页面是否是可以打开开发者工具的有效页面
            String currentUrl = browser.getCefBrowser().getURL();
            if (currentUrl == null || currentUrl.isEmpty() ||
                    currentUrl.equals("about:blank") ||
                    currentUrl.startsWith("file:///jbcefbrowser/")) {
                Messages.showInfoMessage("Please load a web page first before opening F12", "Info");
                return;
            }

            try {
                Project project = ApplicationManager.getApplication().getService(com.intellij.openapi.project.ProjectManager.class)
                        .getOpenProjects()[0]; // 获取当前项目
                // 如果开发者工具已经打开，则关闭它
                if (DevToolsToolWindowFactory.isDevToolsOpen(project)) {
                    DevToolsToolWindowFactory.closeDevTools(project);
                } else {
                    // 否则打开开发者工具
                    DevToolsToolWindowFactory.openDevTools(project, browser.getCefBrowser());
                }
            } catch (Exception e) {
                Messages.showErrorDialog("Failed to toggle developer tools: " + e.getMessage(), "Error");
            }
        });
    }
}
