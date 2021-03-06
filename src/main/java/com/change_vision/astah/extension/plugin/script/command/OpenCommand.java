package com.change_vision.astah.extension.plugin.script.command;

import java.awt.Cursor;
import java.awt.FileDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.script.ScriptEngine;
import javax.swing.JOptionPane;

import com.change_vision.astah.extension.plugin.script.ScriptViewContext;
import com.change_vision.astah.extension.plugin.script.util.Messages;

public class OpenCommand {
    public static void execute(ScriptViewContext context) {
        execute(context, null);
    }

    public static void execute(ScriptViewContext context, String filePath) {
        if (context.isModified) {
            int result = JOptionPane.showConfirmDialog(context.dialog,
                    Messages.getMessage("message.ask_save"));
            if (result == JOptionPane.YES_OPTION) {
                SaveCommand.execute(context);
            } else if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        if (filePath == null) {
            FileDialog fileDialog = new FileDialog(context.dialog,
                    Messages.getMessage("open_script_dialog.title"), FileDialog.LOAD);
            fileDialog.setVisible(true);
            String selectedFile = fileDialog.getFile();
            if (selectedFile == null) {
                return;
            }

            context.setIsModified(false);
            NewCommand.execute(context); // Close current script file

            filePath = fileDialog.getDirectory() + selectedFile;
        }
        File f = new File(filePath);
        context.dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            context.scriptTextArea.read(reader, null);
            reader.close();
            context.scriptTextArea.discardAllEdits();

            context.setCurrentFile(f);
            updateScriptKindByFileExtension(context);
            context.setIsModified(false);
            context.statusBar.setText(Messages.getMessage("status.loaded") + filePath);
            context.historyManager.addFile(context.currentFile.getAbsolutePath());
        } catch (FileNotFoundException exc) {
            context.statusBar.setText(Messages.getMessage("status.file_not_found") + filePath);
        } catch (IOException exc) {
            context.statusBar.setText(Messages.getMessage("status.io_exception") + filePath);
        } finally {
            context.dialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private static void updateScriptKindByFileExtension(ScriptViewContext context) {
        if (context.currentFile == null) {
            return;
        }
        String fileName = context.currentFile.getName();
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot < 0) {
            return;
        }
        String extension = fileName.substring(lastIndexOfDot + 1);

        ScriptEngine engine = context.scriptEngineManager.getEngineByExtension(extension);
        if (engine != null) {
            String langName = engine.getFactory().getLanguageName();
            context.scriptKindCombobox.setSelectedItem(langName);
        }
    }

}
