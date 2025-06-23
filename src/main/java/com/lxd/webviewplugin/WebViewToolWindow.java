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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class WebViewToolWindow implements ToolWindowFactory {

    private static final String URL_MSG = "Please enter the correct URL";
    private JBCefBrowser browser;
    private JTextField urlField;
    private ContentFactory contentFactory;
    private JFrame devToolsFrame;
    // 缩放控制变量
    private double zoomFactor = 1.0;
    private static final double ZOOM_STEP = 0.1;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // 创建主面板
            JPanel panel = new JPanel(new BorderLayout());

            // 构建顶部工具栏
            JPanel topPanel = this.createTopToolbar();

            // 创建浏览器组件
            browser = new JBCefBrowser();
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

        JButton visitButton = this.createGoButton();
        JButton moreButton = this.createMoreDropdownButton();

        // 布局地址栏和按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(visitButton);
        buttonPanel.add(moreButton);

        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        return topPanel;
    }

    private JButton createGoButton() {
        JButton visitButton = new JButton("Go");
        visitButton.addActionListener(e -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                String url = urlField.getText().trim();
                if (!url.isEmpty() && browser != null) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }
                    browser.loadURL(url);
                }
            });
        });
        return visitButton;
    }

    private JButton createMoreDropdownButton() {
        JButton moreButton = new JButton("More", AllIcons.Actions.MoveDown);
        moreButton.setHorizontalTextPosition(SwingConstants.LEFT); // 图标在左，文字在右
        moreButton.setIconTextGap(8); // 文字与图标间距
        JPopupMenu moreMenu = new JPopupMenu();

        // 添加菜单项
        moreMenu.add(createHomeMenuItem());
        moreMenu.add(createDevToolsMenuItem());
        moreMenu.addSeparator();
        moreMenu.add(createZoomInMenuItem());
        moreMenu.add(createZoomOutMenuItem());
        moreMenu.add(createResetZoomMenuItem());

        // 绑定弹出菜单
        moreButton.addActionListener(e -> moreMenu.show(moreButton, 0, moreButton.getHeight()));
        return moreButton;
    }

    private JMenuItem createHomeMenuItem() {
        JMenuItem item = new JMenuItem("Home");
        item.setIcon(AllIcons.Nodes.HomeFolder);
        item.addActionListener(e -> loadWelcomeMessage2());
        return item;
    }

    private JMenuItem createDevToolsMenuItem() {
        JMenuItem item = new JMenuItem("F12 DevTools");
        item.setIcon(AllIcons.Toolwindows.ToolWindowDebugger);
        item.addActionListener(e -> toggleDevTools());
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
     * URL输入框添加Placeholder效果
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
        });
    }

    private void toggleDevTools() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!JBCefApp.isSupported()) {
                Messages.showInfoMessage("JCEF is disabled in IDE settings", "Information");
                return;
            }
            try {
                if (devToolsFrame != null) {
                    devToolsFrame.dispose();
                    devToolsFrame = null;
                    return;
                }
                Point inspectAt = calculateDevToolsPosition();
                CefBrowser devTools = browser.getCefBrowser().getDevTools(inspectAt);
                if (devTools == null) {
                    devTools = browser.getCefBrowser().getDevTools();
                }
                if (devTools == null) {
                    Messages.showInfoMessage("Developer tools are not available", "Information");
                    return;
                }
                devToolsFrame = new JFrame("WebView DevTools");
                devToolsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                devToolsFrame.setSize(800, 600);
                devToolsFrame.setLocationRelativeTo(null);
                Component devToolsUI = devTools.getUIComponent();
                if (devToolsUI != null) {
                    devToolsFrame.add(devToolsUI);
                    devToolsFrame.setVisible(true);
                }
                devToolsFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        devToolsFrame = null;
                    }
                });
            } catch (Exception e) {
                Messages.showInfoMessage("The current version does not support it", "Information");
                if (devToolsFrame != null) {
                    devToolsFrame.dispose();
                    devToolsFrame = null;
                }
            }
        });
    }

    private Point calculateDevToolsPosition() {
        Point mainWindowLoc = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getActiveWindow().getLocation();
        return new Point(mainWindowLoc.x + 650, mainWindowLoc.y + 50);
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

}
