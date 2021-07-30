/**
@file PureFocusSetup.java
@author Graham Bartlett
@copyright Prior Scientific Instruments Limited, 2021
@brief Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus unit

Micro-Manager plugin giving GUI control of the PF-850 setup and configuration.

Licensed under the BSD license.
*/

package org.micromanager.PureFocus;

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
    
    /** Reference to the plugin which owns this */
	private final PureFocus plugin_;
    
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

    
	/** Create dialog.  This is always instantiated when the main window is
     * created, and is shown or hidden as required.
	 * @param parent Base window
     * @param plugin PureFocus plugin
	 * @param gui MM script interface
   	 */
	public PureFocusSetupDialog(PureFocusFrame parent, PureFocus plugin, Studio gui)
	{
        // We want this to be a modeless dialog, to work with the rest of the GUI
        super(parent, "Prior PureFocus PF-850 Setup");
            
		parent_ = parent;
        plugin_ = plugin;
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


    public void updateValues(boolean allValues)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();

        try
        {
            if (continuousScan_.isSelected())
            {
                /* Update at regular intervals */
                for (int i = 0; i < 6; i++)
                {
                    String values = core.getProperty(pf, plugin_.LINE_DATA + Integer.toString(i + 1));

                    for (int j = 0; j < 250; j++ )
                    {
                        if (values.length() < ((j + 1) * 3))
                        {
                            graphData_.updateByIndex((i * 250) + j, (double)0.0);
                        }
                        else
                        {
                            String value = values.substring((j * 3), (j * 3) + 3);
                            int valueInt = Integer.parseInt(value, 16);
                            graphData_.updateByIndex((i * 250) + j, (double)valueInt);
                        }
                    }                    
                }
            }
        }
		catch (Exception ex)
		{
			gui_.logs().showError(ex.getMessage());
		}  
        
        String coordinates = "(" + Integer.toString((int)graphPanel_.getXCrosshair()) + ", " + Integer.toString((int)graphPanel_.getYCrosshair()) + ")";
        crosshairs_.setText(coordinates);
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
                    for (int i = 0; i < 6; i++)
                    {
                        String values = core.getProperty(pf, plugin_.LINE_DATA + Integer.toString(i + 1));

                        for (int j = 0; j < 250; j++ )
                        {
                            if (values.length() < ((j + 1) * 3))
                            {
                                graphData_.updateByIndex((i * 250) + j, (double)0.0);
                            }
                            else
                            {
                                String value = values.substring((j * 3), (j * 3) + 3);
                                int valueInt = Integer.parseInt(value, 16);
                                graphData_.updateByIndex((i * 250) + j, (double)valueInt);
                            }
                        }                    
                    }                    
                }
            }
        }
		catch (Exception ex)
		{
			gui_.logs().showError(ex.getMessage());
		}  
        
                /* Update at regular intervals */
    
        
        
        
    }












}