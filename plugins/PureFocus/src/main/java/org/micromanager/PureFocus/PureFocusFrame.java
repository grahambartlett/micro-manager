/**
@file PureFocusFrame.java
@author Graham Bartlett
@copyright Prior Scientific Instruments Limited, 2021
@brief Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus unit

Micro-Manager plugin giving GUI control of the PF-850 setup and configuration.

Licensed under the BSD license.
*/

package org.micromanager.PureFocus;

import java.util.Vector;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.GroupLayout;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.SwingConstants;

import mmcorej.CMMCore;
import mmcorej.StrVector;
import mmcorej.DeviceType;
import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.micromanager.Studio;
import org.micromanager.internal.utils.WindowPositioning;


/** Top-level GUI for the PureFocus plugin.
 * 
 * This is only instantiated by the plugin if a PureFocus PF-850 device exists.
 * The GUI can therefore assume that calls to the device adapter will work,
 * barring the normal errors of invalid values or loss of comms.
 */
@SuppressWarnings(value = {"serial", "static-access"})    
public class PureFocusFrame extends JFrame implements ActionListener, ChangeListener, ItemListener
{
    /** Reference to the MM/ImageJ GUI */
    private final Studio gui_;
    
    /** Reference to the MM core */
	private final CMMCore core_;
    
    /** Reference to the plugin which owns this */
	private final PureFocus plugin_;
    
    // Child dialogs
    PureFocusObjectiveSlotTableDialog objectiveSlotTableDialog_;
    PureFocusGlobalTableDialog globalTableDialog_;
    PureFocusSetupDialog setupDialog_;

    // Menu elements
    private JCheckBoxMenuItem showObjectiveSlotConfigTable_;
    private JCheckBoxMenuItem showGlobalConfigTable_;
    private JCheckBoxMenuItem showSetup_;
    private JMenuItem about_;
    
	// GUI elements
    private JButton objective1_;
    private JButton objective2_;
    private JButton objective3_;
    private JButton objective4_;
    private JButton objective5_;
    private JButton objective6_;
    
    private JCheckBox inFocus_;
    private JCheckBox sampleDetected_;
    private JCheckBox correctInterface_;
    private JCheckBox servoInLimit_;
    
    private JButton servoOff_;
    private JButton servoOn_;
    
    private JTextField calculationA_;
    private JTextField calculationB_;
    private JTextField calculationAPlusB_;
    private JTextField calculationAMinusB_;
    private JSlider errorSlider_;
    private JTextField error_;
    private JButton zeroTarget_;
    private JButton setTarget_;
    private JTextField target_;
    
    private JTextField zPosition_;
    private JButton zeroZ_;
    private JButton goHome_;
    private JButton liftToLoad_;
    private JTextField liftDistance_;
    private JButton stepUp_;
    private JButton stepDown_;
    private JTextField stepSize_;
    private JButton haltZ_;
    
    private JButton digipotFocus_;
    private JButton digipotOffset_;
    
    private JTextField offsetDefault_;
    private JTextField offsetCurrent_;
    private JButton offsetStepUp_;
    private JButton offsetStepDown_;
    private JTextField offsetStepSize_;
    
    /** Timer for periodic GUI updates */
    final private Timer timer_;
    
    /** Store whether we have shown an error from periodic GUI updates, so that
     * we only show one error when (for example) comms goes down, and do not
     * flood the user with popups.
     */
    private Boolean errorShown_;
    
    /** Set true when reading values back, to block change events */
    private boolean updateInProgress_;
    
    /** State of system, so that the GUI can be updated on change */
    private int objectiveSelected_;
    
    /** Where changes happen, all GUIs call triggerUpdates() to report that
     * new values need to be picked up.  At the timer tick, all GUIs then process
     * these as required.  This means we only need one place which knows how to
     * update all the GUI elements, instead of each event handler needing to know
     * which other GUI elements to trigger.
     */
    private boolean updateGlobals_;
    private boolean updateCurrentObjective_;
    private boolean updateAllObjectives_;
    private boolean updateSettings_;
    
    /** List of objective preset names, for use by the whole GUI.
     * Note that this needs to be a Vector because Swing needs a Vector to
     * initialise a JComboBox list.
     */
    final public Vector<String> objectivePresetNames;
    
    /** Scaling from floating-point error value to slider position */
    static private int ERROR_SLIDER_SCALING = 1000;

   
	/** Creates form.  Instantiated when plugin is opened.
	 * @param gui MM script interface
	 * @param plugin Parent plugin
	 */
	public PureFocusFrame(Studio gui, PureFocus plugin) 
	{
		gui_ = gui;
		core_ = gui.getCMMCore();
		plugin_ = plugin;

        errorShown_ = false;
        
        updateGlobals_ = false;
        updateCurrentObjective_ = false;
        updateAllObjectives_ = false;
        updateSettings_ = false;
        
        // Force update of objective selected, first time through
        objectiveSelected_ = 0;
        
        // Set up list of objective names available
        objectivePresetNames = new Vector<>();
        int i = 0;
        Boolean haveName = true;
        while (haveName)
        {
            try
            {
                core_.setProperty(plugin_.DEVICE_NAME, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                core_.setProperty(plugin_.DEVICE_NAME, plugin_.ARRAY_READ_INDEX, i);
                core_.setProperty(plugin_.DEVICE_NAME, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                String newName = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE_PRESET_NAMES);
                objectivePresetNames.add(newName);
                i++;
            }
            catch (Exception ex)
            {
                haveName = false;
            }
        }
        
		// Set up main window
        initComponents();

        super.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/org/micromanager/icons/microscope.gif")));
        
        objectiveSlotTableDialog_ = new PureFocusObjectiveSlotTableDialog(this, plugin_, gui_);
        objectiveSlotTableDialog_.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        objectiveSlotTableDialog_.setVisible(false);      
        objectiveSlotTableDialog_.addWindowListener(
            new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent windowEvent)
                {
                    showObjectiveSlotConfigTable_.setState(false);
                    objectiveSlotTableDialog_.setVisible(false);
                }
            });

        globalTableDialog_ = new PureFocusGlobalTableDialog(this, plugin_, gui_);
        globalTableDialog_.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        globalTableDialog_.setVisible(false);      
        globalTableDialog_.addWindowListener(
            new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent windowEvent)
                {
                    showGlobalConfigTable_.setState(false);
                    globalTableDialog_.setVisible(false);
                }
            });
        
        setupDialog_ = new PureFocusSetupDialog(this, plugin_, gui_);
        setupDialog_.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setupDialog_.setVisible(false);      
        setupDialog_.addWindowListener(
            new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent windowEvent)
                {
                    showSetup_.setState(false);
                    setupDialog_.setVisible(false);
                }
            });
                
        
        // Only display and update frame contents after everything else is ready
        updateInProgress_ = false;
		pack();
        triggerUpdates(true, true, true, true);
        updateValues();

        // Run timed updates of form.  This must be the last step after everything
        // else is ready.
        timer_ = new Timer(1000, this);
        timer_.setInitialDelay(1000);
        timer_.start();         
	}


    /** This method is called from within the constructor to initialize the form.
	*/
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents()
	{
        setTitle("Prior PureFocus PF-850");
		setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		final JFrame frame = this;
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{			
				frame.dispose();
			}
		});
        
        // Set up menus
        JMenuBar menuBar;
        JMenu menu;

        menuBar = new JMenuBar();
        
        menu = new JMenu("Setup");
        menu.setMnemonic('S');
        menuBar.add(menu);
        
        showSetup_ = new JCheckBoxMenuItem("Setup");
        showSetup_.setMnemonic('G');
        showSetup_.addItemListener(this);
        showSetup_.setSelected(false);
        menu.add(showSetup_);
        
        menu = new JMenu("Advanced");
        menu.setMnemonic('A');
        menuBar.add(menu);
        
        showObjectiveSlotConfigTable_ = new JCheckBoxMenuItem("Objective slot settings");
        showObjectiveSlotConfigTable_.setMnemonic('O');
        showObjectiveSlotConfigTable_.addItemListener(this);
        showObjectiveSlotConfigTable_.setSelected(false);
        menu.add(showObjectiveSlotConfigTable_);
        
        showGlobalConfigTable_ = new JCheckBoxMenuItem("Global settings");
        showGlobalConfigTable_.setMnemonic('G');
        showGlobalConfigTable_.addItemListener(this);
        showGlobalConfigTable_.setSelected(false);
        menu.add(showGlobalConfigTable_);
        
        menu = new JMenu("Help");
        menu.setMnemonic('H');
        menuBar.add(menu);
        
        about_ = new JMenuItem("About");
        about_.setMnemonic('A');
        about_.addActionListener(this);
        menu.add(about_);       

        setJMenuBar(menuBar);

        // Set up layout of panels
        GroupLayout topLayout = new GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(topLayout);
        
        JPanel objectivesPanel, measurementPanel, flagsPanel, servoPanel, positionPanel, digipotPanel, offsetPanel;
        objectivesPanel = new JPanel();
        measurementPanel = new JPanel();
        flagsPanel = new JPanel();
        servoPanel = new JPanel();
        positionPanel = new JPanel();
        digipotPanel = new JPanel();
        offsetPanel = new JPanel(); 

        topLayout.setHorizontalGroup(
            topLayout.createSequentialGroup()
                .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(objectivesPanel)
                    .addComponent(flagsPanel)
                    .addComponent(servoPanel))
                .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(measurementPanel)
                    .addGroup(topLayout.createSequentialGroup()
                        .addComponent(positionPanel)
                        .addComponent(digipotPanel)
                        .addComponent(offsetPanel))));
        
        topLayout.setVerticalGroup(
            topLayout.createSequentialGroup()
                .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(objectivesPanel)
                    .addComponent(measurementPanel))
                .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addGroup(topLayout.createSequentialGroup()
                        .addComponent(flagsPanel)
                        .addComponent(servoPanel))
                    .addComponent(positionPanel)
                    .addComponent(digipotPanel)
                    .addComponent(offsetPanel)));
        
        // Objectives panel
        objective1_ = new JButton("1:");
        objective1_.setHorizontalAlignment(SwingConstants.LEFT);
        objective1_.addActionListener(this);
        objective1_.setMinimumSize(new Dimension(250,1));
        
        objective2_ = new JButton("2:");
        objective2_.setHorizontalAlignment(SwingConstants.LEFT);
        objective2_.addActionListener(this);
        objective2_.setMinimumSize(new Dimension(250,1));
        
        objective3_ = new JButton("3:");
        objective3_.setHorizontalAlignment(SwingConstants.LEFT);
        objective3_.addActionListener(this);
        objective3_.setMinimumSize(new Dimension(250,1));
        
        objective4_ = new JButton("4:");
        objective4_.setHorizontalAlignment(SwingConstants.LEFT);
        objective4_.addActionListener(this);
        objective4_.setMinimumSize(new Dimension(250,1));      
        
        objective5_ = new JButton("5:");
        objective5_.setHorizontalAlignment(SwingConstants.LEFT);
        objective5_.addActionListener(this);
        objective5_.setMinimumSize(new Dimension(250,1));
        
        objective6_ = new JButton("6:");
        objective6_.setHorizontalAlignment(SwingConstants.LEFT);
        objective6_.addActionListener(this);
        objective6_.setMinimumSize(new Dimension(250,1));
 
        objectivesPanel.setBorder(BorderFactory.createTitledBorder("Objectives"));        
        GroupLayout objectivesPanelLayout = new GroupLayout(objectivesPanel);
        objectivesPanel.setLayout(objectivesPanelLayout);
        
        objectivesPanelLayout.setHorizontalGroup(
            objectivesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                .addComponent(objective1_)
                .addComponent(objective2_)
                .addComponent(objective3_)
                .addComponent(objective4_)
                .addComponent(objective5_)
                .addComponent(objective6_));
        
        objectivesPanelLayout.setVerticalGroup(
            objectivesPanelLayout.createSequentialGroup()
                .addComponent(objective1_)
                .addComponent(objective2_)
                .addComponent(objective3_)
                .addComponent(objective4_)
                .addComponent(objective5_)
                .addComponent(objective6_));       
     
        // Flags panel
        JLabel inFocusLabel = new JLabel("In focus?");
        inFocus_ = new JCheckBox();
        inFocus_.setEnabled(false);
        
        JLabel sampleDetectedLabel = new JLabel("Sample detected?");
        sampleDetected_ = new JCheckBox();
        sampleDetected_.setEnabled(false);
        
        JLabel correctInterfaceLabel = new JLabel("Correct interface?");
        correctInterface_ = new JCheckBox();
        correctInterface_.setEnabled(false);
        
        JLabel servoInLimitLabel = new JLabel("Servo in limit?");       
        servoInLimit_ = new JCheckBox();
        servoInLimit_.setEnabled(false);

        flagsPanel.setBorder(BorderFactory.createTitledBorder("Flags"));        
        GroupLayout flagsPanelLayout = new GroupLayout(flagsPanel);
        flagsPanel.setLayout(flagsPanelLayout);
        
        flagsPanelLayout.setHorizontalGroup(
            flagsPanelLayout.createSequentialGroup()
                .addGroup(flagsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(inFocusLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sampleDetectedLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(correctInterfaceLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(servoInLimitLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(flagsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(inFocus_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sampleDetected_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(correctInterface_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(servoInLimit_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));      

        flagsPanelLayout.setVerticalGroup(
            flagsPanelLayout.createSequentialGroup()
                .addGroup(flagsPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(inFocusLabel)
                    .addComponent(inFocus_)) 
                .addGroup(flagsPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(sampleDetectedLabel)
                    .addComponent(sampleDetected_)) 
                .addGroup(flagsPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(correctInterfaceLabel)
                    .addComponent(correctInterface_)) 
                .addGroup(flagsPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(servoInLimitLabel)
                    .addComponent(servoInLimit_)));
        
        // Servo panel        
        servoOff_ = new JButton("Servo off");
        servoOff_.addActionListener(this);
        
        servoOn_ = new JButton("Servo on");
        servoOn_.addActionListener(this);

        servoPanel.setBorder(BorderFactory.createTitledBorder("Servo"));
        GroupLayout servoPanelLayout = new GroupLayout(servoPanel);
        servoPanel.setLayout(servoPanelLayout);
        
        servoPanelLayout.setHorizontalGroup(
            servoPanelLayout.createSequentialGroup()
                .addComponent(servoOff_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(servoOn_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));      

        servoPanelLayout.setVerticalGroup(
            servoPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                .addComponent(servoOff_)
                .addComponent(servoOn_));
        
        // Measurement panel
        JLabel aLabel = new JLabel("A");       
        calculationA_ = new JTextField();
        calculationA_.setMinimumSize(new Dimension(75,1));
        calculationA_.setEditable(false);
        
        JLabel bLabel = new JLabel("B");       
        calculationB_ = new JTextField();
        calculationB_.setMinimumSize(new Dimension(75,1));
        calculationB_.setEditable(false);
        
        JLabel aPlusBLabel = new JLabel("A + B");        
        calculationAPlusB_ = new JTextField();
        calculationAPlusB_.setMinimumSize(new Dimension(75,1));
        calculationAPlusB_.setEditable(false);
        
        JLabel aMinusBLabel = new JLabel("A - B");       
        calculationAMinusB_ = new JTextField();
        calculationAMinusB_.setMinimumSize(new Dimension(75,1));
        calculationAMinusB_.setEditable(false);
        
        JLabel errorLabel = new JLabel("Error");        
        error_ = new JTextField();
        error_.setMinimumSize(new Dimension(75,1));
        error_.setEditable(false);      
        errorSlider_ = new JSlider();
        errorSlider_.setMinimumSize(new Dimension(150,1));
        errorSlider_.setEnabled(false);
        errorSlider_.setMinimum(-ERROR_SLIDER_SCALING);
        errorSlider_.setMaximum(ERROR_SLIDER_SCALING);
        
        JLabel targetLabel = new JLabel("Target");       
        target_ = new JTextField();
        target_.setMinimumSize(new Dimension(75,1));
        target_.setEditable(false);
        
        measurementPanel.setBorder(BorderFactory.createTitledBorder("Measurement"));
        GroupLayout measurementPanelLayout = new GroupLayout(measurementPanel);
        measurementPanel.setLayout(measurementPanelLayout);        
        
        measurementPanelLayout.setHorizontalGroup(
            measurementPanelLayout.createSequentialGroup()
                .addGroup(measurementPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(aLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(bLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(aPlusBLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(aMinusBLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)    
                    .addComponent(errorLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(targetLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(measurementPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(calculationA_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(calculationB_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(calculationAPlusB_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(calculationAMinusB_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(error_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(target_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addComponent(errorSlider_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
            
        measurementPanelLayout.setVerticalGroup(
            measurementPanelLayout.createSequentialGroup()
                .addGroup(measurementPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(aLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(calculationA_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addGroup(measurementPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(bLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(calculationB_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addGroup(measurementPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(aPlusBLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(calculationAPlusB_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addGroup(measurementPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(aMinusBLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(calculationAMinusB_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addGroup(measurementPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(errorLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(error_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(errorSlider_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addGroup(measurementPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(targetLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(target_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)));       
        
        // Position panel
        zeroZ_ = new JButton("Zero Z");
        zeroZ_.addActionListener(this);
        zeroZ_.setMinimumSize(new Dimension(50,1));
        JLabel zPositionLabel = new JLabel("Z position (microns)");        
        zPosition_ = new JTextField();
        zPosition_.setMinimumSize(new Dimension(75,1));  
        
        goHome_ = new JButton("Go to home");
        goHome_.addActionListener(this);
        goHome_.setMinimumSize(new Dimension(50,1));
        
        liftToLoad_ = new JButton("Lift to load");
        liftToLoad_.addActionListener(this);
        liftToLoad_.setMinimumSize(new Dimension(50,1));
        JLabel liftDistanceLabel = new JLabel("Lift distance (microns)");        
        liftDistance_ = new JTextField("100.0");
        liftDistance_.addActionListener(this);
        liftDistance_.setMinimumSize(new Dimension(75,1));    
        
        stepUp_ = new JButton("Step up");
        stepUp_.addActionListener(this);
        stepUp_.setMinimumSize(new Dimension(50,1));
        JLabel stepSizeLabel = new JLabel("Step size (microns)");       
        stepSize_ = new JTextField();
        stepSize_.addActionListener(this);
        stepSize_.setMinimumSize(new Dimension(75,1));      
        
        stepDown_ = new JButton("Step down");
        stepDown_.addActionListener(this);
        stepDown_.setMinimumSize(new Dimension(50,1));
        
        haltZ_ = new JButton("Halt");
        haltZ_.addActionListener(this);
        haltZ_.setMinimumSize(new Dimension(50,1));

        positionPanel.setBorder(BorderFactory.createTitledBorder("Z position"));
        GroupLayout positionPanelLayout = new GroupLayout(positionPanel);
        positionPanel.setLayout(positionPanelLayout);        
        
        positionPanelLayout.setHorizontalGroup(
            positionPanelLayout.createSequentialGroup()
                .addGroup(positionPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(zeroZ_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(goHome_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(liftToLoad_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stepUp_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)    
                    .addComponent(stepDown_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE) 
                    .addComponent(haltZ_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(positionPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(zPositionLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(liftDistanceLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stepSizeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(positionPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(zPosition_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(liftDistance_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stepSize_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
            
        positionPanelLayout.setVerticalGroup(
            positionPanelLayout.createSequentialGroup()
                .addGroup(positionPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(zeroZ_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(zPositionLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(zPosition_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addComponent(goHome_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGroup(positionPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(liftToLoad_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(liftDistanceLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(liftDistance_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addGroup(positionPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(stepUp_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(stepSizeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(stepSize_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addComponent(stepDown_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(haltZ_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
        
        // Digipot panel
        digipotFocus_ = new JButton("Digipot focus");
        digipotFocus_.addActionListener(this);
        digipotFocus_.setMinimumSize(new Dimension(50,1));

        digipotOffset_ = new JButton("Digipot offset");
        digipotOffset_.addActionListener(this);
        digipotOffset_.setMinimumSize(new Dimension(50,1));
        
        digipotPanel.setBorder(BorderFactory.createTitledBorder("Digipot"));
        GroupLayout digipotPanelLayout = new GroupLayout(digipotPanel);
        digipotPanel.setLayout(digipotPanelLayout);        
        
        digipotPanelLayout.setHorizontalGroup(
            digipotPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true) 
                .addComponent(digipotFocus_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE) 
                .addComponent(digipotOffset_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
            
        digipotPanelLayout.setVerticalGroup(
            digipotPanelLayout.createSequentialGroup()
                .addComponent(digipotFocus_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(digipotOffset_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
        
        // Offset panel
        JLabel offsetDefaultLabel = new JLabel("Default offset (microns)");
        offsetDefault_ = new JTextField();
        offsetDefault_.setMinimumSize(new Dimension(75,1));
        
        offsetStepUp_ = new JButton("Step up");
        offsetStepUp_.addActionListener(this);
        offsetStepUp_.setMinimumSize(new Dimension(50,1));
        JLabel offsetCurrentLabel = new JLabel("Current offset (microns)");
        offsetCurrent_ = new JTextField();
        offsetCurrent_.setMinimumSize(new Dimension(75,1));

        offsetStepDown_ = new JButton("Step down");
        offsetStepDown_.addActionListener(this);
        offsetStepDown_.setMinimumSize(new Dimension(50,1));
        JLabel offsetStepSize_Label = new JLabel("Step (microns)");       
        offsetStepSize_ = new JTextField();
        offsetStepSize_.addActionListener(this);
        offsetStepSize_.setMinimumSize(new Dimension(75,1));
        
        offsetPanel.setBorder(BorderFactory.createTitledBorder("Offset"));
        GroupLayout offsetPanelLayout = new GroupLayout(offsetPanel);
        offsetPanel.setLayout(offsetPanelLayout);        
        
        offsetPanelLayout.setHorizontalGroup(
            offsetPanelLayout.createSequentialGroup()
                .addGroup(offsetPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(offsetStepUp_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(offsetStepDown_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(offsetPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(offsetDefaultLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(offsetCurrentLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(offsetStepSize_Label, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(offsetPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
                    .addComponent(offsetDefault_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(offsetCurrent_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(offsetStepSize_, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
            
        offsetPanelLayout.setVerticalGroup(
            offsetPanelLayout.createSequentialGroup()
                .addGroup(offsetPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(offsetDefaultLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(offsetDefault_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addGroup(offsetPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(offsetStepUp_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(offsetCurrentLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(offsetCurrent_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)) 
                .addGroup(offsetPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
                    .addComponent(offsetStepDown_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(offsetStepSize_Label, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(offsetStepSize_, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)));     
    }
    

    @Override
    public void dispose()
    {
        // The timer will be deleted automatically when the frame is collected,
        // but in the meantime it will still be alive and still produce events
        // on the closed window (and potentially without the device existing).
        // We must stop the timer to prevent this happening.
        timer_.stop();
        
        // Tell the plugin that it has no frame attached
        plugin_.tellFrameClosed();
        
        // Close the window
        setVisible(false);
        super.dispose();
    }

    
    public void updateValues()
	{
        Boolean errored = false;
        String errorMessage = "";
        
        updateInProgress_ = true;
        
        // Pick up update flags for use, and clear them ready for next use
        boolean updateGlobals = updateGlobals_;
        updateGlobals_ = false;
        boolean updateCurrentObjective = updateCurrentObjective_;
        updateCurrentObjective_ = false;
        boolean updateAllObjectives = updateAllObjectives_;
        updateAllObjectives_ = false;
        boolean updateSettings = updateSettings_;
        updateGlobals_ = false;
        
        try
        {
            String val;
            int valueInt;
            
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE);
            valueInt = Integer.parseInt(val);
            boolean objectiveChanged = false;
            if (objectiveSelected_ != valueInt)
            {
                // Update objective selected
                objectiveSelected_ = valueInt;
                objectiveChanged = true;
                updateCurrentObjective = true;
                updateAllObjectives = true;
            
                // Check for objective changes, and update buttons at the same time
                switch (objectiveSelected_)
                {
                    case 1:
                        objective1_.setEnabled(false);
                        objective1_.setSelected(true);
                        objective2_.setEnabled(true);
                        objective2_.setSelected(false);                        
                        objective3_.setEnabled(true);
                        objective3_.setSelected(false);  
                        objective4_.setEnabled(true);
                        objective4_.setSelected(false);  
                        objective5_.setEnabled(true);
                        objective5_.setSelected(false);  
                        objective6_.setEnabled(true);
                        objective6_.setSelected(false);                          
                        break;
                    case 2:
                        objective1_.setEnabled(true);
                        objective1_.setSelected(false);
                        objective2_.setEnabled(false);
                        objective2_.setSelected(true);                        
                        objective3_.setEnabled(true);
                        objective3_.setSelected(false);  
                        objective4_.setEnabled(true);
                        objective4_.setSelected(false);  
                        objective5_.setEnabled(true);
                        objective5_.setSelected(false);  
                        objective6_.setEnabled(true);
                        objective6_.setSelected(false);                          
                        break;
                    case 3:
                        objective1_.setEnabled(true);
                        objective1_.setSelected(false);
                        objective2_.setEnabled(true);
                        objective2_.setSelected(false);                        
                        objective3_.setEnabled(false);
                        objective3_.setSelected(true);  
                        objective4_.setEnabled(true);
                        objective4_.setSelected(false);  
                        objective5_.setEnabled(true);
                        objective5_.setSelected(false);  
                        objective6_.setEnabled(true);
                        objective6_.setSelected(false);                          
                        break;
                    case 4:
                        objective1_.setEnabled(true);
                        objective1_.setSelected(false);
                        objective2_.setEnabled(true);
                        objective2_.setSelected(false);                        
                        objective3_.setEnabled(true);
                        objective3_.setSelected(false);  
                        objective4_.setEnabled(false);
                        objective4_.setSelected(true);  
                        objective5_.setEnabled(true);
                        objective5_.setSelected(false);  
                        objective6_.setEnabled(true);
                        objective6_.setSelected(false);                          
                        break;
                    case 5:
                        objective1_.setEnabled(true);
                        objective1_.setSelected(false);
                        objective2_.setEnabled(true);
                        objective2_.setSelected(false);                        
                        objective3_.setEnabled(true);
                        objective3_.setSelected(false);  
                        objective4_.setEnabled(true);
                        objective4_.setSelected(false);  
                        objective5_.setEnabled(false);
                        objective5_.setSelected(true);  
                        objective6_.setEnabled(true);
                        objective6_.setSelected(false);                          
                        break;
                    case 6:
                        objective1_.setEnabled(true);
                        objective1_.setSelected(false);
                        objective2_.setEnabled(true);
                        objective2_.setSelected(false);                        
                        objective3_.setEnabled(true);
                        objective3_.setSelected(false);  
                        objective4_.setEnabled(true);
                        objective4_.setSelected(false);  
                        objective5_.setEnabled(true);
                        objective5_.setSelected(false);  
                        objective6_.setEnabled(false);
                        objective6_.setSelected(true);                          
                        break;                        
                    default:
                        objective1_.setEnabled(false);
                        objective1_.setSelected(false);
                        objective2_.setEnabled(false);
                        objective2_.setSelected(false);                        
                        objective3_.setEnabled(false);
                        objective3_.setSelected(false);  
                        objective4_.setEnabled(false);
                        objective4_.setSelected(false);  
                        objective5_.setEnabled(false);
                        objective5_.setSelected(false);  
                        objective6_.setEnabled(false);
                        objective6_.setSelected(false);                          
                        break;
                }
            }            

            // Objective panel
            if (updateCurrentObjective || updateAllObjectives)
            {
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE_PREFIX + "1-" + plugin_.PRESET);
                objective1_.setText("1: " + val);
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE_PREFIX + "2-" + plugin_.PRESET);
                objective2_.setText("2: " + val);
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE_PREFIX + "3-" + plugin_.PRESET);
                objective3_.setText("3: " + val);  
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE_PREFIX + "4-" + plugin_.PRESET);
                objective4_.setText("4: " + val);
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE_PREFIX + "5-" + plugin_.PRESET);
                objective5_.setText("5: " + val);  
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE_PREFIX + "6-" + plugin_.PRESET);
                objective6_.setText("6: " + val);
            }
            
            if (updateSettings)
            {
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.LIFT_TO_LOAD_DISTANCE_MICRONS);
                liftDistance_.setText(val);
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_POSITION_STEP_MICRONS);
                stepSize_.setText(val);
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OFFSET_POSITION_STEP_MICRONS);
                offsetStepSize_.setText(val);                              
            }          
            
            // Flags panel
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_STATE);
            valueInt = Integer.parseInt(val);
            if ((valueInt != 0) != inFocus_.isSelected())
            {
                inFocus_.setSelected((valueInt != 0));
            }
            
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.IS_SAMPLE_PRESENT);
            valueInt = Integer.parseInt(val);
            if ((valueInt != 0) != servoInLimit_.isSelected())
            {
                sampleDetected_.setSelected((valueInt != 0));
            }              
            
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.IS_INTERFACE_CORRECT);
            valueInt = Integer.parseInt(val);
            if ((valueInt != 0) != servoInLimit_.isSelected())
            {
                correctInterface_.setSelected((valueInt != 0));
            }                

            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.SERVO_IN_LIMIT);
            valueInt = Integer.parseInt(val);
            if ((valueInt != 0) != servoInLimit_.isSelected())
            {
                servoInLimit_.setSelected((valueInt != 0));
            }            
            
            // Servo panel
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.SERVO_ON);
            valueInt = Integer.parseInt(val);
            if ((valueInt != 0) != servoOn_.isSelected())
            {
                if (valueInt != 0)
                {
                    servoOn_.setEnabled(false);
                    servoOn_.setSelected(true); 
                    servoOff_.setEnabled(true);
                    servoOff_.setSelected(false);                 
                }
                else
                {
                    servoOn_.setEnabled(true);
                    servoOn_.setSelected(false); 
                    servoOff_.setEnabled(false);
                    servoOff_.setSelected(true);
                }
                
                updateGlobals = true;
            }
            
            // Measurement panel
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.CALCULATION_ABCD);
            String[] splitValues = val.split(":");
            double calculationA = Double.valueOf(splitValues[0]);
            double calculationB = Double.valueOf(splitValues[1]);
            calculationA_.setText(Double.toString(calculationA));
            calculationB_.setText(Double.toString(calculationB));
            calculationAPlusB_.setText(Double.toString(calculationA + calculationB));
            calculationAMinusB_.setText(Double.toString(calculationA - calculationB));
            
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_ERROR);
            error_.setText(val);
            double error = Double.valueOf(val);
            if (error < -1.0)
            {
                errorSlider_.setValue(-ERROR_SLIDER_SCALING);
            }
            else if (error > 1.0)
            {
                errorSlider_.setValue(ERROR_SLIDER_SCALING);
            }
            else
            {
                errorSlider_.setValue((int)(error * (double)ERROR_SLIDER_SCALING));
            }
           
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_TARGET);
            target_.setText(val);
            
            // Position panel
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_POSITION_MICRONS);
            zPosition_.setText(val);
            
            // Digipot panel
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.DIGIPOT_CONTROLS_OFFSET);
            valueInt = Integer.parseInt(val);
            if ((valueInt != 0) != digipotOffset_.isSelected())
            {
                if (valueInt != 0)
                {
                    digipotOffset_.setEnabled(false);
                    digipotOffset_.setSelected(true); 
                    digipotFocus_.setEnabled(true);
                    digipotFocus_.setSelected(false);                 
                }
                else
                {
                    digipotOffset_.setEnabled(true);
                    digipotOffset_.setSelected(false); 
                    digipotFocus_.setEnabled(false);
                    digipotFocus_.setSelected(true);
                }
                
                updateGlobals = true;
            }
            
            // Offset panel
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE_PREFIX + Integer.toString(objectiveSelected_) + "-" + plugin_.LENS_OFFSET + "0");
            offsetDefault_.setText(val);
            
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OFFSET_POSITION_MICRONS);
            offsetCurrent_.setText(val);
            
            /* Update child windows as required */
            if (updateGlobals)
            {
                globalTableDialog_.updateValues();
            }
            
            objectiveSlotTableDialog_.updateValues(updateCurrentObjective, updateAllObjectives, objectiveSelected_);
            
            setupDialog_.updateValues(updateCurrentObjective || updateAllObjectives);
        }
        catch (Exception ex)
        {
            errored = true;
            errorMessage = ex.getMessage();
        }

        if (errored)
        {
            if (!errorMessage.startsWith("No device with label"))
            {
                // This happens if the plugin is open and the user opens the
                // hardware configuration wizard.  We still want the plugin to
                // stay open (at least unless the user removes the device from
                // the configuration, which is handled separately), but at this
                // point it doesn't have a device attached.  We must ignore
                // these errors.
                if (!errorShown_)
                {
                    // Only show an error once, otherwise we will get a popup
                    // every timer update.  Subsequent errors will not be reported.
                    // When we get a run through without an error (i.e. the fault
                    // has gone away, perhaps by reconnecting the port) then
                    // the flag is cleared and again we will report the next
                    // error.
                    gui_.logs().showError(errorMessage);
                }
            }
        }

        errorShown_ = errored;
        
        updateInProgress_ = false;
    }


	/** @return Name of currently-selected PureFocus */
    public String getPureFocus()
    {
        return plugin_.DEVICE_NAME;
    }
    
    
    @Override
    public void stateChanged(ChangeEvent e)
	{
        String pf = getPureFocus();            
        Object source = e.getSource();
        
        if (!updateInProgress_)
        {           
            // Ignore events during update
            try
            {
    
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
        String pf = getPureFocus();         
        Object source = e.getSource();
        String propertyName = e.getActionCommand();

        if (updateInProgress_)
        {
            // Ignore events during update
        }
        else if (source.getClass() == Timer.class)
        {
            updateValues();
        }     
        else
        {
            if (source.getClass() == JButton.class)
            {
                try
                {
                    if (source == objective1_)
                    {
                        // The call to updateValues() will update the objective
                        // buttons and dialogs for this change
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.OBJECTIVE, 1);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, true, true, true);
                    }
                    else if (source == objective2_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.OBJECTIVE, 2);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, true, true, true);
                    }
                    else if (source == objective3_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.OBJECTIVE, 3);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, true, true, true);
                    }
                    else if (source == objective4_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.OBJECTIVE, 4);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, true, true, true);
                    }
                    else if (source == objective5_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.OBJECTIVE, 5);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, true, true, true);
                    }
                    else if (source == objective6_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.OBJECTIVE, 6);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, true, true, true);
                    }
                    else if (source == servoOn_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.SERVO_ON, 1);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, false, false, false);
                    }                    
                    else if (source == servoOff_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.SERVO_ON, 0);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, false, false, false);
                    }                     
                    else if (source == digipotFocus_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.DIGIPOT_CONTROLS_OFFSET, 0);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, false, false, false);
                    }                    
                    else if (source == digipotOffset_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.DIGIPOT_CONTROLS_OFFSET, 1);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(true, false, false, false);
                    }     
                    else if (source == offsetStepUp_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.EXECUTE_COMMAND, plugin_.EXECUTE_COMMAND_OFFSET_STEP_UP);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(false, false, false, true);
                    }
                    else if (source == offsetStepDown_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.EXECUTE_COMMAND, plugin_.EXECUTE_COMMAND_OFFSET_STEP_DOWN);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(false, false, false, true);
                    }
                    else if (source == zeroZ_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.FOCUS_POSITION_MICRONS, 0);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(false, false, false, true);
                    }
                    else if (source == goHome_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.EXECUTE_COMMAND, plugin_.EXECUTE_COMMAND_Z_GO_HOME);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(false, false, false, true);
                    }
                    else if (source == liftToLoad_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.EXECUTE_COMMAND, plugin_.EXECUTE_COMMAND_Z_LIFT_TO_LOAD);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(false, false, false, true);
                    }
                    else if (source == stepUp_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.EXECUTE_COMMAND, plugin_.EXECUTE_COMMAND_Z_STEP_UP);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(false, false, false, true);
                    }
                    else if (source == stepDown_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.EXECUTE_COMMAND, plugin_.EXECUTE_COMMAND_Z_STEP_DOWN);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(false, false, false, true);
                    }
                    else if (source == haltZ_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.EXECUTE_COMMAND, plugin_.EXECUTE_COMMAND_Z_EMERGENCY_STOP);
                        core_.setProperty(pf, plugin_.SERVO_ON, 0);
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        triggerUpdates(false, false, false, true);
                    }
                    else
                    {
                        // Ignore unknown controls
                    }
                }
                catch (Exception ex)
                {
                    try
                    {
                        // Ensure we do not leave this set, because it will lock the GUI
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                    }
                    catch (Exception e2)
                    {
                        // Ignore failure
                    }

                    gui_.logs().showError(ex.getMessage());
                }
                
                updateValues();
            }
            else if (source.getClass() == JTextField.class)
            {
                JTextField widget = (JTextField)source;
                try
                {
                    if (widget == liftDistance_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.LIFT_TO_LOAD_DISTANCE_MICRONS, widget.getText());
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        widget.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.LIFT_TO_LOAD_DISTANCE_MICRONS));
                        triggerUpdates(false, false, false, true);
                    }
                    else if (widget == stepSize_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.FOCUS_POSITION_STEP_MICRONS, widget.getText());
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        widget.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_POSITION_STEP_MICRONS));
                        triggerUpdates(false, false, false, true);
                    }
                    else if (widget == offsetStepSize_)
                    {
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
                        core_.setProperty(pf, plugin_.OFFSET_POSITION_STEP_MICRONS, widget.getText());
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                        widget.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.OFFSET_POSITION_STEP_MICRONS));
                        triggerUpdates(false, false, false, true);
                    }
                    else
                    {
                        // Ignore unknown controls
                    }                    
                }
                catch (Exception ex)
                {
                    try
                    {
                        // Ensure we do not leave this set, because it will lock the GUI
                        core_.setProperty(pf, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                    }
                    catch (Exception e2)
                    {
                        // Ignore failure
                    }
                    
                    gui_.logs().showError(ex.getMessage());
                }       
            }
            else if (source.getClass() == JMenuItem.class)
            {
                if (source == about_)
                {
                    /** @todo This should be replaced with a more complete "About" dialog
                     * containing the PF-850 image, and complete license details.
                     */
                    JOptionPane.showMessageDialog(this,
                        "Prior PureFocus PF-850 configuration plugin\n"
                            + "Written by G Bartlett\n"
                            + "(c) Prior Scientific Instruments Ltd., 2021\n"
                            + "Distributed under the BSD license following Micro-Manager licensing\n\n"
                            + "For support, please post questions on http://forum.image.sc in\n"
                            + "category \"Development\", or email inquiries@prior.com",
                        "About",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            else
            {
                // Ignore
            }
        }
    }
    
    @Override
    public void itemStateChanged(ItemEvent e)
    {   
        String pf = getPureFocus();         
        Object source = e.getSource();        
        
        if (!updateInProgress_)
        {
            if (source.getClass() == JCheckBoxMenuItem.class)
            {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem)source;
                Boolean newState = item.isSelected();

                if (source == showObjectiveSlotConfigTable_)
                {
                    // Ensure dialog starts up with correct values and objective enabled.
                    try
                    {
                        objectiveSlotTableDialog_.updateValues(true, true, objectiveSelected_);    
                        objectiveSlotTableDialog_.setVisible(newState);
                    }
                    catch (Exception ex)
                    {
                        // Todo
                    }
                }
                else if (source == showGlobalConfigTable_)
                {
                    globalTableDialog_.updateValues();
                    globalTableDialog_.setVisible(newState);
                }
                else if (source == showSetup_)
                {
                    setupDialog_.updateValues(true);
                    setupDialog_.setVisible(newState);
                }                
                else
                {
                    // Unknown
                }     
            }
            else
            {
                // Ignore
            }      
        }
    }

    
    /** Trigger updates for GUI elements.
     * Called by all components of the plugin, so that we can have a consistent
     * way to propagate changes of values across the GUI.  Any value set true
     * indicates that those values need to be updated.
     * @param updateGlobals Update global settings values
     * @param updateCurrentObjective Update settings for the current objective
     * @param updateAllObjectives Update settings for all objectives
     * @param updateSettings Update volatile settings values
     */
    public void triggerUpdates(boolean updateGlobals, 
        boolean updateCurrentObjective,
        boolean updateAllObjectives,
        boolean updateSettings)
    {
        if (updateGlobals)
        {
            updateGlobals_ = true;
        }

        if (updateCurrentObjective)
        {
            updateCurrentObjective_ = true;
        }
        
        if (updateAllObjectives)
        {
            updateAllObjectives_ = true;
        }
        
        if (updateSettings)
        {
            updateSettings_ = true;
        }
    }
}
