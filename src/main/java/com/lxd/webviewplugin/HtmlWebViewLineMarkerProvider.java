package com.lxd.webviewplugin;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.function.Supplier;

public class HtmlWebViewLineMarkerProvider implements LineMarkerProvider {
    
    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        // 只在HTML文件的<html>标签添加标记
        try {
            if (element instanceof XmlTag && element.getParent() instanceof XmlDocument) {
                XmlTag tag = (XmlTag) element;
                if ("html".equals(tag.getName())) {
                    PsiFile psiFile = element.getContainingFile();
                    if (psiFile instanceof XmlFile && psiFile.getName().endsWith(".html")) {
                        VirtualFile virtualFile = psiFile.getVirtualFile();
                        if (virtualFile != null) {
                            return new LineMarkerInfo<>(
                                    element,
                                    element.getTextRange(),
                                    AllIcons.Javaee.WebModuleGroup,
                                    e -> "Open in WebView",
                                    new WebViewNavigationHandler(virtualFile),
                                    GutterIconRenderer.Alignment.CENTER,
                                    () -> "Open in WebView"
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {

        }

        return null;
    }
    
    public void collectSlowLineMarkers(@NotNull Collection<? extends PsiElement> elements,
                                      @NotNull Collection<? super LineMarkerInfo<?>> result) {
        // 可以留空，或者处理需要更多时间的标记
    }
    
    private static class WebViewNavigationHandler implements GutterIconNavigationHandler<PsiElement> {
        private final VirtualFile file;
        
        WebViewNavigationHandler(VirtualFile file) {
            this.file = file;
        }
        
        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
            Project project = elt.getProject();
            if (project != null) {
                openInWebView(project, file);
            }
        }
        
        private void openInWebView(Project project, VirtualFile file) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebView");
            if (toolWindow != null) {
                toolWindow.show(() -> {
                    String fileUrl = "file://" + file.getPath();
                    WebViewToolWindow.loadUrlFromExternal(project, fileUrl);
                });
            }
        }
    }
}
