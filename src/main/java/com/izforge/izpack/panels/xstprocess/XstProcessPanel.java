package com.izforge.izpack.panels.xstprocess;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.installer.gui.IzPanel;
import com.izforge.izpack.util.PlatformModelMatcher;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class XstProcessPanel extends IzPanel implements AbstractUIProcessHandler {
    private static final long serialVersionUID = 3258417209583155251L;
    protected JLabel processLabel;
    protected JProgressBar overallProgressBar;
    private boolean validated = false;

    private ProcessPanelWorker worker;
    private int noOfJobs = 0;
    private int currentJob = 0;

    private JTextPane outputPane;
    private boolean finishedWork = false;

    public XstProcessPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources, RulesEngine rules, PlatformModelMatcher matcher) {
        super(panel, parent, installData, resources);

        this.worker = new ProcessPanelWorker(installData, rules, resources, matcher);
        this.worker.setHandler(this);
        JLabel heading = new JLabel();
        Font font = heading.getFont();
        font = font.deriveFont(Font.BOLD, font.getSize() * 2.0F);
        heading.setFont(font);
        heading.setHorizontalAlignment(JLabel.CENTER);
        heading.setText(getString("ProcessPanel.heading"));
        heading.setVerticalAlignment(JLabel.TOP);
        BorderLayout layout = new BorderLayout();
        layout.setHgap(2);
        layout.setVgap(2);
        setLayout(layout);
        add(heading, BorderLayout.NORTH);

        JPanel subpanel = new JPanel();
        subpanel.setAlignmentX(0.5F);
        subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.Y_AXIS));

        this.processLabel = new JLabel();
        this.processLabel.setAlignmentX(0.5F);
        this.processLabel.setText(" ");
        subpanel.add(this.processLabel);

        this.overallProgressBar = new JProgressBar();
        this.overallProgressBar.setAlignmentX(0.5F);
        this.overallProgressBar.setStringPainted(true);
        subpanel.add(this.overallProgressBar);

        this.outputPane = new JTextPane();
        this.outputPane.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(this.outputPane);
        subpanel.add(outputScrollPane);

        add(subpanel, BorderLayout.CENTER);
    }

    @Override
    public boolean isValidated() {
        return this.validated;
    }

    @Override
    public void startProcessing(final int noOfJobs) {
        this.noOfJobs = noOfJobs;
        SwingUtilities.invokeLater(() -> {
            overallProgressBar.setMaximum(noOfJobs);
            overallProgressBar.setIndeterminate(true);
            parent.lockPrevButton();
        });
    }
    @Override
    public void finishProcessing(final boolean unlockPrev, final boolean unlockNext) {
        SwingUtilities.invokeLater(() -> {
            overallProgressBar.setIndeterminate(false);
            String noOfJobsStr = Integer.toString(noOfJobs);
            overallProgressBar.setString(noOfJobsStr + " / " + noOfJobsStr);
            processLabel.setText(" ");
            processLabel.setEnabled(false);
            validated = true;
            installData.setInstallSuccess(worker.getResult());
            if (installData.getPanels().indexOf(this) != installData.getPanels().size() - 1) {
                if (unlockNext) {
                    parent.unlockNextButton();
                }
            }
            if (unlockPrev) {
                parent.unlockPrevButton();
            }
            finishedWork = installData.isInstallSuccess();
        });
    }

    @Override
    public void logOutput(String message, boolean stderr) {
        final boolean stdError = stderr;
        final String logMessage = message;
        final StyledDocument doc = outputPane.getStyledDocument();
        final SimpleAttributeSet style = new SimpleAttributeSet();
        
        SwingUtilities.invokeLater(() -> {
            if (stdError) {
                StyleConstants.setForeground(style, Color.RED);
            }
            try {
                doc.insertString(doc.getLength(), "\n" + logMessage, style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void startProcess(final String jobName) {
        final StyledDocument doc = outputPane.getStyledDocument();
        final SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setBold(style, true);

        SwingUtilities.invokeLater(() -> {
            processLabel.setText(jobName);
            incrementCurrentJob();
            overallProgressBar.setValue(currentJob);
            overallProgressBar.setString(currentJob + " / " + noOfJobs);
            String message = "->Starting Job: " + jobName + " <-";
            try {
                doc.insertString(doc.getLength(), "\n\n" + message, style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void finishProcess() {
        final StyledDocument doc = outputPane.getStyledDocument();
        final SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setBold(style, true);

        SwingUtilities.invokeLater(() -> {
            String message = "\n->Ended Job<-\n";
            try {
                doc.insertString(doc.getLength(), message, style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    @Override
    public void panelActivate() {
        Dimension dimension = parent.getPanelsContainerSize();
        dimension.width -= dimension.width / 4;
        dimension.height = 150;
        setMinimumSize(dimension);
        setMaximumSize(dimension);
        setPreferredSize(dimension);

        parent.lockNextButton();

        currentJob = 0;

        if (!finishedWork) {
            worker.startThread();
        }
    }

    @Override
    public void makeXMLData(IXMLElement panelRoot) {
        // Implementation here if needed
    }

    @Override
    public void skipProcess(String name, String message) {
        processLabel.setText(name);
        incrementCurrentJob();
        overallProgressBar.setValue(currentJob);
        overallProgressBar.setString(currentJob + " / " + noOfJobs);

        final StyledDocument doc = outputPane.getStyledDocument();
        final SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setBold(style, true);

        SwingUtilities.invokeLater(() -> {
            String skipMessage = "\n->Skipping job: " + name + "<-\nReason:" + message + "\n\n";
            try {
                doc.insertString(doc.getLength(), skipMessage, style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void incrementCurrentJob() {
        currentJob++;
    }
}
