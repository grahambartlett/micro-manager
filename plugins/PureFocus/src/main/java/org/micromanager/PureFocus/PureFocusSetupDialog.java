/**
@file PureFocusSetup.java
@author Graham Bartlett
@copyright Prior Scientific Instruments Limited, 2021
@brief Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus unit

Micro-Manager plugin giving GUI control of the PF-850 setup and configuration.

Licensed under the BSD license.
*/

package org.micromanager.PureFocus;

import java.io.File;
import java.nio.file.Files;
import java.io.BufferedOutputStream;
import java.nio.file.StandardOpenOption;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JFileChooser;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.micromanager.Studio;
import org.micromanager.internal.utils.WindowPositioning;


/** Setup dialog for monitoring line reads and configuring the PF-850.
 */
@SuppressWarnings(value = {"serial", "static-access"})  
public class PureFocusSetupDialog extends JDialog implements ActionListener
{
    /** Reference to the MM/ImageJ GUI */
	private final Studio gui_;
    
    /** Reference to the frame which owns this */
	private final PureFocusFrame parent_;
    
    LimitedChartPanel graphPanel_;
    private JFreeChart graph_;
    private XYSeries graphData_;
    
    private JTextField crosshairs_;
    
    private JButton saveToFile_;
    private JButton continuousScan_;
    private JButton singleScan_;
    
    private JTextField defaultPinholeCentre_;
    private JButton setCentre_;
    private JButton autoSetCentre_;
    
    private JTextField currentBackground_;
    private JTextField defaultBackground_;
    private JButton setBackground_;
    private JButton setToZero_;
    private JButton setToAverage_;
    
    private int averageLevel_;
    private int pinholeCentreLocation_;

    
	/** Create dialog.  This is always instantiated when the main window is
     * created, and is shown or hidden as required.
	 * @param parent Base window
	 * @param gui MM script interface
   	 */
	public PureFocusSetupDialog(PureFocusFrame parent, Studio gui)
	{
        // We want this to be a modeless dialog, to work with the rest of the GUI
        super(parent, "Prior PureFocus PF-850 Setup");
            
		parent_ = parent;
        gui_ = gui;
        
		initComponents();
        
        super.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/org/micromanager/icons/microscope.gif")));
        
		updateValues(true);
	}


	/** This method is called from within the constructor to initialize the form.
	*/
	@SuppressWarnings("unchecked")
	private void initComponents()
	{
        int i;
        
        // Set up layout of panels
        GridBagLayout topLayout = new GridBagLayout();
        this.getContentPane().setLayout(topLayout);
        
        GridBagConstraints constraints;
        
        // Initialise graph data/display
        graphData_ = new XYSeries("");
        graphData_.setMaximumItemCount(1500);
        for (i = 0; i < 1500; i ++)
        {
            graphData_.add((double)i, 0.0);
        }        
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(graphData_);
        
        /* Main UI structure */
        graph_ = ChartFactory.createXYLineChart("", "", "", dataset);
        graphPanel_ = new LimitedChartPanel(graph_, 0.0, 1500.0, 500.0, 0.0, 4096.0, 1024.0, Color.blue);
        graphPanel_.setPreferredSize(new Dimension(800, 500));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        this.add(graphPanel_, constraints);
        
        crosshairs_ = new JTextField("(0,0)");
        crosshairs_.setMinimumSize(new Dimension(75,1));
        crosshairs_.setEditable(false);
        crosshairs_.setHorizontalAlignment(JTextField.CENTER);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.CENTER;
        this.add(crosshairs_, constraints);
        
        JPanel setupModePanel = new JPanel();
        GridBagLayout setupModeLayout = new GridBagLayout();
        setupModePanel.setLayout(setupModeLayout);        
        setupModePanel.setBorder(BorderFactory.createTitledBorder("Setup mode"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weightx = 0.0;
        this.add(setupModePanel, constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.weightx = 1.0;
        this.add(new JLabel(""), constraints);   
        
        JPanel pinholeCentrePanel = new JPanel();
        GridBagLayout pinholeCentreLayout = new GridBagLayout();
        pinholeCentrePanel.setLayout(pinholeCentreLayout);        
        pinholeCentrePanel.setBorder(BorderFactory.createTitledBorder("Pinhole centre"));
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.weightx = 0.0;
        this.add(pinholeCentrePanel, constraints);        
        
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.weightx = 1.0;
        this.add(new JLabel(""), constraints);   
          
        JPanel backgroundPanel = new JPanel();
        GridBagLayout backgroundLayout = new GridBagLayout();
        backgroundPanel.setLayout(backgroundLayout);        
        backgroundPanel.setBorder(BorderFactory.createTitledBorder("Background"));
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.weightx = 0.0;
        this.add(backgroundPanel, constraints);           
        
        /* Setup mode panel */
        saveToFile_ = new JButton("Save last line scan to file");
        saveToFile_.addActionListener(this);
        saveToFile_.setMinimumSize(new Dimension(150,1));   
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        setupModePanel.add(saveToFile_, constraints);

        continuousScan_ = new JButton("Continuous line scan");
        continuousScan_.addActionListener(this);
        continuousScan_.setMinimumSize(new Dimension(150,1));  
        continuousScan_.setSelected(true);
        continuousScan_.setEnabled(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        setupModePanel.add(continuousScan_, constraints);

        singleScan_ = new JButton("Single line scan");
        singleScan_.addActionListener(this);
        singleScan_.setMinimumSize(new Dimension(150,1));   
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        setupModePanel.add(singleScan_, constraints);
        
        /* Pinhole panel */
        defaultPinholeCentre_ = new JTextField("0");
        defaultPinholeCentre_.setEditable(true);
        defaultPinholeCentre_.addActionListener(this);
        defaultPinholeCentre_.setMinimumSize(new Dimension(75,1));   
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        pinholeCentrePanel.add(defaultPinholeCentre_, constraints);

        setCentre_ = new JButton("Set centre");
        setCentre_.addActionListener(this);
        setCentre_.setMinimumSize(new Dimension(200,1));   
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        pinholeCentrePanel.add(setCentre_, constraints);

        autoSetCentre_ = new JButton("Auto set centre");
        autoSetCentre_.addActionListener(this);
        autoSetCentre_.setMinimumSize(new Dimension(200,1));   
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        pinholeCentrePanel.add(autoSetCentre_, constraints);  
   
        /* Background panel */
        JLabel currentBackgroundLabel = new JLabel("Current");
        currentBackgroundLabel.setMinimumSize(new Dimension(50,1));
        currentBackgroundLabel.setHorizontalAlignment(JLabel.RIGHT);      
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.NONE;
        backgroundPanel.add(currentBackgroundLabel, constraints);
        
        currentBackground_ = new JTextField("0");
        currentBackground_.setEditable(true);
        currentBackground_.addActionListener(this);
        currentBackground_.setMinimumSize(new Dimension(75,1));         
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        backgroundPanel.add(currentBackground_, constraints);        

        JLabel defaultBackgroundLabel = new JLabel("Default");
        defaultBackgroundLabel.setMinimumSize(new Dimension(75,1));
        defaultBackgroundLabel.setHorizontalAlignment(JLabel.RIGHT);      
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.NONE;
        backgroundPanel.add(defaultBackgroundLabel, constraints);     
        
        defaultBackground_ = new JTextField("0");
        defaultBackground_.setEditable(true);
        defaultBackground_.addActionListener(this);
        defaultBackground_.setMinimumSize(new Dimension(75,1));  
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        backgroundPanel.add(defaultBackground_, constraints);   
        
        setBackground_ = new JButton("Set background");
        setBackground_.addActionListener(this);
        setBackground_.setMinimumSize(new Dimension(75,1));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        backgroundPanel.add(setBackground_, constraints);           
        
        setToZero_ = new JButton("Set to zero");
        setToZero_.addActionListener(this);        
        setToZero_.setMinimumSize(new Dimension(75,1));
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridwidth = 2;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        backgroundPanel.add(setToZero_, constraints);          
        
        setToAverage_ = new JButton("Measure average background and set");
        setToAverage_.addActionListener(this);            
        setToAverage_.setMinimumSize(new Dimension(75,1));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridwidth = 4;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        backgroundPanel.add(setToAverage_, constraints);             

        pack();        
    }    


    public void updateValues(boolean updateCurrentObjective)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();

        try
        {
            if (continuousScan_.isSelected())
            {
                /* Update at regular intervals */
                double averageTotal = 0;
                int maxLevel = 0;
                int maxLevelLocation = 0;
                for (int i = 0; i < 6; i++)
                {
                    String values = core.getProperty(pf, PureFocus.LINE_DATA + Integer.toString(i + 1));

                    for (int j = 0; j < 250; j++ )
                    {
                        if (values.length() < ((j + 1) * 3))
                        {
                            graphData_.updateByIndex((i * 250) + j, 0.0);
                        }
                        else
                        {
                            String value = values.substring((j * 3), (j * 3) + 3);
                            int valueInt = Integer.parseInt(value, 16);
                            graphData_.updateByIndex((i * 250) + j, (double)valueInt);

                            /* Run total for average */
                            averageTotal += (double)valueInt;  

                            /* Look for maximum */
                            if (valueInt > maxLevel)
                            {
                                maxLevel = valueInt;
                                maxLevelLocation = (i * 250) + j;
                            }
                        }
                    }                    
                }                    

                /* Store average and maximum */
                averageLevel_ = (int)((averageTotal * (1.0 / 1500.0)) + 0.5);
                pinholeCentreLocation_ = maxLevelLocation;      
            }
        }
		catch (Exception ex)
		{
			gui_.logs().showError(ex.getMessage());
		}  
        
        String coordinates = "(" + Integer.toString((int)graphPanel_.getXCrosshair()) + ", " + Integer.toString((int)graphPanel_.getYCrosshair()) + ")";
        crosshairs_.setText(coordinates);
        
        if (updateCurrentObjective)
        {
            try
            {
                String background = core.getProperty(pf, PureFocus.CURRENT_PREFIX + PureFocus.BACKGROUND_A);
                defaultBackground_.setText(background);
                
                String centre = core.getProperty(pf, PureFocus.CURRENT_PREFIX + PureFocus.PINHOLE_CENTRE);
                defaultPinholeCentre_.setText(centre);
            }
            catch (Exception ex)
            {
                gui_.logs().showError(ex.getMessage());
            }             
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();            
        Object source = e.getSource();
        String propertyName = e.getActionCommand();
        
        try
        {
            if (source.getClass() == JButton.class)
            {
                if (source == continuousScan_)
                {
                    /* Toggle between single and continuous */
                    singleScan_.setSelected(false);
                    continuousScan_.setSelected(true);
                    continuousScan_.setEnabled(false);
                }
                else if (source == singleScan_)
                {
                    /* If continuous was selected, move back to single */
                    singleScan_.setSelected(true);
                    continuousScan_.setSelected(false);
                    continuousScan_.setEnabled(true);
                    
                    /* Run a single line scan on command */
                    double averageTotal = 0;
                    int maxLevel = 0;
                    int maxLevelLocation = 0;
                    for (int i = 0; i < 6; i++)
                    {
                        String values = core.getProperty(pf, PureFocus.LINE_DATA + Integer.toString(i + 1));

                        for (int j = 0; j < 250; j++ )
                        {
                            if (values.length() < ((j + 1) * 3))
                            {
                                graphData_.updateByIndex((i * 250) + j, 0.0);
                            }
                            else
                            {
                                String value = values.substring((j * 3), (j * 3) + 3);
                                int valueInt = Integer.parseInt(value, 16);
                                graphData_.updateByIndex((i * 250) + j, (double)valueInt);
                                
                                /* Run total for average */
                                averageTotal += (double)valueInt;  
                                
                                /* Look for maximum */
                                if (valueInt > maxLevel)
                                {
                                    maxLevel = valueInt;
                                    maxLevelLocation = (i * 250) + j;
                                }
                            }
                        }                    
                    }                    
                    
                    /* Store average and maximum */
                    averageLevel_ = (int)((averageTotal * (1.0 / 1500.0)) + 0.5);
                    pinholeCentreLocation_ = maxLevelLocation;                 
                }
                else if (source == saveToFile_)
                {
                    JFileChooser fc = new JFileChooser();
                    int result = fc.showSaveDialog(parent_);
                    if (result == JFileChooser.APPROVE_OPTION)
                    {
                        /* Save data */
                        File file = fc.getSelectedFile();
                        BufferedOutputStream output = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
                        
                        for (int i = 0; i < graphData_.getItemCount(); i ++)
                        {
                            Number valueNum = graphData_.getY(i);
                            int value = valueNum.intValue();
                            String outputText = String.valueOf(value) + "\n";
                            output.write(outputText.getBytes());
                        }
                        
                        output.close();
                   }
                }
                else if (source == setCentre_)
                {
                    /* Centre value comes from being typed in */
                    String centreValue = defaultPinholeCentre_.getText();
                    Float centreValueFloat = Float.valueOf(centreValue);
                    centreValue = String.valueOf(centreValueFloat);
                    
                    Object[] options = {"Yes", "No"};
                    int check = JOptionPane.showOptionDialog(this,
                        "Do you wish to globally apply this now?",
                        "PureFocus",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                    
                    if (check == 0)
                    {
                        /* Apply pinhole centre to all objectives */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE);
                        
                        for (int i = 1; i <= 6; i++)
                        {
                            core.setProperty(pf, PureFocus.OBJECTIVE, i);
                            core.setProperty(pf, PureFocus.OBJECTIVE_PREFIX + String.valueOf(i) + "-" + PureFocus.PINHOLE_CENTRE, centreValue);
                        }
                        
                        core.setProperty(pf, PureFocus.OBJECTIVE, currentObjective);            
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, true, false);
                    }
                    else
                    {
                        /* Apply pinhole centre only to current objective */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE);   
                        core.setProperty(pf, PureFocus.OBJECTIVE_PREFIX + currentObjective + "-" + PureFocus.PINHOLE_CENTRE, centreValue);
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, false, false);
                    }
                }
                else if (source == autoSetCentre_)
                {
                    /* Centre value comes from detecting peak within data */
                    Float centreValueFloat = (float)pinholeCentreLocation_;
                    String centreValue = String.valueOf(centreValueFloat);                    
                    
                    Object[] options = {"Yes", "No"};
                    int check = JOptionPane.showOptionDialog(this,
                        "Do you wish to globally apply this now?",
                        "PureFocus",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                    
                    if (check == 0)
                    {
                        /* Apply pinhole centre to all objectives */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE);
                        
                        for (int i = 1; i <= 6; i++)
                        {
                            core.setProperty(pf, PureFocus.OBJECTIVE, i);
                            core.setProperty(pf, PureFocus.OBJECTIVE_PREFIX + String.valueOf(i) + "-" + PureFocus.PINHOLE_CENTRE, centreValue);
                        }
                        
                        core.setProperty(pf, PureFocus.OBJECTIVE, currentObjective);    
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, true, false);
                    }
                    else
                    {
                        /* Apply pinhole centre only to current objective */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE);   
                        core.setProperty(pf, PureFocus.OBJECTIVE_PREFIX + currentObjective + "-" + PureFocus.PINHOLE_CENTRE, centreValue);
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, false, false);
                    }
                }
                else if (source == setBackground_)
                {
                    /* Get background value from text entered */
                    String backgroundValue = currentBackground_.getText();
                    Float backgroundValueFloat = Float.valueOf(backgroundValue);
                    backgroundValue = String.valueOf(backgroundValueFloat);                    
                    
                    Object[] options = {"Yes", "No"};
                    int check = JOptionPane.showOptionDialog(this,
                        "Do you also wish to apply this to all objectives and make it the default background?",
                        "PureFocus",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                    
                    if (check == 0)
                    {
                        /* Apply background to all objectives */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE);
                        
                        for (int i = 1; i <= 6; i++)
                        {
                            String prefix = PureFocus.OBJECTIVE_PREFIX + String.valueOf(i) + "-";
                            core.setProperty(pf, PureFocus.OBJECTIVE, i);
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_A, backgroundValue);
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_B, backgroundValue);
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_C, backgroundValue);
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_D, backgroundValue);
                        }
                        
                        core.setProperty(pf, PureFocus.OBJECTIVE, currentObjective);    
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, true, false);
                    }
                    else
                    {
                        /* Apply background only to current objective */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE);
                        String prefix = PureFocus.OBJECTIVE_PREFIX + currentObjective + "-";
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_A, backgroundValue);
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_B, backgroundValue);
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_C, backgroundValue);
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_D, backgroundValue);
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, false, false);
                    }
                }
                else if (source == setToZero_)
                {
                    /* Background is just set to zero */
                    Object[] options = {"Yes", "No"};
                    int check = JOptionPane.showOptionDialog(this,
                        "Do you also wish to apply this to all objectives and make it the default background?",
                        "PureFocus",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                    
                    if (check == 0)
                    {
                        /* Apply background to all objectives */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE);
                        
                        for (int i = 1; i <= 6; i++)
                        {
                            String prefix = PureFocus.OBJECTIVE_PREFIX + String.valueOf(i) + "-";
                            core.setProperty(pf, PureFocus.OBJECTIVE, i);
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_A, "0");
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_B, "0");
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_C, "0");
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_D, "0");
                        }
                        
                        core.setProperty(pf, PureFocus.OBJECTIVE, currentObjective);            
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, true, false);
                    }
                    else
                    {
                        /* Apply background only to current objective */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE); 
                        String prefix = PureFocus.OBJECTIVE_PREFIX + currentObjective + "-";
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_A, "0");
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_B, "0");
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_C, "0");
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_D, "0");
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, false, false);
                    }
                }
                else if (source == setToAverage_)
                {
                    /* Background is set to average of all graph points */
                    String backgroundValue = String.valueOf(averageLevel_);
                    
                    Object[] options = {"Yes", "No"};
                    int check = JOptionPane.showOptionDialog(this,
                        "Do you also wish to apply this to all objectives and make it the default background?",
                        "PureFocus",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                    
                    if (check == 0)
                    {
                        /* Apply background to all objectives */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE);
                        
                        for (int i = 1; i <= 6; i++)
                        {
                            String prefix = PureFocus.OBJECTIVE_PREFIX + String.valueOf(i) + "-";
                            core.setProperty(pf, PureFocus.OBJECTIVE, i);
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_A, backgroundValue);
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_B, backgroundValue);
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_C, backgroundValue);
                            core.setProperty(pf, prefix + PureFocus.BACKGROUND_D, backgroundValue);
                        }
                        
                        core.setProperty(pf, PureFocus.OBJECTIVE, currentObjective);            
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, true, false);
                    }
                    else
                    {
                        /* Apply background only to current objective */
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);
                        String currentObjective = core.getProperty(pf, PureFocus.OBJECTIVE);
                        String prefix = PureFocus.OBJECTIVE_PREFIX + currentObjective + "-";
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_A, backgroundValue);
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_B, backgroundValue);
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_C, backgroundValue);
                        core.setProperty(pf, prefix + PureFocus.BACKGROUND_D, backgroundValue);
                        core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                        
                        parent_.triggerUpdates(false, true, false, false);
                    }
                }
                else
                {
                    // Unknown so ignore it
                }
            }                  
            else
            {
                // Unknown so ignore it
            }
        }
		catch (Exception ex)
		{
            try
            {
                // Ensure we do not leave this set, because it will lock the GUI
                core.setProperty(pf, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
            }
            catch (Exception e2)
            {
                // Ignore failure
            }      
            
			gui_.logs().showError(ex.getMessage());
		}  
    }












}