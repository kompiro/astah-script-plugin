package com.change_vision.astah.extension.plugin.script;

import java.io.File;

import javax.script.ScriptEngineManager;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.osgi.framework.BundleContext;

import com.change_vision.astah.extension.plugin.script.command.ReloadCommand;
import com.change_vision.astah.extension.plugin.script.util.FileModificationChecker;
import com.change_vision.astah.extension.plugin.script.util.Messages;

public class ScriptViewContext {
    private static final String MODIFIED_INDICATOR_STR = "(*)";
    public static BundleContext bundleContext;

    public JDialog dialog;
    public JComboBox scriptKindCombobox;
    public ScriptTextArea scriptTextArea;
    public ScriptOutput scriptOutput;
    public JLabel statusBar;

    public File currentFile;
    public boolean isModified = false;
    public HistoryManager historyManager;
    public FileModificationChecker fileModificationChecker;
    public ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    public void setIsModified(boolean isModified) {
        this.isModified = isModified;
        updateTitleBar();
    }

    public void setCurrentFile(File file) {
        if (file == null) {
            setIsModified(false);
            if (fileModificationChecker != null) {
                fileModificationChecker.stop();
                fileModificationChecker = null;
            }
        }

        currentFile = file;
        updateTitleBar();

        if (currentFile != null && fileModificationChecker == null) {
            fileModificationChecker = new FileModificationChecker(currentFile, new Runnable() {
                @Override
                public void run() {
                    onModifiedByOtherTool();
                }
            });
            fileModificationChecker.start();
        }
    }

    private void updateTitleBar() {
        String modifiedIndicator = isModified ? MODIFIED_INDICATOR_STR : "";
        if (currentFile == null) {
            dialog.setTitle(Messages.getMessage("new_script.title") + modifiedIndicator);
        } else {
            dialog.setTitle("[" + currentFile.getName() + "]" + modifiedIndicator + " "
                    + currentFile.getAbsolutePath());
        }
    }

    private void onModifiedByOtherTool() {
        int result = JOptionPane.showConfirmDialog(dialog,
                Messages.getMessage("message.ask_on_modified_by_other_tool"));
        if (result == JOptionPane.YES_OPTION) {
            ReloadCommand.execute(this);
        } else if (result == JOptionPane.CANCEL_OPTION) {
            return;
        }
    }
}
