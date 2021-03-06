package com.change_vision.astah.extension.plugin.script;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.RecordableTextAction;

import com.change_vision.astah.extension.plugin.script.command.BrowseCommand;
import com.change_vision.astah.extension.plugin.script.command.CloseCommand;
import com.change_vision.astah.extension.plugin.script.command.NewCommand;
import com.change_vision.astah.extension.plugin.script.command.OpenCommand;
import com.change_vision.astah.extension.plugin.script.command.ReloadCommand;
import com.change_vision.astah.extension.plugin.script.command.RunCommand;
import com.change_vision.astah.extension.plugin.script.command.SaveAsCommand;
import com.change_vision.astah.extension.plugin.script.command.SaveCommand;
import com.change_vision.astah.extension.plugin.script.util.Messages;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.ui.IWindow;

public class ScriptView {
    private static final double DIVIDER_LOCATION = 0.8;
    private static final int DIALOG_WIDTH = 500;
    private static final int DIALOG_HEIGHT = 700;

    private static ScriptView instance; // singleton
    private ScriptViewContext context = new ScriptViewContext();

    private ScriptView() {
    }

    public static ScriptView getInstance() {
        if (instance == null) {
            instance = new ScriptView();
        }
        return instance;
    }

    public void show(IWindow window) throws ClassNotFoundException, ProjectNotFoundException {
        if (context.dialog == null) {
            createAndShowScriptDialog(window);
        } else {
            context.dialog.setVisible(true);
        }
    }

    public void createAndShowScriptDialog(IWindow window) throws ClassNotFoundException,
            ProjectNotFoundException {
        Window parentWindow = null;
        if (window != null) {
            parentWindow = window.getParent();
        }
        JDialog dialog = new JDialog(parentWindow, Messages.getMessage("dialog.title"));

        JPanel cp = new JPanel(new BorderLayout());

        // Script Text & Output Text
        JSplitPane mainPane = createMainPanel();
        cp.add(mainPane, BorderLayout.CENTER);

        JMenuBar menuBar = createMenuBar();
        dialog.setJMenuBar(menuBar);

        JToolBar toolBar = createToolBar();
        cp.add(toolBar, BorderLayout.NORTH);

        cp.add(createStatusBar(), BorderLayout.SOUTH);

        dialog.setContentPane(cp);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (context.isModified) {
                    int result = JOptionPane.showConfirmDialog(context.dialog,
                            Messages.getMessage("message.ask_save"));
                    if (result == JOptionPane.YES_OPTION) {
                        SaveCommand.execute(context);
                    } else if (result == JOptionPane.CANCEL_OPTION) {
                        //We don't close the dialog if user canceled
                        return;
                    }
                }
                context.dialog.setVisible(false);
            }
        });
        dialog.pack();
        dialog.setSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        dialog.setLocationRelativeTo(parentWindow);
        dialog.setVisible(true);
        mainPane.setDividerLocation(DIVIDER_LOCATION);

        context.dialog = dialog;
        NewCommand.execute(context);
    }

    private JLabel createStatusBar() {
        JLabel statusBar = new JLabel();
        context.statusBar = statusBar;
        return statusBar;
    }

    private JSplitPane createMainPanel() {
        // Script Text
        RTextScrollPane scriptPane = createScriptEditorTextArea();

        // Output Text
        context.scriptOutput = new ScriptOutput();
        JScrollPane outputPane = new JScrollPane(context.scriptOutput);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(scriptPane);
        splitPane.setBottomComponent(outputPane);

        return splitPane;
    }

    private RTextScrollPane createScriptEditorTextArea() {
        ScriptTextArea scriptTextArea = new ScriptTextArea();
        scriptTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                // Ignore because this is called even if we change only
                // the style of document
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                context.setIsModified(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                context.setIsModified(true);
            }
        });
        context.scriptTextArea = scriptTextArea;
        RTextScrollPane areaWithScroll = new RTextScrollPane(scriptTextArea);
        areaWithScroll.setFoldIndicatorEnabled(true);
        return areaWithScroll;
    }

    private JComboBox createScriptKindCombobox() {
        final JComboBox scriptKindCombobox = new JComboBox();

        final List<ScriptEngineFactory> engineFactories = ScriptEngineHelper
                .getScriptEngineFactories(context.scriptEngineManager);
        for (ScriptEngineFactory engineFactory : engineFactories) {
            scriptKindCombobox.addItem(engineFactory.getLanguageName());
        }
        scriptKindCombobox.setSelectedItem(ScriptTextArea.DEFAULT_LANGUAGE);

        scriptKindCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object item = e.getItem();
                    context.scriptTextArea.updateSyntaxByLanguage((String) item);
                }
            }
        });

        return scriptKindCombobox;
    }

    private JMenuBar createMenuBar() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        final int shortcutKeyMask = tk.getMenuShortcutKeyMask();

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu(Messages.getMessage("file_menu.label"));
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem item;
        fileMenu.add(item = new JMenuItem(Messages.getMessage("action.new.label"),
                getIcon("images/new.png")));
        item.setMnemonic(KeyEvent.VK_N);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutKeyMask));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NewCommand.execute(context);
            }
        });

        fileMenu.add(item = new JMenuItem(Messages.getMessage("action.open.label"),
                getIcon("images/open.png")));
        item.setMnemonic(KeyEvent.VK_O);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutKeyMask));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OpenCommand.execute(context);
            }
        });

        fileMenu.add(item = new JMenuItem(Messages.getMessage("action.reload.label"),
                getIcon("images/reload.png")));
        item.setMnemonic(KeyEvent.VK_R);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ReloadCommand.execute(context);
            }
        });

        fileMenu.add(item = new JMenuItem(Messages.getMessage("action.save.label"),
                getIcon("images/save.png")));
        item.setMnemonic(KeyEvent.VK_S);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SaveCommand.execute(context);
            }
        });

        fileMenu.add(item = new JMenuItem(Messages.getMessage("action.save_as.label"),
                getIcon("images/saveAs.png")));
        item.setMnemonic(KeyEvent.VK_A);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask
                | KeyEvent.SHIFT_MASK));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SaveAsCommand.execute(context);
            }
        });

        fileMenu.addSeparator();

        fileMenu.add(item = new JMenuItem(Messages.getMessage("action.close.label"),
                getIcon("images/close.png")));
        item.setMnemonic(KeyEvent.VK_C);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutKeyMask));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CloseCommand.execute(context);
            }
        });

        menuBar.add(fileMenu);

        JMenu historyMenu = new JMenu(Messages.getMessage("history_menu.label"));
        fileMenu.add(historyMenu);
        context.historyManager = new HistoryManager(historyMenu, context);
        context.historyManager.updateRecentFileMenu();

        menuBar.add(historyMenu);

        JMenu editMenu = new JMenu(Messages.getMessage("edit_menu.label"));
        fileMenu.setMnemonic(KeyEvent.VK_E);

        RecordableTextAction undoAction = RTextArea.getAction(RTextArea.UNDO_ACTION);
        editMenu.add(new JMenuItem(undoAction));

        RecordableTextAction redoAction = RTextArea.getAction(RTextArea.REDO_ACTION);
        editMenu.add(new JMenuItem(redoAction));

        editMenu.addSeparator();

        RecordableTextAction cutAction = RTextArea.getAction(RTextArea.CUT_ACTION);
        editMenu.add(new JMenuItem(cutAction));

        RecordableTextAction copyAction = RTextArea.getAction(RTextArea.COPY_ACTION);
        editMenu.add(new JMenuItem(copyAction));

        RecordableTextAction pasteAction = RTextArea.getAction(RTextArea.PASTE_ACTION);
        editMenu.add(new JMenuItem(pasteAction));

        editMenu.addSeparator();

        RecordableTextAction selectAllAction = RTextArea.getAction(RTextArea.SELECT_ALL_ACTION);
        editMenu.add(new JMenuItem(selectAllAction));

        menuBar.add(editMenu);

        JMenu actionMenu = new JMenu(Messages.getMessage("action_menu.label"));
        actionMenu.setMnemonic(KeyEvent.VK_A);
        actionMenu.add(item = new JMenuItem(Messages.getMessage("action.run.label"),
                getIcon("images/run.png")));
        item.setMnemonic(KeyEvent.VK_R);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutKeyMask));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RunCommand.execute(context);
            }
        });

        menuBar.add(actionMenu);

        JMenu helpMenu = new JMenu(Messages.getMessage("help_menu.label"));
        helpMenu.setMnemonic(KeyEvent.VK_H);

        helpMenu.add(item = new JMenuItem(Messages.getMessage("action.open_api_guide.label")));
        item.setMnemonic(KeyEvent.VK_A);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BrowseCommand.execute(Messages.getMessage("api_guide_uri"));
            }
        });

        helpMenu.add(item = new JMenuItem(Messages.getMessage("action.open_api_reference.label")));
        item.setMnemonic(KeyEvent.VK_J);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BrowseCommand.execute(Messages.getMessage("api_reference_uri"));
            }
        });

        menuBar.add(helpMenu);
        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        // toolBar.setRollover(true);
        toolBar.setFocusable(false);

        JButton newButton = new JButton(getIcon("images/new.png"));
        toolBar.add(newButton);
        newButton.setToolTipText(Messages.getMessage("action.new.tooltip"));
        newButton.setRequestFocusEnabled(false);
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NewCommand.execute(context);
            }
        });

        JButton openButton = new JButton(getIcon("images/open.png"));
        toolBar.add(openButton);
        openButton.setToolTipText(Messages.getMessage("action.open.tooltip"));
        openButton.setRequestFocusEnabled(false);
        openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OpenCommand.execute(context);
            }
        });

        JButton reloadButton = new JButton(getIcon("images/reload.png"));
        toolBar.add(reloadButton);
        reloadButton.setToolTipText(Messages.getMessage("action.reload.tooltip"));
        reloadButton.setRequestFocusEnabled(false);
        reloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ReloadCommand.execute(context);
            }
        });

        JButton saveButton = new JButton(getIcon("images/save.png"));
        toolBar.add(saveButton);
        saveButton.setToolTipText(Messages.getMessage("action.save.tooltip"));
        saveButton.setRequestFocusEnabled(false);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SaveCommand.execute(context);
            }
        });

        toolBar.addSeparator();

        RecordableTextAction undoAction = RTextArea.getAction(RTextArea.UNDO_ACTION);
        JButton undoButton = new JButton(undoAction);
        undoButton.setIcon(getIcon("images/undo.png"));
        undoButton.setHideActionText(true);
        undoButton.setRequestFocusEnabled(false);
        toolBar.add(undoButton);

        RecordableTextAction redoAction = RTextArea.getAction(RTextArea.REDO_ACTION);
        JButton redoButton = new JButton(redoAction);
        redoButton.setIcon(getIcon("images/redo.png"));
        redoButton.setHideActionText(true);
        redoButton.setRequestFocusEnabled(false);
        toolBar.add(redoButton);

        toolBar.addSeparator();

        JButton runButton = new JButton(getIcon("images/run.png"));
        toolBar.add(runButton);
        runButton.setToolTipText(Messages.getMessage("action.run.tooltip"));
        runButton.setRequestFocusEnabled(false);
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RunCommand.execute(context);
            }
        });

        toolBar.add(Box.createGlue());

        // Script Kind
        context.scriptKindCombobox = createScriptKindCombobox();
        toolBar.add(context.scriptKindCombobox);

        return toolBar;
    }

    private ImageIcon getIcon(String path) {
        URL url = getClass().getClassLoader().getResource(path);
        if (url != null) {
            return new ImageIcon(url);
        } else {
            System.err.println("No icon resource: " + path);
            return null;
        }
    }

}